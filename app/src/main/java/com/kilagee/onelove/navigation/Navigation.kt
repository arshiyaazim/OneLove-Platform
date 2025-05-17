package com.kilagee.onelove.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kilagee.onelove.ui.authentication.ForgotPasswordScreen
import com.kilagee.onelove.ui.authentication.LoginScreen
import com.kilagee.onelove.ui.authentication.RegisterScreen
import com.kilagee.onelove.ui.chat.ChatDetailScreen
import com.kilagee.onelove.ui.chat.ChatScreen
import com.kilagee.onelove.ui.home.HomeScreen
import com.kilagee.onelove.ui.matching.MatchScreen
import com.kilagee.onelove.ui.offers.CreateOfferScreen
import com.kilagee.onelove.ui.offers.OfferDetailScreen
import com.kilagee.onelove.ui.offers.OffersScreen
import com.kilagee.onelove.ui.profile.EditProfileScreen
import com.kilagee.onelove.ui.profile.ProfileScreen
import com.kilagee.onelove.ui.profile.VerificationScreen
import com.kilagee.onelove.ui.settings.SettingsScreen
import com.kilagee.onelove.ui.settings.UserPreferencesScreen
import com.kilagee.onelove.ui.wallet.WalletScreen

@Composable
fun Navigation(
    navController: NavHostController,
    startDestination: String = Screen.Login.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Authentication routes
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        
        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }
        
        // Main app routes
        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }
        
        composable(Screen.Verification.route) {
            VerificationScreen(navController = navController)
        }
        
        // Chat routes
        composable(Screen.Chat.route) {
            ChatScreen(navController = navController)
        }
        
        composable(
            route = Screen.ChatDetail.route,
            arguments = listOf(
                navArgument("chatId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatDetailScreen(
                navController = navController,
                chatId = chatId
            )
        }
        
        // Offers routes
        composable(Screen.Offers.route) {
            OffersScreen(navController = navController)
        }
        
        composable(Screen.CreateOffer.route) {
            CreateOfferScreen(navController = navController)
        }
        
        composable(
            route = Screen.OfferDetail.route,
            arguments = listOf(
                navArgument("offerId") {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val offerId = backStackEntry.arguments?.getString("offerId") ?: ""
            OfferDetailScreen(
                navController = navController,
                offerId = offerId
            )
        }
        
        // Matching route
        composable(Screen.Matches.route) {
            MatchScreen(navController = navController)
        }
        
        // Wallet route
        composable(Screen.Wallet.route) {
            WalletScreen(navController = navController)
        }
        
        // Settings route
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        
        // User Preferences route
        composable(Screen.UserPreferences.route) {
            UserPreferencesScreen(navController = navController)
        }
    }
}