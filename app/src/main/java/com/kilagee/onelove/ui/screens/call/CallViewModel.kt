package com.kilagee.onelove.ui.screens.call

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallParticipant
import com.kilagee.onelove.data.model.CallSession
import com.kilagee.onelove.data.model.CallStatus
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.domain.repository.CallRepository
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for call screen
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val userRepository: UserRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    // Call ID from navigation arguments
    private val callId: String = checkNotNull(savedStateHandle.get<String>("callId"))
    
    // Call type from navigation arguments
    private val callType: String = savedStateHandle.get<String>("callType") ?: CallType.AUDIO.name
    
    // Whether this is an outgoing call
    private val isOutgoing: Boolean = savedStateHandle.get<Boolean>("isOutgoing") ?: true
    
    // User ID from navigation arguments (outgoing calls only)
    private val calleeId: String? = savedStateHandle.get<String>("userId")
    
    // UI state
    private val _uiState = MutableStateFlow<CallUiState>(CallUiState.Initializing)
    val uiState: StateFlow<CallUiState> = _uiState.asStateFlow()
    
    // One-time events
    private val _events = MutableSharedFlow<CallEvent>()
    val events: SharedFlow<CallEvent> = _events.asSharedFlow()
    
    // Current call
    private val _call = MutableStateFlow<Call?>(null)
    val call: StateFlow<Call?> = _call.asStateFlow()
    
    // Call session
    private val _callSession = MutableStateFlow<CallSession?>(null)
    val callSession: StateFlow<CallSession?> = _callSession.asStateFlow()
    
    // Call participants
    private val _participants = MutableStateFlow<List<CallParticipant>>(emptyList())
    val participants: StateFlow<List<CallParticipant>> = _participants.asStateFlow()
    
    // Remote user
    private val _remoteUser = MutableStateFlow<com.kilagee.onelove.data.model.User?>(null)
    val remoteUser: StateFlow<com.kilagee.onelove.data.model.User?> = _remoteUser.asStateFlow()
    
    // Call duration in seconds
    private val _callDuration = MutableStateFlow(0L)
    val callDuration: StateFlow<Long> = _callDuration.asStateFlow()
    
    // Call control states
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    private val _isVideoEnabled = MutableStateFlow(callType == CallType.VIDEO.name)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()
    
    private val _isSpeakerOn = MutableStateFlow(callType == CallType.VIDEO.name)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()
    
    private val _useFrontCamera = MutableStateFlow(true)
    val useFrontCamera: StateFlow<Boolean> = _useFrontCamera.asStateFlow()
    
    // Active jobs
    private var callJob: Job? = null
    private var sessionJob: Job? = null
    
    init {
        if (isOutgoing && calleeId != null) {
            initiateCall()
        } else {
            loadCall()
        }
    }
    
    /**
     * Initiate a new outgoing call
     */
    private fun initiateCall() {
        viewModelScope.launch {
            _uiState.value = CallUiState.Initializing
            
            calleeId?.let { userId ->
                val result = callRepository.initiateCall(
                    userId = userId,
                    type = if (callType == CallType.VIDEO.name) CallType.VIDEO else CallType.AUDIO
                )
                
                when (result) {
                    is Result.Success -> {
                        _call.value = result.data
                        _uiState.value = CallUiState.Calling
                        
                        // Get remote user details
                        loadRemoteUser()
                        
                        // Start observing call updates
                        observeCall()
                        observeCallSession()
                    }
                    is Result.Error -> {
                        _events.emit(CallEvent.Error(result.message ?: "Failed to start call"))
                        _uiState.value = CallUiState.Error(result.message ?: "Failed to start call")
                    }
                    is Result.Loading -> {
                        // Keep initializing state
                    }
                }
            }
        }
    }
    
    /**
     * Load an existing call
     */
    private fun loadCall() {
        viewModelScope.launch {
            _uiState.value = CallUiState.Initializing
            
            val result = callRepository.getCall(callId)
            
            when (result) {
                is Result.Success -> {
                    _call.value = result.data
                    
                    // Set UI state based on call status
                    _uiState.value = when (result.data.status) {
                        CallStatus.RINGING -> {
                            if (isOutgoing) CallUiState.Calling else CallUiState.Ringing
                        }
                        CallStatus.CONNECTING -> CallUiState.Connecting
                        CallStatus.CONNECTED -> CallUiState.Connected
                        CallStatus.ENDED, CallStatus.MISSED, CallStatus.REJECTED, CallStatus.BUSY, CallStatus.FAILED -> {
                            CallUiState.Ended(result.data.status.name)
                        }
                    }
                    
                    // Get remote user details
                    loadRemoteUser()
                    
                    // Start observing call updates
                    observeCall()
                    observeCallSession()
                }
                is Result.Error -> {
                    _events.emit(CallEvent.Error(result.message ?: "Failed to load call"))
                    _uiState.value = CallUiState.Error(result.message ?: "Failed to load call")
                }
                is Result.Loading -> {
                    // Keep initializing state
                }
            }
        }
    }
    
    /**
     * Load remote user details
     */
    private fun loadRemoteUser() {
        viewModelScope.launch {
            val call = _call.value ?: return@launch
            
            val userId = if (isOutgoing) call.calleeId else call.callerId
            val result = userRepository.getUserById(userId)
            
            if (result is Result.Success) {
                _remoteUser.value = result.data
            }
        }
    }
    
    /**
     * Observe call updates
     */
    private fun observeCall() {
        // Cancel any existing job
        callJob?.cancel()
        
        // Start new job
        callJob = viewModelScope.launch {
            callRepository.getCallFlow(callId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val call = result.data
                        _call.value = call
                        
                        // Update UI state based on call status
                        when (call.status) {
                            CallStatus.RINGING -> {
                                _uiState.value = if (isOutgoing) CallUiState.Calling else CallUiState.Ringing
                            }
                            CallStatus.CONNECTING -> {
                                _uiState.value = CallUiState.Connecting
                            }
                            CallStatus.CONNECTED -> {
                                _uiState.value = CallUiState.Connected
                                
                                // Update call duration
                                call.startTime?.let { startTime ->
                                    val durationSeconds = (System.currentTimeMillis() - startTime.time) / 1000
                                    _callDuration.value = durationSeconds
                                }
                            }
                            CallStatus.ENDED, CallStatus.MISSED, CallStatus.REJECTED, CallStatus.BUSY, CallStatus.FAILED -> {
                                _uiState.value = CallUiState.Ended(call.status.name)
                            }
                        }
                    }
                    is Result.Error -> {
                        _events.emit(CallEvent.Error(result.message ?: "Failed to update call"))
                    }
                    is Result.Loading -> {
                        // Do nothing for loading state
                    }
                }
            }
        }
    }
    
    /**
     * Observe call session updates
     */
    private fun observeCallSession() {
        // Cancel any existing job
        sessionJob?.cancel()
        
        // Start new job
        sessionJob = viewModelScope.launch {
            callRepository.getCallSessionFlow(callId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        val session = result.data
                        _callSession.value = session
                        _participants.value = session.participants
                    }
                    is Result.Error -> {
                        _events.emit(CallEvent.Error(result.message ?: "Failed to update call session"))
                    }
                    is Result.Loading -> {
                        // Do nothing for loading state
                    }
                }
            }
        }
    }
    
    /**
     * Answer an incoming call
     */
    fun answerCall() {
        viewModelScope.launch {
            _uiState.value = CallUiState.Connecting
            
            val result = callRepository.acceptCall(callId, _isVideoEnabled.value)
            
            when (result) {
                is Result.Success -> {
                    _call.value = result.data
                    _uiState.value = CallUiState.Connected
                }
                is Result.Error -> {
                    _events.emit(CallEvent.Error(result.message ?: "Failed to answer call"))
                    _uiState.value = CallUiState.Error(result.message ?: "Failed to answer call")
                }
                is Result.Loading -> {
                    // Keep connecting state
                }
            }
        }
    }
    
    /**
     * Reject an incoming call
     */
    fun rejectCall() {
        viewModelScope.launch {
            val result = callRepository.rejectCall(callId)
            
            if (result is Result.Error) {
                _events.emit(CallEvent.Error(result.message ?: "Failed to reject call"))
            } else {
                _uiState.value = CallUiState.Ended("REJECTED")
            }
        }
    }
    
    /**
     * End the current call
     */
    fun endCall() {
        viewModelScope.launch {
            val result = callRepository.endCall(callId)
            
            if (result is Result.Error) {
                _events.emit(CallEvent.Error(result.message ?: "Failed to end call"))
            } else {
                _uiState.value = CallUiState.Ended("ENDED")
            }
        }
    }
    
    /**
     * Toggle mute status
     */
    fun toggleMute() {
        viewModelScope.launch {
            val newState = !_isMuted.value
            val result = callRepository.toggleMute(callId, newState)
            
            if (result is Result.Success) {
                _isMuted.value = newState
            } else if (result is Result.Error) {
                _events.emit(CallEvent.Error(result.message ?: "Failed to toggle mute"))
            }
        }
    }
    
    /**
     * Toggle video status
     */
    fun toggleVideo() {
        viewModelScope.launch {
            val newState = !_isVideoEnabled.value
            val result = callRepository.toggleVideo(callId, newState)
            
            if (result is Result.Success) {
                _isVideoEnabled.value = newState
            } else if (result is Result.Error) {
                _events.emit(CallEvent.Error(result.message ?: "Failed to toggle video"))
            }
        }
    }
    
    /**
     * Toggle speaker status
     */
    fun toggleSpeaker() {
        viewModelScope.launch {
            val newState = !_isSpeakerOn.value
            val result = callRepository.toggleSpeaker(callId, newState)
            
            if (result is Result.Success) {
                _isSpeakerOn.value = newState
            } else if (result is Result.Error) {
                _events.emit(CallEvent.Error(result.message ?: "Failed to toggle speaker"))
            }
        }
    }
    
    /**
     * Switch camera
     */
    fun switchCamera() {
        viewModelScope.launch {
            val newState = !_useFrontCamera.value
            val result = callRepository.switchCamera(callId, newState)
            
            if (result is Result.Success) {
                _useFrontCamera.value = newState
            } else if (result is Result.Error) {
                _events.emit(CallEvent.Error(result.message ?: "Failed to switch camera"))
            }
        }
    }
    
    /**
     * Clear errors
     */
    fun clearErrors() {
        if (_uiState.value is CallUiState.Error) {
            _uiState.value = CallUiState.Ended("ERROR")
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        callJob?.cancel()
        sessionJob?.cancel()
        
        // End call if still active
        if (_call.value?.status == CallStatus.CONNECTED ||
            _call.value?.status == CallStatus.CONNECTING ||
            _call.value?.status == CallStatus.RINGING) {
            viewModelScope.launch {
                callRepository.endCall(callId)
            }
        }
    }
}

/**
 * UI state for the call screen
 */
sealed class CallUiState {
    object Initializing : CallUiState()
    object Ringing : CallUiState()
    object Calling : CallUiState()
    object Connecting : CallUiState()
    object Connected : CallUiState()
    data class Ended(val reason: String) : CallUiState()
    data class Error(val message: String) : CallUiState()
}

/**
 * Events emitted by the call screen
 */
sealed class CallEvent {
    data class Error(val message: String) : CallEvent()
}