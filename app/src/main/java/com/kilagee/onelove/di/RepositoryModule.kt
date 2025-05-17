package com.kilagee.onelove.di

import com.kilagee.onelove.data.repository.AuthRepository
import com.kilagee.onelove.data.repository.MessageRepository
import com.kilagee.onelove.data.repository.StorageRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.data.repository.impl.AuthRepositoryImpl
import com.kilagee.onelove.data.repository.impl.MessageRepositoryImpl
import com.kilagee.onelove.data.repository.impl.StorageRepositoryImpl
import com.kilagee.onelove.data.repository.impl.UserRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dagger Hilt module for providing repository implementations
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
    
    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository
    
    @Binds
    @Singleton
    abstract fun bindMessageRepository(
        messageRepositoryImpl: MessageRepositoryImpl
    ): MessageRepository
    
    @Binds
    @Singleton
    abstract fun bindStorageRepository(
        storageRepositoryImpl: StorageRepositoryImpl
    ): StorageRepository
    
    // Add more repository bindings as they are implemented
}