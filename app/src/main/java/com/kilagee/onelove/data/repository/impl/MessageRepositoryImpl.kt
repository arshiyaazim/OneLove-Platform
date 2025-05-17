package com.kilagee.onelove.data.repository.impl

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.kilagee.onelove.data.local.dao.MessageDao
import com.kilagee.onelove.data.local.entity.MessageEntity
import com.kilagee.onelove.data.model.Chat
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.MessageType
import com.kilagee.onelove.data.model.Result
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.repository.MessageRepository
import com.kilagee.onelove.data.repository.StorageRepository
import com.kilagee.onelove.data.repository.UserRepository
import com.kilagee.onelove.util.AppError
import com.kilagee.onelove.util.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of MessageRepository using Firebase Firestore and Room
 */
@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val messageDao: MessageDao,
    private val storageRepository: StorageRepository,
    private val userRepository: UserRepository,
    private val networkMonitor: NetworkMonitor
) : MessageRepository {

    companion object {
        private const val TAG = "MessageRepositoryImpl"
        private const val MESSAGES_COLLECTION = "messages"
        private const val CHATS_COLLECTION = "chats"
    }

    override suspend fun getMessageById(messageId: String): Result<Message> {
        try {
            // First try to get from local cache
            val cachedMessage = messageDao.getMessageById(messageId)
            if (cachedMessage != null) {
                return Result.success(cachedMessage.toMessage())
            }
            
            // If not in cache, try to get from Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val documentSnapshot = firestore.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                val message = documentSnapshot.toObject(Message::class.java)
                    ?: return Result.error(AppError.DataError.InvalidData("Failed to parse message data"))
                
                // Cache message locally
                messageDao.insertMessage(MessageEntity.fromMessage(message))
                
                return Result.success(message)
            } else {
                return Result.error(AppError.DataError.NotFound("Message not found"))
            }
        } catch (e: Exception) {
            Timber.e(TAG, "getMessageById error", e)
            return Result.error(AppError.DataError.Other("Failed to get message: ${e.message}", e))
        }
    }

    override suspend fun getChatById(chatId: String): Result<Chat> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val documentSnapshot = firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .get()
                .await()
            
            if (documentSnapshot.exists()) {
                val chat = documentSnapshot.toObject(Chat::class.java)
                    ?: return Result.error(AppError.DataError.InvalidData("Failed to parse chat data"))
                
                return Result.success(chat)
            } else {
                return Result.error(AppError.DataError.NotFound("Chat not found"))
            }
        } catch (e: Exception) {
            Timber.e(TAG, "getChatById error", e)
            return Result.error(AppError.DataError.Other("Failed to get chat: ${e.message}", e))
        }
    }

    override fun getChatByIdFlow(chatId: String): Flow<Result<Chat>> = flow {
        emit(Result.loading())
        
        if (!networkMonitor.isOnline.value) {
            emit(Result.error(AppError.NetworkError.NoConnection("No internet connection")))
            return@flow
        }
        
        val chatRef = firestore.collection(CHATS_COLLECTION).document(chatId)
        
        // Emit initial value
        val initialSnapshot = chatRef.get().await()
        if (initialSnapshot.exists()) {
            val chat = initialSnapshot.toObject(Chat::class.java)
                ?: throw Exception("Failed to parse chat data")
            emit(Result.success(chat))
        } else {
            throw Exception("Chat not found")
        }
        
        // Listen for updates
        val subscription = chatRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(TAG, "getChatByIdFlow listener error", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null && snapshot.exists()) {
                val chat = snapshot.toObject(Chat::class.java)
                if (chat != null) {
                    trySend(Result.success(chat))
                }
            }
        }
        
        // Clean up listener when flow is cancelled
        kotlinx.coroutines.currentCoroutineContext().kotlinx.coroutines.job.invokeOnCompletion {
            subscription.remove()
        }
    }.catch { e ->
        Timber.e(TAG, "getChatByIdFlow error", e)
        
        val error = when (e.message) {
            "Chat not found" -> AppError.DataError.NotFound("Chat not found")
            "Failed to parse chat data" -> AppError.DataError.InvalidData("Failed to parse chat data")
            else -> AppError.DataError.Other("Failed to get chat: ${e.message}", e)
        }
        emit(Result.error(error))
    }

    override suspend fun getOrCreateChatBetweenUsers(userId1: String, userId2: String): Result<Chat> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            // Check if a chat already exists between these users
            val chatQuerySnapshot = firestore.collection(CHATS_COLLECTION)
                .whereArrayContains("participantIds", userId1)
                .get()
                .await()
            
            // Find chat with both participants
            val existingChat = chatQuerySnapshot.documents
                .mapNotNull { it.toObject(Chat::class.java) }
                .firstOrNull { it.participantIds.contains(userId2) && !it.isGroupChat }
            
            if (existingChat != null) {
                return Result.success(existingChat)
            }
            
            // No existing chat, create a new one
            val user1Result = userRepository.getUserById(userId1)
            val user2Result = userRepository.getUserById(userId2)
            
            if (user1Result is Result.Error) {
                return user1Result.map { Chat() }
            }
            
            if (user2Result is Result.Error) {
                return user2Result.map { Chat() }
            }
            
            val user1 = (user1Result as Result.Success).data
            val user2 = (user2Result as Result.Success).data
            
            val newChat = Chat(
                participantIds = listOf(userId1, userId2),
                participantNames = listOf(user1.displayName, user2.displayName),
                participantPhotos = listOf(
                    user1.profilePhotoUrl ?: "",
                    user2.profilePhotoUrl ?: ""
                ),
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            val chatRef = firestore.collection(CHATS_COLLECTION).document()
            val chatWithId = newChat.copy(id = chatRef.id)
            
            chatRef.set(chatWithId).await()
            
            return Result.success(chatWithId)
        } catch (e: Exception) {
            Timber.e(TAG, "getOrCreateChatBetweenUsers error", e)
            return Result.error(AppError.DataError.Other("Failed to get or create chat: ${e.message}", e))
        }
    }

    override suspend fun getChatsForUser(userId: String): Result<List<Chat>> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(CHATS_COLLECTION)
                .whereArrayContains("participantIds", userId)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val chats = querySnapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
            return Result.success(chats)
        } catch (e: Exception) {
            Timber.e(TAG, "getChatsForUser error", e)
            return Result.error(AppError.DataError.Other("Failed to get chats: ${e.message}", e))
        }
    }

    override fun getChatsForUserFlow(userId: String): Flow<Result<List<Chat>>> = flow {
        emit(Result.loading())
        
        if (!networkMonitor.isOnline.value) {
            emit(Result.error(AppError.NetworkError.NoConnection("No internet connection")))
            return@flow
        }
        
        val chatQuery = firestore.collection(CHATS_COLLECTION)
            .whereArrayContains("participantIds", userId)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
        
        // Emit initial result
        val initialSnapshot = chatQuery.get().await()
        val initialChats = initialSnapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
        emit(Result.success(initialChats))
        
        // Listen for updates
        val subscription = chatQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(TAG, "getChatsForUserFlow listener error", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val chats = snapshot.documents.mapNotNull { it.toObject(Chat::class.java) }
                trySend(Result.success(chats))
            }
        }
        
        // Clean up listener when flow is cancelled
        kotlinx.coroutines.currentCoroutineContext().kotlinx.coroutines.job.invokeOnCompletion {
            subscription.remove()
        }
    }.catch { e ->
        Timber.e(TAG, "getChatsForUserFlow error", e)
        emit(Result.error(AppError.DataError.Other("Failed to get chats: ${e.message}", e)))
    }

    override suspend fun getMessagesInChat(chatId: String, limit: Int): Result<List<Message>> {
        try {
            // First try to get from local cache
            val cachedMessages = messageDao.getMessagesByChatId(chatId)
            if (cachedMessages.isNotEmpty()) {
                return Result.success(cachedMessages.map { it.toMessage() })
            }
            
            // If not in cache or empty results, try to get from Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("isDeleted", false)
                .orderBy("createdAt", Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            
            val messages = querySnapshot.documents.mapNotNull { it.toObject(Message::class.java) }
            
            // Cache messages locally
            messageDao.insertMessages(messages.map { MessageEntity.fromMessage(it) })
            
            return Result.success(messages)
        } catch (e: Exception) {
            Timber.e(TAG, "getMessagesInChat error", e)
            return Result.error(AppError.DataError.Other("Failed to get messages: ${e.message}", e))
        }
    }

    override fun getMessagesInChatFlow(chatId: String): Flow<Result<List<Message>>> = flow {
        emit(Result.loading())
        
        // First emit from cache if available
        val cachedMessages = messageDao.getMessagesByChatId(chatId)
        if (cachedMessages.isNotEmpty()) {
            emit(Result.success(cachedMessages.map { it.toMessage() }))
        }
        
        if (!networkMonitor.isOnline.value) {
            if (cachedMessages.isEmpty()) {
                emit(Result.error(AppError.NetworkError.NoConnection("No internet connection")))
            }
            return@flow
        }
        
        val messagesQuery = firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("isDeleted", false)
            .orderBy("createdAt", Query.Direction.ASCENDING)
        
        // Listen for updates
        val subscription = messagesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(TAG, "getMessagesInChatFlow listener error", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                val messages = snapshot.documents.mapNotNull { it.toObject(Message::class.java) }
                
                // Cache messages locally
                kotlinx.coroutines.GlobalScope.kotlinx.coroutines.launch {
                    messageDao.insertMessages(messages.map { MessageEntity.fromMessage(it) })
                }
                
                trySend(Result.success(messages))
            }
        }
        
        // Clean up listener when flow is cancelled
        kotlinx.coroutines.currentCoroutineContext().kotlinx.coroutines.job.invokeOnCompletion {
            subscription.remove()
        }
    }.catch { e ->
        Timber.e(TAG, "getMessagesInChatFlow error", e)
        emit(Result.error(AppError.DataError.Other("Failed to get messages: ${e.message}", e)))
    }

    override suspend fun getUnreadMessagesForUser(userId: String): Result<List<Message>> {
        try {
            // First try to get from local cache
            val cachedMessages = messageDao.getUnreadMessagesForUser(userId)
            if (cachedMessages.isNotEmpty()) {
                return Result.success(cachedMessages.map { it.toMessage() })
            }
            
            // If not in cache or empty results, try to get from Firestore
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .whereEqualTo("isDeleted", false)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            
            val messages = querySnapshot.documents.mapNotNull { it.toObject(Message::class.java) }
            
            // Cache messages locally
            messageDao.insertMessages(messages.map { MessageEntity.fromMessage(it) })
            
            return Result.success(messages)
        } catch (e: Exception) {
            Timber.e(TAG, "getUnreadMessagesForUser error", e)
            return Result.error(AppError.DataError.Other("Failed to get unread messages: ${e.message}", e))
        }
    }

    override suspend fun getUnreadMessageCount(userId: String): Result<Int> {
        try {
            // First try to get from local cache
            val cachedCount = messageDao.getUnreadMessageCountForUser(userId)
            
            // If we have network, get the latest count from Firestore
            if (networkMonitor.isOnline.value) {
                val querySnapshot = firestore.collection(MESSAGES_COLLECTION)
                    .whereEqualTo("receiverId", userId)
                    .whereEqualTo("isRead", false)
                    .whereEqualTo("isDeleted", false)
                    .count()
                    .get()
                    .await()
                
                return Result.success(querySnapshot.count.toInt())
            }
            
            // If offline, use cached count
            return Result.success(cachedCount)
        } catch (e: Exception) {
            Timber.e(TAG, "getUnreadMessageCount error", e)
            return Result.error(AppError.DataError.Other("Failed to get unread message count: ${e.message}", e))
        }
    }

    override fun getUnreadMessageCountFlow(userId: String): Flow<Result<Int>> = flow {
        emit(Result.loading())
        
        // First emit from cache
        val cachedCount = messageDao.getUnreadMessageCountForUser(userId)
        emit(Result.success(cachedCount))
        
        if (!networkMonitor.isOnline.value) {
            return@flow
        }
        
        val messagesQuery = firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .whereEqualTo("isDeleted", false)
        
        // Listen for updates
        val subscription = messagesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(TAG, "getUnreadMessageCountFlow listener error", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                trySend(Result.success(snapshot.size()))
            }
        }
        
        // Clean up listener when flow is cancelled
        kotlinx.coroutines.currentCoroutineContext().kotlinx.coroutines.job.invokeOnCompletion {
            subscription.remove()
        }
    }.catch { e ->
        Timber.e(TAG, "getUnreadMessageCountFlow error", e)
        emit(Result.error(AppError.DataError.Other("Failed to get unread message count: ${e.message}", e)))
    }

    override suspend fun getUnreadMessageCountInChat(chatId: String, userId: String): Result<Int> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val querySnapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .whereEqualTo("isDeleted", false)
                .count()
                .get()
                .await()
            
            return Result.success(querySnapshot.count.toInt())
        } catch (e: Exception) {
            Timber.e(TAG, "getUnreadMessageCountInChat error", e)
            return Result.error(AppError.DataError.Other("Failed to get unread message count in chat: ${e.message}", e))
        }
    }

    override fun getUnreadMessageCountInChatFlow(chatId: String, userId: String): Flow<Result<Int>> = flow {
        emit(Result.loading())
        
        if (!networkMonitor.isOnline.value) {
            emit(Result.error(AppError.NetworkError.NoConnection("No internet connection")))
            return@flow
        }
        
        val messagesQuery = firestore.collection(MESSAGES_COLLECTION)
            .whereEqualTo("chatId", chatId)
            .whereEqualTo("receiverId", userId)
            .whereEqualTo("isRead", false)
            .whereEqualTo("isDeleted", false)
        
        // Listen for updates
        val subscription = messagesQuery.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Timber.e(TAG, "getUnreadMessageCountInChatFlow listener error", error)
                return@addSnapshotListener
            }
            
            if (snapshot != null) {
                trySend(Result.success(snapshot.size()))
            }
        }
        
        // Clean up listener when flow is cancelled
        kotlinx.coroutines.currentCoroutineContext().kotlinx.coroutines.job.invokeOnCompletion {
            subscription.remove()
        }
    }.catch { e ->
        Timber.e(TAG, "getUnreadMessageCountInChatFlow error", e)
        emit(Result.error(AppError.DataError.Other("Failed to get unread message count in chat: ${e.message}", e)))
    }

    override suspend fun sendTextMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        text: String
    ): Result<Message> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val timestamp = Timestamp.now()
            
            val newMessage = Message(
                chatId = chatId,
                senderId = senderId,
                receiverId = receiverId,
                messageType = MessageType.TEXT,
                content = text,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            val messageRef = firestore.collection(MESSAGES_COLLECTION).document()
            val messageWithId = newMessage.copy(id = messageRef.id)
            
            messageRef.set(messageWithId).await()
            
            // Update chat with last message
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to text,
                        "lastMessageType" to MessageType.TEXT.name,
                        "lastMessageTime" to timestamp,
                        "lastMessageSenderId" to senderId,
                        "updatedAt" to timestamp,
                        "unreadCounts.$receiverId" to FieldValue.increment(1)
                    )
                )
                .await()
            
            // Cache message locally
            messageDao.insertMessage(MessageEntity.fromMessage(messageWithId))
            
            return Result.success(messageWithId)
        } catch (e: Exception) {
            Timber.e(TAG, "sendTextMessage error", e)
            return Result.error(AppError.DataError.Other("Failed to send message: ${e.message}", e))
        }
    }

    override suspend fun sendMediaMessage(
        chatId: String,
        senderId: String,
        receiverId: String,
        mediaUri: Uri,
        messageType: MessageType,
        caption: String?
    ): Result<Message> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            // Upload media to Storage
            val uploadResult = when (messageType) {
                MessageType.IMAGE -> storageRepository.uploadMessageImage(chatId, senderId, mediaUri)
                MessageType.VIDEO -> storageRepository.uploadMessageVideo(chatId, senderId, mediaUri)
                MessageType.AUDIO -> {
                    val resultAudio = storageRepository.uploadMessageAudio(chatId, senderId, mediaUri)
                    if (resultAudio is Result.Error) {
                        return resultAudio.map { Message() }
                    }
                    
                    val audioUrl = (resultAudio as Result.Success).data
                    Result.success(Pair(audioUrl, ""))
                }
                else -> return Result.error(AppError.DataError.InvalidData("Invalid message type for media"))
            }
            
            if (uploadResult is Result.Error) {
                return uploadResult.map { Message() }
            }
            
            val (mediaUrl, thumbnailUrl) = (uploadResult as Result.Success).data
            
            val timestamp = Timestamp.now()
            
            val newMessage = Message(
                chatId = chatId,
                senderId = senderId,
                receiverId = receiverId,
                messageType = messageType,
                content = caption ?: "",
                mediaUrl = mediaUrl,
                mediaThumbnail = if (messageType != MessageType.AUDIO) thumbnailUrl else null,
                createdAt = timestamp,
                updatedAt = timestamp
            )
            
            val messageRef = firestore.collection(MESSAGES_COLLECTION).document()
            val messageWithId = newMessage.copy(id = messageRef.id)
            
            messageRef.set(messageWithId).await()
            
            // Update chat with last message
            val lastMessage = when {
                caption != null && caption.isNotEmpty() -> caption
                messageType == MessageType.IMAGE -> "📷 Image"
                messageType == MessageType.VIDEO -> "🎬 Video"
                messageType == MessageType.AUDIO -> "🎵 Audio"
                else -> "Media"
            }
            
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update(
                    mapOf(
                        "lastMessage" to lastMessage,
                        "lastMessageType" to messageType.name,
                        "lastMessageTime" to timestamp,
                        "lastMessageSenderId" to senderId,
                        "updatedAt" to timestamp,
                        "unreadCounts.$receiverId" to FieldValue.increment(1)
                    )
                )
                .await()
            
            // Cache message locally
            messageDao.insertMessage(MessageEntity.fromMessage(messageWithId))
            
            return Result.success(messageWithId)
        } catch (e: Exception) {
            Timber.e(TAG, "sendMediaMessage error", e)
            return Result.error(AppError.DataError.Other("Failed to send media message: ${e.message}", e))
        }
    }

    override suspend fun markMessageAsRead(messageId: String): Result<Unit> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val readAt = Timestamp.now()
            
            firestore.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .update(
                    mapOf(
                        "isRead" to true,
                        "readAt" to readAt
                    )
                )
                .await()
            
            // Update local cache
            messageDao.markMessageAsRead(messageId, readAt.toDate().time)
            
            // Get message to update chat unread count
            val messageSnapshot = firestore.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .get()
                .await()
            
            if (messageSnapshot.exists()) {
                val message = messageSnapshot.toObject(Message::class.java)
                if (message != null) {
                    // Decrement unread count for the receiver
                    firestore.collection(CHATS_COLLECTION)
                        .document(message.chatId)
                        .update("unreadCounts.${message.receiverId}", FieldValue.increment(-1))
                        .await()
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "markMessageAsRead error", e)
            return Result.error(AppError.DataError.Other("Failed to mark message as read: ${e.message}", e))
        }
    }

    override suspend fun markAllMessagesAsRead(chatId: String, userId: String): Result<Unit> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            val readAt = Timestamp.now()
            val readAtMillis = readAt.toDate().time
            
            // Get all unread messages in the chat for the user
            val querySnapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("chatId", chatId)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .await()
            
            // Update each message
            val batch = firestore.batch()
            querySnapshot.documents.forEach { doc ->
                batch.update(
                    doc.reference,
                    mapOf(
                        "isRead" to true,
                        "readAt" to readAt
                    )
                )
            }
            batch.commit().await()
            
            // Update local cache
            messageDao.markChatMessagesAsRead(chatId, userId, readAtMillis)
            
            // Reset unread count in chat
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .update("unreadCounts.$userId", 0)
                .await()
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "markAllMessagesAsRead error", e)
            return Result.error(AppError.DataError.Other("Failed to mark all messages as read: ${e.message}", e))
        }
    }

    override suspend fun deleteMessage(messageId: String): Result<Unit> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            // Get message to check if it's the last message in the chat
            val messageSnapshot = firestore.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .get()
                .await()
            
            if (!messageSnapshot.exists()) {
                return Result.error(AppError.DataError.NotFound("Message not found"))
            }
            
            val message = messageSnapshot.toObject(Message::class.java)
                ?: return Result.error(AppError.DataError.InvalidData("Failed to parse message data"))
            
            // Mark message as deleted
            firestore.collection(MESSAGES_COLLECTION)
                .document(messageId)
                .update("isDeleted", true)
                .await()
            
            // Delete from local cache
            messageDao.deleteMessageById(messageId)
            
            // Check if it was the last message in the chat
            val chatSnapshot = firestore.collection(CHATS_COLLECTION)
                .document(message.chatId)
                .get()
                .await()
            
            if (chatSnapshot.exists()) {
                val chat = chatSnapshot.toObject(Chat::class.java)
                if (chat != null && chat.lastMessageSenderId == message.senderId) {
                    // Get the new last message
                    val lastMessageSnapshot = firestore.collection(MESSAGES_COLLECTION)
                        .whereEqualTo("chatId", message.chatId)
                        .whereEqualTo("isDeleted", false)
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(1)
                        .get()
                        .await()
                    
                    if (!lastMessageSnapshot.isEmpty) {
                        val lastMessage = lastMessageSnapshot.documents[0].toObject(Message::class.java)
                        if (lastMessage != null) {
                            // Update chat with new last message
                            firestore.collection(CHATS_COLLECTION)
                                .document(message.chatId)
                                .update(
                                    mapOf(
                                        "lastMessage" to lastMessage.content,
                                        "lastMessageType" to lastMessage.messageType.name,
                                        "lastMessageTime" to lastMessage.createdAt,
                                        "lastMessageSenderId" to lastMessage.senderId
                                    )
                                )
                                .await()
                        }
                    } else {
                        // No messages left, update with empty message
                        firestore.collection(CHATS_COLLECTION)
                            .document(message.chatId)
                            .update(
                                mapOf(
                                    "lastMessage" to "",
                                    "lastMessageType" to MessageType.SYSTEM.name,
                                    "lastMessageTime" to Timestamp.now(),
                                    "lastMessageSenderId" to ""
                                )
                            )
                            .await()
                    }
                }
            }
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "deleteMessage error", e)
            return Result.error(AppError.DataError.Other("Failed to delete message: ${e.message}", e))
        }
    }

    override suspend fun deleteChat(chatId: String): Result<Unit> {
        try {
            if (!networkMonitor.isOnline.value) {
                return Result.error(AppError.NetworkError.NoConnection("No internet connection"))
            }
            
            // Delete chat document
            firestore.collection(CHATS_COLLECTION)
                .document(chatId)
                .delete()
                .await()
            
            // Mark all chat messages as deleted
            val messagesQuerySnapshot = firestore.collection(MESSAGES_COLLECTION)
                .whereEqualTo("chatId", chatId)
                .get()
                .await()
            
            val batch = firestore.batch()
            messagesQuerySnapshot.documents.forEach { doc ->
                batch.update(doc.reference, "isDeleted", true)
            }
            batch.commit().await()
            
            // Delete from local cache
            messageDao.deleteMessagesByChatId(chatId)
            
            return Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(TAG, "deleteChat error", e)
            return Result.error(AppError.DataError.Other("Failed to delete chat: ${e.message}", e))
        }
    }

    // Additional methods to be implemented for the remaining interface functions...
}