package com.kilagee.onelove.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // Admin status
    private val _isAdmin = MutableStateFlow(false)
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    init {
        checkAdminStatus()
    }

    /**
     * Check if the current user is an admin
     */
    private fun checkAdminStatus() = viewModelScope.launch {
        try {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                val adminDoc = firestore.collection("admins").document(userId).get().await()
                _isAdmin.value = adminDoc.exists()
            } else {
                _isAdmin.value = false
            }
        } catch (e: Exception) {
            _isAdmin.value = false
        }
    }

    /**
     * Logout the current user
     */
    fun logout() {
        auth.signOut()
    }
}