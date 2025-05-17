package com.kilagee.onelove.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kilagee.onelove.data.model.Match
import com.kilagee.onelove.data.model.Message
import com.kilagee.onelove.data.model.User
import com.kilagee.onelove.data.model.UserSubscription

/**
 * Room database for the app
 */
@Database(
    entities = [
        User::class,
        Message::class,
        Match::class,
        UserSubscription::class
        // Add other entities here
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class OneLoveDatabase : RoomDatabase() {
    
    /**
     * Get user DAO
     */
    abstract fun userDao(): UserDao
    
    /**
     * Get message DAO
     */
    abstract fun messageDao(): MessageDao
    
    /**
     * Get match DAO
     */
    abstract fun matchDao(): MatchDao
    
    /**
     * Get subscription DAO
     */
    abstract fun subscriptionDao(): SubscriptionDao
    
    companion object {
        @Volatile
        private var INSTANCE: OneLoveDatabase? = null
        
        /**
         * Get database instance
         */
        fun getInstance(context: Context): OneLoveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OneLoveDatabase::class.java,
                    "onelove_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}