package com.kilagee.onelove.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.compositionLocalOf

/**
 * Composition local for snackbar host state
 */
val LocalSnackbarHostState = compositionLocalOf { SnackbarHostState() }