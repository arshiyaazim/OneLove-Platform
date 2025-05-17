package com.kilagee.onelove.ui.settings

import androidx.compose.runtime.Composable
import com.kilagee.onelove.ui.webview.WebViewScreen

/**
 * Screen for displaying the app's help center
 */
@Composable
fun HelpCenterScreen(
    navigateBack: () -> Unit
) {
    // Link to the OneLove dating app help center
    val helpCenterUrl = "https://onelove.kilagee.com/help-center"
    
    WebViewScreen(
        url = helpCenterUrl,
        navigateBack = navigateBack
    )
}