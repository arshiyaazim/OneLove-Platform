package com.kilagee.onelove.data.firestore

/**
 * Firestore collection and field names for the OneLove application
 * 
 * This class defines the schema of the Firestore database to ensure consistent
 * access across the application. It helps prevent typos and makes refactoring easier.
 */
object FirestoreSchema {

    /**
     * User collection schema
     */
    object Users {
        const val COLLECTION = "users"
        
        // Fields
        const val ID = "id"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val FIRST_NAME = "firstName"
        const val LAST_NAME = "lastName"
        const val PHONE_NUMBER = "phoneNumber"
        const val DATE_OF_BIRTH = "dateOfBirth"
        const val GENDER = "gender"
        const val LOOKING_FOR = "lookingFor"
        const val BIO = "bio"
        const val PROFILE_PHOTO_URL = "profilePhotoUrl"
        const val PHOTO_URLS = "photoUrls"
        const val INTERESTS = "interests"
        const val OCCUPATION = "occupation"
        const val EDUCATION = "education"
        const val LOCATION = "location"
        const val LOCATION_NAME = "locationName"
        const val GEOHASH = "geohash"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val LAST_ACTIVE = "lastActive"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val IS_ONLINE = "isOnline"
        const val IS_PROFILE_COMPLETE = "isProfileComplete"
        const val IS_VERIFIED = "isVerified"
        const val VERIFICATION_LEVEL = "verificationLevel"
        const val IS_PREMIUM = "isPremium"
        const val USER_PREFERENCES = "userPreferences"
        const val LANGUAGE = "language"
        const val COUNTRY_CODE = "countryCode"
        const val REGION = "region"
        const val POINTS = "points"
        const val LAST_LOGIN = "lastLogin"
        const val DEVICE_TOKEN = "deviceToken"
        const val USER_STATUS = "userStatus"
        const val ACCOUNT_STATUS = "accountStatus"
        const val SUBSCRIPTION_TIER = "subscriptionTier"
        const val PERSONALITY_TRAITS = "personalityTraits"
        const val COMMUNICATION_STYLE = "communicationStyle"
        const val HEIGHT = "height"
        const val RELIGION = "religion"
        const val RELATIONSHIP_TYPE = "relationshipType"
        const val HAS_CHILDREN = "hasChildren"
        const val WANTS_CHILDREN = "wantsChildren"
        const val DRINKING = "drinking"
        const val SMOKING = "smoking"
        const val EXERCISE = "exercise"
        const val DIET = "diet"
        const val PETS = "pets"
        const val LANGUAGES_SPOKEN = "languagesSpoken"
        const val BLOCKED_USERS = "blockedUsers"
        const val IS_HIDDEN = "isHidden"
        const val VISIBILITY_SETTINGS = "visibilitySettings"
        const val PRIVACY_SETTINGS = "privacySettings"
        const val NOTIFICATION_SETTINGS = "notificationSettings"
        const val ACCOUNT_TYPE = "accountType"
        
        // Sub-collections
        const val MATCHES_SUBCOLLECTION = "matches"
        const val MESSAGES_SUBCOLLECTION = "messages"
        const val OFFERS_SUBCOLLECTION = "offers"
        const val SUBSCRIPTIONS_SUBCOLLECTION = "subscriptions"
        const val VERIFICATIONS_SUBCOLLECTION = "verifications"
        const val REPORTS_SUBCOLLECTION = "reports"
        const val FAVORITES_SUBCOLLECTION = "favorites"
        const val SETTINGS_SUBCOLLECTION = "settings"
        const val PAYMENT_METHODS_SUBCOLLECTION = "paymentMethods"
        const val TRANSACTIONS_SUBCOLLECTION = "transactions"
        const val ACTIVITY_LOG_SUBCOLLECTION = "activityLog"
        const val AI_INTERACTIONS_SUBCOLLECTION = "aiInteractions"
    }
    
    /**
     * Matches collection schema
     */
    object Matches {
        const val COLLECTION = "matches"
        
        // Fields
        const val ID = "id"
        const val USER_ONE_ID = "userOneId"
        const val USER_TWO_ID = "userTwoId"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val LAST_INTERACTION = "lastInteraction"
        const val MATCH_STATUS = "matchStatus"
        const val MATCH_SCORE = "matchScore"
        const val COMPATIBILITY_FACTORS = "compatibilityFactors"
        const val USER_ONE_LIKED_AT = "userOneLikedAt"
        const val USER_TWO_LIKED_AT = "userTwoLikedAt"
        const val MATCH_CREATED_AT = "matchCreatedAt"
        const val LAST_MESSAGE_PREVIEW = "lastMessagePreview"
        const val LAST_MESSAGE_TIMESTAMP = "lastMessageTimestamp"
        const val USER_ONE_READ_TIMESTAMP = "userOneReadTimestamp"
        const val USER_TWO_READ_TIMESTAMP = "userTwoReadTimestamp"
        const val USER_ONE_TYPING = "userOneTyping"
        const val USER_TWO_TYPING = "userTwoTyping"
        const val UNREAD_COUNT_USER_ONE = "unreadCountUserOne"
        const val UNREAD_COUNT_USER_TWO = "unreadCountUserTwo"
        const val IS_ACTIVE = "isActive"
        const val MATCH_TYPE = "matchType"
        const val MUTED_BY_USER_ONE = "mutedByUserOne"
        const val MUTED_BY_USER_TWO = "mutedByUserTwo"
        
