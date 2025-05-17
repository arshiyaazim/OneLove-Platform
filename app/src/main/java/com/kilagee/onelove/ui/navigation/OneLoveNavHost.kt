package com.kilagee.onelove.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.kilagee.onelove.ui.screens.auth.LoginScreen
import com.kilagee.onelove.ui.screens.auth.RegisterScreen
import com.kilagee.onelove.ui.screens.chat.ChatListScreen
import com.kilagee.onelove.ui.screens.chat.ChatScreen
import com.kilagee.onelove.ui.screens.calls.CallScreen
import com.kilagee.onelove.ui.screens.discover.DiscoverScreen
import com.kilagee.onelove.ui.screens.discover.ProfileDetailScreen
import com.kilagee.onelove.ui.screens.profile.EditProfileScreen
import com.kilagee.onelove.ui.screens.profile.MyProfileScreen
import com.kilagee.onelove.ui.screens.settings.SettingsScreen
import com.kilagee.onelove.ui.screens.subscription.PaymentActionScreen
import com.kilagee.onelove.ui.screens.subscription.SubscriptionScreen

/**
 * Sealed class for navigation routes
 */
sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Discover : Screen("discover")
    object ChatList : Screen("chat_list")
    object Chat : Screen("chat/{chatId}") {
        fun createRoute(chatId: String) = "chat/$chatId"
    }
    object Call : Screen("call/{callId}/{isVideo}") {
        fun createRoute(callId: String, isVideo: Boolean) = "call/$callId/$isVideo"
    }
    object ProfileDetail : Screen("profile/{userId}") {
        fun createRoute(userId: String) = "profile/$userId"
    }
    object MyProfile : Screen("my_profile")
    object EditProfile : Screen("edit_profile")
    object Settings : Screen("settings")
    object Subscription : Screen("subscription")
    object PaymentAction : Screen("payment_action/{actionUrl}/{subscriptionId}") {
        fun createRoute(actionUrl: String, subscriptionId: String): String {
            val encodedUrl = java.net.URLEncoder.encode(actionUrl, "UTF-8")
            return "payment_action/$encodedUrl/$subscriptionId"
        }
    }
}

/**
 * Main navigation component for the app
 */
@Composable
fun OneLoveNavHost(
    navController: NavHostController
) {
    val actions = remember(navController) { NavigationActions(navController) }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // Auth screens
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = actions.navigateToDiscover,
                onRegisterClick = actions.navigateToRegister,
                viewModel = hiltViewModel()
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = actions.navigateToDiscover,
                onLoginClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        // Main navigation screens
        composable(Screen.Discover.route) {
            DiscoverScreen(
                onProfileClick = actions.navigateToProfileDetail,
                onChatClick = actions.navigateToChatList,
                onMyProfileClick = actions.navigateToMyProfile,
                viewModel = hiltViewModel()
            )
        }
        
        // Chat screens
        composable(Screen.ChatList.route) {
            ChatListScreen(
                onChatClick = actions.navigateToChat,
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(
                chatId = chatId,
                onBackClick = { navController.popBackStack() },
                onCallClick = { callId, isVideo -> actions.navigateToCall(callId, isVideo) },
                onProfileClick = actions.navigateToProfileDetail,
                viewModel = hiltViewModel()
            )
        }
        
        // Call screen
        composable(
            route = Screen.Call.route,
            arguments = listOf(
                navArgument("callId") { type = NavType.StringType },
                navArgument("isVideo") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val isVideo = backStackEntry.arguments?.getBoolean("isVideo") ?: false
            CallScreen(
                callId = callId,
                isVideo = isVideo,
                onEndCall = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        // Profile screens
        composable(
            route = Screen.ProfileDetail.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileDetailScreen(
                userId = userId,
                onBackClick = { navController.popBackStack() },
                onChatClick = { chatId -> actions.navigateToChat(chatId) },
                onCallClick = { callId, isVideo -> actions.navigateToCall(callId, isVideo) },
                viewModel = hiltViewModel()
            )
        }
        
        composable(Screen.MyProfile.route) {
            MyProfileScreen(
                onEditProfileClick = actions.navigateToEditProfile,
                onSettingsClick = actions.navigateToSettings,
                onSubscriptionClick = actions.navigateToSubscription,
                onBackClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                onSaveClick = { navController.popBackStack() },
                onCancelClick = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
        
        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(
                onBackClick = { navController.popBackStack() },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                viewModel = hiltViewModel()
            )
        }
        
        // Subscription
        composable(Screen.Subscription.route) {
            SubscriptionScreen(
                navigateBack = { navController.popBackStack() },
                navigateToPayment = { actionUrl ->
                    val subscriptionId = "temp_id" // This would come from the purchase result
                    actions.navigateToPaymentAction(actionUrl, subscriptionId)
                },
                viewModel = hiltViewModel()
            )
        }
        
        // Payment action screen
        composable(
            route = Screen.PaymentAction.route,
            arguments = listOf(
                navArgument("actionUrl") { type = NavType.StringType },
                navArgument("subscriptionId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val actionUrl = backStackEntry.arguments?.getString("actionUrl") ?: ""
            val subscriptionId = backStackEntry.arguments?.getString("subscriptionId") ?: ""
            val decodedUrl = java.net.URLDecoder.decode(actionUrl, "UTF-8")
            
            PaymentActionScreen(
                actionUrl = decodedUrl,
                subscriptionId = subscriptionId,
                onComplete = { navController.popBackStack() },
                viewModel = hiltViewModel()
            )
        }
    }
}

/**
 * Helper class for navigation actions
 */
class NavigationActions(private val navController: NavHostController) {
    
    val navigateToLogin: () -> Unit = {
        navController.navigate(Screen.Login.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    
    val navigateToRegister: () -> Unit = {
        navController.navigate(Screen.Register.route)
    }
    
    val navigateToDiscover: () -> Unit = {
        navController.navigate(Screen.Discover.route) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }
    
    val navigateToChatList: () -> Unit = {
        navController.navigate(Screen.ChatList.route)
    }
    
    val navigateToChat: (String) -> Unit = { chatId ->
        navController.navigate(Screen.Chat.createRoute(chatId))
    }
    
    val navigateToCall: (String, Boolean) -> Unit = { callId, isVideo ->
        navController.navigate(Screen.Call.createRoute(callId, isVideo))
    }
    
    val navigateToProfileDetail: (String) -> Unit = { userId ->
        navController.navigate(Screen.ProfileDetail.createRoute(userId))
    }
    
    val navigateToMyProfile: () -> Unit = {
        navController.navigate(Screen.MyProfile.route)
    }
    
    val navigateToEditProfile: () -> Unit = {
        navController.navigate(Screen.EditProfile.route)
    }
    
    val navigateToSettings: () -> Unit = {
        navController.navigate(Screen.Settings.route)
    }
    
    val navigateToSubscription: () -> Unit = {
        navController.navigate(Screen.Subscription.route)
    }
    
    val navigateToPaymentAction: (String, String) -> Unit = { actionUrl, subscriptionId ->
        navController.navigate(Screen.PaymentAction.createRoute(actionUrl, subscriptionId))
    }
}