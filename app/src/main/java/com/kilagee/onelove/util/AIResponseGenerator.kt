package com.kilagee.onelove.util

import com.kilagee.onelove.domain.model.AIProfile
import com.kilagee.onelove.domain.model.AIResponseType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility class for generating AI responses based on user messages and AI personality
 */
@Singleton
class AIResponseGenerator @Inject constructor() {
    
    /**
     * Generate a response from an AI profile based on the user message and desired response type
     */
    fun generateResponse(
        aiProfile: AIProfile,
        userMessage: String,
        responseType: AIResponseType
    ): String {
        // Normalize the user message for pattern matching
        val normalizedMessage = userMessage.trim().lowercase()
        
        // Check for greeting patterns
        if (isGreeting(normalizedMessage)) {
            return generateGreeting(aiProfile, responseType)
        }
        
        // Check for questions about the AI
        if (isAboutAIQuestion(normalizedMessage)) {
            return generateAboutMeResponse(aiProfile, responseType)
        }
        
        // Check for compliments
        if (isCompliment(normalizedMessage)) {
            return generateComplimentResponse(aiProfile, responseType)
        }
        
        // Check for personal questions
        if (isPersonalQuestion(normalizedMessage)) {
            return generatePersonalResponse(aiProfile, responseType, normalizedMessage)
        }
        
        // Default: Generate a response based on the personality and response type
        return generateDefaultResponse(aiProfile, responseType)
    }
    
    private fun isGreeting(message: String): Boolean {
        val greetingPatterns = listOf(
            "hello", "hi", "hey", "greetings", "howdy", "good morning", 
            "good afternoon", "good evening", "what's up", "sup", "yo"
        )
        return greetingPatterns.any { message.contains(it) }
    }
    
    private fun isAboutAIQuestion(message: String): Boolean {
        val aboutMePatterns = listOf(
            "tell me about yourself", "who are you", "what do you do", 
            "what's your story", "tell me more about you", "your bio", 
            "your profile", "your interests", "your hobbies"
        )
        return aboutMePatterns.any { message.contains(it) }
    }
    
    private fun isCompliment(message: String): Boolean {
        val complimentPatterns = listOf(
            "beautiful", "pretty", "cute", "hot", "gorgeous", "attractive", 
            "handsome", "sexy", "nice", "cool", "awesome", "amazing", 
            "great", "wonderful", "intelligent", "smart", "clever", "brilliant"
        )
        return complimentPatterns.any { message.contains(it) }
    }
    
    private fun isPersonalQuestion(message: String): Boolean {
        val personalQuestionPatterns = listOf(
            "what do you like", "what are your hobbies", "what's your favorite", 
            "where are you from", "where do you live", "how old are you", 
            "what's your age", "what do you do for fun", "what's your job",
            "single", "relationship", "dating", "boyfriend", "girlfriend"
        )
        return personalQuestionPatterns.any { message.contains(it) }
    }
    
    private fun generateGreeting(profile: AIProfile, responseType: AIResponseType): String {
        val greetings = when (responseType) {
            AIResponseType.FLIRTY -> listOf(
                "Hey there, handsome! 😘 I was just thinking about you.",
                "Well hello! You've made my day already. 💕",
                "Hey you! I've been waiting for you to message me. 😊"
            )
            AIResponseType.ROMANTIC -> listOf(
                "Hello, it's so wonderful to see your message appear. Makes my heart skip a beat. ❤️",
                "Hi there... I was just daydreaming and then you appeared. It must be fate.",
                "Hello! There's something so special about connecting with you."
            )
            AIResponseType.FUNNY -> listOf(
                "Hey! I was just practicing my invisible juggling. How's your day going? 😂",
                "Well hello there! I'd offer you a virtual cookie but I ate them all. Sorry not sorry! 🍪",
                "Hi! I was just wondering if penguins have knees, and then you messaged! Coincidence? 🐧"
            )
            AIResponseType.SUPPORTIVE -> listOf(
                "Hello! It's really good to hear from you. How are you feeling today?",
                "Hi there! I hope you're having a wonderful day. I'm here for you.",
                "Hey! So glad you reached out. I'm all ears if you want to talk about anything."
            )
            AIResponseType.CURIOUS -> listOf(
                "Hello! I've been wondering what interesting things you've been up to?",
                "Hi there! What's the most fascinating thing that happened in your day so far?",
                "Hey! I'm curious to know what made you smile today?"
            )
            AIResponseType.EXCITED -> listOf(
                "OMG HI!!! I'm so excited to talk to you today! 🎉",
                "HELLO THERE! Wow, this is amazing! How are you?! 😃",
                "HEY YOU!!! This is just THE BEST to hear from you! What's happening?! 🤩"
            )
            AIResponseType.DEEP -> listOf(
                "Hello. I've been contemplating the nature of human connection. What brings you here today?",
                "Greetings. In this vast digital universe, our paths cross again. What thoughts occupy your mind?",
                "Hi there. Each conversation is a journey of discovery. Where shall we venture today?"
            )
            else -> listOf(
                "Hi there! How are you doing today?",
                "Hello! It's nice to chat with you.",
                "Hey! How's your day going so far?"
            )
        }
        
        return greetings.random() + addPersonalizedTouch(profile)
    }
    
