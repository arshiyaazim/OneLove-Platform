package com.kilagee.onelove.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import androidx.room.ForeignKey
import java.util.Date

@Entity(
    tableName = "posts",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Post(
    @PrimaryKey
    val id: String,
    
    @ColumnInfo(name = "user_id")
    val userId: String,
    
    @ColumnInfo(name = "content")
    val content: String,
    
    @ColumnInfo(name = "media_url")
    val mediaUrl: String? = null,
    
    @ColumnInfo(name = "media_type")
    val mediaType: MediaType? = null,
    
    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),
    
    @ColumnInfo(name = "likes_count")
    val likesCount: Int = 0,
    
    @ColumnInfo(name = "comments_count")
    val commentsCount: Int = 0,
    
    @ColumnInfo(name = "is_featured")
    val isFeatured: Boolean = false,
    
    @ColumnInfo(name = "location")
    val location: String? = null,
    
    @ColumnInfo(name = "visibility")
    val visibility: Visibility = Visibility.PUBLIC,
    
    @ColumnInfo(name = "tags")
    val tags: List<String> = emptyList()
)

enum class MediaType {
    IMAGE,
    VIDEO,
    AUDIO,
    NONE
}

enum class Visibility {
    PUBLIC,
    FRIENDS,
    PRIVATE
}