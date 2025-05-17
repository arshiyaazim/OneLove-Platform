package com.kilagee.onelove.ui.screens.auth

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the phone verification screen
 */
@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // UI state
    private val _uiState = MutableStateFlow<PhoneVerificationUiState>(PhoneVerificationUiState.Initial)
    val uiState: StateFlow<PhoneVerificationUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<PhoneVerificationEvent>()
    val events: SharedFlow<PhoneVerificationEvent> = _events.asSharedFlow()
    
    // We can get the phone number from saved state if we navigate from another screen
    private val initialPhoneNumber = savedStateHandle.get<String>("phoneNumber") ?: ""
    
    // Form state
    private val _phoneNumber = MutableStateFlow(initialPhoneNumber)
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    
    private val _verificationCode = MutableStateFlow("")
    val verificationCode: StateFlow<String> = _verificationCode.asStateFlow()
    
    private val _verificationId = MutableStateFlow("")
    
    private val _isCodeSent = MutableStateFlow(false)
    val isCodeSent: StateFlow<Boolean> = _isCodeSent.asStateFlow()
    
    private val _isPhoneNumberValid = MutableStateFlow(true)
    val isPhoneNumberValid: StateFlow<Boolean> = _isPhoneNumberValid.asStateFlow()
    
    private val _isVerificationCodeValid = MutableStateFlow(true)
    val isVerificationCodeValid: StateFlow<Boolean> = _isVerificationCodeValid.asStateFlow()
    
    /**
     * Update phone number field
     */
    fun updatePhoneNumber(phoneNumber: String) {
        _phoneNumber.value = phoneNumber
        validatePhoneNumber()
    }
    
    /**
     * Update verification code field
     */
    fun updateVerificationCode(code: String) {
        // Allow only numbers and limit to 6 digits
        if (code.matches("^\\d{0,6}$".toRegex())) {
            _verificationCode.value = code
            validateVerificationCode()
        }
    }
    
    /**
     * Validate phone number format
     */
    private fun validatePhoneNumber() {
        _isPhoneNumberValid.value = _phoneNumber.value.isEmpty() || 
                android.util.Patterns.PHONE.matcher(_phoneNumber.value).matches()
    }
    
    /**
     * Validate verification code format
     */
    private fun validateVerificationCode() {
        _isVerificationCodeValid.value = _verificationCode.value.isEmpty() || 
                _verificationCode.value.matches("^\\d{6}$".toRegex())
    }
    
    /**
     * Send verification code
     */
    fun sendVerificationCode() {
        validatePhoneNumber()
        
        if (!_isPhoneNumberValid.value) {
            viewModelScope.launch {
                _events.emit(PhoneVerificationEvent.ValidationError("Please enter a valid phone number"))
            }
            return
        }
        
        _uiState.value = PhoneVerificationUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.sendPhoneVerificationCode(_phoneNumber.value)
            
            when (result) {
                is Result.Success -> {
                    _isCodeSent.value = true
                    _uiState.value = PhoneVerificationUiState.CodeSent
                    _events.emit(PhoneVerificationEvent.CodeSent)
                }
                is Result.Error -> {
                    _uiState.value = PhoneVerificationUiState.Error(
                        result.message ?: "Failed to send verification code"
                    )
                }
                else -> {
                    _uiState.value = PhoneVerificationUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Verify phone with code
     */
    fun verifyCode() {
        validateVerificationCode()
        
        if (!_isVerificationCodeValid.value) {
            viewModelScope.launch {
                _events.emit(PhoneVerificationEvent.ValidationError("Please enter a valid verification code"))
            }
            return
        }
        
        if (_verificationCode.value.length != 6) {
            viewModelScope.launch {
                _events.emit(PhoneVerificationEvent.ValidationError("Verification code must be 6 digits"))
            }
            return
        }
        
        _uiState.value = PhoneVerificationUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.verifyPhoneWithCode(_verificationId.value, _verificationCode.value)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = PhoneVerificationUiState.Verified
                    _events.emit(PhoneVerificationEvent.VerificationSuccess)
                }
                is Result.Error -> {
                    _uiState.value = PhoneVerificationUiState.Error(
                        result.message ?: "Failed to verify code"
                    )
                }
                else -> {
                    _uiState.value = PhoneVerificationUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Link phone number to account
     */
    fun linkPhoneToAccount() {
        validateVerificationCode()
        
        if (!_isVerificationCodeValid.value) {
            viewModelScope.launch {
                _events.emit(PhoneVerificationEvent.ValidationError("Please enter a valid verification code"))
            }
            return
        }
        
        if (_verificationCode.value.length != 6) {
            viewModelScope.launch {
                _events.emit(PhoneVerificationEvent.ValidationError("Verification code must be 6 digits"))
            }
            return
        }
        
        _uiState.value = PhoneVerificationUiState.Loading
        
        viewModelScope.launch {
            val result = authRepository.linkWithPhoneNumber(_verificationId.value, _verificationCode.value)
            
            when (result) {
                is Result.Success -> {
                    _uiState.value = PhoneVerificationUiState.Verified
                    _events.emit(PhoneVerificationEvent.VerificationSuccess)
                }
                is Result.Error -> {
                    _uiState.value = PhoneVerificationUiState.Error(
                        result.message ?: "Failed to link phone number"
                    )
                }
                else -> {
                    _uiState.value = PhoneVerificationUiState.Error("Unknown error")
                }
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is PhoneVerificationUiState.Error) {
            _uiState.value = if (_isCodeSent.value) {
                PhoneVerificationUiState.CodeSent
            } else {
                PhoneVerificationUiState.Initial
            }
        }
    }
    
    /**
     * Set verification ID (called from Activity after SMS is sent)
     */
    fun setVerificationId(verificationId: String) {
        _verificationId.value = verificationId
        _isCodeSent.value = true
        _uiState.value = PhoneVerificationUiState.CodeSent
    }
}

/**
 * UI state for the phone verification screen
 */
sealed class PhoneVerificationUiState {
    object Initial : PhoneVerificationUiState()
    object Loading : PhoneVerificationUiState()
    object CodeSent : PhoneVerificationUiState()
    object Verified : PhoneVerificationUiState()
    data class Error(val message: String) : PhoneVerificationUiState()
}

/**
 * Events emitted by the phone verification screen
 */
sealed class PhoneVerificationEvent {
    object CodeSent : PhoneVerificationEvent()
    object VerificationSuccess : PhoneVerificationEvent()
    object NavigateBack : PhoneVerificationEvent()
    data class ValidationError(val message: String) : PhoneVerificationEvent()
}