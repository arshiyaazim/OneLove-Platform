package com.kilagee.onelove.domain.util

import com.kilagee.onelove.data.model.Notification
import com.kilagee.onelove.data.model.NotificationType
import com.kilagee.onelove.data.model.User
import java.util.Date

/**
 * Utility class that prioritizes notifications based on context and user preferences.
 * Implements a smart algorithm to determine which notifications should be shown to the user
 * and in what order.
 */
class NotificationPrioritizer {
    
    /**
     * Prioritize notifications based on type, content, time, and user context
     * 
     * @param notifications List of notifications to prioritize
     * @param currentUser The current user
     * @param userPreferences User notification preferences
     * @param currentTime Current time (for testing)
     * @return Prioritized list of notifications
     */
    fun prioritizeNotifications(
        notifications: List<Notification>,
        currentUser: User,
        userPreferences: Map<NotificationType, Boolean>,
        currentTime: Date = Date()
    ): List<Notification> {
        // Filter out notifications based on user preferences
        val filteredNotifications = notifications.filter { notification ->
            userPreferences[notification.type] ?: true
        }
        
        // Score each notification based on multiple factors
        val scoredNotifications = filteredNotifications.map { notification ->
            val score = calculateNotificationScore(
                notification = notification,
                currentUser = currentUser,
                currentTime = currentTime
            )
            Pair(notification, score)
        }
        
        // Return notifications sorted by score (higher score = higher priority)
        return scoredNotifications
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    /**
     * Calculate a priority score for a notification based on multiple factors
     * 
     * @param notification The notification to score
     * @param currentUser The current user
     * @param currentTime Current time
     * @return Priority score (higher is more important)
     */
    private fun calculateNotificationScore(
        notification: Notification,
        currentUser: User,
        currentTime: Date
    ): Double {
        var score = 0.0
        
        // Base score based on notification type
        score += when (notification.type) {
            NotificationType.MATCH -> 100.0
            NotificationType.MESSAGE -> 90.0
            NotificationType.CALL_MISSED -> 85.0
            NotificationType.VERIFICATION_APPROVED -> 80.0
            NotificationType.SUBSCRIPTION_EXPIRING -> 75.0
            NotificationType.PAYMENT_SUCCESS -> 70.0
            NotificationType.PAYMENT_FAILED -> 95.0
            NotificationType.PROFILE_VIEW -> 60.0
            NotificationType.APP_UPDATE -> 50.0
            NotificationType.SYSTEM -> 40.0
        }
        
        // Recency factor: newer notifications get higher priority
        val ageInHours = (currentTime.time - notification.createdAt.toDate().time) / (1000 * 60 * 60.0)
        val recencyScore = Math.max(0.0, 100.0 - (ageInHours * 2)) // Degrades over time
        score += recencyScore
        
        // User engagement factor: if the notification is from someone the user interacts with frequently
        if (notification.relatedUserId != null) {
            val interactionScore = calculateInteractionScore(currentUser, notification.relatedUserId)
            score += interactionScore
        }
        
        // Content relevance factor: certain keywords or content types may be more important
        if (notification.content.contains("premium") || 
            notification.content.contains("subscription") ||
            notification.content.contains("verified")) {
            score += 20.0
        }
        
        // Context-aware factor: prioritize differently based on user's subscription status
        if (currentUser.subscriptionTier != null && notification.type == NotificationType.MATCH) {
            score += 10.0 // Premium users might care more about matches
        }
        
        return score
    }
    
    /**
     * Calculate an interaction score based on how frequently the user interacts with another user
     * 
     * @param currentUser The current user
     * @param otherUserId ID of the other user
     * @return Interaction score
     */
    private fun calculateInteractionScore(currentUser: User, otherUserId: String): Double {
        // In a real implementation, we would look at factors like:
        // - How many messages they've exchanged
        // - How recently they've interacted
        // - Whether they're matched
        // - Call history
        // For now, return a placeholder value
        return 25.0
    }
    
    /**
     * Determine if a notification should be shown immediately as a high-priority notification
     * 
     * @param notification The notification to check
     * @param currentUser The current user
     * @return True if the notification is high priority
     */
    fun isHighPriorityNotification(notification: Notification, currentUser: User): Boolean {
        // Messages from matched users are high priority
        if (notification.type == NotificationType.MESSAGE && 
            currentUser.matchedUserIds.contains(notification.relatedUserId)) {
            return true
        }
        
        // Missed calls are high priority
        if (notification.type == NotificationType.CALL_MISSED) {
            return true
        }
        
        // Payment failures are high priority
        if (notification.type == NotificationType.PAYMENT_FAILED) {
            return true
        }
        
        // New matches are high priority for non-premium users (who get fewer matches)
        if (notification.type == NotificationType.MATCH && currentUser.subscriptionTier == null) {
            return true
        }
        
        return false
    }
}