package com.kilagee.onelove.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.kilagee.onelove.R

/**
 * Sealed class representing all navigation destinations in the app
 */
sealed class NavDestinations(
    val route: String,
    @StringRes val titleResId: Int? = null,
    @DrawableRes val iconResId: Int? = null,
    val showInBottomNav: Boolean = false
) {
    // Authentication Flow
    data object Login : NavDestinations(
        route = "login"
    )
    
    data object Register : NavDestinations(
        route = "register"
    )
    
    data object ForgotPassword : NavDestinations(
        route = "forgot_password"
    )
    
    data object PhoneVerification : NavDestinations(
        route = "phone_verification"
    )
    
    // Main Navigation
    data object Home : NavDestinations(
        route = "home",
        titleResId = R.string.nav_home,
        iconResId = R.drawable.ic_home,
        showInBottomNav = true
    )
    
    data object Matches : NavDestinations(
        route = "matches",
        titleResId = R.string.nav_matches,
        iconResId = R.drawable.ic_matches,
        showInBottomNav = true
    )
    
    data object Messages : NavDestinations(
        route = "messages",
        titleResId = R.string.nav_messages,
        iconResId = R.drawable.ic_messages,
        showInBottomNav = true
    )
    
    data object Profile : NavDestinations(
        route = "profile",
        titleResId = R.string.nav_profile,
        iconResId = R.drawable.ic_profile,
        showInBottomNav = true
    )
    
    // Detail Screens
    data object ChatDetail : NavDestinations(
        route = "chat_detail/{matchId}",
    ) {
        fun createRoute(matchId: String) = "chat_detail/$matchId"
    }
    
    data object UserDetail : NavDestinations(
        route = "user_detail/{userId}",
    ) {
        fun createRoute(userId: String) = "user_detail/$userId"
    }
    
    data object EditProfile : NavDestinations(
        route = "edit_profile"
    )
    
    data object Settings : NavDestinations(
        route = "settings"
    )
    
    data object MatchDetail : NavDestinations(
        route = "match_detail/{matchId}",
    ) {
        fun createRoute(matchId: String) = "match_detail/$matchId"
    }
    
    // Features
    data object AIProfiles : NavDestinations(
        route = "ai_profiles"
    )
    
    data object AIChat : NavDestinations(
        route = "ai_chat/{profileId}",
    ) {
        fun createRoute(profileId: String) = "ai_chat/$profileId"
    }
    
    data object Subscription : NavDestinations(
        route = "subscription"
    )
    
    data object Wallet : NavDestinations(
        route = "wallet"
    )
    
    data object Points : NavDestinations(
        route = "points"
    )
    
    data object CreateOffer : NavDestinations(
        route = "create_offer/{matchId}",
    ) {
        fun createRoute(matchId: String) = "create_offer/$matchId"
    }
    
    data object OfferDetail : NavDestinations(
        route = "offer_detail/{offerId}",
    ) {
        fun createRoute(offerId: String) = "offer_detail/$offerId"
    }
    
    data object Verification : NavDestinations(
        route = "verification"
    )
    
    data object Call : NavDestinations(
        route = "call/{callId}",
    ) {
        fun createRoute(callId: String) = "call/$callId"
    }
    
    // Admin Panel
    data object AdminPanel : NavDestinations(
        route = "admin_panel"
    )
    
    data object AdminUserManagement : NavDestinations(
        route = "admin_user_management"
    )
    
    data object AdminContentManagement : NavDestinations(
        route = "admin_content_management"
    )
    
    data object AdminVerificationManagement : NavDestinations(
        route = "admin_verification_management"
    )
    
    data object AdminReportManagement : NavDestinations(
        route = "admin_report_management"
    )
    
    data object AdminAIProfileManagement : NavDestinations(
        route = "admin_ai_profile_management"
    )
    
    data object AdminAnalytics : NavDestinations(
        route = "admin_analytics"
    )
    
    companion object {
        val bottomNavItems = listOf(
            Home,
            Matches,
            Messages,
            Profile
        )
    }
}