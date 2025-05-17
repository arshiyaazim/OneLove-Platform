package com.kilagee.onelove.di

import com.kilagee.onelove.util.AgoraManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AgoraModule {
    
    @Provides
    @Singleton
    fun provideAgoraManager(): AgoraManager = AgoraManager()
    
}