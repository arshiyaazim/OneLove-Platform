package com.kilagee.onelove.ui.authentication

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.MembershipLevel
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.VerificationStatus
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    // Login state
    private val _loginState = MutableStateFlow<Resource<User>?>(null)
    val loginState: StateFlow<Resource<User>?> = _loginState
    
    // Registration state
    private val _registerState = MutableStateFlow<Resource<User>?>(null)
    val registerState: StateFlow<Resource<User>?> = _registerState
    
    // Reset password state
    private val _resetPasswordState = MutableStateFlow<Resource<Unit>?>(null)
    val resetPasswordState: StateFlow<Resource<Unit>?> = _resetPasswordState
    
    // Current user state
    private val _currentUserState = MutableStateFlow<Resource<User?>?>(null)
    val currentUserState: StateFlow<Resource<User?>?> = _currentUserState
    
    init {
        getCurrentUser()
    }
    
    fun login(email: String, password: String) {
        authRepository.login(email, password)
            .onEach { resource ->
                _loginState.value = resource
            }.launchIn(viewModelScope)
    }
    
    fun register(
        firstName: String,
        lastName: String,
        username: String,
        email: String,
        password: String,
        country: String,
        location: String,
        gender: String,
        profileImageUri: Uri?,
        idDocumentUri: Uri?
    ) {
        // Create user object
        val user = User(
            id = null, // Will be set by Firebase
            firstName = firstName,
            lastName = lastName,
            username = username,
            email = email,
            bio = null,
            country = country,
            location = location,
            gender = gender,
            phoneNumber = null,
            birthDate = null,
            education = null,
            job = null,
            interests = emptyList(),
            profileImageUrl = null, // Will be set after upload
            idDocumentUrl = null, // Will be set after upload
            points = 0,
            membershipLevel = MembershipLevel.BASIC,
            verificationStatus = VerificationStatus.PENDING,
            isProfilePrivate = false,
            showDistance = true,
            showLastActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        authRepository.register(email, password, user, profileImageUri, idDocumentUri)
            .onEach { resource ->
                _registerState.value = resource
            }.launchIn(viewModelScope)
    }
    
    fun resetPassword(email: String) {
        authRepository.resetPassword(email)
            .onEach { resource ->
                _resetPasswordState.value = resource
            }.launchIn(viewModelScope)
    }
    
    fun logout() {
        authRepository.logout()
            .onEach {
                // Clear all states
                _loginState.value = null
                _registerState.value = null
                _resetPasswordState.value = null
                _currentUserState.value = null
            }.launchIn(viewModelScope)
    }
    
    fun getCurrentUser() {
        authRepository.getCurrentUser()
            .onEach { resource ->
                _currentUserState.value = resource
            }.launchIn(viewModelScope)
    }
    
    fun isUserAuthenticated(): Boolean {
        return authRepository.isUserAuthenticated()
    }
    
    fun clearLoginState() {
        _loginState.value = null
    }
    
    fun clearRegisterState() {
        _registerState.value = null
    }
    
    fun clearResetPasswordState() {
        _resetPasswordState.value = null
    }
}