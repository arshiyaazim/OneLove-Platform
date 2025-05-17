package com.kilagee.onelove.domain.recommendation

import com.kilagee.onelove.data.model.User
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * RecommendationEngine for providing intelligent match recommendations
 */
@Singleton
class RecommendationEngine @Inject constructor() {

    /**
     * Calculate relevance score between current user and potential match
     * Returns a score between 0 and 1, where higher is better match
     */
    fun calculateRelevanceScore(currentUser: User, potentialMatch: User): Double {
        try {
            var score = 0.0
            var factorsCount = 0
            
            // Age compatibility (weight: high)
            val ageScore = calculateAgeCompatibility(currentUser, potentialMatch)
            if (ageScore > 0) {
                score += ageScore * 0.25 // 25% weight for age
                factorsCount++
            }
            
            // Location proximity (weight: high)
            val distanceScore = calculateLocationProximity(currentUser, potentialMatch)
            if (distanceScore > 0) {
                score += distanceScore * 0.25 // 25% weight for location
                factorsCount++
            }
            
            // Interests similarity (weight: medium-high)
            val interestsScore = calculateInterestsSimilarity(currentUser, potentialMatch)
            if (interestsScore > 0) {
                score += interestsScore * 0.2 // 20% weight for interests
                factorsCount++
            }
            
            // Looking for compatibility (weight: medium-high)
            val lookingForScore = calculateLookingForCompatibility(currentUser, potentialMatch)
            if (lookingForScore > 0) {
                score += lookingForScore * 0.2 // 20% weight for looking for
                factorsCount++
            }
            
            // Verification level (weight: low)
            val verificationScore = calculateVerificationScore(potentialMatch)
            if (verificationScore > 0) {
                score += verificationScore * 0.1 // 10% weight for verification
                factorsCount++
            }
            
            // Normalize the score based on factors count
            return if (factorsCount > 0) score / factorsCount else 0.0
            
        } catch (e: Exception) {
            Timber.e(e, "Error calculating relevance score")
            // Default to 0.5 on error for neutral sorting
            return 0.5
        }
    }
    
    /**
     * Calculate age compatibility score
     */
    private fun calculateAgeCompatibility(currentUser: User, potentialMatch: User): Double {
        // Check if both users have age and preferences set
        val currentUserAge = currentUser.age ?: return 0.0
        val potentialMatchAge = potentialMatch.age ?: return 0.0
        val minAgePreference = currentUser.minAgePreference ?: return 0.0
        val maxAgePreference = currentUser.maxAgePreference ?: return 0.0
        
        // Check if potential match is within user's preferred age range
        val isWithinAgeRange = potentialMatchAge in minAgePreference..maxAgePreference
        
        // Check if current user is within potential match's preferred age range
        val isCurrentUserInMatchRange = if (potentialMatch.minAgePreference != null && potentialMatch.maxAgePreference != null) {
            currentUserAge in potentialMatch.minAgePreference..potentialMatch.maxAgePreference
        } else {
            true // If potential match has no preference, assume compatibility
        }
        
        // If both conditions are met, high score. If not, return 0.
        return if (isWithinAgeRange && isCurrentUserInMatchRange) {
            // Closer to the middle of the range is better
            val rangeMidpoint = (minAgePreference + maxAgePreference) / 2.0
            val distanceFromMidpoint = abs(potentialMatchAge - rangeMidpoint)
            val rangeHalfWidth = (maxAgePreference - minAgePreference) / 2.0
            
            // Normalize to 0-1, where 1 is at midpoint, 0 is at edge of range
            if (rangeHalfWidth > 0) {
                val normalizedDistance = distanceFromMidpoint / rangeHalfWidth
                1.0 - min(1.0, normalizedDistance)
            } else {
                0.8 // Default score if range is very narrow
            }
        } else {
            0.0 // Not compatible
        }
    }
    
    /**
     * Calculate location proximity score
     */
    private fun calculateLocationProximity(currentUser: User, potentialMatch: User): Double {
        // Check if both users have location coordinates
        val currentUserLat = currentUser.latitude ?: return 0.0
        val currentUserLng = currentUser.longitude ?: return 0.0
        val potentialMatchLat = potentialMatch.latitude ?: return 0.0
        val potentialMatchLng = potentialMatch.longitude ?: return 0.0
        
        // Check if distance preference is set
        val maxDistance = currentUser.maxDistance ?: return 0.0
        
        // Calculate distance between users using Haversine formula
        val distance = calculateDistance(
            currentUserLat, currentUserLng, potentialMatchLat, potentialMatchLng
        )
        
        // Check if within max distance
        return if (distance <= maxDistance) {
            // Normalize to 0-1, where 1 is closest
            1.0 - min(1.0, distance / maxDistance)
        } else {
            0.0 // Too far
        }
    }
    
