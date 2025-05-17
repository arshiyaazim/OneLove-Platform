package com.kilagee.onelove.domain.matching

import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserPreferences
import kotlin.math.*

/**
 * Engine for matching users based on different criteria.
 */
class MatchEngine {
    
    companion object {
        private const val LOCATION_WEIGHT = 0.3f
        private const val AGE_WEIGHT = 0.2f
        private const val GENDER_WEIGHT = 0.3f
        private const val INTERESTS_WEIGHT = 0.2f
        
        private const val MAX_AGE_DIFFERENCE = 20 // Max age difference to consider for scoring
        private const val MAX_DISTANCE_KM = 100 // Max distance in KM to consider for scoring
        private const val EARTH_RADIUS_KM = 6371 // Earth radius in kilometers
        
        /**
         * Haversine formula to calculate distance between two points on Earth.
         * Returns distance in kilometers.
         */
        fun calculateDistance(
            lat1: Double, 
            lon1: Double, 
            lat2: Double, 
            lon2: Double
        ): Double {
            val latDistance = Math.toRadians(lat2 - lat1)
            val lonDistance = Math.toRadians(lon2 - lon1)
            
            val a = sin(latDistance / 2) * sin(latDistance / 2) + 
                   cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                   sin(lonDistance / 2) * sin(lonDistance / 2)
            
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            
            return EARTH_RADIUS_KM * c
        }
    }
    
    /**
     * Calculate match percentage between current user and potential match.
     * 
     * @param currentUser The logged-in user
     * @param potentialMatch The user to check for compatibility
     * @return Match percentage from 0 to 100
     */
    fun calculateMatchPercentage(currentUser: User, potentialMatch: User): Int {
        
        val preferences = currentUser.preferences ?: UserPreferences()
        
        // If user is already in rejections list, return 0
        if (currentUser.rejectedUserIds.contains(potentialMatch.id)) {
            return 0
        }
        
        // Calculate individual scores
        val locationScore = calculateLocationScore(currentUser, potentialMatch)
        val ageScore = calculateAgeScore(currentUser, potentialMatch, preferences)
        val genderScore = calculateGenderScore(currentUser, potentialMatch, preferences)
        val interestsScore = calculateInterestsScore(currentUser, potentialMatch)
        
        // Calculate weighted score
        val weightedScore = locationScore * LOCATION_WEIGHT +
                ageScore * AGE_WEIGHT +
                genderScore * GENDER_WEIGHT +
                interestsScore * INTERESTS_WEIGHT
        
        return (weightedScore * 100).toInt()
    }
    
    /**
     * Calculate location compatibility score.
     */
    private fun calculateLocationScore(currentUser: User, potentialMatch: User): Float {
        // If both users have lat/lng, calculate actual distance
        if (currentUser.latitude != null && currentUser.longitude != null && 
            potentialMatch.latitude != null && potentialMatch.longitude != null) {
            
            val distance = calculateDistance(
                currentUser.latitude, currentUser.longitude,
                potentialMatch.latitude, potentialMatch.longitude
            )
            
            // Score based on distance (closer = higher score)
            return 1.0f - min(distance.toFloat(), MAX_DISTANCE_KM.toFloat()) / MAX_DISTANCE_KM
        }
        
        // If no location data, use country/city
        if (currentUser.country.isBlank() || potentialMatch.country.isBlank()) {
            return 0.5f
        }
        
        // Same country is important
        val sameCountry = currentUser.country == potentialMatch.country
        if (!sameCountry) {
            return 0.1f
        }
        
        // If both have city info, check if same city
        if (currentUser.city.isNotBlank() && potentialMatch.city.isNotBlank()) {
            return if (currentUser.city == potentialMatch.city) 1.0f else 0.4f
        }
        
        // Same country but city unknown
        return 0.6f
    }
    
    /**
     * Calculate age compatibility score.
     */
    private fun calculateAgeScore(currentUser: User, potentialMatch: User, preferences: UserPreferences): Float {
        // If no age info, return average score
        val currentUserAge = currentUser.age ?: return 0.5f
        val potentialMatchAge = potentialMatch.age ?: return 0.5f
        
        // Check if within preferred age range
        val minAge = preferences.minAgePreference ?: (currentUserAge - 5)
        val maxAge = preferences.maxAgePreference ?: (currentUserAge + 5)
        
        if (potentialMatchAge < minAge || potentialMatchAge > maxAge) {
            return 0.1f
        }
        
        // Calculate how close the ages are (closer = higher score)
        val ageDifference = abs(currentUserAge - potentialMatchAge)
        return 1.0f - min(ageDifference.toFloat(), MAX_AGE_DIFFERENCE.toFloat()) / MAX_AGE_DIFFERENCE
    }
    
    /**
     * Calculate gender compatibility score.
     */
    private fun calculateGenderScore(currentUser: User, potentialMatch: User, preferences: UserPreferences): Float {
        // If no gender preference, return average score
        val genderPreference = preferences.genderPreference ?: return 0.5f
        
        // If gender preference matches potential match's gender
        return if (genderPreference == potentialMatch.gender ||
            genderPreference == "Any") {
            1.0f
        } else {
            0.0f
        }
    }
    
    /**
     * Calculate interests compatibility score.
     */
    private fun calculateInterestsScore(currentUser: User, potentialMatch: User): Float {
        val currentUserInterests = currentUser.interests
        val potentialMatchInterests = potentialMatch.interests
        
        // If either user has no interests, return average score
        if (currentUserInterests.isEmpty() || potentialMatchInterests.isEmpty()) {
            return 0.5f
        }
        
        // Count the number of shared interests
        val sharedInterests = currentUserInterests.filter { it in potentialMatchInterests }
        val totalInterests = (currentUserInterests + potentialMatchInterests).distinct()
        
        // Calculate Jaccard similarity (intersection over union)
        return if (totalInterests.isNotEmpty()) {
            sharedInterests.size.toFloat() / totalInterests.size
        } else {
            0.0f
        }
    }
    
    /**
     * Filter and sort potential matches for a user.
     * 
     * @param currentUser The logged-in user
     * @param potentialMatches List of all potential matches
     * @param minMatchPercentage Minimum match percentage to include (default: 50%)
     * @return Sorted list of potential matches with their match percentages
     */
    fun findMatches(
        currentUser: User,
        potentialMatches: List<User>,
        minMatchPercentage: Int = 50
    ): List<MatchResult> {
        // Calculate match percentage for each potential match
        val results = potentialMatches
            .filter { it.id != currentUser.id } // Exclude current user
            .map { potentialMatch ->
                val percentage = calculateMatchPercentage(currentUser, potentialMatch)
                MatchResult(potentialMatch, percentage)
            }
            .filter { it.matchPercentage >= minMatchPercentage }
            .sortedByDescending { it.matchPercentage }
        
        return results
    }
    
    /**
     * Result class containing a potential match and the calculated match percentage.
     */
    data class MatchResult(
        val user: User,
        val matchPercentage: Int
    )
}