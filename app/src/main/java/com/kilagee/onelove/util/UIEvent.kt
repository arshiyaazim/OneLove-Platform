package com.kilagee.onelove.util

/**
 * Sealed class for UI events that can be emitted from ViewModels to UI
 */
sealed class UIEvent {
    data class ShowToast(val message: String) : UIEvent()
    data class ShowSnackbar(val message: String, val action: String? = null) : UIEvent()
    object NavigateBack : UIEvent()
    data class Navigate(val route: String) : UIEvent()
    data class ShowDialog(val title: String, val message: String) : UIEvent()
}