package com.kilagee.onelove.ui.settings

import androidx.compose.runtime.Composable
import com.kilagee.onelove.ui.webview.WebViewScreen

/**
 * Screen for displaying the app's terms of service
 */
@Composable
fun TermsOfServiceScreen(
    navigateBack: () -> Unit
) {
    // Link to the OneLove dating app terms of service
    val termsOfServiceUrl = "https://onelove.kilagee.com/terms-of-service"
    
    WebViewScreen(
        url = termsOfServiceUrl,
        navigateBack = navigateBack
    )
}