package com.kilagee.onelove.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kilagee.onelove.OneLoveApplication.Companion.CHANNEL_CALLS
import com.kilagee.onelove.OneLoveApplication.Companion.CHANNEL_MATCHES
import com.kilagee.onelove.OneLoveApplication.Companion.CHANNEL_MESSAGES
import com.kilagee.onelove.OneLoveApplication.Companion.CHANNEL_OFFERS
import com.kilagee.onelove.OneLoveApplication.Companion.CHANNEL_SYSTEM
import com.kilagee.onelove.R
import com.kilagee.onelove.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger

class OneLoveMessagingService : FirebaseMessagingService() {
    
    private val notificationIdCounter = AtomicInteger(0)
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Log the message if appropriate
        remoteMessage.notification?.let { notification ->
            val title = notification.title ?: getString(R.string.app_name)
            val body = notification.body ?: ""
            sendNotification(title, body, remoteMessage.data)
        }
        
        // Handle data messages (even without notification)
        if (remoteMessage.data.isNotEmpty()) {
            // Process data payload
            handleDataMessage(remoteMessage.data)
        }
    }
    
    private fun handleDataMessage(data: Map<String, String>) {
        val type = data["type"] ?: return
        val title: String
        val body: String
        val channelId: String
        
        when (type) {
            "message" -> {
                val senderId = data["senderId"] ?: return
                val senderName = data["senderName"] ?: "Someone"
                val messageContent = data["message"] ?: ""
                
                title = senderName
                body = messageContent
                channelId = CHANNEL_MESSAGES
            }
            "match" -> {
                val matchedUserId = data["matchedUserId"] ?: return
                val matchedUserName = data["matchedUserName"] ?: "Someone"
                
                title = getString(R.string.its_a_match)
                body = getString(R.string.match_message, matchedUserName)
                channelId = CHANNEL_MATCHES
            }
            "call" -> {
                val callerId = data["callerId"] ?: return
                val callerName = data["callerName"] ?: "Someone"
                val callType = data["callType"] ?: "audio"
                
                title = getString(R.string.incoming_call)
                body = if (callType == "video") {
                    "$callerName is video calling you"
                } else {
                    "$callerName is audio calling you"
                }
                channelId = CHANNEL_CALLS
            }
            "offer" -> {
                val offerId = data["offerId"] ?: return
                val senderName = data["senderName"] ?: "Someone"
                val offerType = data["offerType"] ?: ""
                
                title = "New Offer from $senderName"
                body = "You received a new $offerType offer"
                channelId = CHANNEL_OFFERS
            }
            else -> {
                title = data["title"] ?: getString(R.string.app_name)
                body = data["body"] ?: ""
                channelId = CHANNEL_SYSTEM
            }
        }
        
        sendNotification(title, body, data, channelId)
    }
    
    private fun sendNotification(
        title: String,
        body: String,
        data: Map<String, String>,
        channelId: String = CHANNEL_SYSTEM
    ) {
        val notificationId = notificationIdCounter.incrementAndGet()
        
        // Intent to open the app (MainActivity)
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            
            // Add data as extras to handle specific routing in MainActivity
            data.entries.forEach { entry ->
                putExtra(entry.key, entry.value)
            }
        }
        
        val pendingIntentFlag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, notificationId, intent, pendingIntentFlag
        )
        
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_app_logo)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            
        // Special handling for calls - they need to be high priority with full-screen intent
        if (channelId == CHANNEL_CALLS) {
            notificationBuilder
                .setFullScreenIntent(pendingIntent, true)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
        }
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        
        // Send the new token to server for this user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            updateUserFcmToken(currentUser.uid, token)
        }
    }
    
    private fun updateUserFcmToken(userId: String, token: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val userRef = FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(userId)
                
                userRef.update("fcmToken", token)
                    .addOnSuccessListener {
                        // Token updated successfully
                    }
                    .addOnFailureListener { e ->
                        // Failed to update token
                    }
            } catch (e: Exception) {
                // Handle exceptions
            }
        }
    }
}