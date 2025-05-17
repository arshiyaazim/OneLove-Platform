package com.kilagee.onelove.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

/**
 * Data class for AI response pools
 */
data class AIResponsePool(
    @DocumentId
    val id: String = "",
    
    val name: String = "",
    
    @PropertyName("personality_type")
    val personalityType: PersonalityType = PersonalityType.ROMANTIC,
    
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    val responses: List<AIResponseTemplate> = listOf(),
    
    @PropertyName("follow_up_responses")
    val followUpResponses: List<AIFollowUpResponse> = listOf(),
    
    @PropertyName("conversation_starters")
    val conversationStarters: List<String> = listOf(),
    
    @PropertyName("topic_transitions")
    val topicTransitions: List<String> = listOf(),
    
    @PropertyName("emotional_responses")
    val emotionalResponses: Map<EmotionType, List<String>> = mapOf(),
    
    @PropertyName("flirty_phrases")
    val flirtyPhrases: List<String> = listOf(),
    
    @PropertyName("romantic_phrases")
    val romanticPhrases: List<String> = listOf(),
    
    @PropertyName("humorous_phrases")
    val humorousPhrases: List<String> = listOf(),
    
    @PropertyName("intellectual_phrases")
    val intellectualPhrases: List<String> = listOf(),
    
    @PropertyName("supportive_phrases")
    val supportivePhrases: List<String> = listOf(),
    
    @PropertyName("question_phrases")
    val questionPhrases: List<String> = listOf(),
    
    val emojis: List<String> = listOf()
)

/**
 * Data class for an individual AI response template
 */
data class AIResponseTemplate(
    val id: String = "",
    
    val topic: String = "",
    
    val keywords: List<String> = listOf(),
    
    val negativeKeywords: List<String> = listOf(),
    
    val style: ResponseStyle = ResponseStyle.CASUAL,
    
    val text: String = "",
    
    val variations: List<String> = listOf(),
    
    @PropertyName("requires_context")
    val requiresContext: Boolean = false,
    
    @PropertyName("can_start_conversation")
    val canStartConversation: Boolean = false,
    
    val priority: Int = 0, // Higher values = higher priority
    
    val category: ResponseCategory = ResponseCategory.GENERAL,
    
    @PropertyName("emotion_type")
    val emotionType: EmotionType? = null,
    
    @PropertyName("emoji_compatible")
    val emojiCompatible: Boolean = true
)

/**
 * Data class for contextual follow-up responses
 */
data class AIFollowUpResponse(
    val id: String = "",
    
    @PropertyName("trigger_id")
    val triggerId: String = "", // ID of the template that triggers this follow-up
    
    val text: String = "",
    
    val variations: List<String> = listOf(),
    
    val delay: Int = 0, // Delay in seconds before sending this follow-up
    
    @PropertyName("condition_keywords")
    val conditionKeywords: List<String> = listOf(), // Must be present in user's reply
    
    @PropertyName("negative_condition_keywords")
    val negativeConditionKeywords: List<String> = listOf(), // Must NOT be present in user's reply
    
    val style: ResponseStyle = ResponseStyle.CASUAL
)

/**
 * Categories for AI responses
 */
enum class ResponseCategory {
    GREETING,
    FAREWELL,
    FLIRTING,
    ROMANCE,
    PERSONAL_INFO,
    HOBBIES_INTERESTS,
    DAILY_LIFE,
    FUTURE_PLANS,
    RELATIONSHIP_TALK,
    CASUAL_CHAT,
    DEEP_CONVERSATION,
    HUMOR,
    EMOTIONAL_SUPPORT,
    COMPLIMENT,
    QUESTION,
    GENERAL
}

/**
 * Emotion types for AI responses
 */
enum class EmotionType {
    HAPPY,
    SAD,
    EXCITED,
    CURIOUS,
    FLIRTY,
    ROMANTIC,
    SURPRISED,
    CONCERNED,
    APOLOGETIC,
    GRATEFUL,
    THOUGHTFUL,
    AMUSED,
    NEUTRAL
}

/**
 * Data class for an AI message in a conversation
 */
data class AIMessage(
    val id: String = "",
    
    @PropertyName("conversation_id")
    val conversationId: String = "",
    
    @PropertyName("ai_profile_id")
    val aiProfileId: String = "",
    
    @PropertyName("user_id")
    val userId: String = "",
    
    val content: String = "",
    
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("response_template_id")
    val responseTemplateId: String? = null,
    
    @PropertyName("is_from_ai")
    val isFromAI: Boolean = false,
    
    @PropertyName("emotion_type")
    val emotionType: EmotionType? = null,
    
    @PropertyName("response_style")
    val responseStyle: ResponseStyle? = null,
    
    val topic: String? = null,
    
    val read: Boolean = false,
    
    @PropertyName("response_delay")
    val responseDelay: Int = 0
)

/**
 * Data class for AI conversation
 */
data class AIConversation(
    @DocumentId
    val id: String = "",
    
    @PropertyName("ai_profile_id")
    val aiProfileId: String = "",
    
    @PropertyName("user_id")
    val userId: String = "",
    
    @PropertyName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    @PropertyName("updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
    
    @PropertyName("last_message")
    val lastMessage: String = "",
    
    @PropertyName("last_message_time")
    val lastMessageTime: Long = System.currentTimeMillis(),
    
    @PropertyName("message_count")
    val messageCount: Int = 0,
    
    @PropertyName("unread_count")
    val unreadCount: Int = 0,
    
    @PropertyName("current_topics")
    val currentTopics: List<String> = listOf(),
    
    @PropertyName("is_active")
    val isActive: Boolean = true,
    
    @PropertyName("ai_personality_type")
    val aiPersonalityType: PersonalityType = PersonalityType.ROMANTIC,
    
    @PropertyName("ai_name")
    val aiName: String = "",
    
    @PropertyName("ai_photo_url")
    val aiPhotoUrl: String = ""
)