package com.kilagee.onelove.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kilagee.onelove.OneLoveApplication.Companion.CHAT_NOTIFICATION_CHANNEL_ID
import com.kilagee.onelove.OneLoveApplication.Companion.GENERAL_NOTIFICATION_CHANNEL_ID
import com.kilagee.onelove.OneLoveApplication.Companion.MATCHES_NOTIFICATION_CHANNEL_ID
import com.kilagee.onelove.OneLoveApplication.Companion.OFFERS_NOTIFICATION_CHANNEL_ID
import com.kilagee.onelove.OneLoveApplication.Companion.PAYMENTS_NOTIFICATION_CHANNEL_ID
import com.kilagee.onelove.R
import com.kilagee.onelove.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class OneloveFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var firestore: FirebaseFirestore

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.d("From: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        remoteMessage.data.isNotEmpty().let {
            Timber.d("Message data payload: ${remoteMessage.data}")
            
            val type = remoteMessage.data["type"]
            val title = remoteMessage.data["title"] ?: getString(R.string.app_name)
            val message = remoteMessage.data["message"] ?: ""
            val senderId = remoteMessage.data["senderId"]
            
            // Handle different notification types
            when (type) {
                "chat" -> handleChatNotification(title, message, senderId)
                "match" -> handleMatchNotification(title, message, senderId)
                "offer" -> handleOfferNotification(title, message, senderId)
                "payment" -> handlePaymentNotification(title, message)
                else -> handleDefaultNotification(title, message)
            }
        }
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: ${it.body}")
            it.body?.let { body -> 
                handleDefaultNotification(it.title ?: getString(R.string.app_name), body)
            }
        }
    }
    
    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")
        sendRegistrationToServer(token)
    }
    
    private fun sendRegistrationToServer(token: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            scope.launch {
                try {
                    val userDocRef = firestore.collection("users").document(currentUser.uid)
                    userDocRef.update("fcmToken", token)
                        .addOnSuccessListener { Timber.d("FCM Token updated successfully") }
                        .addOnFailureListener { e -> Timber.e(e, "Failed to update FCM token") }
                } catch (e: Exception) {
                    Timber.e(e, "Error updating FCM token")
                }
            }
        }
    }
    
    private fun handleChatNotification(title: String, message: String, senderId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigateTo", "chat")
            senderId?.let { putExtra("userId", it) }
        }
        
        showNotification(
            CHAT_NOTIFICATION_CHANNEL_ID,
            System.currentTimeMillis().toInt(), // Unique ID based on time
            title,
            message,
            intent
        )
    }
    
    private fun handleMatchNotification(title: String, message: String, matchId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigateTo", "matches")
            matchId?.let { putExtra("matchId", it) }
        }
        
        showNotification(
            MATCHES_NOTIFICATION_CHANNEL_ID,
            System.currentTimeMillis().toInt(), // Unique ID based on time
            title,
            message,
            intent
        )
    }
    
    private fun handleOfferNotification(title: String, message: String, offerId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigateTo", "offers")
            offerId?.let { putExtra("offerId", it) }
        }
        
        showNotification(
            OFFERS_NOTIFICATION_CHANNEL_ID,
            System.currentTimeMillis().toInt(), // Unique ID based on time
            title,
            message,
            intent
        )
    }
    
    private fun handlePaymentNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("navigateTo", "wallet")
        }
        
        showNotification(
            PAYMENTS_NOTIFICATION_CHANNEL_ID,
            System.currentTimeMillis().toInt(), // Unique ID based on time
            title,
            message,
            intent
        )
    }
    
    private fun handleDefaultNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        
        showNotification(
            GENERAL_NOTIFICATION_CHANNEL_ID,
            System.currentTimeMillis().toInt(), // Unique ID based on time
            title,
            message,
            intent
        )
    }
    
    private fun showNotification(
        channelId: String,
        notificationId: Int,
        title: String,
        message: String,
        intent: Intent
    ) {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}