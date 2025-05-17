package com.kilagee.onelove.data.model

enum class VerificationStatus {
    UNVERIFIED,     // User hasn't submitted verification
    PENDING,        // Verification submitted but not yet reviewed
    VERIFIED,       // User is verified
    REJECTED        // Verification was rejected
}