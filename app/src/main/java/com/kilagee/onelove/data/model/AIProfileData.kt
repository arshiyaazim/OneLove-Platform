package com.kilagee.onelove.data.model

import com.kilagee.onelove.domain.model.AIProfile
import java.util.UUID
import kotlin.random.Random

/**
 * Provider for AI profile data
 */
object AIProfileData {
    
    // Lists of data for generating random AI profiles
    private val maleFirstNames = listOf(
        "James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles",
        "Christopher", "Daniel", "Matthew", "Anthony", "Mark", "Donald", "Steven", "Paul", "Andrew", "Joshua",
        "Kenneth", "Kevin", "Brian", "George", "Timothy", "Ronald", "Jason", "Edward", "Jeffrey", "Ryan",
        "Jacob", "Gary", "Nicholas", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Scott", "Brandon",
        "Benjamin", "Samuel", "Gregory", "Alexander", "Patrick", "Frank", "Raymond", "Jack", "Dennis", "Jerry"
    )
    
    private val femaleFirstNames = listOf(
        "Mary", "Patricia", "Jennifer", "Linda", "Elizabeth", "Barbara", "Susan", "Jessica", "Sarah", "Karen",
        "Lisa", "Nancy", "Betty", "Margaret", "Sandra", "Ashley", "Kimberly", "Emily", "Donna", "Michelle",
        "Carol", "Amanda", "Dorothy", "Melissa", "Deborah", "Stephanie", "Rebecca", "Sharon", "Laura", "Cynthia",
        "Kathleen", "Amy", "Angela", "Shirley", "Anna", "Ruth", "Brenda", "Pamela", "Nicole", "Katherine",
        "Samantha", "Christine", "Emma", "Catherine", "Debra", "Virginia", "Rachel", "Carolyn", "Janet", "Maria"
    )
    
    private val lastNames = listOf(
        "Smith", "Johnson", "Williams", "Jones", "Brown", "Davis", "Miller", "Wilson", "Moore", "Taylor",
        "Anderson", "Thomas", "Jackson", "White", "Harris", "Martin", "Thompson", "Garcia", "Martinez", "Robinson",
        "Clark", "Rodriguez", "Lewis", "Lee", "Walker", "Hall", "Allen", "Young", "Hernandez", "King",
        "Wright", "Lopez", "Hill", "Scott", "Green", "Adams", "Baker", "Gonzalez", "Nelson", "Carter",
        "Mitchell", "Perez", "Roberts", "Turner", "Phillips", "Campbell", "Parker", "Evans", "Edwards", "Collins"
    )
    
    private val countries = listOf(
        "USA", "Canada", "UK", "Australia", "Germany", "France", "Italy", "Spain", "Japan", "South Korea",
        "Brazil", "Argentina", "Mexico", "Sweden", "Norway", "Denmark", "Netherlands", "Belgium", "Russia", "Ukraine",
        "India", "China", "Singapore", "New Zealand", "South Africa", "Ireland", "Poland", "Switzerland", "Austria", "Portugal"
    )
    
    private val cities = mapOf(
        "USA" to listOf("New York", "Los Angeles", "Chicago", "Houston", "Phoenix", "Philadelphia", "San Antonio", "San Diego", "Dallas", "San Jose"),
        "Canada" to listOf("Toronto", "Montreal", "Vancouver", "Calgary", "Edmonton", "Ottawa", "Quebec City", "Winnipeg", "Hamilton", "Halifax"),
        "UK" to listOf("London", "Manchester", "Birmingham", "Glasgow", "Liverpool", "Edinburgh", "Bristol", "Leeds", "Sheffield", "Newcastle"),
        "Australia" to listOf("Sydney", "Melbourne", "Brisbane", "Perth", "Adelaide", "Gold Coast", "Canberra", "Newcastle", "Wollongong", "Hobart"),
        "Germany" to listOf("Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt", "Stuttgart", "Düsseldorf", "Leipzig", "Dortmund", "Essen"),
        "France" to listOf("Paris", "Marseille", "Lyon", "Toulouse", "Nice", "Nantes", "Strasbourg", "Montpellier", "Bordeaux", "Lille"),
        "Italy" to listOf("Rome", "Milan", "Naples", "Turin", "Palermo", "Genoa", "Bologna", "Florence", "Bari", "Catania"),
        "Spain" to listOf("Madrid", "Barcelona", "Valencia", "Seville", "Zaragoza", "Málaga", "Murcia", "Palma", "Las Palmas", "Bilbao"),
        "Japan" to listOf("Tokyo", "Yokohama", "Osaka", "Nagoya", "Sapporo", "Kobe", "Kyoto", "Fukuoka", "Kawasaki", "Saitama"),
        "South Korea" to listOf("Seoul", "Busan", "Incheon", "Daegu", "Daejeon", "Gwangju", "Suwon", "Ulsan", "Seongnam", "Goyang")
    )
    
