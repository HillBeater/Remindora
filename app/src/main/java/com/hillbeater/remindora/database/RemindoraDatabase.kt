package com.hillbeater.remindora.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hillbeater.remindora.utils.Converters

@Database(entities = [UserInformation::class, Reminder::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class RemindoraDatabase : RoomDatabase() {
    abstract fun userInfoDao(): UserInfoDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: RemindoraDatabase? = null

        fun getDatabase(context: Context): RemindoraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    RemindoraDatabase::class.java,
                    "remindora_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}