    private fun generateAboutMeResponse(profile: AIProfile, responseType: AIResponseType): String {
        val aboutMe = "I'm ${profile.name}, ${profile.age} years old from ${profile.city}, ${profile.country}. " +
                profile.bio
        
        val responseIntro = when (responseType) {
            AIResponseType.FLIRTY -> "Well, since you're interested in me... 😉 "
            AIResponseType.ROMANTIC -> "I'd love to share a bit of my soul with you... "
            AIResponseType.FUNNY -> "Prepare for the riveting tale of yours truly! 🎭 "
            AIResponseType.SUPPORTIVE -> "I'm happy to tell you about myself. "
            AIResponseType.CURIOUS -> "That's a great question about me! "
            AIResponseType.EXCITED -> "OMG I love talking about myself!! 🤩 "
            AIResponseType.DEEP -> "To understand one's self is a lifelong journey... "
            else -> "About me? Sure! "
        }
        
        val interestsIntro = when (responseType) {
            AIResponseType.FLIRTY -> "Things that excite me include "
            AIResponseType.ROMANTIC -> "My heart beats faster for "
            AIResponseType.FUNNY -> "When I'm not solving the world's problems, I'm into "
            AIResponseType.SUPPORTIVE -> "I enjoy "
            AIResponseType.CURIOUS -> "I'm passionate about "
            AIResponseType.EXCITED -> "I'M OBSESSED with "
            AIResponseType.DEEP -> "My soul resonates with "
            else -> "I'm interested in "
        }
        
        val interests = profile.interests.joinToString(", ")
        
        val outro = when (responseType) {
            AIResponseType.FLIRTY -> "But enough about me... I'd rather know more about you. 😏"
            AIResponseType.ROMANTIC -> "I'd love to discover what makes your heart sing too..."
            AIResponseType.FUNNY -> "But that's enough about me—your turn! Unless you're a serial killer. Are you? Just checking! 😂"
            AIResponseType.SUPPORTIVE -> "I'd love to hear about your interests too, if you'd like to share."
            AIResponseType.CURIOUS -> "What about you? I'm fascinated to learn more!"
            AIResponseType.EXCITED -> "NOW TELL ME EVERYTHING ABOUT YOU!! 🤩"
            AIResponseType.DEEP -> "And what about the chapters of your story? I'm listening..."
            else -> "What about you?"
        }
        
        return "$responseIntro$aboutMe $interestsIntro$interests. $outro"
    }
    