    private val personalityTypes = listOf(
        "Adventurous", "Creative", "Intellectual", "Ambitious", "Romantic",
        "Humorous", "Caring", "Spiritual", "Athletic", "Artistic",
        "Analytical", "Outgoing", "Introverted", "Passionate", "Practical",
        "Spontaneous", "Thoughtful", "Playful", "Mysterious", "Sophisticated"
    )
    
    private val interests = listOf(
        "Hiking", "Photography", "Cooking", "Reading", "Travel",
        "Music", "Art", "Movies", "Gaming", "Fitness",
        "Dancing", "Writing", "Yoga", "Meditation", "Animals",
        "Technology", "Fashion", "Sports", "Theatre", "Volunteering",
        "Gardening", "Cycling", "Swimming", "Singing", "Painting",
        "Astronomy", "History", "Languages", "Science", "Philosophy",
        "Politics", "Kayaking", "Climbing", "Surfing", "Running",
        "Birdwatching", "Fishing", "Camping", "Wine Tasting", "Cooking"
    )
    
    private val malePhotoUrls = listOf(
        "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1568602471122-7832951cc4c5?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=870&q=80",
        "https://images.unsplash.com/photo-1564564321837-a57b7070ac4f?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=876&q=80",
        "https://images.unsplash.com/photo-1548142813-c348350df52b?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=778&q=80",
        "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1552374196-c4e7ffc6e126?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1531727991582-cfd25ce79613?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1499996860823-5214fcc65f8f?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=932&q=80"
    )
    
    private val femalePhotoUrls = listOf(
        "https://images.unsplash.com/photo-1494790108377-be9c29b29330?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=870&q=80",
        "https://images.unsplash.com/photo-1544005313-94ddf0286df2?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=776&q=80",
        "https://images.unsplash.com/photo-1546961329-78bef0414d7c?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1531123897727-8f129e1688ce?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=776&q=80",
        "https://images.unsplash.com/photo-1524250502761-1ac6f2e30d43?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=776&q=80",
        "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=774&q=80",
        "https://images.unsplash.com/photo-1523824921871-d6f1a15151f1?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=776&q=80",
        "https://images.unsplash.com/photo-1552699611-e2c208d5d9cf?ixlib=rb-4.0.3&ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&auto=format&fit=crop&w=836&q=80"
    )
    