        // Sub-collections
        const val MESSAGES_SUBCOLLECTION = "messages"
        const val OFFERS_SUBCOLLECTION = "offers"
        const val CALLS_SUBCOLLECTION = "calls"
    }
    
    /**
     * Messages collection schema
     */
    object Messages {
        const val COLLECTION = "messages"
        
        // Fields
        const val ID = "id"
        const val MATCH_ID = "matchId"
        const val SENDER_ID = "senderId"
        const val RECEIVER_ID = "receiverId"
        const val CONTENT = "content"
        const val CREATED_AT = "createdAt"
        const val MESSAGE_TYPE = "messageType"
        const val IS_READ = "isRead"
        const val READ_AT = "readAt"
        const val MEDIA_URL = "mediaUrl"
        const val MEDIA_TYPE = "mediaType"
        const val MEDIA_THUMBNAIL_URL = "mediaThumbnailUrl"
        const val OFFER_ID = "offerId"
        const val OFFER_REFERENCE = "offerReference"
        const val IS_DELETED = "isDeleted"
        const val DELETED_AT = "deletedAt"
        const val IS_EDITED = "isEdited"
        const val EDITED_AT = "editedAt"
        const val STATUS = "status"
        const val LOCATION = "location"
        const val LOCATION_NAME = "locationName"
        const val LATITUDE = "latitude"
        const val LONGITUDE = "longitude"
        const val REPLY_TO_MESSAGE_ID = "replyToMessageId"
        const val METADATA = "metadata"
        const val IS_AI_GENERATED = "isAIGenerated"
        const val AI_PROFILE_ID = "aiProfileId"
    }
    
    /**
     * Offers collection schema
     */
    object Offers {
        const val COLLECTION = "offers"
        
        // Fields
        const val ID = "id"
        const val SENDER_ID = "senderId"
        const val RECEIVER_ID = "receiverId"
        const val MATCH_ID = "matchId"
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val AMOUNT = "amount"
        const val CURRENCY = "currency"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val EXPIRY_DATE = "expiryDate"
        const val STATUS = "status"
        const val ACCEPTED_AT = "acceptedAt"
        const val REJECTED_AT = "rejectedAt"
        const val CANCELLED_AT = "cancelledAt"
        const val PAYMENT_STATUS = "paymentStatus"
        const val PAYMENT_INTENT_ID = "paymentIntentId"
        const val PAYMENT_METHOD_ID = "paymentMethodId"
        const val TRANSACTION_ID = "transactionId"
        const val OFFER_TYPE = "offerType"
        const val TERMS_ACCEPTED = "termsAccepted"
        const val MEDIA_URL = "mediaUrl"
        const val MEDIA_TYPE = "mediaType"
        const val MESSAGE_PREVIEW = "messagePreview"
        const val IS_PREMIUM_OFFER = "isPremiumOffer"
        const val NOTES = "notes"
        const val METADATA = "metadata"
    }
    
    /**
     * Subscriptions collection schema
     */
    object Subscriptions {
        const val COLLECTION = "subscriptions"
        
        // Fields
        const val ID = "id"
        const val USER_ID = "userId"
        const val PLAN_ID = "planId"
        const val STATUS = "status"
        const val CURRENT_PERIOD_START = "currentPeriodStart"
        const val CURRENT_PERIOD_END = "currentPeriodEnd"
        const val CANCEL_AT_PERIOD_END = "cancelAtPeriodEnd"
        const val CANCELED_AT = "canceledAt"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val PAYMENT_METHOD_ID = "paymentMethodId"
        const val STRIPE_SUBSCRIPTION_ID = "stripeSubscriptionId"
        const val STRIPE_CUSTOMER_ID = "stripeCustomerId"
        const val AMOUNT = "amount"
        const val CURRENCY = "currency"
        const val SUBSCRIPTION_TIER = "subscriptionTier"
        const val AUTO_RENEW = "autoRenew"
        const val PROMO_CODE = "promoCode"
        const val DISCOUNT_AMOUNT = "discountAmount"
        const val DISCOUNT_PERCENTAGE = "discountPercentage"
        const val ORIGINAL_AMOUNT = "originalAmount"
        const val RECEIPT_URL = "receiptUrl"
        const val PLATFORM = "platform"
        const val PAYMENT_PROVIDER = "paymentProvider"
        const val TRANSACTION_ID = "transactionId"
        const val RECEIPT_DATA = "receiptData"
        const val PURCHASE_TOKEN = "purchaseToken"
        const val PRODUCT_ID = "productId"
        const val CANCEL_REASON = "cancelReason"
        const val METADATA = "metadata"
    }
    
