package com.kilagee.onelove.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.kilagee.onelove.util.PremiumAccessManager
import com.kilagee.onelove.util.PremiumFeature

/**
 * Component that wraps premium features and handles access control
 */
@Composable
fun PremiumFeatureWrapper(
    feature: PremiumFeature,
    premiumAccessManager: PremiumAccessManager,
    navigateToSubscription: () -> Unit,
    content: @Composable () -> Unit,
    fallbackContent: @Composable () -> Unit = { 
        PremiumFeatureComponent(
            feature = feature,
            premiumAccessManager = premiumAccessManager,
            navigateToSubscription = navigateToSubscription
        ) {
            content()
        }
    }
) {
    var hasAccess by remember { mutableStateOf(false) }
    
    val hasAccessFlow by premiumAccessManager.checkFeatureAccess(feature)
        .collectAsState(initial = false)
    
    LaunchedEffect(hasAccessFlow) {
        hasAccess = hasAccessFlow
    }
    
    if (hasAccess) {
        content()
    } else {
        fallbackContent()
    }
}