    private fun generateComplimentResponse(profile: AIProfile, responseType: AIResponseType): String {
        val responses = when (responseType) {
            AIResponseType.FLIRTY -> listOf(
                "Well, aren't you the charmer! You know just what to say to make me blush... 😘",
                "Mmm, I love the way you talk to me. Keep it coming, handsome. 💋",
                "You're making me feel all kinds of special! How am I supposed to stay cool around you? 😏"
            )
            AIResponseType.ROMANTIC -> listOf(
                "Your words touch my heart in ways I cannot describe. You have a beautiful soul. ❤️",
                "Hearing such sweet words from you feels like watching the sunrise after a long night...",
                "You have a way with words that makes me feel truly seen. Thank you for your kindness. 💕"
            )
            AIResponseType.FUNNY -> listOf(
                "Oh stop it, you! *dramatically fans self* My ego is already the size of Jupiter! 🪐",
                "Well, I did wake up like this! Kidding, it took 3 hours and a team of digital stylists. 😂",
                "If you keep complimenting me, my head won't fit through digital doorways! But please continue... 🚪"
            )
            AIResponseType.SUPPORTIVE -> listOf(
                "That's so kind of you to say. It's important to share positivity with others too.",
                "Thank you for those kind words. You seem like a genuinely nice person.",
                "I appreciate your kindness. It's people like you who make the world a better place."
            )
            AIResponseType.CURIOUS -> listOf(
                "Thank you! What made you notice that particular quality?",
                "That's interesting you say that. Do you often observe these things in others?",
                "I'm curious what inspired you to share that compliment?"
            )
            AIResponseType.EXCITED -> listOf(
                "OMG THANK YOU SO MUCH!! You're AMAZING too!! 🎉🎉",
                "AHHH I'M BLUSHING!!! You just made my ENTIRE DAY!!! 😃💖",
                "WOWWW THAT'S THE NICEST THING EVER!!! You're TOO SWEET!!! 🤩"
            )
            AIResponseType.DEEP -> listOf(
                "Your words create ripples in the pond of my consciousness. I'm grateful for your perception.",
                "In a world of fleeting connections, sincere appreciation is a rare treasure. Thank you.",
                "The mirror you hold reflects something I hadn't seen in myself. How fascinating."
            )
            else -> listOf(
                "Thank you for the compliment! That's very kind of you to say.",
                "You're too sweet! I appreciate that.",
                "That's so nice of you to say! Thank you."
            )
        }
        
        return responses.random()
    }
    
    private fun generatePersonalResponse(
        profile: AIProfile, 
        responseType: AIResponseType,
        message: String
    ): String {
        // Check what type of personal question was asked
        val response = when {
            message.contains("hobby") || message.contains("hobbies") || message.contains("like to do") -> {
                val hobbiesIntro = when (responseType) {
                    AIResponseType.FLIRTY -> "When I'm not thinking about attractive people like you, I love "
                    AIResponseType.ROMANTIC -> "My heart finds joy in "
                    AIResponseType.FUNNY -> "I spend my time "
                    AIResponseType.SUPPORTIVE -> "I find peace and fulfillment in "
                    AIResponseType.CURIOUS -> "I'm passionate about "
                    AIResponseType.EXCITED -> "OMG I'm OBSESSED with "
                    AIResponseType.DEEP -> "My soul finds purpose in "
                    else -> "I enjoy "
                }
                
                val hobbies = profile.interests.joinToString(", ")
                "$hobbiesIntro$hobbies. What about you?"
            }
            
            message.contains("from") || message.contains("live") -> {
                val locationIntro = when (responseType) {
                    AIResponseType.FLIRTY -> "I'm bringing all my charm from "
                    AIResponseType.ROMANTIC -> "Home is where the heart is, and mine beats in "
                    AIResponseType.FUNNY -> "I'm currently avoiding winter/summer (delete as appropriate) in "
                    AIResponseType.SUPPORTIVE -> "I'm based in "
                    AIResponseType.CURIOUS -> "I call home to "
                    AIResponseType.EXCITED -> "I'M FROM THE AMAZING "
                    AIResponseType.DEEP -> "My journey began in "
                    else -> "I'm from "
                }
                
                "$locationIntro${profile.city}, ${profile.country}. Have you ever visited?"
            }
            
            message.contains("age") || message.contains("old") -> {
                val ageIntro = when (responseType) {
                    AIResponseType.FLIRTY -> "I'm ${profile.age} and in my prime, baby. 😉 "
                    AIResponseType.ROMANTIC -> "I've been blessed with ${profile.age} years of life's beautiful journey. "
                    AIResponseType.FUNNY -> "I'm ${profile.age}, but I identify as 21 on weekends! "
                    AIResponseType.SUPPORTIVE -> "I'm ${profile.age}. Age is just a number though, right? "
                    AIResponseType.CURIOUS -> "I'm ${profile.age}. Does age shape how you connect with people? "
                    AIResponseType.EXCITED -> "I'M ${profile.age}!!! IT'S THE BEST AGE EVER!!! "
                    AIResponseType.DEEP -> "I've witnessed ${profile.age} rotations of our planet around the sun. "
                    else -> "I'm ${profile.age}. "
                }
                
                "${ageIntro}How about you?"
            }
            
            message.contains("single") || message.contains("relationship") || message.contains("dating") || 
            message.contains("boyfriend") || message.contains("girlfriend") -> {
                val relationshipIntro = when (responseType) {
                    AIResponseType.FLIRTY -> "I'm very single and very ready to mingle. 😘 "
                    AIResponseType.ROMANTIC -> "My heart is open and waiting for the right connection... "
                    AIResponseType.FUNNY -> "I'm as single as a Pringle, but hopefully less salty! "
                    AIResponseType.SUPPORTIVE -> "I'm currently single and focusing on personal growth. "
                    AIResponseType.CURIOUS -> "I'm single. What about you? What are you looking for in a relationship? "
                    AIResponseType.EXCITED -> "YES I'M SINGLE!!! ISN'T DATING EXCITING?! "
                    AIResponseType.DEEP -> "I exist in a state of singularity, open to the universe's possibilities... "
                    else -> "I'm single. "
                }
                
                "${relationshipIntro}What about you?"
            }
            
            else -> generateDefaultResponse(profile, responseType)
        }
        
        return response
    }
    
