package com.hillbeater.remindora.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface ReminderDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: Reminder): Long

    @Update
    suspend fun updateReminder(reminder: Reminder)

    @Delete
    suspend fun deleteReminder(reminder: Reminder)

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllRemindersByCreatedTime(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isDeleted = 0 ORDER BY date ASC, time ASC")
    suspend fun getAllRemindersByDateAndTime(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE isDeleted = 1 ORDER BY createdAt DESC")
    suspend fun getAllTrashedReminders(): List<Reminder>

    @Query("SELECT * FROM reminders WHERE id = :reminderId")
    suspend fun getReminderById(reminderId: Int): Reminder?

    @Query("DELETE FROM reminders WHERE id = :reminderId")
    suspend fun deleteReminderPermanently(reminderId: Int)

    @Query("DELETE FROM reminders")
    suspend fun deleteAllTrashedReminders()

    @Query("UPDATE reminders SET isDeleted = 0 WHERE id = :reminderId")
    suspend fun recoverReminder(reminderId: Int)

    @Query("UPDATE reminders SET isDeleted = 0")
    suspend fun recoverAllReminders()



}