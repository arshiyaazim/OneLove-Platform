package com.kilagee.onelove.ui.navigation

/**
 * Navigation routes for the app
 */
object Routes {
    // Auth routes
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot_password"
    const val PROFILE_SETUP = "profile_setup"
    
    // Main routes
    const val DISCOVER = "discover"
    const val MATCHES = "matches"
    const val CHAT = "chat"
    const val CHAT_DETAIL = "chat/{matchId}"
    const val PROFILE = "profile"
    const val OTHER_PROFILE = "profile/{userId}"
    const val PROFILE_EDIT = "profile/edit"
    const val SETTINGS = "settings"
    const val NOTIFICATIONS = "notifications"
    
    // Subscription routes
    const val SUBSCRIPTION = "subscription"
    const val SUBSCRIPTION_PLANS = "subscription/plans"
    const val PAYMENT_METHODS = "payment/methods"
    const val ADD_PAYMENT_METHOD = "payment/add"
    
    // Call routes
    const val AUDIO_CALL = "call/audio"
    const val AUDIO_CALL_WITH_USER = "call/audio/{userId}"
    const val VIDEO_CALL = "call/video"
    const val VIDEO_CALL_WITH_USER = "call/video/{userId}"
    
    // Admin routes
    const val ADMIN = "admin"
    const val ADMIN_USERS = "admin/users"
    const val ADMIN_REPORTS = "admin/reports"
    const val ADMIN_VERIFICATIONS = "admin/verifications"
    const val ADMIN_ANALYTICS = "admin/analytics"
    const val ADMIN_SETTINGS = "admin/settings"
}