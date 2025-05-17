package com.kilagee.onelove.ui.screens.subscription

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.kilagee.onelove.ui.components.OneLoveTopBar
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Screen for handling Stripe payment actions
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaymentActionScreen(
    actionUrl: String,
    subscriptionId: String,
    onComplete: () -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val isProcessing by viewModel.isProcessing.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    var isLoading by remember { mutableStateOf(true) }
    
    // Show error in snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            scope.launch {
                snackbarHostState.showSnackbar(it)
                viewModel.clearErrorMessage()
            }
        }
    }
    
    Scaffold(
        topBar = {
            OneLoveTopBar(
                title = "Complete Payment",
                onBackClick = onComplete
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView for Stripe payment
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString()
                                
                                if (url != null) {
                                    Timber.d("PaymentAction URL: $url")
                                    
                                    // Check for success or failure redirects
                                    if (url.contains("payment_intent_success") || 
                                        url.contains("setup_intent_success") ||
                                        url.contains("success")
                                    ) {
                                        // Extract the client secret from the URL
                                        val clientSecret = extractClientSecret(url)
                                        if (clientSecret != null) {
                                            viewModel.completePaymentAction(clientSecret, subscriptionId)
                                            onComplete()
                                            return true
                                        }
                                    } else if (url.contains("payment_intent_cancel") || 
                                            url.contains("setup_intent_cancel") ||
                                            url.contains("canceled") ||
                                            url.contains("cancelled") ||
                                            url.contains("failure") ||
                                            url.contains("failed")
                                    ) {
                                        onComplete()
                                        return true
                                    }
                                }
                                return false
                            }
                            
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }
                        }
                        loadUrl(actionUrl)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Loading indicator
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Processing indicator
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

/**
 * Extract client secret from the URL
 */
private fun extractClientSecret(url: String): String? {
    val regex = "(payment_intent|setup_intent)_client_secret=([^&]*)".toRegex()
    val matchResult = regex.find(url)
    return matchResult?.groupValues?.getOrNull(2)
}