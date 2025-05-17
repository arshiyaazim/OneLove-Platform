package com.kilagee.onelove.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.CallRepository
import com.kilagee.onelove.domain.repository.CallState
import com.kilagee.onelove.domain.repository.CallHistoryItem
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.SessionDescription
import timber.log.Timber
import javax.inject.Inject

/**
 * Call view model
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {
    
    private val _callHistory = MutableStateFlow<Result<List<CallHistoryItem>>>(Result.Loading)
    val callHistory: StateFlow<Result<List<CallHistoryItem>>> = _callHistory
    
    private val _callWithUser = MutableStateFlow<Result<List<CallHistoryItem>>>(Result.Loading)
    val callWithUser: StateFlow<Result<List<CallHistoryItem>>> = _callWithUser
    
    private val _callState = MutableStateFlow<CallState>(CallState.IDLE)
    val callState: StateFlow<CallState> = _callState
    
    private val _localStream = MutableStateFlow<MediaStream?>(null)
    val localStream: StateFlow<MediaStream?> = _localStream
    
    private val _remoteStream = MutableStateFlow<MediaStream?>(null)
    val remoteStream: StateFlow<MediaStream?> = _remoteStream
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted
    
    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled
    
    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn
    
    private val _activeCallId = MutableStateFlow<String?>(null)
    val activeCallId: StateFlow<String?> = _activeCallId
    
    private val _callActionState = MutableStateFlow<CallActionState>(CallActionState.Idle)
    val callActionState: StateFlow<CallActionState> = _callActionState
    
    init {
        loadCallHistory()
        observeCallState()
        observeMediaStreams()
    }
    
    /**
     * Load call history
     */
    fun loadCallHistory() {
        viewModelScope.launch {
            callRepository.getCallHistory()
                .catch { e ->
                    Timber.e(e, "Error loading call history")
                    _callHistory.value = Result.Error("Failed to load call history: ${e.message}")
                }
                .collect { result ->
                    _callHistory.value = result
                }
        }
    }
    
    /**
     * Load call history with a specific user
     * 
     * @param userId ID of the user
     */
    fun loadCallHistoryWithUser(userId: String) {
        viewModelScope.launch {
            callRepository.getCallHistoryWithUser(userId)
                .catch { e ->
                    Timber.e(e, "Error loading call history with user")
                    _callWithUser.value = Result.Error("Failed to load call history: ${e.message}")
                }
                .collect { result ->
                    _callWithUser.value = result
                }
        }
    }
    
    /**
     * Observe call state
     */
    private fun observeCallState() {
        viewModelScope.launch {
            callRepository.getCallState()
                .catch { e ->
                    Timber.e(e, "Error observing call state")
                    _callState.value = CallState.IDLE
                }
                .collect { state ->
                    _callState.value = state
                }
        }
    }
    
    /**
     * Observe media streams
     */
    private fun observeMediaStreams() {
        // Observe local stream
        viewModelScope.launch {
            callRepository.getLocalStream()
                .catch { e ->
                    Timber.e(e, "Error observing local stream")
                    _localStream.value = null
                }
                .collect { stream ->
                    _localStream.value = stream
                }
        }
        
        // Observe remote stream
        viewModelScope.launch {
            callRepository.getRemoteStream()
                .catch { e ->
                    Timber.e(e, "Error observing remote stream")
                    _remoteStream.value = null
                }
                .collect { stream ->
                    _remoteStream.value = stream
                }
        }
    }
    
    /**
     * Initialize WebRTC
     */
    fun initializeWebRTC() {
        viewModelScope.launch {
            try {
                _callActionState.value = CallActionState.Initializing
                val result = callRepository.initializeWebRTC()
                
                when (result) {
                    is Result.Success -> {
                        _callActionState.value = CallActionState.Initialized
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _callActionState.value = CallActionState.Initializing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error initializing WebRTC")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to initialize WebRTC")
            }
        }
    }
    
    /**
     * Start a call with a user
     * 
     * @param user User to call
     * @param isVideoCall Whether this is a video call
     */
    fun startCall(user: User, isVideoCall: Boolean) {
        viewModelScope.launch {
            try {
                _callActionState.value = CallActionState.Starting
                val result = callRepository.startCall(user, isVideoCall)
                
                when (result) {
                    is Result.Success -> {
                        _activeCallId.value = result.data
                        _callActionState.value = CallActionState.Started
                        _isVideoEnabled.value = isVideoCall
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _callActionState.value = CallActionState.Starting
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error starting call")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to start call")
            }
        }
    }
    
    /**
     * Answer an incoming call
     * 
     * @param callId ID of the call
     * @param withVideo Whether to answer with video
     */
    fun answerCall(callId: String, withVideo: Boolean) {
        viewModelScope.launch {
            try {
                _callActionState.value = CallActionState.Answering
                val result = callRepository.answerCall(callId, withVideo)
                
                when (result) {
                    is Result.Success -> {
                        _activeCallId.value = callId
                        _callActionState.value = CallActionState.Answered
                        _isVideoEnabled.value = withVideo
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _callActionState.value = CallActionState.Answering
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error answering call")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to answer call")
            }
        }
    }
    
    /**
     * End the current call
     */
    fun endCall() {
        viewModelScope.launch {
            try {
                _callActionState.value = CallActionState.Ending
                val result = callRepository.endCall()
                
                when (result) {
                    is Result.Success -> {
                        _activeCallId.value = null
                        _callActionState.value = CallActionState.Ended
                        // Refresh call history
                        loadCallHistory()
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _callActionState.value = CallActionState.Ending
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error ending call")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to end call")
            }
        }
    }
    
    /**
     * Decline an incoming call
     * 
     * @param callId ID of the call
     */
    fun declineCall(callId: String) {
        viewModelScope.launch {
            try {
                _callActionState.value = CallActionState.Declining
                val result = callRepository.declineCall(callId)
                
                when (result) {
                    is Result.Success -> {
                        _callActionState.value = CallActionState.Declined
                        // Refresh call history
                        loadCallHistory()
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        _callActionState.value = CallActionState.Declining
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error declining call")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to decline call")
            }
        }
    }
    
    /**
     * Toggle audio mute
     */
    fun toggleMute() {
        viewModelScope.launch {
            try {
                val newMuteState = !_isMuted.value
                val result = callRepository.toggleAudioMute(newMuteState)
                
                when (result) {
                    is Result.Success -> {
                        _isMuted.value = newMuteState
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling mute")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to toggle mute")
            }
        }
    }
    
    /**
     * Toggle video
     */
    fun toggleVideo() {
        viewModelScope.launch {
            try {
                val newVideoState = !_isVideoEnabled.value
                val result = callRepository.toggleVideo(newVideoState)
                
                when (result) {
                    is Result.Success -> {
                        _isVideoEnabled.value = newVideoState
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling video")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to toggle video")
            }
        }
    }
    
    /**
     * Toggle speaker
     */
    fun toggleSpeaker() {
        viewModelScope.launch {
            try {
                val newSpeakerState = !_isSpeakerOn.value
                val result = callRepository.toggleSpeaker(newSpeakerState)
                
                when (result) {
                    is Result.Success -> {
                        _isSpeakerOn.value = newSpeakerState
                    }
                    is Result.Error -> {
                        _callActionState.value = CallActionState.Error(result.message)
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error toggling speaker")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to toggle speaker")
            }
        }
    }
    
    /**
     * Switch camera
     */
    fun switchCamera() {
        viewModelScope.launch {
            try {
                val result = callRepository.switchCamera()
                
                if (result is Result.Error) {
                    _callActionState.value = CallActionState.Error(result.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error switching camera")
                _callActionState.value = CallActionState.Error(e.message ?: "Failed to switch camera")
            }
        }
    }
    
    /**
     * Handle an ICE candidate
     * 
     * @param callId ID of the call
     * @param candidate ICE candidate
     */
    fun handleIceCandidate(callId: String, candidate: IceCandidate) {
        viewModelScope.launch {
            try {
                callRepository.addIceCandidate(callId, candidate)
            } catch (e: Exception) {
                Timber.e(e, "Error handling ICE candidate")
            }
        }
    }
    
    /**
     * Handle remote session description
     * 
     * @param callId ID of the call
     * @param description Remote session description
     */
    fun handleRemoteDescription(callId: String, description: SessionDescription) {
        viewModelScope.launch {
            try {
                callRepository.setRemoteDescription(callId, description)
            } catch (e: Exception) {
                Timber.e(e, "Error handling remote description")
            }
        }
    }
    
    /**
     * Clean up WebRTC resources
     */
    override fun onCleared() {
        callRepository.cleanup()
        super.onCleared()
    }
}

/**
 * Call action state
 */
sealed class CallActionState {
    object Idle : CallActionState()
    object Initializing : CallActionState()
    object Initialized : CallActionState()
    object Starting : CallActionState()
    object Started : CallActionState()
    object Answering : CallActionState()
    object Answered : CallActionState()
    object Ending : CallActionState()
    object Ended : CallActionState()
    object Declining : CallActionState()
    object Declined : CallActionState()
    data class Error(val message: String) : CallActionState()
}