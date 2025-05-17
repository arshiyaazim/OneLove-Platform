package com.kilagee.onelove.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.kilagee.onelove.data.model.AIConversation
import com.kilagee.onelove.data.model.AIMessage
import com.kilagee.onelove.data.model.AIProfile
import com.kilagee.onelove.data.model.AIResponsePool
import com.kilagee.onelove.data.model.EmotionType
import com.kilagee.onelove.data.model.Gender
import com.kilagee.onelove.data.model.PersonalityType
import com.kilagee.onelove.data.model.ResponseStyle
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.AIProfileRepository
import com.kilagee.onelove.domain.repository.AIProfileStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseAIProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val functions: FirebaseFunctions
) : AIProfileRepository {
    
    private val profilesCollection = firestore.collection("ai_profiles")
    private val responsesCollection = firestore.collection("ai_response_pools")
    private val conversationsCollection = firestore.collection("ai_conversations")
    private val messagesCollection = firestore.collection("ai_messages")
    private val statsCollection = firestore.collection("ai_profile_stats")
    
    override fun getAIProfiles(
        limit: Int,
        lastProfileId: String?,
        gender: Gender?,
        minAge: Int?,
        maxAge: Int?,
        country: String?,
        personalityTypes: List<PersonalityType>?,
        interestTags: List<String>?
    ): Flow<Resource<List<AIProfile>>> = flow {
        emit(Resource.Loading)
        
        try {
            var query = profilesCollection
                .whereEqualTo("isActive", true)
                .limit(limit.toLong())
            
            // Apply filters if provided
            if (gender != null) {
                query = query.whereEqualTo("gender", gender)
            }
            
            if (minAge != null) {
                query = query.whereGreaterThanOrEqualTo("age", minAge)
            }
            
            if (maxAge != null) {
                query = query.whereLessThanOrEqualTo("age", maxAge)
            }
            
            if (country != null) {
                query = query.whereEqualTo("country", country)
            }
            
            if (personalityTypes != null && personalityTypes.isNotEmpty()) {
                if (personalityTypes.size == 1) {
                    query = query.whereEqualTo("personalityType", personalityTypes.first())
                } else {
                    query = query.whereIn("personalityType", personalityTypes.map { it.name })
                }
            }
            
            // Use startAfter for pagination if lastProfileId is provided
            if (lastProfileId != null) {
                val lastProfile = profilesCollection.document(lastProfileId).get().await()
                if (lastProfile.exists()) {
                    query = query.startAfter(lastProfile)
                }
            }
            
            // Order by creation date (newest first)
            query = query.orderBy("createdAt", Query.Direction.DESCENDING)
            
            val documents = query.get().await()
            val profiles = documents.toObjects(AIProfile::class.java)
            
            // Apply interest tag filtering client-side if needed
            val filteredProfiles = if (interestTags != null && interestTags.isNotEmpty()) {
                profiles.filter { profile ->
                    profile.interests.any { interest -> 
                        interestTags.any { tag -> interest.contains(tag, ignoreCase = true) }
                    }
                }
            } else {
                profiles
            }
            
            emit(Resource.success(filteredProfiles))
        } catch (e: Exception) {
            emit(Resource.error("Failed to load AI profiles: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getAIProfileById(profileId: String): Flow<Resource<AIProfile>> = flow {
        emit(Resource.Loading)
        
        try {
            val document = profilesCollection.document(profileId).get().await()
            if (document.exists()) {
                val profile = document.toObject(AIProfile::class.java)
                if (profile != null) {
                    emit(Resource.success(profile))
                } else {
                    emit(Resource.error("Failed to parse AI profile"))
                }
            } else {
                emit(Resource.error("AI profile not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to load AI profile: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getRecommendedAIProfiles(
        userId: String,
        limit: Int
    ): Flow<Resource<List<AIProfile>>> = flow {
        emit(Resource.Loading)
        
        try {
            // Call a Cloud Function to get personalized recommendations
            val data = hashMapOf(
                "userId" to userId,
                "limit" to limit
            )
            
            val result = functions
                .getHttpsCallable("getRecommendedAIProfiles")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val profileIds = result?.get("profileIds") as? List<String>
            
            if (profileIds != null && profileIds.isNotEmpty()) {
                // Batch get the profiles
                val profiles = profileIds.chunked(10).flatMap { chunk ->
                    val docs = firestore.collection("ai_profiles")
                        .whereIn("__name__", chunk)
                        .get()
                        .await()
                    
                    docs.toObjects(AIProfile::class.java)
                }
                
                emit(Resource.success(profiles))
            } else {
                // Fallback to a basic query if no recommendations
                val docs = profilesCollection
                    .whereEqualTo("isActive", true)
                    .orderBy("matchRate", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                
                val profiles = docs.toObjects(AIProfile::class.java)
                emit(Resource.success(profiles))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to load recommended AI profiles: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getResponsePoolForProfile(profileId: String): Flow<Resource<AIResponsePool>> = flow {
        emit(Resource.Loading)
        
        try {
            val profileDoc = profilesCollection.document(profileId).get().await()
            val profile = profileDoc.toObject(AIProfile::class.java)
            
            if (profile != null) {
                val responsePoolId = profile.responsePoolId
                
                if (responsePoolId.isNotEmpty()) {
                    val responsePoolDoc = responsesCollection.document(responsePoolId).get().await()
                    val responsePool = responsePoolDoc.toObject(AIResponsePool::class.java)
                    
                    if (responsePool != null) {
                        emit(Resource.success(responsePool))
                    } else {
                        emit(Resource.error("Failed to parse response pool"))
                    }
                } else {
                    // Try to get a response pool matching the personality type
                    val responsePoolDocs = responsesCollection
                        .whereEqualTo("personalityType", profile.personalityType)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!responsePoolDocs.isEmpty) {
                        val responsePool = responsePoolDocs.documents[0].toObject(AIResponsePool::class.java)
                        if (responsePool != null) {
                            emit(Resource.success(responsePool))
                        } else {
                            emit(Resource.error("Failed to parse response pool"))
                        }
                    } else {
                        emit(Resource.error("No response pool found for this profile"))
                    }
                }
            } else {
                emit(Resource.error("AI profile not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to load response pool: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun createConversation(
        userId: String,
        profileId: String
    ): Flow<Resource<AIConversation>> = flow {
        emit(Resource.Loading)
        
        try {
            // Check if a conversation already exists
            val existingConvDocs = conversationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("aiProfileId", profileId)
                .whereEqualTo("isActive", true)
                .limit(1)
                .get()
                .await()
            
            if (!existingConvDocs.isEmpty) {
                val existingConversation = existingConvDocs.documents[0].toObject(AIConversation::class.java)
                if (existingConversation != null) {
                    emit(Resource.success(existingConversation))
                    return@flow
                }
            }
            
            // Get the profile to create a new conversation
            val profileDoc = profilesCollection.document(profileId).get().await()
            val profile = profileDoc.toObject(AIProfile::class.java)
            
            if (profile != null) {
                val conversationId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                
                val conversation = AIConversation(
                    id = conversationId,
                    aiProfileId = profileId,
                    userId = userId,
                    createdAt = now,
                    updatedAt = now,
                    lastMessage = "",
                    lastMessageTime = now,
                    messageCount = 0,
                    unreadCount = 0,
                    currentTopics = listOf(),
                    isActive = true,
                    aiPersonalityType = profile.personalityType,
                    aiName = profile.name,
                    aiPhotoUrl = profile.photoUrl
                )
                
                conversationsCollection.document(conversationId).set(conversation).await()
                
                // Generate an initial message from the AI
                val initialMessage = withContext(Dispatchers.IO) {
                    generateInitialAIMessage(conversation, profile)
                }
                
                if (initialMessage != null) {
                    // Update conversation with initial message
                    val updatedConversation = conversation.copy(
                        lastMessage = initialMessage.content,
                        lastMessageTime = initialMessage.createdAt,
                        messageCount = 1,
                        unreadCount = 1
                    )
                    
                    conversationsCollection.document(conversationId).set(updatedConversation).await()
                    emit(Resource.success(updatedConversation))
                } else {
                    emit(Resource.success(conversation))
                }
            } else {
                emit(Resource.error("AI profile not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to create conversation: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    private suspend fun generateInitialAIMessage(conversation: AIConversation, profile: AIProfile): AIMessage? {
        try {
            // Get the response pool
            val responsePoolId = profile.responsePoolId
            
            val responsePoolDoc = if (responsePoolId.isNotEmpty()) {
                responsesCollection.document(responsePoolId).get().await()
            } else {
                val responsePoolDocs = responsesCollection
                    .whereEqualTo("personalityType", profile.personalityType)
                    .limit(1)
                    .get()
                    .await()
                
                if (!responsePoolDocs.isEmpty) responsePoolDocs.documents[0] else null
            }
            
            val responsePool = responsePoolDoc?.toObject(AIResponsePool::class.java)
            
            if (responsePool != null && responsePool.conversationStarters.isNotEmpty()) {
                // Choose a random conversation starter
                val starter = responsePool.conversationStarters.random()
                
                val messageId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                
                val message = AIMessage(
                    id = messageId,
                    conversationId = conversation.id,
                    aiProfileId = profile.id,
                    userId = conversation.userId,
                    content = starter,
                    createdAt = now,
                    responseTemplateId = null,
                    isFromAI = true,
                    emotionType = EmotionType.NEUTRAL,
                    responseStyle = ResponseStyle.CASUAL,
                    topic = "greeting",
                    read = false,
                    responseDelay = 0
                )
                
                messagesCollection.document(messageId).set(message).await()
                return message
            }
            
            return null
        } catch (e: Exception) {
            return null
        }
    }
    
    override fun getUserAIConversations(userId: String): Flow<Resource<List<AIConversation>>> = flow {
        emit(Resource.Loading)
        
        try {
            val documents = conversationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val conversations = documents.toObjects(AIConversation::class.java)
            emit(Resource.success(conversations))
        } catch (e: Exception) {
            emit(Resource.error("Failed to load conversations: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getConversation(conversationId: String): Flow<Resource<AIConversation>> = flow {
        emit(Resource.Loading)
        
        try {
            val document = conversationsCollection.document(conversationId).get().await()
            if (document.exists()) {
                val conversation = document.toObject(AIConversation::class.java)
                if (conversation != null) {
                    emit(Resource.success(conversation))
                } else {
                    emit(Resource.error("Failed to parse conversation"))
                }
            } else {
                emit(Resource.error("Conversation not found"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to load conversation: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun sendMessageToAI(
        conversationId: String,
        userId: String,
        message: String
    ): Flow<Resource<AIMessage>> = flow {
        emit(Resource.Loading)
        
        try {
            val conversationDoc = conversationsCollection.document(conversationId).get().await()
            val conversation = conversationDoc.toObject(AIConversation::class.java)
            
            if (conversation != null && conversation.userId == userId) {
                val messageId = UUID.randomUUID().toString()
                val now = System.currentTimeMillis()
                
                val userMessage = AIMessage(
                    id = messageId,
                    conversationId = conversationId,
                    aiProfileId = conversation.aiProfileId,
                    userId = userId,
                    content = message,
                    createdAt = now,
                    responseTemplateId = null,
                    isFromAI = false,
                    emotionType = null,
                    responseStyle = null,
                    topic = null,
                    read = true,
                    responseDelay = 0
                )
                
                messagesCollection.document(messageId).set(userMessage).await()
                
                // Update conversation
                val updatedConversation = conversation.copy(
                    lastMessage = message,
                    lastMessageTime = now,
                    messageCount = conversation.messageCount + 1,
                    updatedAt = now
                )
                
                conversationsCollection.document(conversationId).set(updatedConversation).await()
                
                // Generate AI response asynchronously
                generateAIResponse(conversationId, messageId)
                
                emit(Resource.success(userMessage))
            } else {
                emit(Resource.error("Conversation not found or access denied"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to send message: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun generateAIResponse(
        conversationId: String,
        userMessageId: String,
        preferredStyle: ResponseStyle?,
        preferredEmotion: EmotionType?
    ): Flow<Resource<AIMessage>> = flow {
        emit(Resource.Loading)
        
        try {
            // Call a Cloud Function to generate the response
            val data = hashMapOf(
                "conversationId" to conversationId,
                "userMessageId" to userMessageId
            )
            
            preferredStyle?.let { data["preferredStyle"] = it.name }
            preferredEmotion?.let { data["preferredEmotion"] = it.name }
            
            val result = functions
                .getHttpsCallable("generateAIResponse")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val aiMessageId = result?.get("messageId") as? String
            
            if (aiMessageId != null) {
                // Get the generated message
                val messageDoc = messagesCollection.document(aiMessageId).get().await()
                val aiMessage = messageDoc.toObject(AIMessage::class.java)
                
                if (aiMessage != null) {
                    emit(Resource.success(aiMessage))
                } else {
                    emit(Resource.error("Failed to parse AI message"))
                }
            } else {
                emit(Resource.error("Failed to generate AI response"))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to generate AI response: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getConversationMessages(
        conversationId: String,
        limit: Int,
        beforeMessageId: String?
    ): Flow<Resource<List<AIMessage>>> = flow {
        emit(Resource.Loading)
        
        try {
            var query = messagesCollection
                .whereEqualTo("conversationId", conversationId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
            
            if (beforeMessageId != null) {
                val beforeMessage = messagesCollection.document(beforeMessageId).get().await()
                if (beforeMessage.exists()) {
                    query = query.startAfter(beforeMessage)
                }
            }
            
            val documents = query.get().await()
            val messages = documents.toObjects(AIMessage::class.java).sortedBy { it.createdAt }
            
            emit(Resource.success(messages))
        } catch (e: Exception) {
            emit(Resource.error("Failed to load messages: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun markMessagesAsRead(
        conversationId: String,
        messageIds: List<String>
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val batch = firestore.batch()
            
            messageIds.forEach { messageId ->
                val messageRef = messagesCollection.document(messageId)
                batch.update(messageRef, mapOf("read" to true))
            }
            
            // Update conversation unread count
            val conversationRef = conversationsCollection.document(conversationId)
            batch.update(conversationRef, mapOf("unreadCount" to 0))
            
            batch.commit().await()
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to mark messages as read: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun deleteConversation(conversationId: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            // Just mark the conversation as inactive, don't actually delete
            conversationsCollection.document(conversationId)
                .update(mapOf("isActive" to false))
                .await()
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to delete conversation: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getUnreadMessagesCount(userId: String): Flow<Resource<Int>> = flow {
        emit(Resource.Loading)
        
        try {
            val conversationDocs = conversationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .get()
                .await()
            
            val conversations = conversationDocs.toObjects(AIConversation::class.java)
            val totalUnread = conversations.sumOf { it.unreadCount }
            
            emit(Resource.success(totalUnread))
        } catch (e: Exception) {
            emit(Resource.error("Failed to get unread count: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun reportAIBehavior(
        conversationId: String,
        messageId: String,
        reason: String,
        details: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading)
        
        try {
            val data = hashMapOf(
                "conversationId" to conversationId,
                "messageId" to messageId,
                "reason" to reason
            )
            
            details?.let { data["details"] = it }
            
            functions
                .getHttpsCallable("reportAIBehavior")
                .call(data)
                .await()
            
            emit(Resource.success(Unit))
        } catch (e: Exception) {
            emit(Resource.error("Failed to report AI behavior: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun getAIProfileStats(profileId: String): Flow<Resource<AIProfileStats>> = flow {
        emit(Resource.Loading)
        
        try {
            val statsDoc = statsCollection.document(profileId).get().await()
            
            if (statsDoc.exists()) {
                val data = statsDoc.data
                
                if (data != null) {
                    val messageCount = (data["messageCount"] as? Number)?.toInt() ?: 0
                    val conversationCount = (data["conversationCount"] as? Number)?.toInt() ?: 0
                    val avgConversationLength = (data["avgConversationLength"] as? Number)?.toInt() ?: 0
                    val popularity = (data["popularity"] as? Number)?.toDouble() ?: 0.0
                    val positiveRatingPercentage = (data["positiveRatingPercentage"] as? Number)?.toDouble() ?: 0.0
                    
                    @Suppress("UNCHECKED_CAST")
                    val topResponseCategories = (data["topResponseCategories"] as? Map<String, Number>)?.map {
                        Pair(it.key, it.value.toInt())
                    } ?: listOf()
                    
                    val activeConversations = (data["activeConversations"] as? Number)?.toInt() ?: 0
                    val avgResponseTime = (data["avgResponseTime"] as? Number)?.toLong() ?: 0L
                    
                    val stats = AIProfileStats(
                        profileId = profileId,
                        messageCount = messageCount,
                        conversationCount = conversationCount,
                        avgConversationLength = avgConversationLength,
                        popularity = popularity,
                        positiveRatingPercentage = positiveRatingPercentage,
                        topResponseCategories = topResponseCategories,
                        activeConversations = activeConversations,
                        avgResponseTime = avgResponseTime
                    )
                    
                    emit(Resource.success(stats))
                } else {
                    emit(Resource.error("Failed to parse AI profile stats"))
                }
            } else {
                // Return default stats if none exist
                val defaultStats = AIProfileStats(
                    profileId = profileId,
                    messageCount = 0,
                    conversationCount = 0,
                    avgConversationLength = 0,
                    popularity = 0.5,
                    positiveRatingPercentage = 0.0,
                    topResponseCategories = listOf(),
                    activeConversations = 0,
                    avgResponseTime = 0
                )
                
                emit(Resource.success(defaultStats))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to load AI profile stats: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
    
    override fun searchAIProfiles(
        query: String,
        limit: Int
    ): Flow<Resource<List<AIProfile>>> = flow {
        emit(Resource.Loading)
        
        try {
            // Call a Cloud Function to search with proper indexing
            val data = hashMapOf(
                "query" to query,
                "limit" to limit
            )
            
            val result = functions
                .getHttpsCallable("searchAIProfiles")
                .call(data)
                .await()
                .data as? Map<*, *>
                
            val profileIds = result?.get("profileIds") as? List<String>
            
            if (profileIds != null && profileIds.isNotEmpty()) {
                // Batch get the profiles
                val profiles = profileIds.chunked(10).flatMap { chunk ->
                    val docs = firestore.collection("ai_profiles")
                        .whereIn("__name__", chunk)
                        .get()
                        .await()
                    
                    docs.toObjects(AIProfile::class.java)
                }
                
                emit(Resource.success(profiles))
            } else {
                // Fallback - basic search by name
                val docsName = profilesCollection
                    .whereGreaterThanOrEqualTo("name", query)
                    .whereLessThanOrEqualTo("name", query + "\uf8ff")
                    .whereEqualTo("isActive", true)
                    .limit(limit.toLong())
                    .get()
                    .await()
                
                val profiles = docsName.toObjects(AIProfile::class.java)
                emit(Resource.success(profiles))
            }
        } catch (e: Exception) {
            emit(Resource.error("Failed to search AI profiles: ${e.message}"))
        }
    }.flowOn(Dispatchers.IO)
}