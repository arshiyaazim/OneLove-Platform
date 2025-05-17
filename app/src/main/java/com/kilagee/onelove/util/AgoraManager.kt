package com.kilagee.onelove.util

import android.content.Context
import android.util.Log
import io.agora.rtc.Constants
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.models.ClientRoleOptions
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling Agora RTC Engine operations
 */
@Singleton
class AgoraManager @Inject constructor() {
    companion object {
        private const val TAG = "AgoraManager"
        
        // This will be retrieved from secrets management
        private var appId: String? = null
        
        // Call this method early in the app lifecycle to set the Agora App ID
        fun setAppId(agoraAppId: String) {
            appId = agoraAppId
        }
    }
    
    private var rtcEngine: RtcEngine? = null
    private var localVideoCanvas: VideoCanvas? = null
    private var remoteVideoCanvas: VideoCanvas? = null
    
    /**
     * Initialize the Agora RTC Engine
     * 
     * @param context Application context
     * @param eventHandler Event handler for RTC events
     * @return The initialized RTC Engine or null if initialization failed
     */
    fun initializeEngine(context: Context, eventHandler: IRtcEngineEventHandler): RtcEngine? {
        if (appId.isNullOrBlank()) {
            Log.e(TAG, "Agora App ID is not set. Call setAppId() first.")
            return null
        }
        
        try {
            rtcEngine = RtcEngine.create(context, appId, eventHandler)
            return rtcEngine
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Agora RTC Engine: ${e.message}")
            return null
        }
    }
    
    /**
     * Join a channel for a video call
     * 
     * @param token Security token for the channel
     * @param channelName Name of the channel to join
     * @param uid User ID in the channel (0 means auto-assign)
     * @param setAsAudience Whether to join as audience (for broadcast scenarios)
     */
    fun joinVideoChannel(token: String?, channelName: String, uid: Int = 0, setAsAudience: Boolean = false) {
        rtcEngine?.let { engine ->
            // Set video encoder configuration
            engine.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_640x360,
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_15,
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
            
            // Enable video module
            engine.enableVideo()
            
            // Set client role (broadcaster or audience)
            if (setAsAudience) {
                val clientRoleOptions = ClientRoleOptions()
                clientRoleOptions.audienceLatencyLevel = Constants.AUDIENCE_LATENCY_LEVEL_LOW_LATENCY
                engine.setClientRole(Constants.CLIENT_ROLE_AUDIENCE, clientRoleOptions)
            } else {
                engine.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)
            }
            
            // Join the channel
            engine.joinChannel(token, channelName, null, uid)
        }
    }
    
    /**
     * Join a channel for an audio-only call
     * 
     * @param token Security token for the channel
     * @param channelName Name of the channel to join
     * @param uid User ID in the channel (0 means auto-assign)
     */
    fun joinAudioChannel(token: String?, channelName: String, uid: Int = 0) {
        rtcEngine?.let { engine ->
            // Disable video for audio-only calls
            engine.disableVideo()
            
            // Set audio profile
            engine.setAudioProfile(
                Constants.AUDIO_PROFILE_DEFAULT,
                Constants.AUDIO_SCENARIO_DEFAULT
            )
            
            // Enable audio module
            engine.enableAudio()
            
            // Join the channel
            engine.joinChannel(token, channelName, null, uid)
        }
    }
    
    /**
     * Setup local video view
     * 
     * @param container View to render local video
     * @param uid Local user ID
     */
    fun setupLocalVideo(container: android.view.View, uid: Int = 0) {
        rtcEngine?.let { engine ->
            localVideoCanvas = VideoCanvas(container, VideoCanvas.RENDER_MODE_HIDDEN, uid)
            engine.setupLocalVideo(localVideoCanvas)
            engine.startPreview()
        }
    }
    
    /**
     * Setup remote video view
     * 
     * @param container View to render remote video
     * @param uid Remote user ID
     */
    fun setupRemoteVideo(container: android.view.View, uid: Int) {
        rtcEngine?.let { engine ->
            remoteVideoCanvas = VideoCanvas(container, VideoCanvas.RENDER_MODE_HIDDEN, uid)
            engine.setupRemoteVideo(remoteVideoCanvas)
        }
    }
    
    /**
     * Leave the current channel
     */
    fun leaveChannel() {
        rtcEngine?.leaveChannel()
    }
    
    /**
     * Toggle local audio (mute/unmute)
     * 
     * @param muted Whether to mute the local audio
     */
    fun muteLocalAudio(muted: Boolean) {
        rtcEngine?.muteLocalAudioStream(muted)
    }
    
    /**
     * Toggle local video (enable/disable)
     * 
     * @param enabled Whether to enable the local video
     */
    fun enableLocalVideo(enabled: Boolean) {
        if (enabled) {
            rtcEngine?.enableVideo()
        } else {
            rtcEngine?.disableVideo()
        }
    }
    
    /**
     * Toggle speaker phone
     * 
     * @param enabled Whether to enable the speaker phone
     */
    fun enableSpeakerphone(enabled: Boolean) {
        rtcEngine?.setEnableSpeakerphone(enabled)
    }
    
    /**
     * Switch between front and rear cameras
     */
    fun switchCamera() {
        rtcEngine?.switchCamera()
    }
    
    /**
     * Release all resources
     */
    fun release() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
        localVideoCanvas = null
        remoteVideoCanvas = null
    }
}