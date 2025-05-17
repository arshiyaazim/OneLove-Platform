package com.kilagee.onelove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Available screens in the app
 */
sealed class Screen(val route: String) {
    // Authentication screens
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    
    // Main tabs
    object Home : Screen("home")
    object Matches : Screen("matches")
    object Offers : Screen("offers")
    object AiChat : Screen("ai_chat")
    object Videos : Screen("videos")
    object Profile : Screen("profile")
    object Settings : Screen("settings")
    
    // Feature screens
    object Chat : Screen("chat/{userId}") {
        fun createRoute(userId: String) = "chat/$userId"
    }
    object VideoCall : Screen("video_call/{userId}") {
        fun createRoute(userId: String) = "video_call/$userId"
    }
    object AudioCall : Screen("audio_call/{userId}") {
        fun createRoute(userId: String) = "audio_call/$userId"
    }
    object UserDetail : Screen("user_detail/{userId}") {
        fun createRoute(userId: String) = "user_detail/$userId"
    }
    object EditProfile : Screen("edit_profile")
    object Verification : Screen("verification")
    object Subscriptions : Screen("subscriptions")
    object Wallet : Screen("wallet")
    object Rewards : Screen("rewards")
    object Help : Screen("help")
    
    // Admin screens
    object AdminDashboard : Screen("admin/dashboard")
    object AdminUsers : Screen("admin/users")
    object AdminVerifications : Screen("admin/verifications")
    object AdminContent : Screen("admin/content")
    object AdminAnalytics : Screen("admin/analytics")
    object AdminSettings : Screen("admin/settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication routes
        composable(route = Screen.Login.route) {
            // LoginScreen(navController = navController)
        }
        composable(route = Screen.Register.route) {
            // RegisterScreen(navController = navController)
        }
        composable(route = Screen.ForgotPassword.route) {
            // ForgotPasswordScreen(navController = navController)
        }
        
        // Main tab routes
        composable(route = Screen.Home.route) {
            // HomeScreen(navController = navController)
        }
        composable(route = Screen.Matches.route) {
            // MatchesScreen(navController = navController)
        }
        composable(route = Screen.Offers.route) {
            // OffersScreen(navController = navController)
        }
        composable(route = Screen.AiChat.route) {
            // AiChatScreen(navController = navController)
        }
        composable(route = Screen.Videos.route) {
            // VideosScreen(navController = navController)
        }
        composable(route = Screen.Profile.route) {
            // ProfileScreen(navController = navController)
        }
        composable(route = Screen.Settings.route) {
            // SettingsScreen(navController = navController)
        }
        
        // Feature screens
        composable(route = Screen.Chat.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // ChatScreen(userId = userId, navController = navController)
        }
        composable(route = Screen.VideoCall.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // VideoCallScreen(userId = userId, navController = navController)
        }
        composable(route = Screen.AudioCall.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // AudioCallScreen(userId = userId, navController = navController)
        }
        composable(route = Screen.UserDetail.route) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            // UserDetailScreen(userId = userId, navController = navController)
        }
        composable(route = Screen.EditProfile.route) {
            // EditProfileScreen(navController = navController)
        }
        composable(route = Screen.Verification.route) {
            // VerificationScreen(navController = navController)
        }
        composable(route = Screen.Subscriptions.route) {
            // SubscriptionsScreen(navController = navController)
        }
        composable(route = Screen.Wallet.route) {
            // WalletScreen(navController = navController)
        }
        composable(route = Screen.Rewards.route) {
            // RewardsScreen(navController = navController)
        }
        composable(route = Screen.Help.route) {
            // HelpScreen(navController = navController)
        }
        
        // Admin screens
        composable(route = Screen.AdminDashboard.route) {
            // AdminDashboardScreen(navController = navController)
        }
        composable(route = Screen.AdminUsers.route) {
            // AdminUsersScreen(navController = navController)
        }
        composable(route = Screen.AdminVerifications.route) {
            // AdminVerificationsScreen(navController = navController)
        }
        composable(route = Screen.AdminContent.route) {
            // AdminContentScreen(navController = navController)
        }
        composable(route = Screen.AdminAnalytics.route) {
            // AdminAnalyticsScreen(navController = navController)
        }
        composable(route = Screen.AdminSettings.route) {
            // AdminSettingsScreen(navController = navController)
        }
    }
}