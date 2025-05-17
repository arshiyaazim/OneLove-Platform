package com.kilagee.onelove.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.kilagee.onelove.ui.navigation.OneLoveNavHost
import com.kilagee.onelove.ui.theme.OneLoveTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Main activity for the app
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        Timber.d("MainActivity onCreate")
        
        setContent {
            OneLoveApp()
        }
    }
}

/**
 * Main composable for the app
 */
@Composable
fun OneLoveApp() {
    OneLoveTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            OneLoveNavHost(navController = navController)
        }
    }
}