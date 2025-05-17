package com.kilagee.onelove.ui.calls

import androidx.lifecycle.ViewModel
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.model.User
import com.kilagee.onelove.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {
    
    /**
     * Get user by ID
     */
    fun getUserById(userId: String): Flow<Resource<User>> = flow {
        emit(Resource.Loading)
        
        try {
            val user = userRepository.getUserById(userId).await()
            emit(user)
        } catch (e: Exception) {
            emit(Resource.error("Failed to load user: ${e.message}"))
        }
    }.catch { e ->
        emit(Resource.error("Failed to load user: ${e.message}"))
    }
}