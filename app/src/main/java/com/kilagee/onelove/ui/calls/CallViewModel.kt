package com.kilagee.onelove.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kilagee.onelove.data.model.Call
import com.kilagee.onelove.data.model.CallStatus
import com.kilagee.onelove.data.model.CallType
import com.kilagee.onelove.domain.model.Resource
import com.kilagee.onelove.domain.repository.CallRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    private val callRepository: CallRepository
) : ViewModel() {
    
    // State for active call
    private val _activeCallState = MutableStateFlow<Resource<Call>?>(null)
    val activeCallState: StateFlow<Resource<Call>?> = _activeCallState
    
    // State for call history
    private val _callHistoryState = MutableStateFlow<Resource<List<Call>>>(Resource.Loading)
    val callHistoryState: StateFlow<Resource<List<Call>>> = _callHistoryState
    
    // State for Agora token
    private val _tokenState = MutableStateFlow<Resource<String>?>(null)
    val tokenState: StateFlow<Resource<String>?> = _tokenState
    
    /**
     * Initiate a call to a user
     */
    fun initiateCall(receiverId: String, callType: CallType) {
        viewModelScope.launch {
            _activeCallState.value = Resource.Loading
            
            callRepository.createCall(receiverId, callType)
                .onEach { resource ->
                    _activeCallState.value = resource
                }
                .catch { e ->
                    _activeCallState.value = Resource.error("Failed to initiate call: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Initiate a group call
     */
    fun initiateGroupCall(participantIds: List<String>, callType: CallType) {
        viewModelScope.launch {
            _activeCallState.value = Resource.Loading
            
            callRepository.createGroupCall(participantIds, callType)
                .onEach { resource ->
                    _activeCallState.value = resource
                }
                .catch { e ->
                    _activeCallState.value = Resource.error("Failed to initiate group call: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Get call by ID
     */
    fun getCall(callId: String) {
        viewModelScope.launch {
            _activeCallState.value = Resource.Loading
            
            callRepository.getCall(callId)
                .onEach { resource ->
                    _activeCallState.value = resource
                }
                .catch { e ->
                    _activeCallState.value = Resource.error("Failed to get call: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load call history
     */
    fun loadCallHistory() {
        viewModelScope.launch {
            _callHistoryState.value = Resource.Loading
            
            callRepository.getCalls()
                .onEach { resource ->
                    _callHistoryState.value = resource
                }
                .catch { e ->
                    _callHistoryState.value = Resource.error("Failed to load call history: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Load call history with a specific user
     */
    fun loadCallHistoryWithUser(userId: String) {
        viewModelScope.launch {
            _callHistoryState.value = Resource.Loading
            
            callRepository.getCallHistoryWithUser(userId)
                .onEach { resource ->
                    _callHistoryState.value = resource
                }
                .catch { e ->
                    _callHistoryState.value = Resource.error("Failed to load call history: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Answer a call
     */
    fun answerCall(callId: String) {
        viewModelScope.launch {
            callRepository.answerCall(callId)
                .onEach { resource ->
                    _activeCallState.value = resource
                }
                .catch { e ->
                    _activeCallState.value = Resource.error("Failed to answer call: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Decline a call
     */
    fun declineCall(callId: String) {
        viewModelScope.launch {
            callRepository.declineCall(callId)
                .onEach { resource ->
                    _activeCallState.value = resource
                }
                .catch { e ->
                    _activeCallState.value = Resource.error("Failed to decline call: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * End a call
     */
    fun endCall(callId: String, duration: Long) {
        viewModelScope.launch {
            callRepository.endCall(callId, Date(), duration)
                .onEach { resource ->
                    _activeCallState.value = resource
                }
                .catch { e ->
                    _activeCallState.value = Resource.error("Failed to end call: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Get Agora token for a call
     */
    fun getAgoraToken(callId: String, userId: String, channelName: String) {
        viewModelScope.launch {
            _tokenState.value = Resource.Loading
            
            callRepository.getAgoraToken(callId, userId, channelName)
                .onEach { resource ->
                    _tokenState.value = resource
                }
                .catch { e ->
                    _tokenState.value = Resource.error("Failed to get token: ${e.message}")
                }
                .launchIn(viewModelScope)
        }
    }
    
    /**
     * Clear active call state
     */
    fun clearActiveCallState() {
        _activeCallState.value = null
    }
    
    /**
     * Clear token state
     */
    fun clearTokenState() {
        _tokenState.value = null
    }
    
    /**
     * Delete a call record
     */
    fun deleteCallRecord(callId: String) {
        viewModelScope.launch {
            callRepository.deleteCall(callId)
                .onEach { 
                    // Reload call history on success
                    if (it is Resource.Success) {
                        loadCallHistory()
                    }
                }
                .launchIn(viewModelScope)
        }
    }
}