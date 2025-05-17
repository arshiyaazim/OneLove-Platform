package com.kilagee.onelove.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kilagee.onelove.R

/**
 * Main screen destinations in the app
 */
object OneLoveDestinations {
    // Authentication flow
    const val SPLASH_ROUTE = "splash"
    const val LOGIN_ROUTE = "login"
    const val REGISTER_ROUTE = "register"
    const val FORGOT_PASSWORD_ROUTE = "forgot_password"
    
    // Main app screens
    const val DISCOVER_ROUTE = "discover"
    const val MATCHES_ROUTE = "matches"
    const val CHAT_ROUTE = "chat"
    const val CHAT_DETAIL_ROUTE = "chat/{chatId}"
    const val PROFILE_ROUTE = "profile"
    const val EDIT_PROFILE_ROUTE = "edit_profile"
    const val NOTIFICATIONS_ROUTE = "notifications"
    const val SETTINGS_ROUTE = "settings"
    
    // Feature screens
    const val VERIFICATION_ROUTE = "verification"
    const val AI_CHAT_ROUTE = "ai_chat"
    const val SUBSCRIPTION_ROUTE = "subscription"
    const val PAYMENT_ROUTE = "payment"
    const val VIDEO_CALL_ROUTE = "video_call/{userId}"
    const val AUDIO_CALL_ROUTE = "audio_call/{userId}"
    
    // Profile viewing
    const val VIEW_PROFILE_ROUTE = "view_profile/{userId}"
    
    // Admin screens
    const val ADMIN_DASHBOARD_ROUTE = "admin/dashboard"
    const val ADMIN_USERS_ROUTE = "admin/users"
    const val ADMIN_VERIFICATIONS_ROUTE = "admin/verifications"
    const val ADMIN_REPORTS_ROUTE = "admin/reports"
    const val ADMIN_ANALYTICS_ROUTE = "admin/analytics"
    
    // Chat argument
    const val CHAT_ID = "chatId"
    
    // User argument
    const val USER_ID = "userId"
}

/**
 * Bottom navigation destinations
 */
enum class BottomNavItem(
    @StringRes val titleRes: Int,
    @DrawableRes val iconRes: Int,
    val route: String
) {
    DISCOVER(R.string.discover, R.drawable.ic_discover, OneLoveDestinations.DISCOVER_ROUTE),
    MATCHES(R.string.matches, R.drawable.ic_matches, OneLoveDestinations.MATCHES_ROUTE),
    CHAT(R.string.chat, R.drawable.ic_chat, OneLoveDestinations.CHAT_ROUTE),
    PROFILE(R.string.profile, R.drawable.ic_profile, OneLoveDestinations.PROFILE_ROUTE)
}

/**
 * Authentication screens implementation of NavigationDestination
 */
object LoginDestination : NavigationDestination {
    override val route = OneLoveDestinations.LOGIN_ROUTE
    override val titleRes = R.string.login
}

object RegisterDestination : NavigationDestination {
    override val route = OneLoveDestinations.REGISTER_ROUTE
    override val titleRes = R.string.register
}

object ForgotPasswordDestination : NavigationDestination {
    override val route = OneLoveDestinations.FORGOT_PASSWORD_ROUTE
    override val titleRes = R.string.reset_password
}

/**
 * Main app screens implementation of NavigationDestination
 */
object DiscoverDestination : NavigationDestination {
    override val route = OneLoveDestinations.DISCOVER_ROUTE
    override val titleRes = R.string.discover
    override val iconRes = R.drawable.ic_discover
}

object MatchesDestination : NavigationDestination {
    override val route = OneLoveDestinations.MATCHES_ROUTE
    override val titleRes = R.string.matches
    override val iconRes = R.drawable.ic_matches
}

object ChatDestination : NavigationDestination {
    override val route = OneLoveDestinations.CHAT_ROUTE
    override val titleRes = R.string.chats
    override val iconRes = R.drawable.ic_chat
}

object ChatDetailDestination : NavigationDestination {
    override val route = OneLoveDestinations.CHAT_DETAIL_ROUTE
    override val titleRes = R.string.chat
    
    fun createRoute(chatId: String): String {
        return "chat/$chatId"
    }
}

object ProfileDestination : NavigationDestination {
    override val route = OneLoveDestinations.PROFILE_ROUTE
    override val titleRes = R.string.profile
    override val iconRes = R.drawable.ic_profile
}

object EditProfileDestination : NavigationDestination {
    override val route = OneLoveDestinations.EDIT_PROFILE_ROUTE
    override val titleRes = R.string.edit_profile
}

object NotificationsDestination : NavigationDestination {
    override val route = OneLoveDestinations.NOTIFICATIONS_ROUTE
    override val titleRes = R.string.notifications
}

object SettingsDestination : NavigationDestination {
    override val route = OneLoveDestinations.SETTINGS_ROUTE
    override val titleRes = R.string.settings
}

/**
 * Feature screens implementation of NavigationDestination
 */
object VerificationDestination : NavigationDestination {
    override val route = OneLoveDestinations.VERIFICATION_ROUTE
    override val titleRes = R.string.verification
}

object AiChatDestination : NavigationDestination {
    override val route = OneLoveDestinations.AI_CHAT_ROUTE
    override val titleRes = R.string.ai_chat
}

object SubscriptionDestination : NavigationDestination {
    override val route = OneLoveDestinations.SUBSCRIPTION_ROUTE
    override val titleRes = R.string.subscription
}

object PaymentDestination : NavigationDestination {
    override val route = OneLoveDestinations.PAYMENT_ROUTE
    override val titleRes = R.string.payment_methods
}

object VideoCallDestination : NavigationDestination {
    override val route = OneLoveDestinations.VIDEO_CALL_ROUTE
    override val titleRes = R.string.video_call
    
    fun createRoute(userId: String): String {
        return "video_call/$userId"
    }
}

object AudioCallDestination : NavigationDestination {
    override val route = OneLoveDestinations.AUDIO_CALL_ROUTE
    override val titleRes = R.string.audio_call
    
    fun createRoute(userId: String): String {
        return "audio_call/$userId"
    }
}

object ViewProfileDestination : NavigationDestination {
    override val route = OneLoveDestinations.VIEW_PROFILE_ROUTE
    override val titleRes = R.string.profile
    
    fun createRoute(userId: String): String {
        return "view_profile/$userId"
    }
}