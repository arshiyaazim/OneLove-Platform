package com.kilagee.onelove.data.model

import com.google.firebase.firestore.GeoPoint

/**
 * Represents a geographical location with coordinates and address information
 */
data class GeoLocation(
    val geoPoint: GeoPoint = GeoPoint(0.0, 0.0),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val country: String = "",
    val zipCode: String = "",
    val formattedAddress: String = ""
) {
    constructor(latitude: Double, longitude: Double) : this(
        geoPoint = GeoPoint(latitude, longitude),
        latitude = latitude,
        longitude = longitude
    )
    
    /**
     * Calculate distance between two locations in kilometers
     */
    fun distanceTo(other: GeoLocation): Double {
        val earthRadius = 6371.0 // kilometers
        
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(other.latitude)
        val lon2 = Math.toRadians(other.longitude)
        
        val dLat = lat2 - lat1
        val dLon = lon2 - lon1
        
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Provides a readable address string
     */
    fun getDisplayAddress(): String {
        return formattedAddress.ifEmpty {
            listOfNotNull(
                address.takeIf { it.isNotEmpty() },
                city.takeIf { it.isNotEmpty() },
                state.takeIf { it.isNotEmpty() },
                zipCode.takeIf { it.isNotEmpty() },
                country.takeIf { it.isNotEmpty() }
            ).joinToString(", ")
        }
    }
    
    companion object {
        /**
         * Create GeoLocation from a Firebase GeoPoint
         */
        fun fromGeoPoint(geoPoint: GeoPoint): GeoLocation {
            return GeoLocation(
                geoPoint = geoPoint,
                latitude = geoPoint.latitude,
                longitude = geoPoint.longitude
            )
        }
    }
}