    /**
     * Calculate interests similarity score
     */
    private fun calculateInterestsSimilarity(currentUser: User, potentialMatch: User): Double {
        val currentUserInterests = currentUser.interests ?: return 0.0
        val potentialMatchInterests = potentialMatch.interests ?: return 0.0
        
        // If either has no interests, return a low default score
        if (currentUserInterests.isEmpty() || potentialMatchInterests.isEmpty()) {
            return 0.1
        }
        
        // Count common interests
        val commonInterests = currentUserInterests.intersect(potentialMatchInterests.toSet())
        
        // Calculate Jaccard similarity (intersection over union)
        val unionSize = currentUserInterests.size + potentialMatchInterests.size - commonInterests.size
        
        return if (unionSize > 0) {
            commonInterests.size.toDouble() / unionSize
        } else {
            0.1 // Default low score
        }
    }
    
    /**
     * Calculate looking for compatibility
     */
    private fun calculateLookingForCompatibility(currentUser: User, potentialMatch: User): Double {
        val currentUserLookingFor = currentUser.lookingFor ?: return 0.0
        val potentialMatchLookingFor = potentialMatch.lookingFor ?: return 0.0
        
        // If either has no looking for preferences, return a low default score
        if (currentUserLookingFor.isEmpty() || potentialMatchLookingFor.isEmpty()) {
            return 0.1
        }
        
        // Count common looking for preferences
        val commonLookingFor = currentUserLookingFor.intersect(potentialMatchLookingFor.toSet())
        
        return if (commonLookingFor.isNotEmpty()) {
            // Normalize to 0-1 based on how many common items
            commonLookingFor.size.toDouble() / min(currentUserLookingFor.size, potentialMatchLookingFor.size)
        } else {
            0.0 // No common looking for preferences
        }
    }
    
    /**
     * Calculate verification score boost
     */
    private fun calculateVerificationScore(user: User): Double {
        return when (user.verificationLevel) {
            0 -> 0.0  // Not verified
            1 -> 0.5  // Basic verification
            2 -> 0.8  // Medium verification
            else -> 1.0  // Fully verified (3+)
        }
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     * Returns distance in kilometers
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // kilometers
        
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        
        val c = 2 * acos(sqrt(a))
        
        return earthRadius * c
    }
    
    /**
     * Filter and sort potential matches for a user
     */
    fun getRecommendedMatches(currentUser: User, potentialMatches: List<User>): List<User> {
        // Calculate relevance scores for all potential matches
        val scoredMatches = potentialMatches.map { match ->
            val score = calculateRelevanceScore(currentUser, match)
            Pair(match, score)
        }
        
        // Sort by score (descending)
        return scoredMatches
            .sortedByDescending { it.second }
            .map { it.first }
    }
    
    /**
     * Get a better AI score than TF-IDF or basic ML for this app
     * Use user interactions to refine recommendations
     */
    fun updateUserPreferencesBasedOnInteractions(
        user: User,
        likedProfiles: List<User>,
        dislikedProfiles: List<User>
    ): Map<String, Double> {
        val interestWeights = mutableMapOf<String, Double>()
        
        // Process liked profiles
        likedProfiles.forEach { likedProfile ->
            likedProfile.interests?.forEach { interest ->
                interestWeights[interest] = (interestWeights[interest] ?: 0.0) + 1.0
            }
        }
        
        // Process disliked profiles
        dislikedProfiles.forEach { dislikedProfile ->
            dislikedProfile.interests?.forEach { interest ->
                interestWeights[interest] = (interestWeights[interest] ?: 0.0) - 0.5
            }
        }
        
        // Normalize weights
        if (interestWeights.isNotEmpty()) {
            val minWeight = interestWeights.values.minOrNull() ?: 0.0
            val maxWeight = interestWeights.values.maxOrNull() ?: 1.0
            val range = maxWeight - minWeight
            
            if (range > 0) {
                interestWeights.forEach { (interest, weight) ->
                    interestWeights[interest] = (weight - minWeight) / range
                }
            }
        }
        
        return interestWeights
    }
}