    /**
     * Subscription Plans collection schema
     */
    object SubscriptionPlans {
        const val COLLECTION = "subscriptionPlans"
        
        // Fields
        const val ID = "id"
        const val NAME = "name"
        const val DESCRIPTION = "description"
        const val FEATURES = "features"
        const val AMOUNT = "amount"
        const val CURRENCY = "currency"
        const val BILLING_PERIOD = "billingPeriod"
        const val BILLING_PERIOD_UNIT = "billingPeriodUnit"
        const val TIER = "tier"
        const val IS_ACTIVE = "isActive"
        const val DISCOUNT_PERCENTAGE = "discountPercentage"
        const val TRIAL_PERIOD_DAYS = "trialPeriodDays"
        const val PRODUCT_ID_ANDROID = "productIdAndroid"
        const val PRODUCT_ID_IOS = "productIdIOS"
        const val STRIPE_PRICE_ID = "stripePriceId"
        const val STRIPE_PRODUCT_ID = "stripeProductId"
        const val SORT_ORDER = "sortOrder"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val METADATA = "metadata"
    }
    
    /**
     * AI Profiles collection schema
     */
    object AIProfiles {
        const val COLLECTION = "aiProfiles"
        
        // Fields
        const val ID = "id"
        const val NAME = "name"
        const val GENDER = "gender"
        const val AGE = "age"
        const val BIO = "bio"
        const val DESCRIPTION = "description"
        const val PERSONALITY_TYPE = "personalityType"
        const val INTERESTS = "interests"
        const val TRAITS = "traits"
        const val OCCUPATION = "occupation"
        const val BACKGROUND = "background"
        const val PROFILE_PHOTO_URL = "profilePhotoUrl"
        const val GALLERY_PHOTOS = "galleryPhotos"
        const val VOICE_URL = "voiceUrl"
        const val BEHAVIORS = "behaviors"
        const val GREETINGS = "greetings"
        const val FAREWELLS = "farewells"
        const val QUESTIONS = "questions"
        const val RESPONSES = "responses"
        const val ICEBREAKERS = "icebreakers"
        const val CATEGORY = "category"
        const val TAGS = "tags"
        const val IS_PREMIUM_ONLY = "isPremiumOnly"
        const val IS_ACTIVE = "isActive"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val POPULARITY_SCORE = "popularityScore"
        const val INTERACTION_COUNT = "interactionCount"
        const val AVERAGE_RATING = "averageRating"
        const val RATING_COUNT = "ratingCount"
        const val METADATA = "metadata"
    }
    
    /**
     * Verifications collection schema
     */
    object Verifications {
        const val COLLECTION = "verifications"
        
        // Fields
        const val ID = "id"
        const val USER_ID = "userId"
        const val TYPE = "type"
        const val STATUS = "status"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val VERIFIED_AT = "verifiedAt"
        const val VERIFICATION_DATA = "verificationData"
        const val DOCUMENT_URL = "documentUrl"
        const val DOCUMENT_TYPE = "documentType"
        const val EXPIRY_DATE = "expiryDate"
        const val REJECTION_REASON = "rejectionReason"
        const val ADMIN_NOTES = "adminNotes"
        const val ADMIN_ID = "adminId"
        const val VERIFICATION_LEVEL = "verificationLevel"
        const val METADATA = "metadata"
    }
    
    /**
     * Reports collection schema
     */
    object Reports {
        const val COLLECTION = "reports"
        
        // Fields
        const val ID = "id"
        const val REPORTER_ID = "reporterId"
        const val REPORTED_USER_ID = "reportedUserId"
        const val REASON = "reason"
        const val DESCRIPTION = "description"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val STATUS = "status"
        const val EVIDENCE_URLS = "evidenceUrls"
        const val MESSAGE_ID = "messageId"
        const val MATCH_ID = "matchId"
        const val RESOLVED_AT = "resolvedAt"
        const val RESOLVED_BY = "resolvedBy"
        const val RESOLUTION_NOTES = "resolutionNotes"
        const val SEVERITY = "severity"
        const val ACTION_TAKEN = "actionTaken"
        const val METADATA = "metadata"
    }
    
    /**
     * User Activity collection schema
     */
    object UserActivity {
        const val COLLECTION = "userActivity"
        
