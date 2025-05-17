package com.kilagee.onelove.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.os.bundleOf
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kilagee.onelove.R
import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationActionType
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.ui.MainActivity
import com.kilagee.onelove.ui.calls.IncomingCallActivity
import com.kilagee.onelove.ui.notifications.NotificationsActivity
import java.util.Date
import java.util.UUID

class OneLoveFirebaseMessagingService : FirebaseMessagingService() {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        val currentUser = auth.currentUser ?: return
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: "OneLove"
            val body = notification.body ?: ""
            val imageUrl = notification.imageUrl?.toString()
            
            // Handle data payload
            val data = remoteMessage.data
            val notificationType = getNotificationType(data["type"])
            val channelId = getChannelId(notificationType)
            
            // Create notification ID
            val notificationId = UUID.randomUUID().toString()
            
            // Determine action type and data
            val (actionType, actionData) = determineAction(data, notificationType)
            
            // Save notification to Firestore
            saveNotification(
                notificationId = notificationId,
                userId = currentUser.uid,
                title = title,
                body = body,
                type = notificationType,
                data = data,
                actionType = actionType,
                actionData = actionData
            )
            
            // Special handling for calls
            if (notificationType == NotificationType.CALL && data["call_action"] == "incoming") {
                handleIncomingCall(data)
                return
            }
            
            // Create and show notification
            if (imageUrl != null) {
                // Notification with image
                loadImageAndShowNotification(
                    notificationId = notificationId.hashCode(),
                    title = title,
                    body = body,
                    imageUrl = imageUrl,
                    channelId = channelId,
                    data = data,
                    notificationType = notificationType
                )
            } else {
                // Simple notification
                showNotification(
                    notificationId = notificationId.hashCode(),
                    title = title,
                    body = body,
                    channelId = channelId,
                    data = data,
                    notificationType = notificationType
                )
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Update FCM token in Firestore for the current user
        val currentUser = auth.currentUser ?: return
        
        firestore.collection("users")
            .document(currentUser.uid)
            .update("fcmToken", token)
            .addOnFailureListener { e ->
                // Log failure but don't crash
                e.printStackTrace()
            }
    }
    
    private fun getNotificationType(type: String?): NotificationType {
        return when (type) {
            "message" -> NotificationType.MESSAGE
            "match" -> NotificationType.MATCH
            "call" -> NotificationType.CALL
            "payment_success", "payment_failed" -> NotificationType.PAYMENT
            "subscription_activated", "subscription_canceled", "subscription_expiring" -> NotificationType.SUBSCRIPTION
            "offer" -> NotificationType.OFFER
            else -> NotificationType.SYSTEM
        }
    }
    
    private fun getChannelId(type: NotificationType): String {
        return when (type) {
            NotificationType.MESSAGE, NotificationType.MATCH -> "messages"
            NotificationType.CALL -> "calls"
            NotificationType.PAYMENT -> "payments"
            NotificationType.SUBSCRIPTION -> "subscriptions"
            NotificationType.OFFER -> "offers"
            NotificationType.SYSTEM -> "messages" // Default
        }
    }
    
    private fun determineAction(
        data: Map<String, String>, 
        type: NotificationType
    ): Pair<NotificationActionType?, String?> {
        return when (type) {
            NotificationType.MESSAGE -> {
                val conversationId = data["conversation_id"]
                if (conversationId != null) {
                    Pair(NotificationActionType.OPEN_CONVERSATION, conversationId)
                } else {
                    Pair(null, null)
                }
            }
            NotificationType.MATCH -> {
                val profileId = data["profile_id"]
                if (profileId != null) {
                    Pair(NotificationActionType.OPEN_PROFILE, profileId)
                } else {
                    Pair(null, null)
                }
            }
            NotificationType.CALL -> {
                val callId = data["call_id"]
                if (callId != null) {
                    Pair(NotificationActionType.OPEN_CALL, callId)
                } else {
                    Pair(null, null)
                }
            }
            NotificationType.PAYMENT -> {
                val paymentId = data["payment_id"]
                if (paymentId != null) {
                    Pair(NotificationActionType.OPEN_PAYMENT, paymentId)
                } else {
                    Pair(NotificationActionType.OPEN_SUBSCRIPTION, null)
                }
            }
            NotificationType.SUBSCRIPTION -> {
                Pair(NotificationActionType.OPEN_SUBSCRIPTION, null)
            }
            NotificationType.OFFER -> {
                val offerId = data["offer_id"]
                if (offerId != null) {
                    Pair(NotificationActionType.OPEN_OFFER, offerId)
                } else {
                    Pair(null, null)
                }
            }
            NotificationType.SYSTEM -> {
                val activity = data["activity"]
                if (activity != null) {
                    Pair(NotificationActionType.CUSTOM_ACTIVITY, activity)
                } else {
                    Pair(null, null)
                }
            }
        }
    }
    
