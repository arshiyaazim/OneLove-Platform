package com.kilagee.onelove.ui.navigation

/**
 * Screen routes for navigation
 */
sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object Discover : Screen("discover")
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{conversationId}") {
        fun createRoute(conversationId: String) = "chat/$conversationId"
    }
    object AIProfiles : Screen("ai_profiles")
    object AIChat : Screen("ai_chat/{profileId}") {
        fun createRoute(profileId: String) = "ai_chat/$profileId"
    }
    object Profile : Screen("profile")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object Subscription : Screen("subscription")
    object MyMembership : Screen("my_membership")
    object AddPaymentMethod : Screen("add_payment_method")
    object Offers : Screen("offers")
    object OfferDetail : Screen("offer_detail/{offerId}") {
        fun createRoute(offerId: String) = "offer_detail/$offerId"
    }
    object Notifications : Screen("notifications")
    object VideoCall : Screen("video_call/{callId}") {
        fun createRoute(callId: String) = "video_call/$callId"
    }
    object IncomingCall : Screen("incoming_call/{callId}") {
        fun createRoute(callId: String) = "incoming_call/$callId"
    }
    object PointsStore : Screen("points_store")
    object Visitors : Screen("visitors")
    object ProfileBoost : Screen("profile_boost")
    object WebView : Screen("webview/{url}") {
        fun createRoute(url: String) = "webview/$url"
    }
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService : Screen("terms_of_service")
    object HelpCenter : Screen("help_center")
}