package com.kilagee.onelove.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.kilagee.onelove.data.local.SubscriptionDao
import com.kilagee.onelove.data.local.UserDao
import com.kilagee.onelove.data.model.BillingPeriod
import com.kilagee.onelove.data.model.PaymentMethod
import com.kilagee.onelove.data.model.SubscriptionPlan
import com.kilagee.onelove.data.model.SubscriptionPurchaseResult
import com.kilagee.onelove.data.model.SubscriptionStatus
import com.kilagee.onelove.data.model.SubscriptionTier
import com.kilagee.onelove.data.model.UserSubscription
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import com.kilagee.onelove.domain.util.Result
import com.stripe.android.PaymentConfiguration
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.ConfirmSetupIntentParams
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of SubscriptionRepository that integrates with Stripe
 */
@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val subscriptionDao: SubscriptionDao,
    private val userDao: UserDao
) : SubscriptionRepository {

    private val subscriptionPlansCollection = firestore.collection("subscription_plans")
    private val userSubscriptionsCollection = firestore.collection("user_subscriptions")
    private val paymentMethodsCollection = firestore.collection("payment_methods")
    
    init {
        // Initialize Stripe with public key
        // In production, this key should be fetched from a secure backend
        val context = OneLoveApp.instance.applicationContext
        val stripePublishableKey = context.getString(R.string.stripe_publishable_key)
        PaymentConfiguration.init(context, stripePublishableKey)
    }
    
    /**
     * Get available subscription plans
     */
    override fun getSubscriptionPlans(): Flow<Result<List<SubscriptionPlan>>> = callbackFlow {
        trySend(Result.Loading)
        
        try {
            val snapshot = subscriptionPlansCollection
                .orderBy("displayOrder")
                .get()
                .await()
                
            val plans = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SubscriptionPlan::class.java)
            }
            
            trySend(Result.Success(plans))
        } catch (e: Exception) {
            Timber.e(e, "Error fetching subscription plans")
            trySend(Result.Error("Failed to fetch subscription plans: ${e.message}"))
        }
        
        awaitClose()
    }.catch { e ->
        Timber.e(e, "Exception in getSubscriptionPlans flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Get user's current subscription
     */
    override fun getUserSubscription(userId: String): Flow<Result<UserSubscription?>> = callbackFlow {
        trySend(Result.Loading)
        
        try {
            // Try to get from local database first
            val localSubscription = subscriptionDao.getSubscriptionByUserIdFlow(userId)
            
            // Set up Firestore listener for real-time updates
            val query = userSubscriptionsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("status", SubscriptionStatus.ACTIVE.name)
                .limit(1)
                
            val listener = query.addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Timber.e(exception, "Error listening to subscription updates")
                    trySend(Result.Error("Failed to get subscription updates: ${exception.message}"))
                    return@addSnapshotListener
                }
                
                if (snapshot != null && !snapshot.isEmpty) {
                    val subscription = snapshot.documents[0].toObject(UserSubscription::class.java)
                    if (subscription != null) {
                        // Cache in local database
                        try {
                            subscriptionDao.insertSubscription(subscription)
                        } catch (e: Exception) {
                            Timber.e(e, "Error caching subscription")
                        }
                        
                        trySend(Result.Success(subscription))
                    } else {
                        trySend(Result.Success(null))
                    }
                } else {
                    // Check if we have a local subscription
                    try {
                        val local = localSubscription.first()
                        if (local != null) {
                            // Check if it's expired
                            if (local.endDate.before(Date())) {
                                // Expired subscription
                                val updated = local.copy(status = SubscriptionStatus.EXPIRED)
                                subscriptionDao.updateSubscription(updated)
                                trySend(Result.Success(null))
                            } else {
                                trySend(Result.Success(local))
                            }
                        } else {
                            trySend(Result.Success(null))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error checking local subscription")
                        trySend(Result.Success(null))
                    }
                }
            }
            
            awaitClose { listener.remove() }
        } catch (e: Exception) {
            Timber.e(e, "Error getting user subscription")
            trySend(Result.Error("Failed to get subscription: ${e.message}"))
            close()
        }
    }.catch { e ->
        Timber.e(e, "Exception in getUserSubscription flow")
        emit(Result.Error("Exception: ${e.message}"))
    }
    
    /**
     * Subscribe user to a plan
     */
    override suspend fun subscribeToPlan(
        userId: String,
        planId: String,
        paymentMethodId: String?,
        billingPeriod: BillingPeriod
    ): Result<SubscriptionPurchaseResult> {
        try {
            // Get the plan details
            val planDoc = subscriptionPlansCollection.document(planId).get().await()
            val plan = planDoc.toObject(SubscriptionPlan::class.java)
                ?: return Result.Error("Subscription plan not found")
            
            // If it's a free plan, create subscription directly
            if (plan.tier == SubscriptionTier.FREE) {
                return createFreeSubscription(userId, plan)
            }
            
            // For paid plans, call Stripe through Firebase Functions
            val createSubscriptionData = hashMapOf(
                "userId" to userId,
                "planId" to planId,
                "paymentMethodId" to paymentMethodId,
                "billingPeriod" to billingPeriod.name
            )
            
            val result = functions
                .getHttpsCallable("createSubscription")
                .call(createSubscriptionData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                // Get subscription details from response
                val subscriptionData = result["subscription"] as? Map<*, *>
                    ?: return Result.Error("Invalid subscription data")
                
                val subscriptionId = subscriptionData["id"] as? String
                    ?: return Result.Error("Invalid subscription ID")
                
                val customerId = subscriptionData["customer"] as? String
                val status = subscriptionData["status"] as? String
                
                // Check if payment requires additional action
                val requiresAction = result["requiresAction"] as? Boolean ?: false
                val clientSecret = result["clientSecret"] as? String
                
                if (requiresAction && clientSecret != null) {
                    // Return result with action required
                    return Result.Success(
                        SubscriptionPurchaseResult(
                            success = false,
                            error = null,
                            requiresAction = true,
                            actionUrl = clientSecret
                        )
                    )
                }
                
                // Create subscription record
                val endDate = calculateSubscriptionEndDate(billingPeriod)
                val subscription = UserSubscription(
                    id = subscriptionId,
                    userId = userId,
                    tier = plan.tier,
                    billingPeriod = billingPeriod,
                    startDate = Date(),
                    endDate = endDate,
                    autoRenew = true,
                    status = mapStripeStatusToLocal(status ?: "active"),
                    stripeCustomerId = customerId,
                    stripeSubscriptionId = subscriptionId,
                    stripePaymentMethodId = paymentMethodId,
                    lastPaymentDate = Date(),
                    nextBillingDate = endDate
                )
                
                // Save to Firestore
                userSubscriptionsCollection.document(subscriptionId).set(subscription).await()
                
                // Update user's subscription status
                updateUserSubscriptionStatus(userId, plan.tier, true)
                
                // Cache locally
                subscriptionDao.insertSubscription(subscription)
                
                return Result.Success(
                    SubscriptionPurchaseResult(
                        success = true,
                        subscription = subscription
                    )
                )
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error subscribing to plan")
            return Result.Error("Failed to subscribe: ${e.message}")
        }
    }
    
    /**
     * Complete subscription purchase with action
     */
    override suspend fun completeSubscription(
        userId: String,
        subscriptionId: String,
        actionSecret: String
    ): Result<SubscriptionPurchaseResult> {
        try {
            val completeData = hashMapOf(
                "userId" to userId,
                "subscriptionId" to subscriptionId,
                "paymentIntentId" to actionSecret
            )
            
            val result = functions
                .getHttpsCallable("completeSubscription")
                .call(completeData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                // Get the subscription from Firestore
                val subscriptionDoc = userSubscriptionsCollection.document(subscriptionId).get().await()
                val subscription = subscriptionDoc.toObject(UserSubscription::class.java)
                    ?: return Result.Error("Subscription not found")
                
                // Update user's subscription status
                updateUserSubscriptionStatus(userId, subscription.tier, true)
                
                // Cache locally
                subscriptionDao.insertSubscription(subscription)
                
                return Result.Success(
                    SubscriptionPurchaseResult(
                        success = true,
                        subscription = subscription
                    )
                )
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error completing subscription")
            return Result.Error("Failed to complete subscription: ${e.message}")
        }
    }
    
    /**
     * Cancel subscription
     */
    override suspend fun cancelSubscription(userId: String): Result<Boolean> {
        try {
            // Get active subscription
            val subscription = subscriptionDao.getActiveSubscriptionByUserId(userId)
                ?: return Result.Error("No active subscription found")
            
            // Call Firebase function to cancel in Stripe
            val cancelData = hashMapOf(
                "userId" to userId,
                "subscriptionId" to subscription.stripeSubscriptionId
            )
            
            val result = functions
                .getHttpsCallable("cancelSubscription")
                .call(cancelData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                // Update subscription in Firestore
                val updates = hashMapOf<String, Any>(
                    "status" to SubscriptionStatus.CANCELED.name,
                    "autoRenew" to false,
                    "canceledAt" to Date()
                )
                
                userSubscriptionsCollection.document(subscription.id).update(updates).await()
                
                // Update local cache
                val updatedSubscription = subscription.copy(
                    status = SubscriptionStatus.CANCELED,
                    autoRenew = false,
                    canceledAt = Date()
                )
                subscriptionDao.updateSubscription(updatedSubscription)
                
                // Update user's subscription status to free
                updateUserSubscriptionStatus(userId, SubscriptionTier.FREE, false)
                
                return Result.Success(true)
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error canceling subscription")
            return Result.Error("Failed to cancel subscription: ${e.message}")
        }
    }
    
    /**
     * Update subscription auto-renew setting
     */
    override suspend fun updateAutoRenew(userId: String, autoRenew: Boolean): Result<Boolean> {
        try {
            // Get active subscription
            val subscription = subscriptionDao.getActiveSubscriptionByUserId(userId)
                ?: return Result.Error("No active subscription found")
            
            // Call Firebase function to update in Stripe
            val updateData = hashMapOf(
                "userId" to userId,
                "subscriptionId" to subscription.stripeSubscriptionId,
                "autoRenew" to autoRenew
            )
            
            val result = functions
                .getHttpsCallable("updateSubscription")
                .call(updateData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                // Update subscription in Firestore
                userSubscriptionsCollection.document(subscription.id)
                    .update("autoRenew", autoRenew)
                    .await()
                
                // Update local cache
                val updatedSubscription = subscription.copy(autoRenew = autoRenew)
                subscriptionDao.updateSubscription(updatedSubscription)
                
                return Result.Success(true)
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating auto-renew")
            return Result.Error("Failed to update auto-renew: ${e.message}")
        }
    }
    
    /**
     * Change subscription plan
     */
    override suspend fun changeSubscriptionPlan(
        userId: String,
        newPlanId: String,
        newBillingPeriod: BillingPeriod
    ): Result<SubscriptionPurchaseResult> {
        try {
            // Get active subscription
            val subscription = subscriptionDao.getActiveSubscriptionByUserId(userId)
                ?: return Result.Error("No active subscription found")
            
            // Get the new plan details
            val planDoc = subscriptionPlansCollection.document(newPlanId).get().await()
            val newPlan = planDoc.toObject(SubscriptionPlan::class.java)
                ?: return Result.Error("New subscription plan not found")
            
            // Call Firebase function to change plan in Stripe
            val changeData = hashMapOf(
                "userId" to userId,
                "subscriptionId" to subscription.stripeSubscriptionId,
                "newPlanId" to newPlanId,
                "billingPeriod" to newBillingPeriod.name
            )
            
            val result = functions
                .getHttpsCallable("changeSubscriptionPlan")
                .call(changeData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                // Get updated subscription details from response
                val subscriptionData = result["subscription"] as? Map<*, *>
                    ?: return Result.Error("Invalid subscription data")
                
                val newStatus = subscriptionData["status"] as? String
                
                // Calculate new end date
                val endDate = calculateSubscriptionEndDate(newBillingPeriod)
                
                // Update subscription in Firestore and local cache
                val updatedSubscription = subscription.copy(
                    tier = newPlan.tier,
                    billingPeriod = newBillingPeriod,
                    endDate = endDate,
                    status = mapStripeStatusToLocal(newStatus ?: subscription.status.name),
                    nextBillingDate = endDate
                )
                
                userSubscriptionsCollection.document(subscription.id).set(updatedSubscription).await()
                subscriptionDao.updateSubscription(updatedSubscription)
                
                // Update user's subscription status
                updateUserSubscriptionStatus(userId, newPlan.tier, true)
                
                return Result.Success(
                    SubscriptionPurchaseResult(
                        success = true,
                        subscription = updatedSubscription
                    )
                )
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error changing subscription plan")
            return Result.Error("Failed to change plan: ${e.message}")
        }
    }
    
    /**
     * Get user's payment methods
     */
    override fun getUserPaymentMethods(userId: String): Flow<Result<List<PaymentMethod>>> = flow {
        emit(Result.Loading)
        
        try {
            // Call Firebase function to get payment methods from Stripe
            val methodsData = hashMapOf("userId" to userId)
            val result = functions
                .getHttpsCallable("getUserPaymentMethods")
                .call(methodsData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                emit(Result.Error("Invalid response from server"))
                return@flow
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                val paymentMethodsList = result["paymentMethods"] as? List<*> ?: emptyList<Any>()
                val methods = paymentMethodsList.mapNotNull { methodData ->
                    if (methodData is Map<*, *>) {
                        PaymentMethod(
                            id = methodData["id"] as? String ?: "",
                            brand = methodData["brand"] as? String ?: "Unknown",
                            last4 = methodData["last4"] as? String ?: "****",
                            expiryMonth = (methodData["expMonth"] as? Number)?.toInt() ?: 0,
                            expiryYear = (methodData["expYear"] as? Number)?.toInt() ?: 0,
                            isDefault = methodData["isDefault"] as? Boolean ?: false,
                            country = methodData["country"] as? String,
                            customerId = methodData["customerId"] as? String
                        )
                    } else {
                        null
                    }
                }
                
                emit(Result.Success(methods))
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                emit(Result.Error(error))
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting payment methods")
            emit(Result.Error("Failed to get payment methods: ${e.message}"))
        }
    }
    
    /**
     * Add payment method
     */
    override suspend fun addPaymentMethod(
        userId: String,
        cardNumber: String,
        expiryMonth: Int,
        expiryYear: Int,
        cvc: String,
        isDefault: Boolean
    ): Result<PaymentMethod> {
        try {
            // Call Firebase function to add payment method to Stripe
            val cardData = hashMapOf(
                "userId" to userId,
                "cardNumber" to cardNumber,
                "expiryMonth" to expiryMonth,
                "expiryYear" to expiryYear,
                "cvc" to cvc,
                "isDefault" to isDefault
            )
            
            val result = functions
                .getHttpsCallable("addPaymentMethod")
                .call(cardData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                val methodData = result["paymentMethod"] as? Map<*, *>
                    ?: return Result.Error("Invalid payment method data")
                
                val paymentMethod = PaymentMethod(
                    id = methodData["id"] as? String ?: "",
                    brand = methodData["brand"] as? String ?: "Unknown",
                    last4 = methodData["last4"] as? String ?: "****",
                    expiryMonth = (methodData["expMonth"] as? Number)?.toInt() ?: 0,
                    expiryYear = (methodData["expYear"] as? Number)?.toInt() ?: 0,
                    isDefault = methodData["isDefault"] as? Boolean ?: false,
                    country = methodData["country"] as? String,
                    customerId = methodData["customerId"] as? String
                )
                
                return Result.Success(paymentMethod)
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error adding payment method")
            return Result.Error("Failed to add payment method: ${e.message}")
        }
    }
    
    /**
     * Remove payment method
     */
    override suspend fun removePaymentMethod(userId: String, paymentMethodId: String): Result<Boolean> {
        try {
            // Call Firebase function to remove payment method from Stripe
            val removeData = hashMapOf(
                "userId" to userId,
                "paymentMethodId" to paymentMethodId
            )
            
            val result = functions
                .getHttpsCallable("removePaymentMethod")
                .call(removeData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                return Result.Success(true)
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error removing payment method")
            return Result.Error("Failed to remove payment method: ${e.message}")
        }
    }
    
    /**
     * Set default payment method
     */
    override suspend fun setDefaultPaymentMethod(userId: String, paymentMethodId: String): Result<Boolean> {
        try {
            // Call Firebase function to set default payment method in Stripe
            val defaultData = hashMapOf(
                "userId" to userId,
                "paymentMethodId" to paymentMethodId
            )
            
            val result = functions
                .getHttpsCallable("setDefaultPaymentMethod")
                .call(defaultData)
                .await()
                .data as? Map<*, *>
                
            if (result == null) {
                return Result.Error("Invalid response from server")
            }
            
            val success = result["success"] as? Boolean ?: false
            
            if (success) {
                return Result.Success(true)
            } else {
                val error = result["error"] as? String ?: "Unknown error"
                return Result.Error(error)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error setting default payment method")
            return Result.Error("Failed to set default payment method: ${e.message}")
        }
    }
    
    /**
     * Check if subscribed to specific tier
     */
    override suspend fun isSubscribedToTier(userId: String, tier: SubscriptionTier): Result<Boolean> {
        try {
            // Get active subscription
            val subscription = subscriptionDao.getActiveSubscriptionByUserId(userId)
            
            // If no subscription or tier is FREE, anyone can access FREE tier
            if (subscription == null) {
                return Result.Success(tier == SubscriptionTier.FREE)
            }
            
            // Check tier level
            val userTierLevel = getTierLevel(subscription.tier)
            val requestedTierLevel = getTierLevel(tier)
            
            // User can access this tier if their tier level is >= requested tier level
            return Result.Success(userTierLevel >= requestedTierLevel)
        } catch (e: Exception) {
            Timber.e(e, "Error checking subscription tier")
            return Result.Error("Failed to check subscription tier: ${e.message}")
        }
    }
    
    /**
     * Create a free subscription
     */
    private suspend fun createFreeSubscription(
        userId: String,
        plan: SubscriptionPlan
    ): Result<SubscriptionPurchaseResult> {
        try {
            // Create a free subscription that never expires
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.YEAR, 100) // Free "forever"
            
            val subscription = UserSubscription(
                id = UUID.randomUUID().toString(),
                userId = userId,
                tier = SubscriptionTier.FREE,
                billingPeriod = BillingPeriod.YEARLY,
                startDate = Date(),
                endDate = calendar.time,
                autoRenew = false,
                status = SubscriptionStatus.ACTIVE
            )
            
            // Save to Firestore
            userSubscriptionsCollection.document(subscription.id).set(subscription).await()
            
            // Update user's subscription status
            updateUserSubscriptionStatus(userId, SubscriptionTier.FREE, true)
            
            // Cache locally
            subscriptionDao.insertSubscription(subscription)
            
            return Result.Success(
                SubscriptionPurchaseResult(
                    success = true,
                    subscription = subscription
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating free subscription")
            return Result.Error("Failed to create free subscription: ${e.message}")
        }
    }
    
    /**
     * Update user's subscription status in the user document
     */
    private suspend fun updateUserSubscriptionStatus(
        userId: String,
        tier: SubscriptionTier,
        isPremium: Boolean
    ) {
        try {
            // Update user document
            val updates = hashMapOf<String, Any>(
                "subscriptionType" to tier.name,
                "isPremium" to isPremium
            )
            
            firestore.collection("users").document(userId).update(updates).await()
            
            // Update local cache
            val user = userDao.getUserById(userId)
            if (user != null) {
                userDao.updateUser(
                    user.copy(
                        subscriptionType = tier.name,
                        isPremium = isPremium
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating user subscription status")
        }
    }
    
    /**
     * Calculate subscription end date based on billing period
     */
    private fun calculateSubscriptionEndDate(billingPeriod: BillingPeriod): Date {
        val calendar = Calendar.getInstance()
        
        when (billingPeriod) {
            BillingPeriod.MONTHLY -> calendar.add(Calendar.MONTH, 1)
            BillingPeriod.YEARLY -> calendar.add(Calendar.YEAR, 1)
        }
        
        return calendar.time
    }
    
    /**
     * Map Stripe subscription status to local enum
     */
    private fun mapStripeStatusToLocal(status: String): SubscriptionStatus {
        return when (status.lowercase()) {
            "active" -> SubscriptionStatus.ACTIVE
            "canceled" -> SubscriptionStatus.CANCELED
            "incomplete" -> SubscriptionStatus.INCOMPLETE
            "past_due" -> SubscriptionStatus.PAST_DUE
            "trialing" -> SubscriptionStatus.TRIALING
            else -> SubscriptionStatus.INCOMPLETE
        }
    }
    
    /**
     * Get numeric level for a tier for comparison
     */
    private fun getTierLevel(tier: SubscriptionTier): Int {
        return when (tier) {
            SubscriptionTier.FREE -> 0
            SubscriptionTier.PREMIUM -> 1
            SubscriptionTier.GOLD -> 2
        }
    }
}