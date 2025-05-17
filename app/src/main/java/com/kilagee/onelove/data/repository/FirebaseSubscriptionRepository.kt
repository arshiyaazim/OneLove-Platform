package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.kilagee.onelove.data.database.dao.SubscriptionDao
import com.kilagee.onelove.data.model.PaymentProvider
import com.kilagee.onelove.data.model.Subscription
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.PaymentRepository
import com.kilagee.onelove.domain.repository.SubscriptionPlan
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import com.kilagee.onelove.domain.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseSubscriptionRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val subscriptionDao: SubscriptionDao,
    private val userRepository: UserRepository,
    private val paymentRepository: PaymentRepository
) : SubscriptionRepository {

    private val subscriptionsCollection = firestore.collection("subscriptions")
    private val plansCollection = firestore.collection("subscription_plans")
    
    override fun getSubscriptionPlans(): Flow<Resource<List<SubscriptionPlan>>> = flow {
        emit(Resource.Loading)
        
        try {
            // Get plans from Firestore
            val plansSnapshot = plansCollection
                .orderBy("priceUsd")
                .get()
                .await()
                
            val plans = plansSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                try {
                    val type = SubscriptionType.valueOf(data["type"] as? String ?: "BASIC")
                    val priceUsd = (data["priceUsd"] as? Number)?.toDouble() ?: 0.0
                    val name = data["name"] as? String ?: ""
                    val description = data["description"] as? String ?: ""
                    val features = data["features"] as? List<String> ?: listOf()
                    val durationMonths = (data["durationMonths"] as? Number)?.toInt() ?: 1
                    val popular = data["popular"] as? Boolean ?: false
                    
                    SubscriptionPlan(
                        type = type,
                        name = name,
                        description = description,
                        priceUsd = priceUsd,
                        features = features,
                        durationMonths = durationMonths,
                        popular = popular
                    )
                } catch (e: Exception) {
                    null
                }
            }
            
            // If no plans from Firestore, provide defaults
            if (plans.isEmpty()) {
                val defaultPlans = createDefaultPlans()
                emit(Resource.success(defaultPlans))
            } else {
                emit(Resource.success(plans))
            }
        } catch (e: Exception) {
            // If error, provide defaults
            val defaultPlans = createDefaultPlans()
            emit(Resource.success(defaultPlans))
        }
    }
    
    private fun createDefaultPlans(): List<SubscriptionPlan> {
        return listOf(
            SubscriptionPlan(
                type = SubscriptionType.BASIC,
                name = "Basic",
                description = "Free access with limited features",
                priceUsd = 0.0,
                features = listOf(
                    "Limited swipes per day",
                    "Basic matching",
                    "Text chat only",
                    "Limited offers"
                ),
                durationMonths = 0 // Forever free
            ),
            SubscriptionPlan(
                type = SubscriptionType.BOOST,
                name = "Boost",
                description = "Get more visibility and matches",
                priceUsd = 9.99,
                features = listOf(
                    "Profile boosting",
                    "More daily swipes",
                    "Text chat only",
                    "See who liked you",
                    "Unlimited offers"
                ),
                durationMonths = 1
            ),
            SubscriptionPlan(
                type = SubscriptionType.UNLIMITED,
                name = "Unlimited",
                description = "Unlock all core features",
                priceUsd = 19.99,
                features = listOf(
                    "Unlimited swipes",
                    "Profile boosting",
                    "Audio & video calls",
                    "See who visits your profile",
                    "Unlimited offers",
                    "Priority matching"
                ),
                durationMonths = 1,
                popular = true
            ),
            SubscriptionPlan(
                type = SubscriptionType.PREMIUM,
                name = "Premium",
                description = "Complete access to all features",
                priceUsd = 29.99,
                features = listOf(
                    "All Unlimited features",
                    "Background verification",
                    "Ad-free experience",
                    "Premium badge",
                    "Exclusive events access",
                    "Priority support"
                ),
                durationMonths = 1
            )
        )
    }
    
    override fun createSubscription(
        type: SubscriptionType,
        paymentMethodId: String,
        autoRenew: Boolean
    ): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get subscription plan details
            val plansResource = getSubscriptionPlans().await()
            if (plansResource !is Resource.Success) {
                emit(Resource.error("Failed to get subscription plans"))
                return@flow
            }
            
            val plan = plansResource.data.find { it.type == type }
            if (plan == null) {
                emit(Resource.error("Invalid subscription type"))
                return@flow
            }
            
            // Basic plan is free, no payment needed
            if (type == SubscriptionType.BASIC) {
                val subscriptionId = UUID.randomUUID().toString()
                
                // Calculate end date (forever for basic)
                val startDate = Date()
                val calendar = Calendar.getInstance()
                calendar.time = startDate
                calendar.add(Calendar.YEAR, 100) // Basically forever
                val endDate = calendar.time
                
                val subscription = Subscription(
                    id = subscriptionId,
                    userId = currentUser.uid,
                    type = type,
                    status = SubscriptionStatus.ACTIVE,
                    paymentProvider = PaymentProvider.STRIPE,
                    providerSubscriptionId = null,
                    priceUsd = 0.0,
                    startDate = startDate,
                    currentPeriodEnd = endDate,
                    autoRenew = false,
                    canceledAt = null,
                    features = plan.features.joinToString(","),
                    createdAt = startDate,
                    updatedAt = startDate
                )
                
                // Save to Firestore
                subscriptionsCollection.document(subscriptionId).set(subscription).await()
                
                // Save to Room
                subscriptionDao.insertSubscription(subscription)
                
                // Update user's premium status
                updateUserPremiumStatus(currentUser.uid, true)
                
                emit(Resource.success(subscription))
                return@flow
            }
            
            // For paid subscriptions, create a subscription with Stripe
            val data = hashMapOf(
                "payment_method_id" to paymentMethodId,
                "subscription_type" to type.name,
                "price_usd" to plan.priceUsd,
                "auto_renew" to autoRenew,
                "plan_id" to type.name.lowercase(),
                "user_id" to currentUser.uid
            )
            
            val result = functions
                .getHttpsCallable("createSubscription")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val providerSubscriptionId = result?.get("id") as? String
            val status = result?.get("status") as? String
            
            if (providerSubscriptionId == null) {
                emit(Resource.error("Failed to create subscription"))
                return@flow
            }
            
            // Calculate end date based on plan duration
            val startDate = Date()
            val calendar = Calendar.getInstance()
            calendar.time = startDate
            calendar.add(Calendar.MONTH, plan.durationMonths)
            val endDate = calendar.time
            
            // Create subscription record
            val subscriptionId = UUID.randomUUID().toString()
            val subscription = Subscription(
                id = subscriptionId,
                userId = currentUser.uid,
                type = type,
                status = if (status == "active") SubscriptionStatus.ACTIVE else SubscriptionStatus.PENDING,
                paymentProvider = PaymentProvider.STRIPE,
                providerSubscriptionId = providerSubscriptionId,
                priceUsd = plan.priceUsd,
                startDate = startDate,
                currentPeriodEnd = endDate,
                autoRenew = autoRenew,
                canceledAt = null,
                features = plan.features.joinToString(","),
                createdAt = startDate,
                updatedAt = startDate
            )
            
            // Save to Firestore
            subscriptionsCollection.document(subscriptionId).set(subscription).await()
            
            // Save to Room
            subscriptionDao.insertSubscription(subscription)
            
            // Update user's premium status if subscription is active
            if (subscription.status == SubscriptionStatus.ACTIVE) {
                updateUserPremiumStatus(currentUser.uid, true)
            }
            
            emit(Resource.success(subscription))
        } catch (e: Exception) {
            emit(Resource.error("Failed to create subscription: ${e.message}"))
        }
    }
    
    override fun updateSubscription(
        subscriptionId: String,
        status: SubscriptionStatus?,
        autoRenew: Boolean?
    ): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the subscription
            val subscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
            val subscription = subscriptionDoc.toObject(Subscription::class.java)
            
            if (subscription == null) {
                emit(Resource.error("Subscription not found"))
                return@flow
            }
            
            // Verify that the subscription belongs to the current user
            if (subscription.userId != currentUser.uid) {
                emit(Resource.error("Unauthorized access to subscription"))
                return@flow
            }
            
            // Update fields
            val updates = mutableMapOf<String, Any>()
            updates["updatedAt"] = Date()
            
            if (status != null) {
                updates["status"] = status.name
                
                // If subscription is being canceled/expired, record the cancellation time
                if (status == SubscriptionStatus.CANCELED || status == SubscriptionStatus.EXPIRED) {
                    val now = Date()
                    updates["canceledAt"] = now
                    
                    // Update user's premium status
                    updateUserPremiumStatus(currentUser.uid, false)
                }
            }
            
            if (autoRenew != null) {
                updates["autoRenew"] = autoRenew
                
                // Update on Stripe if provider subscription ID exists
                if (subscription.providerSubscriptionId != null) {
                    val data = hashMapOf(
                        "subscription_id" to subscription.providerSubscriptionId,
                        "cancel_at_period_end" to !autoRenew
                    )
                    
                    functions
                        .getHttpsCallable("updateSubscription")
                        .call(data)
                        .await()
                }
            }
            
            // Update in Firestore
            subscriptionsCollection.document(subscriptionId).update(updates).await()
            
            // Get updated subscription
            val updatedSubscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
            val updatedSubscription = updatedSubscriptionDoc.toObject(Subscription::class.java)
            
            if (updatedSubscription != null) {
                // Update in Room
                subscriptionDao.updateSubscription(updatedSubscription)
                
                emit(Resource.success(updatedSubscription))
            } else {
                emit(Resource.error("Failed to update subscription"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to update subscription: ${e.message}"))
        }
    }
    
    override fun cancelSubscription(
        subscriptionId: String,
        cancelImmediately: Boolean
    ): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the subscription
            val subscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
            val subscription = subscriptionDoc.toObject(Subscription::class.java)
            
            if (subscription == null) {
                emit(Resource.error("Subscription not found"))
                return@flow
            }
            
            // Verify that the subscription belongs to the current user
            if (subscription.userId != currentUser.uid) {
                emit(Resource.error("Unauthorized access to subscription"))
                return@flow
            }
            
            val now = Date()
            val updates = mutableMapOf<String, Any>()
            updates["updatedAt"] = now
            updates["autoRenew"] = false
            
            if (cancelImmediately) {
                updates["status"] = SubscriptionStatus.CANCELED.name
                updates["canceledAt"] = now
                
                // Update user's premium status
                updateUserPremiumStatus(currentUser.uid, false)
            } else {
                updates["status"] = SubscriptionStatus.ACTIVE.name
                // Will be canceled at period end
            }
            
            // Update on Stripe if provider subscription ID exists
            if (subscription.providerSubscriptionId != null) {
                val data = hashMapOf<String, Any>(
                    "subscription_id" to subscription.providerSubscriptionId,
                    "cancel_at_period_end" to !cancelImmediately
                )
                
                if (cancelImmediately) {
                    data["cancel_immediately"] = true
                }
                
                functions
                    .getHttpsCallable("cancelSubscription")
                    .call(data)
                    .await()
            }
            
            // Update in Firestore
            subscriptionsCollection.document(subscriptionId).update(updates).await()
            
            // Get updated subscription
            val updatedSubscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
            val updatedSubscription = updatedSubscriptionDoc.toObject(Subscription::class.java)
            
            if (updatedSubscription != null) {
                // Update in Room
                subscriptionDao.updateSubscription(updatedSubscription)
                
                emit(Resource.success(updatedSubscription))
            } else {
                emit(Resource.error("Failed to cancel subscription"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to cancel subscription: ${e.message}"))
        }
    }
    
    override fun getUserSubscriptions(): Flow<Resource<List<Subscription>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get subscriptions from Firestore
            val subscriptionsSnapshot = subscriptionsCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val subscriptions = subscriptionsSnapshot.documents.mapNotNull {
                it.toObject(Subscription::class.java)
            }
            
            // Update local cache
            subscriptions.forEach { subscriptionDao.insertSubscription(it) }
            
            emit(Resource.success(subscriptions))
        } catch (e: Exception) {
            // Try to get from local database if network fails
            try {
                val userId = auth.currentUser?.uid ?: ""
                val localSubscriptions = subscriptionDao.getSubscriptionsForUser(userId).value
                
                if (localSubscriptions != null && localSubscriptions.isNotEmpty()) {
                    emit(Resource.success(localSubscriptions))
                } else {
                    emit(Resource.error("Failed to get subscriptions: ${e.message}"))
                }
            } catch (ex: Exception) {
                emit(Resource.error("Failed to get subscriptions: ${e.message}"))
            }
        }
    }
    
    override fun getActiveSubscriptions(): Flow<Resource<List<Subscription>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            val now = Date()
            
            // Get active subscriptions from Firestore
            val subscriptionsSnapshot = subscriptionsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", SubscriptionStatus.ACTIVE.name)
                .whereGreaterThan("currentPeriodEnd", now)
                .get()
                .await()
                
            val subscriptions = subscriptionsSnapshot.documents.mapNotNull {
                it.toObject(Subscription::class.java)
            }
            
            emit(Resource.success(subscriptions))
        } catch (e: Exception) {
            // Try to get from local database if network fails
            try {
                val userId = auth.currentUser?.uid ?: ""
                val localSubscriptions = subscriptionDao.getSubscriptionsByStatus(userId, SubscriptionStatus.ACTIVE).value
                
                if (localSubscriptions != null) {
                    val now = Date()
                    val activeSubscriptions = localSubscriptions.filter { it.currentPeriodEnd.after(now) }
                    emit(Resource.success(activeSubscriptions))
                } else {
                    emit(Resource.error("Failed to get active subscriptions: ${e.message}"))
                }
            } catch (ex: Exception) {
                emit(Resource.error("Failed to get active subscriptions: ${e.message}"))
            }
        }
    }
    
    override fun getSubscriptionById(subscriptionId: String): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading)
        
        try {
            // Try to get from local cache first
            val localSubscription = subscriptionDao.getSubscriptionById(subscriptionId)
            
            if (localSubscription != null) {
                emit(Resource.success(localSubscription))
            }
            
            // Get from Firestore for most up-to-date data
            val subscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
            val subscription = subscriptionDoc.toObject(Subscription::class.java)
            
            if (subscription != null) {
                // Update local cache
                subscriptionDao.insertSubscription(subscription)
                emit(Resource.success(subscription))
            } else if (localSubscription == null) {
                emit(Resource.error("Subscription not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get subscription: ${e.message}"))
        }
    }
    
    override fun hasActiveSubscription(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            val now = Date()
            
            // Check if user has any active subscription
            val hasActive = subscriptionDao.hasActiveSubscription(currentUser.uid, now)
            
            if (hasActive) {
                emit(Resource.success(true))
                return@flow
            }
            
            // Double-check with Firestore
            val subscriptionsSnapshot = subscriptionsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", SubscriptionStatus.ACTIVE.name)
                .whereGreaterThan("currentPeriodEnd", now)
                .limit(1)
                .get()
                .await()
                
            val hasActiveFirestore = !subscriptionsSnapshot.isEmpty
            
            emit(Resource.success(hasActiveFirestore))
        } catch (e: Exception) {
            // Fallback to local database
            try {
                val userId = auth.currentUser?.uid ?: ""
                val now = Date()
                val hasActive = subscriptionDao.hasActiveSubscription(userId, now)
                emit(Resource.success(hasActive))
            } catch (ex: Exception) {
                emit(Resource.error("Failed to check active subscription: ${e.message}"))
            }
        }
    }
    
    override fun hasActiveSubscriptionOfType(type: SubscriptionType): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            val now = Date()
            
            // Check if user has active subscription of the specified type
            val hasActiveOfType = subscriptionDao.hasActiveSubscriptionOfType(currentUser.uid, type, now)
            
            if (hasActiveOfType) {
                emit(Resource.success(true))
                return@flow
            }
            
            // Double-check with Firestore
            val subscriptionsSnapshot = subscriptionsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", SubscriptionStatus.ACTIVE.name)
                .whereEqualTo("type", type.name)
                .whereGreaterThan("currentPeriodEnd", now)
                .limit(1)
                .get()
                .await()
                
            val hasActiveOfTypeFirestore = !subscriptionsSnapshot.isEmpty
            
            emit(Resource.success(hasActiveOfTypeFirestore))
        } catch (e: Exception) {
            // Fallback to local database
            try {
                val userId = auth.currentUser?.uid ?: ""
                val now = Date()
                val hasActiveOfType = subscriptionDao.hasActiveSubscriptionOfType(userId, type, now)
                emit(Resource.success(hasActiveOfType))
            } catch (ex: Exception) {
                emit(Resource.error("Failed to check active subscription of type: ${e.message}"))
            }
        }
    }
    
    override fun getCurrentActiveSubscription(): Flow<Resource<Subscription?>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            val now = Date()
            
            // Get current active subscription from local cache
            val localSubscription = subscriptionDao.getCurrentActiveSubscription(currentUser.uid, now)
            
            if (localSubscription != null) {
                emit(Resource.success(localSubscription))
            }
            
            // Get from Firestore for most up-to-date data
            val subscriptionsSnapshot = subscriptionsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", SubscriptionStatus.ACTIVE.name)
                .whereGreaterThan("currentPeriodEnd", now)
                .orderBy("currentPeriodEnd", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .orderBy("type", com.google.firebase.firestore.Query.Direction.DESCENDING) // Higher tier first
                .limit(1)
                .get()
                .await()
                
            if (!subscriptionsSnapshot.isEmpty) {
                val subscription = subscriptionsSnapshot.documents.first().toObject(Subscription::class.java)
                
                if (subscription != null) {
                    // Update local cache
                    subscriptionDao.insertSubscription(subscription)
                    emit(Resource.success(subscription))
                } else if (localSubscription == null) {
                    emit(Resource.success(null))
                }
            } else if (localSubscription == null) {
                emit(Resource.success(null))
            }
        } catch (e: Exception) {
            // Fallback to local database
            try {
                val userId = auth.currentUser?.uid ?: ""
                val now = Date()
                val localSubscription = subscriptionDao.getCurrentActiveSubscription(userId, now)
                emit(Resource.success(localSubscription))
            } catch (ex: Exception) {
                emit(Resource.error("Failed to get current active subscription: ${e.message}"))
            }
        }
    }
    
    override fun updateSubscriptionPaymentMethod(
        subscriptionId: String,
        paymentMethodId: String
    ): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the subscription
            val subscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
            val subscription = subscriptionDoc.toObject(Subscription::class.java)
            
            if (subscription == null) {
                emit(Resource.error("Subscription not found"))
                return@flow
            }
            
            // Verify that the subscription belongs to the current user
            if (subscription.userId != currentUser.uid) {
                emit(Resource.error("Unauthorized access to subscription"))
                return@flow
            }
            
            // Update payment method on Stripe if provider subscription ID exists
            if (subscription.providerSubscriptionId != null) {
                val data = hashMapOf(
                    "subscription_id" to subscription.providerSubscriptionId,
                    "payment_method_id" to paymentMethodId
                )
                
                functions
                    .getHttpsCallable("updateSubscriptionPaymentMethod")
                    .call(data)
                    .await()
                
                // Update subscription in Firestore
                val updates = mapOf(
                    "updatedAt" to Date()
                )
                
                subscriptionsCollection.document(subscriptionId).update(updates).await()
                
                // Get updated subscription
                val updatedSubscriptionDoc = subscriptionsCollection.document(subscriptionId).get().await()
                val updatedSubscription = updatedSubscriptionDoc.toObject(Subscription::class.java)
                
                if (updatedSubscription != null) {
                    // Update in Room
                    subscriptionDao.updateSubscription(updatedSubscription)
                    
                    emit(Resource.success(updatedSubscription))
                } else {
                    emit(Resource.error("Failed to update subscription payment method"))
                }
            } else {
                emit(Resource.error("Cannot update payment method for this subscription"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to update subscription payment method: ${e.message}"))
        }
    }
    
    override fun syncSubscriptions(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Call Firebase function to sync subscriptions
            val data = hashMapOf(
                "user_id" to currentUser.uid
            )
            
            functions
                .getHttpsCallable("syncSubscriptions")
                .call(data)
                .await()
            
            // Get updated subscriptions from Firestore
            val subscriptionsSnapshot = subscriptionsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                
            val subscriptions = subscriptionsSnapshot.documents.mapNotNull {
                it.toObject(Subscription::class.java)
            }
            
            // Update local cache
            subscriptions.forEach { subscriptionDao.insertSubscription(it) }
            
            // Update user's premium status based on active subscriptions
            val now = Date()
            val hasActive = subscriptions.any { 
                it.status == SubscriptionStatus.ACTIVE && it.currentPeriodEnd.after(now) && it.type != SubscriptionType.BASIC
            }
            
            updateUserPremiumStatus(currentUser.uid, hasActive)
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to sync subscriptions: ${e.message}"))
        }
    }
    
    private suspend fun updateUserPremiumStatus(userId: String, isPremium: Boolean) {
        try {
            // Update user's premium status in Firestore
            firestore.collection("users")
                .document(userId)
                .update("isPremium", isPremium)
                .await()
        } catch (e: Exception) {
            // Log error but don't throw
            println("Failed to update user premium status: ${e.message}")
        }
    }
    
    // Extension function to get first emission from a Flow
    private suspend fun <T> Flow<T>.await(): T? {
        var result: T? = null
        this.collect { result = it }
        return result
    }
}