package com.kilagee.onelove.data.model

enum class OfferStatus {
    PENDING,        // Offer is waiting for response
    ACCEPTED,       // Offer has been accepted
    REJECTED,       // Offer has been rejected
    COUNTER_OFFER,  // Counter offer has been made
    CANCELLED,      // Offer has been cancelled
    COMPLETED       // Offer has been fulfilled
}