package com.kilagee.onelove.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kilagee.onelove.R
import com.kilagee.onelove.ui.MainActivity
import timber.log.Timber

/**
 * OneLove Firebase Messaging Service for handling push notifications
 */
class OneLoveFirebaseMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Timber.d("From: ${remoteMessage.from}")
        
        // Check if message contains a data payload
        remoteMessage.data.isNotEmpty().let {
            Timber.d("Message data payload: ${remoteMessage.data}")
            
            // Handle the data payload
            val title = remoteMessage.data["title"] ?: "OneLove"
            val body = remoteMessage.data["body"] ?: "You have a new notification"
            val type = remoteMessage.data["type"] ?: "general"
            val targetId = remoteMessage.data["targetId"]
            
            sendNotification(title, body, type, targetId)
        }
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let {
            Timber.d("Message Notification Body: ${it.body}")
            
            sendNotification(
                it.title ?: "OneLove",
                it.body ?: "You have a new notification",
                "general",
                null
            )
        }
    }
    
    override fun onNewToken(token: String) {
        Timber.d("Refreshed token: $token")
        
        // Save the token to Firestore for this user
        // This would typically be handled by a repository
        sendRegistrationToServer(token)
    }
    
    /**
     * Send the token to your server (Firestore in this case)
     */
    private fun sendRegistrationToServer(token: String) {
        // TODO: Implement sending the token to the server via repository
    }
    
    /**
     * Create and show a notification with the given parameters
     */
    private fun sendNotification(
        title: String,
        messageBody: String,
        type: String,
        targetId: String?
    ) {
        val channelId = when (type) {
            "message" -> "messages"
            "match" -> "matches"
            "like" -> "likes"
            "call" -> "calls"
            else -> "general"
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("notification_type", type)
            if (targetId != null) {
                putExtra("target_id", targetId)
            }
        }
        
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
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channels for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = when (type) {
                "message" -> "Messages"
                "match" -> "Matches"
                "like" -> "Likes"
                "call" -> "Calls"
                else -> "General"
            }
            
            val channelDescription = "OneLove $channelName notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }
            
            notificationManager.createNotificationChannel(channel)
        }
        
        // Use a unique ID for each type of notification
        val notificationId = when (type) {
            "message" -> 1
            "match" -> 2
            "like" -> 3
            "call" -> 4
            else -> 0
        }
        
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}