package com.kilagee.onelove.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Data class representing a call between users
 */
@Parcelize
data class Call(
    val id: String,
    val callerId: String,
    val calleeId: String,
    val type: CallType,
    val status: CallStatus,
    val startTime: Date? = null,
    val endTime: Date? = null,
    val duration: Long = 0, // in seconds
    val isIncoming: Boolean = false,
    val quality: CallQuality? = null,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Enum representing call type
 */
@Parcelize
enum class CallType : Parcelable {
    AUDIO,
    VIDEO
}

/**
 * Enum representing call status
 */
@Parcelize
enum class CallStatus : Parcelable {
    RINGING,
    CONNECTING,
    CONNECTED,
    ENDED,
    MISSED,
    REJECTED,
    BUSY,
    FAILED
}

/**
 * Enum representing call quality
 */
@Parcelize
enum class CallQuality : Parcelable {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    BAD
}

/**
 * Data class representing a call offer
 */
@Parcelize
data class CallOffer(
    val id: String,
    val callId: String,
    val senderId: String,
    val receiverId: String,
    val type: CallType,
    val createdAt: Date,
    val expiresAt: Date, // When the offer expires
    val sdp: String, // Session Description Protocol
    val status: CallOfferStatus,
    val metadata: Map<String, String> = emptyMap()
) : Parcelable

/**
 * Enum representing call offer status
 */
@Parcelize
enum class CallOfferStatus : Parcelable {
    PENDING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    CANCELED
}

/**
 * Data class representing call session details
 */
@Parcelize
data class CallSession(
    val id: String,
    val callId: String,
    val participants: List<CallParticipant>,
    val roomId: String? = null, // For group calls
    val startTime: Date,
    val endTime: Date? = null,
    val status: CallSessionStatus,
    val activeParticipants: Int = 0,
    val settings: CallSettings? = null
) : Parcelable

/**
 * Data class representing a call participant
 */
@Parcelize
data class CallParticipant(
    val userId: String,
    val joinTime: Date? = null,
    val leaveTime: Date? = null,
    val status: ParticipantStatus = ParticipantStatus.IDLE,
    val hasAudio: Boolean = true,
    val hasVideo: Boolean = true,
    val isMuted: Boolean = false,
    val isVideoEnabled: Boolean = true,
    val isSpeaking: Boolean = false,
    val connectionQuality: CallQuality? = null
) : Parcelable

/**
 * Enum representing participant status
 */
@Parcelize
enum class ParticipantStatus : Parcelable {
    IDLE,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    DISCONNECTED
}

/**
 * Enum representing call session status
 */
@Parcelize
enum class CallSessionStatus : Parcelable {
    INITIALIZING,
    ACTIVE,
    ENDED,
    FAILED
}

/**
 * Data class representing call settings
 */
@Parcelize
data class CallSettings(
    val enableVideo: Boolean = true,
    val enableAudio: Boolean = true,
    val enableScreenShare: Boolean = false,
    val allowRecording: Boolean = false,
    val maxParticipants: Int = 2,
    val allowJoining: Boolean = true,
    val autoAcceptAll: Boolean = false,
    val autoRecordCall: Boolean = false,
    val preferFrontCamera: Boolean = true,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM
) : Parcelable

/**
 * Enum representing video quality
 */
@Parcelize
enum class VideoQuality : Parcelable {
    LOW,
    MEDIUM,
    HIGH,
    HD,
    FULL_HD
}