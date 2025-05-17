package com.kilagee.onelove.navigation

/**
 * Navigation destinations for the app
 */
sealed class Screen(val route: String) {
    
    // Auth screens
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    
    // Main screens
    object Home : Screen("home")
    object Discover : Screen("discover")
    object Messages : Screen("messages")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    
    // User-related screens
    object UserProfile : Screen("user_profile/{userId}") {
        fun createRoute(userId: String) = "user_profile/$userId"
    }
    object EditProfile : Screen("edit_profile")
    object Preferences : Screen("preferences")
    object Contacts : Screen("contacts")
    
    // Match-related screens
    object MatchDetails : Screen("match_details/{matchId}") {
        fun createRoute(matchId: String) = "match_details/$matchId"
    }
    object Matches : Screen("matches")
    
    // Message-related screens
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    
    // Call-related screens
    object CallHistory : Screen("call_history")
    object IncomingCall : Screen("incoming_call/{callId}") {
        fun createRoute(callId: String) = "incoming_call/$callId"
    }
    object OngoingCall : Screen("ongoing_call/{callId}") {
        fun createRoute(callId: String) = "ongoing_call/$callId"
    }
    
    // Offer-related screens
    object Offers : Screen("offers")
    object OfferDetails : Screen("offer_details/{offerId}") {
        fun createRoute(offerId: String) = "offer_details/$offerId"
    }
    object CreateOffer : Screen("create_offer/{userId}") {
        fun createRoute(userId: String) = "create_offer/$userId"
    }
    
    // Notification-related screens
    object Notifications : Screen("notifications")
    
    // Payment-related screens
    object Wallet : Screen("wallet")
    object AddFunds : Screen("add_funds")
    object WithdrawFunds : Screen("withdraw_funds")
    object PaymentDetails : Screen("payment_details/{paymentId}") {
        fun createRoute(paymentId: String) = "payment_details/$paymentId"
    }
    
    // Verification-related screens
    object Verification : Screen("verification")
    object VerificationDetails : Screen("verification_details/{verificationId}") {
        fun createRoute(verificationId: String) = "verification_details/$verificationId"
    }
    
    // Other screens
    object Help : Screen("help")
    object About : Screen("about")
    object PrivacyPolicy : Screen("privacy_policy")
    object TermsOfService : Screen("terms_of_service")
}