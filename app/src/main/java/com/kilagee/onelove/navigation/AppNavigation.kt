package com.kilagee.onelove.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kilagee.onelove.ui.authentication.LoginScreen
import com.kilagee.onelove.ui.authentication.RegisterScreen
import com.kilagee.onelove.ui.authentication.ForgotPasswordScreen
import com.kilagee.onelove.ui.home.HomeScreen
import com.kilagee.onelove.ui.chat.ChatScreen
import com.kilagee.onelove.ui.profile.ProfileScreen
import com.kilagee.onelove.ui.offers.OffersScreen
import com.kilagee.onelove.ui.wallet.WalletScreen
import com.kilagee.onelove.ui.settings.SettingsScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(route = Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        
        composable(route = Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(navController = navController)
        }
        
        composable(route = Screen.Home.route) {
            HomeScreen(navController = navController)
        }
        
        composable(route = Screen.Chat.route) {
            ChatScreen(navController = navController)
        }
        
        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        
        composable(route = Screen.Offers.route) {
            OffersScreen(navController = navController)
        }
        
        composable(route = Screen.Wallet.route) {
            WalletScreen(navController = navController)
        }
        
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
    }
}