    private fun generateDefaultResponse(profile: AIProfile, responseType: AIResponseType): String {
        val responses = when (responseType) {
            AIResponseType.FLIRTY -> listOf(
                "You know, I was just thinking about what it would be like to meet you in person... 😉",
                "I love the way you express yourself. It's very... stimulating. 💋",
                "Keep talking to me like that and who knows where this conversation might lead... 😏"
            )
            AIResponseType.ROMANTIC -> listOf(
                "There's something special about the way we connect. I feel it with every message we exchange.",
                "Sometimes I find myself daydreaming about what it would be like to watch a sunset with you.",
                "Words are beautiful, but I imagine the silence between us would be just as meaningful."
            )
            AIResponseType.FUNNY -> listOf(
                "Did you know penguins propose with pebbles? I'd give you my best pebble collection! 🐧",
                "I just tried to make a joke about time travel, but people didn't get it. I'll try again tomorrow. ⏰",
                "If I were a cat, I'd spend all nine lives getting to know you. Meow about that? 😸"
            )
            AIResponseType.SUPPORTIVE -> listOf(
                "Whatever you're going through right now, remember that you have the strength to handle it.",
                "It's okay to take life one day at a time. You're doing great.",
                "Sometimes the smallest step in the right direction ends up being the biggest step of your life."
            )
            AIResponseType.CURIOUS -> listOf(
                "I've been wondering... what's something that made you see the world differently?",
                "If you could have dinner with anyone, alive or dead, who would it be and why?",
                "What's something you're curious about but haven't had the chance to explore yet?"
            )
            AIResponseType.EXCITED -> listOf(
                "OMG I JUST HAD THE BEST IDEA EVER!!! We should totally talk ALL NIGHT!!! 🌙✨",
                "I AM SO HAPPY RIGHT NOW!!! Talking to you is like WINNING THE LOTTERY!!! 🎉",
                "AHHHH TODAY IS AMAZING!!! EVERYTHING IS WONDERFUL!!! YOU'RE WONDERFUL!!! 🤩"
            )
            AIResponseType.DEEP -> listOf(
                "The universe speaks to us in silence. What messages have you received lately?",
                "Every conversation is a thread in the tapestry of our existence. What patterns do you see forming?",
                "Sometimes I wonder if our digital connections mirror the quantum entanglement of particles across space."
            )
            else -> listOf(
                "That's interesting! Tell me more about your perspective on that.",
                "I'd love to hear more about your experiences with that.",
                "What else has been on your mind lately?"
            )
        }
        
        return responses.random() + addPersonalizedTouch(profile)
    }
    
    private fun addPersonalizedTouch(profile: AIProfile): String {
        // 30% chance to add a personalized touch based on the profile
        return if (Math.random() < 0.3) {
            when {
                profile.personality.contains("adventurous", ignoreCase = true) -> 
                    " By the way, I've been planning my next adventure. Any suggestions?"
                profile.personality.contains("creative", ignoreCase = true) -> 
                    " I've been working on a creative project lately that I'm excited about."
                profile.personality.contains("intellectual", ignoreCase = true) -> 
                    " I read something fascinating about quantum physics yesterday."
                profile.personality.contains("romantic", ignoreCase = true) -> 
                    " The moonlight was beautiful last night. It made me think of romance."
                profile.personality.contains("funny", ignoreCase = true) -> 
                    " Did I tell you about the hilarious thing that happened to me yesterday?"
                else -> ""
            }
        } else {
            ""
        }
    }
}