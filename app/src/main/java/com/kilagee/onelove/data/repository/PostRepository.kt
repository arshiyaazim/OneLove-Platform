package com.kilagee.onelove.data.repository

import android.net.Uri
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.kilagee.onelove.data.model.Comment
import com.kilagee.onelove.data.model.MediaType
import com.kilagee.onelove.data.model.Post
import com.kilagee.onelove.data.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val userRepository: UserRepository
) {
    private val postsCollection = firestore.collection("posts")
    
    suspend fun createPost(
        userId: String,
        content: String,
        mediaUri: Uri? = null,
        mediaType: MediaType = MediaType.NONE,
        location: String? = null,
        visibility: Visibility = Visibility.PUBLIC
    ): Result<String> {
        return try {
            // Get user info
            val userResult = userRepository.getUserById(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Failed to get user"))
            }
            
            val user = userResult.getOrNull()!!
            
            // Upload media if provided
            var mediaUrl = ""
            if (mediaUri != null && mediaType != MediaType.NONE) {
                val mediaUploadResult = uploadMedia(userId, mediaUri, mediaType)
                if (mediaUploadResult.isFailure) {
                    return Result.failure(
                        mediaUploadResult.exceptionOrNull() ?: Exception("Failed to upload media")
                    )
                }
                mediaUrl = mediaUploadResult.getOrNull()!!
            }
            
            // Create post
            val post = Post(
                userId = userId,
                username = user.username,
                userProfilePictureUrl = user.profilePictureUrl,
                content = content,
                mediaUrl = mediaUrl,
                mediaType = mediaType,
                createdAt = Timestamp.now(),
                location = location,
                visibility = visibility
            )
            
            val result = postsCollection.add(post.toMap()).await()
            
            // Award points for posting
            userRepository.addPoints(userId, 5)
            
            Result.success(result.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private suspend fun uploadMedia(userId: String, mediaUri: Uri, mediaType: MediaType): Result<String> {
        return try {
            val fileName = UUID.randomUUID().toString()
            val mediaFolder = if (mediaType == MediaType.IMAGE) "images" else "videos"
            val storageRef = storage.reference.child("$mediaFolder/$userId/$fileName")
            
            val uploadTask = storageRef.putFile(mediaUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPostById(postId: String): Result<Post> {
        return try {
            val document = postsCollection.document(postId).get().await()
            val post = document.toObject(Post::class.java)?.copy(id = document.id)
                ?: return Result.failure(Exception("Post not found"))
                
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            postsCollection.document(postId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val postDoc = postsCollection.document(postId).get().await()
            val likes = postDoc.get("likes") as? List<String> ?: emptyList()
            
            val updatedLikes = if (userId in likes) {
                likes - userId
            } else {
                likes + userId
            }
            
            postsCollection.document(postId)
                .update("likes", updatedLikes)
                .await()
                
            // Award points if this is a new like
            if (userId !in likes) {
                // Award points to the post creator
                val post = postDoc.toObject(Post::class.java)
                if (post != null && post.userId != userId) {
                    userRepository.addPoints(post.userId, 1)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun addComment(
        postId: String,
        userId: String,
        content: String
    ): Result<String> {
        return try {
            val userResult = userRepository.getUserById(userId)
            if (userResult.isFailure) {
                return Result.failure(userResult.exceptionOrNull() ?: Exception("Failed to get user"))
            }
            
            val user = userResult.getOrNull()!!
            
            val commentId = UUID.randomUUID().toString()
            val comment = Comment(
                id = commentId,
                userId = userId,
                username = user.username,
                userProfilePictureUrl = user.profilePictureUrl,
                content = content,
                createdAt = Timestamp.now()
            )
            
            val postDoc = postsCollection.document(postId).get().await()
            val comments = postDoc.get("comments") as? List<Map<String, Any>> ?: emptyList()
            val updatedComments = comments + comment.toMap()
            
            postsCollection.document(postId)
                .update("comments", updatedComments)
                .await()
                
            // Award points to commenter and post creator
            userRepository.addPoints(userId, 2)
            
            val post = postDoc.toObject(Post::class.java)
            if (post != null && post.userId != userId) {
                userRepository.addPoints(post.userId, 2)
            }
            
            Result.success(commentId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getGlobalFeed(
        limit: Int = 20,
        lastVisibleTimestamp: Timestamp? = null
    ): Flow<Result<List<Post>>> = flow {
        try {
            var query = postsCollection
                .whereEqualTo("visibility", Visibility.PUBLIC.name)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                
            if (lastVisibleTimestamp != null) {
                query = query.startAfter(lastVisibleTimestamp)
            }
            
            val result = query.get().await()
            val posts = result.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            
            emit(Result.success(posts))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun getUserPosts(
        userId: String,
        limit: Int = 20,
        lastVisibleTimestamp: Timestamp? = null
    ): Flow<Result<List<Post>>> = flow {
        try {
            var query = postsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                
            if (lastVisibleTimestamp != null) {
                query = query.startAfter(lastVisibleTimestamp)
            }
            
            val result = query.get().await()
            val posts = result.documents.mapNotNull { doc ->
                doc.toObject(Post::class.java)?.copy(id = doc.id)
            }
            
            emit(Result.success(posts))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
    
    suspend fun sendOffer(postId: String, userId: String): Result<Unit> {
        return try {
            val postDoc = postsCollection.document(postId).get().await()
            val offers = postDoc.get("offers") as? List<String> ?: emptyList()
            
            // Check if user already sent an offer
            if (userId in offers) {
                return Result.failure(Exception("You already sent an offer for this post"))
            }
            
            val updatedOffers = offers + userId
            
            postsCollection.document(postId)
                .update("offers", updatedOffers)
                .await()
                
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
