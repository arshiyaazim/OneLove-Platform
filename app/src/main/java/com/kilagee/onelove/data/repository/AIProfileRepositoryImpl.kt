package com.kilagee.onelove.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.model.AIProfileData
import com.kilagee.onelove.domain.model.AIMessage
import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.AIResponseType
import com.kilagee.onelove.domain.model.MessageSender
import com.kilagee.onelove.domain.repository.AIProfileRepository
import com.kilagee.onelove.util.AIResponseGenerator
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import kotlin.random.Random

/**
 * Implementation of AIProfileRepository using Firebase Firestore
 */
class AIProfileRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val responseGenerator: AIResponseGenerator
) : AIProfileRepository {
    
    companion object {
        private const val AI_PROFILES_COLLECTION = "ai_profiles"
        private const val AI_MESSAGES_COLLECTION = "ai_messages"
        private const val AI_CONVERSATIONS_COLLECTION = "ai_conversations"
    }
    
    override fun getAllAIProfiles(): Flow<List<AIProfile>> = callbackFlow {
        val listenerRegistration = firestore
            .collection(AI_PROFILES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val profiles = snapshot?.documents?.mapNotNull { document ->
                    document.data?.let { data ->
                        AIProfile.fromMap(data)
                    }
                } ?: emptyList()
                
                trySend(profiles)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override fun getAIProfiles(
        gender: String?,
        minAge: Int?,
        maxAge: Int?,
        personality: String?,
        limit: Int
    ): Flow<List<AIProfile>> = callbackFlow {
        var query = firestore.collection(AI_PROFILES_COLLECTION)
        
        // Apply filters if provided
        gender?.let {
            query = query.whereEqualTo("gender", it)
        }
        
        minAge?.let {
            query = query.whereGreaterThanOrEqualTo("age", it)
        }
        
        maxAge?.let {
            query = query.whereLessThanOrEqualTo("age", it)
        }
        
        personality?.let {
            query = query.whereArrayContains("personalityTags", it)
        }
        
        // Apply limit
        query = query.limit(limit.toLong())
        
        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            
            val profiles = snapshot?.documents?.mapNotNull { document ->
                document.data?.let { data ->
                    AIProfile.fromMap(data)
                }
            } ?: emptyList()
            
            trySend(profiles)
        }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override fun getAIProfileById(profileId: String): Flow<AIProfile?> = callbackFlow {
        val listenerRegistration = firestore
            .collection(AI_PROFILES_COLLECTION)
            .document(profileId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val profile = snapshot?.data?.let {
                    AIProfile.fromMap(it)
                }
                
                trySend(profile)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override fun getFeaturedAIProfiles(limit: Int): Flow<List<AIProfile>> = callbackFlow {
        val listenerRegistration = firestore
            .collection(AI_PROFILES_COLLECTION)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val profiles = snapshot?.documents?.mapNotNull { document ->
                    document.data?.let { data ->
                        AIProfile.fromMap(data)
                    }
                } ?: emptyList()
                
                trySend(profiles)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override suspend fun createAIProfile(profile: AIProfile): Result<AIProfile> {
        return try {
            val profileId = profile.id.takeIf { it.isNotEmpty() } ?: UUID.randomUUID().toString()
            val profileMap = profile.copy(id = profileId).toMap()
            
            firestore
                .collection(AI_PROFILES_COLLECTION)
                .document(profileId)
                .set(profileMap)
                .await()
            
            Result.success(profile.copy(id = profileId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateAIProfile(profile: AIProfile): Result<AIProfile> {
        return try {
            val profileMap = profile.toMap()
            
            firestore
                .collection(AI_PROFILES_COLLECTION)
                .document(profile.id)
                .update(profileMap)
                .await()
            
            Result.success(profile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteAIProfile(profileId: String): Result<Boolean> {
        return try {
            firestore
                .collection(AI_PROFILES_COLLECTION)
                .document(profileId)
                .delete()
                .await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getConversationMessages(conversationId: String): Flow<List<AIMessage>> = callbackFlow {
        val listenerRegistration = firestore
            .collection(AI_MESSAGES_COLLECTION)
            .whereEqualTo("conversationId", conversationId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val messages = snapshot?.documents?.mapNotNull { document ->
                    document.data?.let { data ->
                        AIMessage.fromMap(data)
                    }
                } ?: emptyList()
                
                trySend(messages)
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override suspend fun sendMessageToAI(
        conversationId: String,
        message: String,
        preferredResponseType: AIResponseType?
    ): Result<AIMessage> {
        return try {
            // First, save the user's message
            val userMessageId = UUID.randomUUID().toString()
            val userMessage = AIMessage(
                id = userMessageId,
                conversationId = conversationId,
                sender = MessageSender.USER,
                content = message,
                timestamp = System.currentTimeMillis(),
                isRead = true
            )
            
            firestore
                .collection(AI_MESSAGES_COLLECTION)
                .document(userMessageId)
                .set(userMessage.toMap())
                .await()
            
            // Then, generate and save the AI's response
            val responseType = preferredResponseType ?: AIResponseType.values().random()
            
            // Get the AI profile associated with this conversation
            val aiProfileSnapshot = firestore
                .collection(AI_PROFILES_COLLECTION)
                .whereEqualTo("conversationId", conversationId)
                .get()
                .await()
            
            val aiProfile = aiProfileSnapshot.documents.firstOrNull()?.data?.let {
                AIProfile.fromMap(it)
            } ?: throw Exception("AI profile not found for conversation $conversationId")
            
            // Generate AI response based on the profile's personality and the response type
            val aiResponse = responseGenerator.generateResponse(
                aiProfile = aiProfile,
                userMessage = message,
                responseType = responseType
            )
            
            val aiMessageId = UUID.randomUUID().toString()
            val aiMessage = AIMessage(
                id = aiMessageId,
                conversationId = conversationId,
                sender = MessageSender.AI,
                content = aiResponse,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                responseType = responseType
            )
            
            firestore
                .collection(AI_MESSAGES_COLLECTION)
                .document(aiMessageId)
                .set(aiMessage.toMap())
                .await()
            
            // Update the conversation in the AI_CONVERSATIONS_COLLECTION for quick access
            val conversationData = mapOf(
                "conversationId" to conversationId,
                "aiProfileId" to aiProfile.id,
                "lastMessage" to aiMessage.content,
                "lastMessageTimestamp" to aiMessage.timestamp,
                "unreadCount" to 1, // From AI to user
                "updatedAt" to System.currentTimeMillis()
            )
            
            firestore
                .collection(AI_CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .set(conversationData)
                .await()
            
            Result.success(aiMessage)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun markConversationAsRead(conversationId: String): Result<Boolean> {
        return try {
            // Get all unread messages from AI in this conversation
            val unreadMessages = firestore
                .collection(AI_MESSAGES_COLLECTION)
                .whereEqualTo("conversationId", conversationId)
                .whereEqualTo("sender", MessageSender.AI.name)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            // Mark each message as read
            unreadMessages.documents.forEach { document ->
                firestore
                    .collection(AI_MESSAGES_COLLECTION)
                    .document(document.id)
                    .update("isRead", true)
                    .await()
            }
            
            // Update the conversation unread count
            firestore
                .collection(AI_CONVERSATIONS_COLLECTION)
                .document(conversationId)
                .update("unreadCount", 0)
                .await()
            
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override fun getUserAIConversations(userId: String): Flow<List<Pair<AIProfile, AIMessage?>>> = callbackFlow {
        val listenerRegistration = firestore
            .collection(AI_CONVERSATIONS_COLLECTION)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val conversationsData = snapshot?.documents?.mapNotNull { document ->
                    document.data?.let { data ->
                        data["conversationId"] as String to data["aiProfileId"] as String
                    }
                } ?: emptyList()
                
                // Load AI profiles and last messages for each conversation
                if (conversationsData.isNotEmpty()) {
                    try {
                        val result = mutableListOf<Pair<AIProfile, AIMessage?>>()
                        
                        for ((conversationId, aiProfileId) in conversationsData) {
                            // Get AI profile
                            val profileSnapshot = firestore
                                .collection(AI_PROFILES_COLLECTION)
                                .document(aiProfileId)
                                .get()
                                .await()
                            
                            val profile = profileSnapshot.data?.let {
                                AIProfile.fromMap(it)
                            } ?: continue
                            
                            // Get last message
                            val lastMessageSnapshot = firestore
                                .collection(AI_MESSAGES_COLLECTION)
                                .whereEqualTo("conversationId", conversationId)
                                .orderBy("timestamp", Query.Direction.DESCENDING)
                                .limit(1)
                                .get()
                                .await()
                            
                            val lastMessage = lastMessageSnapshot.documents.firstOrNull()?.data?.let {
                                AIMessage.fromMap(it)
                            }
                            
                            result.add(profile to lastMessage)
                        }
                        
                        trySend(result)
                    } catch (e: Exception) {
                        close(e)
                    }
                } else {
                    trySend(emptyList())
                }
            }
        
        awaitClose { listenerRegistration.remove() }
    }
    
    override suspend fun createInitialAIProfiles(): Result<Int> {
        return try {
            val createdProfiles = AIProfileData.getInitialAIProfiles().map { profile ->
                createAIProfile(profile)
            }
            
            val successCount = createdProfiles.count { it.isSuccess }
            Result.success(successCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}