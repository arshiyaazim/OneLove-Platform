package com.kilagee.onelove.ui.screens.calls

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.UserRepository
import com.kilagee.onelove.domain.util.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the call screen
 */
@HiltViewModel
class CallViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Extract call parameters from SavedStateHandle (navigation arguments)
    private val callId: String = checkNotNull(savedStateHandle["callId"])
    private val isVideo: Boolean = checkNotNull(savedStateHandle["isVideo"])
    
    private val _callState = MutableStateFlow<CallState>(CallState.Connecting)
    val callState: StateFlow<CallState> = _callState.asStateFlow()
    
    private val _otherUser = MutableStateFlow<User?>(null)
    val otherUser: StateFlow<User?> = _otherUser.asStateFlow()
    
    private val _callDuration = MutableStateFlow(0)
    val callDuration: StateFlow<Int> = _callDuration.asStateFlow()
    
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()
    
    private val _isSpeakerOn = MutableStateFlow(isVideo)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()
    
    private val _isCameraOn = MutableStateFlow(isVideo)
    val isCameraOn: StateFlow<Boolean> = _isCameraOn.asStateFlow()
    
    private val _isBackCamera = MutableStateFlow(false)
    val isBackCamera: StateFlow<Boolean> = _isBackCamera.asStateFlow()
    
    private var callTimerJob: kotlinx.coroutines.Job? = null
    
    // TODO: Replace with your app's Agora App ID
    private val appId = ""
    
    // Agora RTC engine instance
    private var rtcEngine: RtcEngine? = null
    
    /**
     * Initialize the call
     */
    init {
        try {
            initializeAgoraEngine()
            loadOtherUser()
            
            // Start the call
            joinChannel()
            
            // Start call timer
            startCallTimer()
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize call")
            _callState.value = CallState.Error("Failed to initialize call: ${e.message}")
        }
    }
    
    /**
     * Initialize the Agora RTC engine
     */
    private fun initializeAgoraEngine() {
        try {
            rtcEngine = RtcEngine.create(context, appId, object : IRtcEngineEventHandler() {
                override fun onJoinChannelSuccess(channel: String, uid: Int, elapsed: Int) {
                    Timber.d("Join channel success: $channel, uid: $uid")
                    _callState.value = CallState.Connected
                }
                
                override fun onUserJoined(uid: Int, elapsed: Int) {
                    Timber.d("User joined: $uid")
                    setupRemoteVideo(uid)
                }
                
                override fun onUserOffline(uid: Int, reason: Int) {
                    Timber.d("User offline: $uid, reason: $reason")
                    if (reason == Constants.USER_OFFLINE_QUIT) {
                        _callState.value = CallState.Ended("Call ended by other user")
                    }
                }
                
                override fun onError(err: Int) {
                    Timber.e("Agora error: $err")
                    _callState.value = CallState.Error("Call error: $err")
                }
            })
            
            // Configure the engine for video or audio call
            if (isVideo) {
                rtcEngine?.enableVideo()
            } else {
                rtcEngine?.disableVideo()
            }
            
            // Set audio profile
            rtcEngine?.setAudioProfile(Constants.AUDIO_PROFILE_DEFAULT, Constants.AUDIO_SCENARIO_DEFAULT)
            
            // Enable dual stream mode for better network adaptation
            rtcEngine?.enableDualStreamMode(true)
            
            // Set speaker on if it's a video call
            rtcEngine?.setEnableSpeakerphone(isVideo)
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Agora RTC engine")
            throw e
        }
    }
    
    /**
     * Join the call channel
     */
    private fun joinChannel() {
        try {
            // Join the channel with token, channel name, optional info, and optionally assigned uid
            rtcEngine?.joinChannel(null, callId, null, 0)
            
            // Setup local video if this is a video call
            if (isVideo) {
                setupLocalVideo()
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Failed to join channel")
            _callState.value = CallState.Error("Failed to join call: ${e.message}")
        }
    }
    
    /**
     * Setup local video view
     */
    private fun setupLocalVideo() {
        rtcEngine?.setupLocalVideo(VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, 0))
    }
    
    /**
     * Setup remote video view
     */
    private fun setupRemoteVideo(uid: Int) {
        rtcEngine?.setupRemoteVideo(VideoCanvas(null, Constants.RENDER_MODE_HIDDEN, uid))
    }
    
    /**
     * Set local video container view
     */
    fun setLocalVideoView(view: android.view.SurfaceView) {
        try {
            rtcEngine?.setupLocalVideo(VideoCanvas(view, Constants.RENDER_MODE_FIT, 0))
        } catch (e: Exception) {
            Timber.e(e, "Failed to set local video view")
        }
    }
    
    /**
     * Set remote video container view
     */
    fun setRemoteVideoView(view: android.view.SurfaceView) {
        try {
            // Assuming the first remote user has ID 1
            rtcEngine?.setupRemoteVideo(VideoCanvas(view, Constants.RENDER_MODE_FIT, 1))
        } catch (e: Exception) {
            Timber.e(e, "Failed to set remote video view")
        }
    }
    
    /**
     * Load the other user details
     */
    private fun loadOtherUser() {
        viewModelScope.launch {
            userRepository.getUserById(callId).collectLatest { result ->
                when (result) {
                    is Result.Success -> {
                        _otherUser.value = result.data
                    }
                    is Result.Error -> {
                        Timber.e("Error loading other user: ${result.message}")
                    }
                    is Result.Loading -> {
                        // Already handled by the call state
                    }
                }
            }
        }
    }
    
    /**
     * Start the call timer
     */
    private fun startCallTimer() {
        callTimerJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _callDuration.value = _callDuration.value + 1
            }
        }
    }
    
    /**
     * Toggle mute/unmute
     */
    fun toggleMute() {
        val newMuteState = !_isMuted.value
        _isMuted.value = newMuteState
        rtcEngine?.muteLocalAudioStream(newMuteState)
    }
    
    /**
     * Toggle speaker
     */
    fun toggleSpeaker() {
        val newSpeakerState = !_isSpeakerOn.value
        _isSpeakerOn.value = newSpeakerState
        rtcEngine?.setEnableSpeakerphone(newSpeakerState)
    }
    
    /**
     * Toggle camera
     */
    fun toggleCamera() {
        val newCameraState = !_isCameraOn.value
        _isCameraOn.value = newCameraState
        rtcEngine?.muteLocalVideoStream(!newCameraState)
    }
    
    /**
     * Switch between front and back camera
     */
    fun switchCamera() {
        _isBackCamera.value = !_isBackCamera.value
        rtcEngine?.switchCamera()
    }
    
    /**
     * End the call
     */
    fun endCall() {
        try {
            rtcEngine?.leaveChannel()
            callTimerJob?.cancel()
            _callState.value = CallState.Ended("Call ended")
        } catch (e: Exception) {
            Timber.e(e, "Error ending call")
        }
    }
    
    /**
     * Clean up resources
     */
    override fun onCleared() {
        super.onCleared()
        callTimerJob?.cancel()
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
    }
}

/**
 * Call state sealed class
 */
sealed class CallState {
    object Connecting : CallState()
    object Connected : CallState()
    data class Ended(val reason: String) : CallState()
    data class Error(val message: String) : CallState()
}