    private fun saveNotification(
        notificationId: String,
        userId: String,
        title: String,
        body: String,
        type: NotificationType,
        data: Map<String, String>,
        actionType: NotificationActionType?,
        actionData: String?
    ) {
        val notification = hashMapOf(
            "id" to notificationId,
            "userId" to userId,
            "title" to title,
            "body" to body,
            "timestamp" to Date(),
            "read" to false,
            "type" to type.name,
            "data" to data,
            "actionType" to (actionType?.name ?: null),
            "actionData" to actionData
        )
        
        firestore.collection("notifications")
            .document(notificationId)
            .set(notification)
            .addOnFailureListener { e ->
                // Log failure but don't crash
                e.printStackTrace()
            }
    }
    
    private fun handleIncomingCall(data: Map<String, String>) {
        val callId = data["call_id"] ?: return
        val callerId = data["caller_id"] ?: return
        val callerName = data["caller_name"] ?: "Unknown"
        val callerPhoto = data["caller_photo"]
        val callType = data["call_type"] ?: "audio" // audio or video
        
        // Launch incoming call activity
        val intent = Intent(this, IncomingCallActivity::class.java).apply {
            putExtra("call_id", callId)
            putExtra("caller_id", callerId)
            putExtra("caller_name", callerName)
            putExtra("caller_photo", callerPhoto)
            putExtra("call_type", callType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        startActivity(intent)
    }
    
    private fun showNotification(
        notificationId: Int,
        title: String,
        body: String,
        channelId: String,
        data: Map<String, String>,
        notificationType: NotificationType
    ) {
        // Create intent based on notification type
        val intent = createIntent(notificationType, data)
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = notificationManager.getNotificationChannel(channelId)
            if (channel == null) {
                val name = when (channelId) {
                    "messages" -> "Messages"
                    "calls" -> "Calls"
                    "payments" -> "Payments"
                    "subscriptions" -> "Subscriptions"
                    "offers" -> "Offers"
                    else -> "Notifications"
                }
                
                val description = "Notifications for $name"
                val importance = when (channelId) {
                    "calls" -> NotificationManager.IMPORTANCE_HIGH
                    "messages" -> NotificationManager.IMPORTANCE_HIGH
                    else -> NotificationManager.IMPORTANCE_DEFAULT
                }
                
                val newChannel = NotificationChannel(channelId, name, importance).apply {
                    this.description = description
                }
                
                notificationManager.createNotificationChannel(newChannel)
            }
        }
        
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    
    private fun loadImageAndShowNotification(
        notificationId: Int,
        title: String,
        body: String,
        imageUrl: String,
        channelId: String,
        data: Map<String, String>,
        notificationType: NotificationType
    ) {
        // Load image with Glide
        Handler(Looper.getMainLooper()).post {
            Glide.with(applicationContext)
                .asBitmap()
                .load(imageUrl)
                .into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        // Create a big picture style notification with the loaded image
                        val bigPictureStyle = NotificationCompat.BigPictureStyle()
                            .bigPicture(resource)
                            .setBigContentTitle(title)
                            .setSummaryText(body)
                        
                        // Create intent based on notification type
                        val intent = createIntent(notificationType, data)
                        
                        val pendingIntent = PendingIntent.getActivity(
                            applicationContext,
                            notificationId,
                            intent,
                            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                        )
                        
                        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        
                        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(title)
                            .setContentText(body)
                            .setAutoCancel(true)
                            .setSound(defaultSoundUri)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                            .setStyle(bigPictureStyle)
                            .setContentIntent(pendingIntent)
                        
                        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(notificationId, notificationBuilder.build())
                    }
                    
                    override fun onLoadCleared(placeholder: Drawable?) {
                        // Image loading was cleared, show standard notification instead
                        showNotification(
                            notificationId = notificationId,
                            title = title,
                            body = body,
                            channelId = channelId,
                            data = data,
                            notificationType = notificationType
                        )
                    }
                    
                    override fun onLoadFailed(errorDrawable: Drawable?) {
                        // Image loading failed, show standard notification instead
                        showNotification(
                            notificationId = notificationId,
                            title = title,
                            body = body,
                            channelId = channelId,
                            data = data,
                            notificationType = notificationType
                        )
                    }
                })
        }
    }
    
    private fun createIntent(
        notificationType: NotificationType,
        data: Map<String, String>
    ): Intent {
        // Default intent opens the notifications screen
        val intent = Intent(this, NotificationsActivity::class.java)
        
        when (notificationType) {
            NotificationType.MESSAGE -> {
                val conversationId = data["conversation_id"]
                
                if (conversationId != null) {
                    // Navigate to specific conversation
                    try {
                        // Try to determine if it's a regular or AI conversation
                        if (data["ai_profile_id"] != null) {
                            // AI chat
                            intent.setClassName(this, "com.kilagee.onelove.ui.ai.AIChatActivity")
                            intent.putExtra("conversation_id", conversationId)
                        } else {
                            // Regular chat
                            intent.setClassName(this, "com.kilagee.onelove.ui.chat.ChatActivity")
                            intent.putExtra("conversation_id", conversationId)
                        }
                    } catch (e: Exception) {
                        // Fall back to notifications activity
                        intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                    }
                }
            }
            NotificationType.MATCH -> {
                val profileId = data["profile_id"]
                
                if (profileId != null) {
                    // Navigate to profile
                    try {
                        intent.setClassName(this, "com.kilagee.onelove.ui.profile.ProfileActivity")
                        intent.putExtra("profile_id", profileId)
                    } catch (e: Exception) {
                        // Fall back to notifications activity
                        intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                    }
                }
            }
            NotificationType.CALL -> {
                // Handled specially in handleIncomingCall
                // This is for missed calls or other call notifications
                val callId = data["call_id"]
                
                if (callId != null) {
                    try {
                        intent.setClassName(this, "com.kilagee.onelove.ui.calls.CallHistoryActivity")
                    } catch (e: Exception) {
                        // Fall back to notifications activity
                        intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                    }
                }
            }
            NotificationType.PAYMENT -> {
                val paymentId = data["payment_id"]
                
                if (paymentId != null) {
                    try {
                        intent.setClassName(this, "com.kilagee.onelove.ui.payment.PaymentDetailsActivity")
                        intent.putExtra("payment_id", paymentId)
                    } catch (e: Exception) {
                        try {
                            // Fall back to subscription activity
                            intent.setClassName(this, "com.kilagee.onelove.ui.subscription.MyMembershipActivity")
                        } catch (e2: Exception) {
                            // Fall back to notifications activity
                            intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                        }
                    }
                } else {
                    try {
                        // Navigate to subscription screen
                        intent.setClassName(this, "com.kilagee.onelove.ui.subscription.MyMembershipActivity")
                    } catch (e: Exception) {
                        // Fall back to notifications activity
                        intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                    }
                }
            }
            NotificationType.SUBSCRIPTION -> {
                try {
                    // Navigate to subscription screen
                    intent.setClassName(this, "com.kilagee.onelove.ui.subscription.MyMembershipActivity")
                } catch (e: Exception) {
                    // Fall back to notifications activity
                    intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                }
            }
            NotificationType.OFFER -> {
                val offerId = data["offer_id"]
                
                if (offerId != null) {
                    try {
                        intent.setClassName(this, "com.kilagee.onelove.ui.offers.OfferDetailsActivity")
                        intent.putExtra("offer_id", offerId)
                    } catch (e: Exception) {
                        // Fall back to notifications activity
                        intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
                    }
                }
            }
            NotificationType.SYSTEM -> {
                // Default to notifications activity
                intent.setClassName(this, "com.kilagee.onelove.ui.notifications.NotificationsActivity")
            }
        }
        
        // Pass notification ID so it can be marked as read
        intent.putExtra("notification_id", data["notification_id"])
        
        return intent
    }
}