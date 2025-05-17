package com.kilagee.onelove.ui.settings

import androidx.compose.runtime.Composable
import com.kilagee.onelove.ui.webview.WebViewScreen

/**
 * Screen for displaying the app's privacy policy
 */
@Composable
fun PrivacyPolicyScreen(
    navigateBack: () -> Unit
) {
    // Link to the OneLove dating app privacy policy
    val privacyPolicyUrl = "https://onelove.kilagee.com/privacy-policy"
    
    WebViewScreen(
        url = privacyPolicyUrl,
        navigateBack = navigateBack
    )
}