    /**
     * Generate AI profile bios based on personality
     */
    private fun generateBio(firstName: String, personality: String, interests: List<String>): String {
        val personalityLower = personality.lowercase()
        
        return when {
            personalityLower.contains("adventurous") ->
                "Always seeking the next thrill and exploring new horizons. Life is too short to stay in one place. $firstName loves ${interests.random()} and ${interests.random()}, and is always planning the next adventure."
                
            personalityLower.contains("creative") ->
                "$firstName sees the world through an artistic lens. Whether it's ${interests.random()} or ${interests.random()}, creativity flows through everything $firstName does. Looking for someone who appreciates the beauty in the details."
                
            personalityLower.contains("intellectual") ->
                "Curious mind and lifelong learner. $firstName enjoys deep conversations about ${interests.random()} and ${interests.random()}. Believes that knowledge is the key to understanding ourselves and the world around us."
                
            personalityLower.contains("ambitious") ->
                "Driven by passion and always aiming higher. $firstName balances career goals with a love for ${interests.random()} and ${interests.random()}. Looking for someone equally motivated and enthusiastic about life."
                
            personalityLower.contains("romantic") ->
                "A true believer in love and connection. $firstName enjoys ${interests.random()} and ${interests.random()}, especially when shared with someone special. Believes that the most beautiful moments in life are the ones where hearts connect."
                
            personalityLower.contains("humorous") ->
                "Life's too short not to laugh! $firstName finds humor in everyday situations and loves making others smile. Enjoys ${interests.random()} and ${interests.random()}, preferably with someone who doesn't take life too seriously."
                
            personalityLower.contains("caring") ->
                "$firstName puts heart into everything, whether it's ${interests.random()} or helping a friend in need. Compassion and kindness are the most important values. Looking for authentic connections."
                
            personalityLower.contains("spiritual") ->
                "On a journey of self-discovery and inner peace. $firstName finds balance through ${interests.random()} and ${interests.random()}. Seeking someone who understands that the most important journey is the one within."
                
            personalityLower.contains("athletic") ->
                "Fitness enthusiast who believes in pushing limits. $firstName's idea of a perfect day includes ${interests.random()} and ${interests.random()}. Looking for someone who shares a passion for an active lifestyle."
                
            personalityLower.contains("artistic") ->
                "Expressing emotions through art is $firstName's way of connecting with the world. Passionate about ${interests.random()} and ${interests.random()}. Hoping to find someone who appreciates the beauty of creative expression."
                
            else ->
                "$firstName enjoys ${interests.random()}, ${interests.random()}, and ${interests.random()}. Looking for meaningful connections with people who share similar interests or can introduce new perspectives."
        }
    }
    
    /**
     * Get 1000+ initial AI profiles for the app
     */
    fun getInitialAIProfiles(): List<AIProfile> {
        val profiles = mutableListOf<AIProfile>()
        val random = Random(System.currentTimeMillis())
        
        // Create 500+ male profiles
        for (i in 1..550) {
            val firstName = maleFirstNames.random(random)
            val lastName = lastNames.random(random)
            val country = countries.random(random)
            val city = cities[country]?.random(random) ?: "Unknown"
            val age = random.nextInt(18, 55)
            val personality = personalityTypes.random(random)
            
            // Select 3-5 random interests
            val profileInterests = interests.shuffled(random).take(random.nextInt(3, 6))
            
            // Select 1-3 personality tags
            val personalityTags = personalityTypes.shuffled(random).take(random.nextInt(1, 4))
            
            val bio = generateBio(firstName, personality, profileInterests)
            
            profiles.add(
                AIProfile(
                    id = UUID.randomUUID().toString(),
                    name = "$firstName $lastName",
                    photoUrl = malePhotoUrls.random(random),
                    age = age,
                    gender = "Male",
                    country = country,
                    city = city,
                    personality = personality,
                    bio = bio,
                    interests = profileInterests,
                    conversationId = UUID.randomUUID().toString(),
                    personalityTags = personalityTags,
                    createdAt = System.currentTimeMillis() - random.nextLong(0, 30 * 24 * 60 * 60 * 1000),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        
        // Create 500+ female profiles
        for (i in 1..550) {
            val firstName = femaleFirstNames.random(random)
            val lastName = lastNames.random(random)
            val country = countries.random(random)
            val city = cities[country]?.random(random) ?: "Unknown"
            val age = random.nextInt(18, 55)
            val personality = personalityTypes.random(random)
            
            // Select 3-5 random interests
            val profileInterests = interests.shuffled(random).take(random.nextInt(3, 6))
            
            // Select 1-3 personality tags
            val personalityTags = personalityTypes.shuffled(random).take(random.nextInt(1, 4))
            
            val bio = generateBio(firstName, personality, profileInterests)
            
            profiles.add(
                AIProfile(
                    id = UUID.randomUUID().toString(),
                    name = "$firstName $lastName",
                    photoUrl = femalePhotoUrls.random(random),
                    age = age,
                    gender = "Female",
                    country = country,
                    city = city,
                    personality = personality,
                    bio = bio,
                    interests = profileInterests,
                    conversationId = UUID.randomUUID().toString(),
                    personalityTags = personalityTags,
                    createdAt = System.currentTimeMillis() - random.nextLong(0, 30 * 24 * 60 * 60 * 1000),
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        
        return profiles
    }
}