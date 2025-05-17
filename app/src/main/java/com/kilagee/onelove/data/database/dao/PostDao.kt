package com.kilagee.onelove.data.database.dao

import androidx.room.*
import com.kilagee.onelove.data.model.Post
import kotlinx.coroutines.flow.Flow

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: Post)
    
    @Update
    suspend fun updatePost(post: Post)
    
    @Delete
    suspend fun deletePost(post: Post)
    
    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostById(postId: String): Flow<Post?>
    
    @Query("SELECT * FROM posts WHERE user_id = :userId ORDER BY created_at DESC")
    fun getPostsByUserId(userId: String): Flow<List<Post>>
    
    @Query("SELECT * FROM posts ORDER BY created_at DESC LIMIT :limit")
    fun getRecentPosts(limit: Int): Flow<List<Post>>
    
    @Query("SELECT * FROM posts WHERE is_featured = 1 ORDER BY created_at DESC")
    fun getFeaturedPosts(): Flow<List<Post>>
    
    @Query("UPDATE posts SET likes_count = likes_count + 1 WHERE id = :postId")
    suspend fun incrementLikes(postId: String)
    
    @Query("UPDATE posts SET comments_count = comments_count + 1 WHERE id = :postId")
    suspend fun incrementComments(postId: String)
}