        // Fields
        const val ID = "id"
        const val USER_ID = "userId"
        const val ACTIVITY_TYPE = "activityType"
        const val TIMESTAMP = "timestamp"
        const val DETAILS = "details"
        const val METADATA = "metadata"
    }
    
    /**
     * Calls collection schema
     */
    object Calls {
        const val COLLECTION = "calls"
        
        // Fields
        const val ID = "id"
        const val CALLER_ID = "callerId"
        const val RECEIVER_ID = "receiverId"
        const val MATCH_ID = "matchId"
        const val STARTED_AT = "startedAt"
        const val ENDED_AT = "endedAt"
        const val DURATION = "duration"
        const val CALL_TYPE = "callType"
        const val STATUS = "status"
        const val CALL_TOKEN = "callToken"
        const val CHANNEL_NAME = "channelName"
        const val IS_PREMIUM_CALL = "isPremiumCall"
        const val QUALITY_METRICS = "qualityMetrics"
        const val NOTES = "notes"
        const val METADATA = "metadata"
    }
    
    /**
     * Notifications collection schema
     */
    object Notifications {
        const val COLLECTION = "notifications"
        
        // Fields
        const val ID = "id"
        const val USER_ID = "userId"
        const val TYPE = "type"
        const val TITLE = "title"
        const val BODY = "body"
        const val CREATED_AT = "createdAt"
        const val IS_READ = "isRead"
        const val READ_AT = "readAt"
        const val DATA = "data"
        const val PRIORITY = "priority"
        const val ACTION_URL = "actionUrl"
        const val IMAGE_URL = "imageUrl"
        const val SENDER_ID = "senderId"
        const val RELATED_ID = "relatedId"
        const val RELATED_TYPE = "relatedType"
        const val IS_DELETED = "isDeleted"
        const val DELETED_AT = "deletedAt"
        const val METADATA = "metadata"
    }
    
    /**
     * Content collection schema (for dynamic content managed by admins)
     */
    object Content {
        const val COLLECTION = "content"
        
        // Fields
        const val ID = "id"
        const val TITLE = "title"
        const val CONTENT = "content"
        const val CONTENT_TYPE = "contentType"
        const val PUBLISHED_AT = "publishedAt"
        const val UPDATED_AT = "updatedAt"
        const val AUTHOR_ID = "authorId"
        const val STATUS = "status"
        const val CATEGORY = "category"
        const val TAGS = "tags"
        const val FEATURED_IMAGE_URL = "featuredImageUrl"
        const val LANGUAGE = "language"
        const val REGION = "region"
        const val VISIBILITY = "visibility"
        const val PRIORITY = "priority"
        const val EXPIRY_DATE = "expiryDate"
        const val VERSION = "version"
        const val METADATA = "metadata"
    }
    
    /**
     * Settings collection schema (global app settings)
     */
    object Settings {
        const val COLLECTION = "settings"
        
        // Fields
        const val ID = "id"
        const val NAME = "name"
        const val VALUE = "value"
        const val TYPE = "type"
        const val DESCRIPTION = "description"
        const val UPDATED_AT = "updatedAt"
        const val UPDATED_BY = "updatedBy"
        const val IS_PUBLIC = "isPublic"
        const val CATEGORY = "category"
        const val METADATA = "metadata"
    }
    
    /**
     * Admin collection schema
     */
    object Admins {
        const val COLLECTION = "admins"
        
        // Fields
        const val ID = "id"
        const val EMAIL = "email"
        const val DISPLAY_NAME = "displayName"
        const val ROLE = "role"
        const val PERMISSIONS = "permissions"
        const val CREATED_AT = "createdAt"
        const val UPDATED_AT = "updatedAt"
        const val LAST_LOGIN = "lastLogin"
        const val IS_ACTIVE = "isActive"
        const val CREATED_BY = "createdBy"
        const val METADATA = "metadata"
    }
    
    /**
     * Admin Logs collection schema
     */
    object AdminLogs {
        const val COLLECTION = "adminLogs"
        
        // Fields
        const val ID = "id"
        const val ADMIN_ID = "adminId"
        const val ACTION = "action"
        const val TARGET_TYPE = "targetType"
        const val TARGET_ID = "targetId"
        const val TIMESTAMP = "timestamp"
        const val DETAILS = "details"
        const val IP_ADDRESS = "ipAddress"
        const val METADATA = "metadata"
    }
    
    /**
     * Analytics collection schema
     */
    object Analytics {
        const val COLLECTION = "analytics"
        
        // Fields
        const val ID = "id"
        const val NAME = "name"
        const val VALUE = "value"
        const val TIMESTAMP = "timestamp"
        const val PERIOD = "period"
        const val CATEGORY = "category"
        const val DIMENSION = "dimension"
        const val METADATA = "metadata"
    }
}