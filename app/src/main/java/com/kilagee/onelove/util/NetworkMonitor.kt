package com.kilagee.onelove.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.core.content.getSystemService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Network connectivity monitor
 * Observes and reports network state changes
 */
@Singleton
class NetworkMonitor @Inject constructor(
    context: Context
) {
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        val connectivityManager = context.getSystemService<ConnectivityManager>()
        if (connectivityManager == null) {
            Timber.w("Could not get connectivity manager")
            _isOnline.value = false
        } else {
            // Check initial state
            val network = connectivityManager.activeNetwork
            val isConnected = network != null && isNetworkValid(connectivityManager, network)
            _isOnline.value = isConnected
            
            // Register callback for network changes
            val networkCallback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    _isOnline.update { true }
                    Timber.d("Network available")
                }
                
                override fun onLost(network: Network) {
                    _isOnline.update { false }
                    Timber.d("Network lost")
                }
                
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) {
                    val hasInternet = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_INTERNET
                    )
                    val hasValidated = networkCapabilities.hasCapability(
                        NetworkCapabilities.NET_CAPABILITY_VALIDATED
                    )
                    _isOnline.update { hasInternet && hasValidated }
                    Timber.d("Network capabilities changed: internet=$hasInternet, validated=$hasValidated")
                }
            }
            
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }
    
    private fun isNetworkValid(connectivityManager: ConnectivityManager, network: Network): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities != null &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}