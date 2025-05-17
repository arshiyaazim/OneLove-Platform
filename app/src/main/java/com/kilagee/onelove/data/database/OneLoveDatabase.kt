package com.kilagee.onelove.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kilagee.onelove.data.database.dao.*
import com.kilagee.onelove.data.model.*
import com.kilagee.onelove.domain.util.DateConverter
import com.kilagee.onelove.domain.util.ListConverter

@Database(
    entities = [
        User::class,
        Post::class,
        Message::class,
        Chat::class,
        Offer::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(DateConverter::class, ListConverter::class)
abstract class OneLoveDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun postDao(): PostDao
    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun offerDao(): OfferDao
}