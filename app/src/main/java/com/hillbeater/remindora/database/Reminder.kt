package com.hillbeater.remindora.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val date: String,
    val time: String,
    val content: String,
    val bgColor: Int,
    val textColor: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val pushNotification: Boolean = false
)
