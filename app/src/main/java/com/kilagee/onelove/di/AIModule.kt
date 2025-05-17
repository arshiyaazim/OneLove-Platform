package com.kilagee.onelove.di

import com.google.firebase.firestore.FirebaseFirestore
import com.kilagee.onelove.data.repository.AIProfileRepositoryImpl
import com.kilagee.onelove.domain.repository.AIProfileRepository
import com.kilagee.onelove.util.AIResponseGenerator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for AI-related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AIModule {
    
    /**
     * Provides the AIProfileRepository implementation
     */
    @Provides
    @Singleton
    fun provideAIProfileRepository(
        firestore: FirebaseFirestore,
        responseGenerator: AIResponseGenerator
    ): AIProfileRepository {
        return AIProfileRepositoryImpl(firestore, responseGenerator)
    }
    
    /**
     * Provides the AIResponseGenerator
     */
    @Provides
    @Singleton
    fun provideAIResponseGenerator(): AIResponseGenerator {
        return AIResponseGenerator()
    }
}