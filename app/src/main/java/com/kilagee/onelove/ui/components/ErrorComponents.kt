package com.kilagee.onelove.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kilagee.onelove.R
import com.kilagee.onelove.util.ErrorHandler
import com.kilagee.onelove.util.ErrorResult

/**
 * A full screen error display for critical errors
 */
@Composable
fun FullScreenError(
    error: ErrorResult,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Error icon based on error type
            val errorIconRes = when (error.category) {
                ErrorHandler.NETWORK_ERROR -> R.drawable.ic_network_error
                ErrorHandler.AUTH_ERROR -> R.drawable.ic_auth_error
                ErrorHandler.SERVER_ERROR -> R.drawable.ic_server_error
                else -> R.drawable.ic_general_error
            }
            
            Image(
                painter = painterResource(id = errorIconRes),
                contentDescription = "Error Icon",
                modifier = Modifier.size(120.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error title based on category
            val errorTitle = when (error.category) {
                ErrorHandler.NETWORK_ERROR -> "Connection Problem"
                ErrorHandler.AUTH_ERROR -> "Authentication Error"
                ErrorHandler.SERVER_ERROR -> "Server Error"
                ErrorHandler.CLIENT_ERROR -> "Request Error"
                else -> "Unexpected Error"
            }
            
            Text(
                text = errorTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onRetry,
                shape = RoundedCornerShape(50)
            ) {
                Text(
                    text = "Try Again",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * An empty state with a message and optional action
 */
@Composable
fun EmptyState(
    title: String,
    message: String,
    iconRes: Int,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(100.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = actionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * A dialog to display errors
 */
@Composable
fun ErrorDialog(
    error: ErrorResult,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Error icon based on error type
                val errorIconRes = when (error.category) {
                    ErrorHandler.NETWORK_ERROR -> R.drawable.ic_network_error
                    ErrorHandler.AUTH_ERROR -> R.drawable.ic_auth_error
                    ErrorHandler.SERVER_ERROR -> R.drawable.ic_server_error
                    else -> R.drawable.ic_general_error
                }
                
                Image(
                    painter = painterResource(id = errorIconRes),
                    contentDescription = "Error Icon",
                    modifier = Modifier.size(48.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Error title based on category
                val errorTitle = when (error.category) {
                    ErrorHandler.NETWORK_ERROR -> "Connection Problem"
                    ErrorHandler.AUTH_ERROR -> "Authentication Error"
                    ErrorHandler.SERVER_ERROR -> "Server Error"
                    ErrorHandler.CLIENT_ERROR -> "Request Error"
                    else -> "Unexpected Error"
                }
                
                Text(
                    text = errorTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = error.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        if (onRetry != null) onRetry() else onDismiss()
                    },
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (onRetry != null) "Try Again" else "OK",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

/**
 * Custom Snackbar host for showing errors
 */
@Composable
fun ErrorSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
        snackbar = { data ->
            Snackbar(
                modifier = Modifier.padding(16.dp),
                containerColor = when {
                    data.visuals.actionLabel?.contains("error", ignoreCase = true) == true -> 
                        MaterialTheme.colorScheme.errorContainer
                    data.visuals.actionLabel?.contains("warning", ignoreCase = true) == true ->
                        Color(0xFFFFF9C4) // Pale yellow
                    else -> MaterialTheme.colorScheme.inverseSurface
                },
                contentColor = when {
                    data.visuals.actionLabel?.contains("error", ignoreCase = true) == true -> 
                        MaterialTheme.colorScheme.onErrorContainer
                    data.visuals.actionLabel?.contains("warning", ignoreCase = true) == true ->
                        Color(0xFF5D4037) // Dark brown
                    else -> MaterialTheme.colorScheme.inverseOnSurface
                },
                actionColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = data.visuals.message)
            }
        }
    )
}