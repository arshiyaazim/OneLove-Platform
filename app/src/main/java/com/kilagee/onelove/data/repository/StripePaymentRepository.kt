package com.kilagee.onelove.data.repository

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.kilagee.onelove.data.database.dao.PaymentDao
import com.kilagee.onelove.data.model.Payment
import com.kilagee.onelove.data.model.PaymentProvider
import com.kilagee.onelove.data.model.PaymentStatus
import com.kilagee.onelove.data.model.PaymentType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.*
import com.stripe.android.PaymentConfiguration
import com.stripe.android.Stripe
import com.stripe.android.model.ConfirmPaymentIntentParams
import com.stripe.android.model.PaymentMethodCreateParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StripePaymentRepository @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions,
    private val paymentDao: PaymentDao
) : PaymentRepository {

    private val paymentsCollection = firestore.collection("payments")
    private val paymentMethodsCollection = firestore.collection("payment_methods")
    private var stripe: Stripe? = null

    private fun initializeStripe(publishableKey: String) {
        if (stripe == null) {
            PaymentConfiguration.init(context, publishableKey)
            stripe = Stripe(context, publishableKey)
        }
    }
    
    override fun createPaymentIntent(
        amountUsd: Double,
        type: PaymentType,
        subscriptionId: String?,
        offerId: String?,
        provider: PaymentProvider
    ): Flow<Resource<PaymentIntent>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Call Firebase function to create a payment intent on the server
            val data = hashMapOf(
                "amount" to (amountUsd * 100).toInt(), // Stripe uses cents
                "currency" to "usd",
                "payment_type" to type.name,
                "user_id" to currentUser.uid
            )
            
            // Add optional parameters if they exist
            subscriptionId?.let { data["subscription_id"] = it }
            offerId?.let { data["offer_id"] = it }
            
            val result = functions
                .getHttpsCallable("createPaymentIntent")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val clientSecret = result?.get("clientSecret") as? String
            val intentId = result?.get("id") as? String
            val amount = result?.get("amount") as? Number
            val requiresAction = result?.get("requires_action") as? Boolean ?: false
            val paymentMethodTypes = result?.get("payment_method_types") as? List<*> ?: listOf("card")
            val currency = result?.get("currency") as? String ?: "usd"
            val status = result?.get("status") as? String ?: "requires_payment_method"
            
            if (clientSecret != null && intentId != null && amount != null) {
                val paymentIntent = PaymentIntent(
                    id = intentId,
                    clientSecret = clientSecret,
                    amountUsd = amount.toDouble() / 100, // Convert back to dollars
                    requiresAction = requiresAction,
                    status = status,
                    paymentMethodTypes = paymentMethodTypes.map { it.toString() },
                    currency = currency
                )
                
                // Initialize Stripe with publishable key
                val publishableKey = result["publishable_key"] as? String
                if (publishableKey != null) {
                    initializeStripe(publishableKey)
                }
                
                // Save payment intent to Firestore with pending status
                val paymentId = UUID.randomUUID().toString()
                val payment = Payment(
                    id = paymentId,
                    userId = currentUser.uid,
                    amountUsd = paymentIntent.amountUsd,
                    amountLocal = paymentIntent.amountUsd, // Using USD as local currency for now
                    currency = paymentIntent.currency.uppercase(),
                    status = PaymentStatus.PENDING,
                    type = type,
                    provider = PaymentProvider.STRIPE,
                    providerPaymentId = intentId,
                    providerTransactionId = null,
                    subscriptionId = subscriptionId,
                    offerId = offerId,
                    requiresAction = requiresAction,
                    actionUrl = null,
                    receiptUrl = null,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                paymentsCollection.document(paymentId).set(payment).await()
                paymentDao.insertPayment(payment)
                
                emit(Resource.success(paymentIntent))
            } else {
                emit(Resource.error("Failed to create payment intent"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Error creating payment intent: ${e.message}"))
        }
    }
    
    override fun confirmPaymentIntent(
        paymentIntentId: String,
        paymentMethodId: String
    ): Flow<Resource<Payment>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the payment from Firestore
            val paymentsQuery = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("providerPaymentId", paymentIntentId)
                .limit(1)
                .get()
                .await()
                
            if (paymentsQuery.isEmpty) {
                emit(Resource.error("Payment not found"))
                return@flow
            }
            
            val paymentDoc = paymentsQuery.documents.first()
            val payment = paymentDoc.toObject(Payment::class.java)
            
            if (payment == null) {
                emit(Resource.error("Invalid payment data"))
                return@flow
            }
            
            // Call Firebase function to confirm the payment intent
            val data = hashMapOf(
                "payment_intent_id" to paymentIntentId,
                "payment_method_id" to paymentMethodId
            )
            
            val result = functions
                .getHttpsCallable("confirmPaymentIntent")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val status = result?.get("status") as? String
            val succeeded = status == "succeeded"
            val requiresAction = status == "requires_action"
            val nextAction = result?.get("next_action") as? Map<*, *>
            val receiptUrl = result?.get("receipt_url") as? String
            
            // Update payment in Firestore and Room
            val updatedPayment = payment.copy(
                status = if (succeeded) PaymentStatus.SUCCEEDED else PaymentStatus.PENDING,
                requiresAction = requiresAction,
                actionUrl = nextAction?.get("redirect_to_url") as? String,
                receiptUrl = receiptUrl,
                updatedAt = Date()
            )
            
            paymentsCollection.document(payment.id).set(updatedPayment).await()
            paymentDao.updatePayment(updatedPayment)
            
            emit(Resource.success(updatedPayment))
        } catch (e: Exception) {
            emit(Resource.error("Error confirming payment: ${e.message}"))
        }
    }
    
    override fun getUserPayments(): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments from Firestore
            val paymentsSnapshot = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val payments = paymentsSnapshot.documents.mapNotNull {
                it.toObject(Payment::class.java)
            }
            
            // Update local cache
            paymentDao.insertPayments(payments)
            
            emit(Resource.success(payments))
        } catch (e: Exception) {
            // Try to get from local database if network fails
            try {
                val userId = auth.currentUser?.uid ?: ""
                val localPayments = paymentDao.getPaymentsForUser(userId).value
                
                if (localPayments != null && localPayments.isNotEmpty()) {
                    emit(Resource.success(localPayments))
                } else {
                    emit(Resource.error("Failed to get payments: ${e.message}"))
                }
            } catch (ex: Exception) {
                emit(Resource.error("Failed to get payments: ${e.message}"))
            }
        }
    }
    
    override fun getPaymentsByStatus(status: PaymentStatus): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments from Firestore
            val paymentsSnapshot = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", status.name)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val payments = paymentsSnapshot.documents.mapNotNull {
                it.toObject(Payment::class.java)
            }
            
            emit(Resource.success(payments))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get payments by status: ${e.message}"))
        }
    }
    
    override fun getPaymentsByType(type: PaymentType): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments from Firestore
            val paymentsSnapshot = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("type", type.name)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val payments = paymentsSnapshot.documents.mapNotNull {
                it.toObject(Payment::class.java)
            }
            
            emit(Resource.success(payments))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get payments by type: ${e.message}"))
        }
    }
    
    override fun getPaymentById(paymentId: String): Flow<Resource<Payment>> = flow {
        emit(Resource.Loading)
        
        try {
            // Try to get from local cache first
            val localPayment = paymentDao.getPaymentById(paymentId)
            
            if (localPayment != null) {
                emit(Resource.success(localPayment))
            }
            
            // Get from Firestore for most up-to-date data
            val paymentDoc = paymentsCollection.document(paymentId).get().await()
            val payment = paymentDoc.toObject(Payment::class.java)
            
            if (payment != null) {
                paymentDao.insertPayment(payment)
                emit(Resource.success(payment))
            } else if (localPayment == null) {
                emit(Resource.error("Payment not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to get payment: ${e.message}"))
        }
    }
    
    override fun getPaymentsForSubscription(subscriptionId: String): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments from Firestore
            val paymentsSnapshot = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("subscriptionId", subscriptionId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val payments = paymentsSnapshot.documents.mapNotNull {
                it.toObject(Payment::class.java)
            }
            
            emit(Resource.success(payments))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get payments for subscription: ${e.message}"))
        }
    }
    
    override fun getPaymentsForOffer(offerId: String): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments from Firestore
            val paymentsSnapshot = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("offerId", offerId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()
                
            val payments = paymentsSnapshot.documents.mapNotNull {
                it.toObject(Payment::class.java)
            }
            
            emit(Resource.success(payments))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get payments for offer: ${e.message}"))
        }
    }
    
    override fun getTotalSpentInDateRange(startDate: Date, endDate: Date): Flow<Resource<Double>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments from Firestore
            val paymentsSnapshot = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", PaymentStatus.SUCCEEDED.name)
                .whereGreaterThanOrEqualTo("createdAt", startDate)
                .whereLessThanOrEqualTo("createdAt", endDate)
                .get()
                .await()
                
            var total = 0.0
            paymentsSnapshot.documents.forEach {
                val payment = it.toObject(Payment::class.java)
                if (payment != null) {
                    total += payment.amountUsd
                }
            }
            
            emit(Resource.success(total))
        } catch (e: Exception) {
            // Try to get from local database if network fails
            try {
                val userId = auth.currentUser?.uid ?: ""
                val total = paymentDao.getTotalSpentInDateRange(userId, startDate, endDate) ?: 0.0
                emit(Resource.success(total))
            } catch (ex: Exception) {
                emit(Resource.error("Failed to get total spent: ${e.message}"))
            }
        }
    }
    
    override fun handlePaymentRequiringAction(
        paymentId: String, 
        actionResult: String?
    ): Flow<Resource<Payment>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the payment
            val paymentDoc = paymentsCollection.document(paymentId).get().await()
            val payment = paymentDoc.toObject(Payment::class.java)
            
            if (payment == null) {
                emit(Resource.error("Payment not found"))
                return@flow
            }
            
            // Call Firebase function to handle the action result
            val data = hashMapOf(
                "payment_id" to payment.providerPaymentId,
                "action_result" to (actionResult ?: "")
            )
            
            val result = functions
                .getHttpsCallable("handlePaymentAction")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val status = result?.get("status") as? String
            val succeeded = status == "succeeded"
            val requiresAction = status == "requires_action"
            val nextAction = result?.get("next_action") as? Map<*, *>
            val receiptUrl = result?.get("receipt_url") as? String
            
            // Update payment in Firestore and Room
            val updatedPayment = payment.copy(
                status = if (succeeded) PaymentStatus.SUCCEEDED else PaymentStatus.PENDING,
                requiresAction = requiresAction,
                actionUrl = nextAction?.get("redirect_to_url") as? String,
                receiptUrl = receiptUrl,
                updatedAt = Date()
            )
            
            paymentsCollection.document(payment.id).set(updatedPayment).await()
            paymentDao.updatePayment(updatedPayment)
            
            emit(Resource.success(updatedPayment))
        } catch (e: Exception) {
            emit(Resource.error("Failed to handle payment action: ${e.message}"))
        }
    }
    
    override fun requestRefund(
        paymentId: String, 
        reason: String?, 
        amountUsd: Double?
    ): Flow<Resource<Payment>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get the payment
            val paymentDoc = paymentsCollection.document(paymentId).get().await()
            val payment = paymentDoc.toObject(Payment::class.java)
            
            if (payment == null) {
                emit(Resource.error("Payment not found"))
                return@flow
            }
            
            if (payment.status != PaymentStatus.SUCCEEDED) {
                emit(Resource.error("Only successful payments can be refunded"))
                return@flow
            }
            
            // Call Firebase function to process the refund
            val data = hashMapOf(
                "payment_id" to payment.providerPaymentId,
                "reason" to (reason ?: "requested_by_customer")
            )
            
            // Add amount if partial refund
            amountUsd?.let {
                data["amount"] = (it * 100).toInt() // Stripe uses cents
            }
            
            val result = functions
                .getHttpsCallable("refundPayment")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val refundId = result?.get("id") as? String
            val status = result?.get("status") as? String
            val succeeded = status == "succeeded"
            
            if (refundId == null) {
                emit(Resource.error("Failed to process refund"))
                return@flow
            }
            
            // Create a refund payment
            val refundPaymentId = UUID.randomUUID().toString()
            val refundPayment = Payment(
                id = refundPaymentId,
                userId = currentUser.uid,
                amountUsd = amountUsd ?: payment.amountUsd,
                amountLocal = amountUsd ?: payment.amountLocal,
                currency = payment.currency,
                status = if (succeeded) PaymentStatus.REFUNDED else PaymentStatus.PENDING,
                type = PaymentType.REFUND,
                provider = payment.provider,
                providerPaymentId = refundId,
                providerTransactionId = payment.providerPaymentId, // Original payment ID
                subscriptionId = payment.subscriptionId,
                offerId = payment.offerId,
                requiresAction = false,
                actionUrl = null,
                receiptUrl = null,
                createdAt = Date(),
                updatedAt = Date()
            )
            
            // Update original payment
            val updatedPayment = payment.copy(
                status = PaymentStatus.REFUNDED,
                updatedAt = Date()
            )
            
            // Save to Firestore and Room
            paymentsCollection.document(refundPaymentId).set(refundPayment).await()
            paymentsCollection.document(payment.id).set(updatedPayment).await()
            
            paymentDao.insertPayment(refundPayment)
            paymentDao.updatePayment(updatedPayment)
            
            emit(Resource.success(refundPayment))
        } catch (e: Exception) {
            emit(Resource.error("Failed to request refund: ${e.message}"))
        }
    }
    
    override fun savePaymentMethod(
        paymentMethodDetails: PaymentMethodDetails
    ): Flow<Resource<String>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Create payment method parameters based on type
            val params = when (paymentMethodDetails.type) {
                "card" -> {
                    if (paymentMethodDetails.cardNumber == null || 
                        paymentMethodDetails.expiryMonth == null || 
                        paymentMethodDetails.expiryYear == null || 
                        paymentMethodDetails.cvc == null) {
                        emit(Resource.error("Card details are incomplete"))
                        return@flow
                    }
                    
                    val cardParams = PaymentMethodCreateParams.Card.Builder()
                        .setNumber(paymentMethodDetails.cardNumber)
                        .setExpiryMonth(paymentMethodDetails.expiryMonth)
                        .setExpiryYear(paymentMethodDetails.expiryYear)
                        .setCvc(paymentMethodDetails.cvc)
                        .build()
                        
                    val billingDetails = paymentMethodDetails.billingAddress?.let {
                        PaymentMethodCreateParams.BillingDetails.Builder()
                            .setName(paymentMethodDetails.billingName)
                            .setEmail(paymentMethodDetails.billingEmail)
                            .setPhone(paymentMethodDetails.billingPhone)
                            .setAddress(
                                PaymentMethodCreateParams.BillingDetails.Address.Builder()
                                    .setLine1(it.line1)
                                    .setLine2(it.line2)
                                    .setCity(it.city)
                                    .setState(it.state)
                                    .setPostalCode(it.postalCode)
                                    .setCountry(it.country)
                                    .build()
                            )
                            .build()
                    }
                    
                    PaymentMethodCreateParams.create(
                        cardParams,
                        billingDetails
                    )
                }
                else -> {
                    emit(Resource.error("Unsupported payment method type"))
                    return@flow
                }
            }
            
            // Call Firebase function to create the payment method
            val data = hashMapOf(
                "type" to paymentMethodDetails.type,
                "user_id" to currentUser.uid,
                "billing_name" to paymentMethodDetails.billingName,
                "billing_email" to paymentMethodDetails.billingEmail,
                "billing_phone" to paymentMethodDetails.billingPhone
            )
            
            val result = functions
                .getHttpsCallable("createPaymentMethod")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val paymentMethodId = result?.get("id") as? String
            
            if (paymentMethodId == null) {
                emit(Resource.error("Failed to create payment method"))
                return@flow
            }
            
            // Save to Firestore
            val paymentMethod = PaymentMethod(
                id = paymentMethodId,
                type = paymentMethodDetails.type,
                last4 = result["last4"] as? String,
                brand = result["brand"] as? String,
                expiryMonth = (result["exp_month"] as? Number)?.toInt(),
                expiryYear = (result["exp_year"] as? Number)?.toInt(),
                holderName = paymentMethodDetails.billingName,
                isDefault = false,
                createdAt = Date()
            )
            
            paymentMethodsCollection
                .document(paymentMethodId)
                .set(mapOf(
                    "id" to paymentMethod.id,
                    "userId" to currentUser.uid,
                    "type" to paymentMethod.type,
                    "last4" to paymentMethod.last4,
                    "brand" to paymentMethod.brand,
                    "expiryMonth" to paymentMethod.expiryMonth,
                    "expiryYear" to paymentMethod.expiryYear,
                    "holderName" to paymentMethod.holderName,
                    "isDefault" to paymentMethod.isDefault,
                    "createdAt" to paymentMethod.createdAt
                ))
                .await()
                
            // If this is the first payment method, set it as default
            val methodsQuery = paymentMethodsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                
            if (methodsQuery.size() == 1) {
                paymentMethodsCollection
                    .document(paymentMethodId)
                    .update("isDefault", true)
                    .await()
            }
            
            emit(Resource.success(paymentMethodId))
        } catch (e: Exception) {
            emit(Resource.error("Failed to save payment method: ${e.message}"))
        }
    }
    
    override fun getSavedPaymentMethods(): Flow<Resource<List<PaymentMethod>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payment methods from Firestore
            val methodsSnapshot = paymentMethodsCollection
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()
                
            val paymentMethods = methodsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                PaymentMethod(
                    id = doc.id,
                    type = data["type"] as? String ?: "",
                    last4 = data["last4"] as? String,
                    brand = data["brand"] as? String,
                    expiryMonth = (data["expiryMonth"] as? Number)?.toInt(),
                    expiryYear = (data["expiryYear"] as? Number)?.toInt(),
                    holderName = data["holderName"] as? String,
                    isDefault = data["isDefault"] as? Boolean ?: false,
                    createdAt = data["createdAt"] as? Date ?: Date()
                )
            }
            
            emit(Resource.success(paymentMethods))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get payment methods: ${e.message}"))
        }
    }
    
    override fun deletePaymentMethod(paymentMethodId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Call Firebase function to delete the payment method
            val data = hashMapOf(
                "payment_method_id" to paymentMethodId
            )
            
            functions
                .getHttpsCallable("deletePaymentMethod")
                .call(data)
                .await()
                
            // Delete from Firestore
            paymentMethodsCollection.document(paymentMethodId).delete().await()
            
            // If this was the default payment method, set another one as default
            val methodDoc = paymentMethodsCollection.document(paymentMethodId).get().await()
            val wasDefault = methodDoc.get("isDefault") as? Boolean ?: false
            
            if (wasDefault) {
                val otherMethodsQuery = paymentMethodsCollection
                    .whereEqualTo("userId", currentUser.uid)
                    .limit(1)
                    .get()
                    .await()
                    
                if (!otherMethodsQuery.isEmpty) {
                    val newDefaultId = otherMethodsQuery.documents.first().id
                    paymentMethodsCollection
                        .document(newDefaultId)
                        .update("isDefault", true)
                        .await()
                }
            }
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to delete payment method: ${e.message}"))
        }
    }
    
    override fun setDefaultPaymentMethod(paymentMethodId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Call Firebase function to set as default
            val data = hashMapOf(
                "payment_method_id" to paymentMethodId
            )
            
            functions
                .getHttpsCallable("setDefaultPaymentMethod")
                .call(data)
                .await()
                
            // Update in Firestore - first clear all defaults
            val methodsQuery = paymentMethodsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isDefault", true)
                .get()
                .await()
                
            methodsQuery.documents.forEach { doc ->
                paymentMethodsCollection
                    .document(doc.id)
                    .update("isDefault", false)
                    .await()
            }
            
            // Set the new default
            paymentMethodsCollection
                .document(paymentMethodId)
                .update("isDefault", true)
                .await()
                
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to set default payment method: ${e.message}"))
        }
    }
    
    override fun getPaymentsRequiringAction(): Flow<Resource<List<Payment>>> = flow {
        emit(Resource.Loading)
        
        try {
            val currentUser = auth.currentUser
            
            if (currentUser == null) {
                emit(Resource.error("User not authenticated"))
                return@flow
            }
            
            // Get payments that require action
            val paymentsQuery = paymentsCollection
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("requiresAction", true)
                .whereEqualTo("status", PaymentStatus.PENDING.name)
                .get()
                .await()
                
            val payments = paymentsQuery.documents.mapNotNull {
                it.toObject(Payment::class.java)
            }
            
            emit(Resource.success(payments))
        } catch (e: Exception) {
            // Try to get from local database if network fails
            try {
                val userId = auth.currentUser?.uid ?: ""
                val payments = paymentDao.getPaymentsRequiringAction(userId)
                emit(Resource.success(payments))
            } catch (ex: Exception) {
                emit(Resource.error("Failed to get payments requiring action: ${e.message}"))
            }
        }
    }
}