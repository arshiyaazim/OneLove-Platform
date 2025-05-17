package com.kilagee.onelove.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kilagee.onelove.data.local.converter.DateConverter
import com.kilagee.onelove.data.local.converter.GeoLocationConverter
import com.kilagee.onelove.data.local.converter.ListConverter
import com.kilagee.onelove.data.local.converter.MapConverter
import com.kilagee.onelove.data.local.converter.TimestampConverter
import com.kilagee.onelove.data.local.converter.UserGenderConverter
import com.kilagee.onelove.data.local.dao.AIProfileDao
import com.kilagee.onelove.data.local.dao.CallDao
import com.kilagee.onelove.data.local.dao.MatchDao
import com.kilagee.onelove.data.local.dao.MessageDao
import com.kilagee.onelove.data.local.dao.NotificationDao
import com.kilagee.onelove.data.local.dao.OfferDao
import com.kilagee.onelove.data.local.dao.SubscriptionDao
import com.kilagee.onelove.data.local.dao.UserDao
import com.kilagee.onelove.data.local.entity.AIProfileEntity
import com.kilagee.onelove.data.local.entity.CallEntity
import com.kilagee.onelove.data.local.entity.MatchEntity
import com.kilagee.onelove.data.local.entity.MessageEntity
import com.kilagee.onelove.data.local.entity.NotificationEntity
import com.kilagee.onelove.data.local.entity.OfferEntity
import com.kilagee.onelove.data.local.entity.SubscriptionEntity
import com.kilagee.onelove.data.local.entity.UserEntity

/**
 * Main Room database for the application
 */
@Database(
    entities = [
        UserEntity::class,
        MessageEntity::class,
        MatchEntity::class,
        OfferEntity::class,
        AIProfileEntity::class,
        NotificationEntity::class,
        CallEntity::class,
        SubscriptionEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(
    DateConverter::class,
    TimestampConverter::class,
    ListConverter::class,
    MapConverter::class,
    UserGenderConverter::class,
    GeoLocationConverter::class
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun messageDao(): MessageDao
    abstract fun matchDao(): MatchDao
    abstract fun offerDao(): OfferDao
    abstract fun aiProfileDao(): AIProfileDao
    abstract fun notificationDao(): NotificationDao
    abstract fun callDao(): CallDao
    abstract fun subscriptionDao(): SubscriptionDao
    
    companion object {
        private const val DATABASE_NAME = "onelove_database"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}