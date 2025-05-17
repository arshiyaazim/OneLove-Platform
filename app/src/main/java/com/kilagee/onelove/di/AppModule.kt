package com.kilagee.onelove.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.kilagee.onelove.data.local.MatchDao
import com.kilagee.onelove.data.local.MessageDao
import com.kilagee.onelove.data.local.OneLoveDatabase
import com.kilagee.onelove.data.local.SubscriptionDao
import com.kilagee.onelove.data.local.UserDao
import com.kilagee.onelove.data.repository.AuthRepositoryImpl
import com.kilagee.onelove.data.repository.ChatRepositoryImpl
import com.kilagee.onelove.data.repository.DiscoverRepositoryImpl
import com.kilagee.onelove.data.repository.SubscriptionRepositoryImpl
import com.kilagee.onelove.data.repository.UserRepositoryImpl
import com.kilagee.onelove.domain.recommendation.RecommendationEngine
import com.kilagee.onelove.domain.repository.AuthRepository
import com.kilagee.onelove.domain.repository.ChatRepository
import com.kilagee.onelove.domain.repository.DiscoverRepository
import com.kilagee.onelove.domain.repository.SubscriptionRepository
import com.kilagee.onelove.domain.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provide Firebase Auth
     */
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return Firebase.auth
    }
    
    /**
     * Provide Firebase Firestore
     */
    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return Firebase.firestore
    }
    
    /**
     * Provide Firebase Storage
     */
    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage {
        return Firebase.storage
    }
    
    /**
     * Provide Firebase Functions
     */
    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions {
        return Firebase.functions
    }
    
    /**
     * Provide Room database
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OneLoveDatabase {
        return OneLoveDatabase.getInstance(context)
    }
    
    /**
     * Provide User DAO
     */
    @Provides
    @Singleton
    fun provideUserDao(database: OneLoveDatabase): UserDao {
        return database.userDao()
    }
    
    /**
     * Provide Message DAO
     */
    @Provides
    @Singleton
    fun provideMessageDao(database: OneLoveDatabase): MessageDao {
        return database.messageDao()
    }
    
    /**
     * Provide Match DAO
     */
    @Provides
    @Singleton
    fun provideMatchDao(database: OneLoveDatabase): MatchDao {
        return database.matchDao()
    }
    
    /**
     * Provide Subscription DAO
     */
    @Provides
    @Singleton
    fun provideSubscriptionDao(database: OneLoveDatabase): SubscriptionDao {
        return database.subscriptionDao()
    }
    
    /**
     * Provide Recommendation Engine
     */
    @Provides
    @Singleton
    fun provideRecommendationEngine(): RecommendationEngine {
        return RecommendationEngine()
    }
    
    /**
     * Provide Auth repository
     */
    @Provides
    @Singleton
    fun provideAuthRepository(
        auth: FirebaseAuth,
        firestore: FirebaseFirestore,
        userDao: UserDao
    ): AuthRepository {
        return AuthRepositoryImpl(auth, firestore, userDao)
    }
    
    /**
     * Provide User repository
     */
    @Provides
    @Singleton
    fun provideUserRepository(
        firestore: FirebaseFirestore,
        userDao: UserDao
    ): UserRepository {
        return UserRepositoryImpl(firestore, userDao)
    }
    
    /**
     * Provide Discover repository
     */
    @Provides
    @Singleton
    fun provideDiscoverRepository(
        firestore: FirebaseFirestore,
        authRepository: AuthRepository,
        userDao: UserDao,
        recommendationEngine: RecommendationEngine
    ): DiscoverRepository {
        return DiscoverRepositoryImpl(firestore, authRepository, userDao, recommendationEngine)
    }
    
    /**
     * Provide Chat repository
     */
    @Provides
    @Singleton
    fun provideChatRepository(
        firestore: FirebaseFirestore,
        messageDao: MessageDao,
        matchDao: MatchDao
    ): ChatRepository {
        return ChatRepositoryImpl(firestore, messageDao, matchDao)
    }
    
    /**
     * Provide Subscription repository
     */
    @Provides
    @Singleton
    fun provideSubscriptionRepository(
        firestore: FirebaseFirestore,
        functions: FirebaseFunctions,
        subscriptionDao: SubscriptionDao,
        userDao: UserDao
    ): SubscriptionRepository {
        return SubscriptionRepositoryImpl(firestore, functions, subscriptionDao, userDao)
    }
}