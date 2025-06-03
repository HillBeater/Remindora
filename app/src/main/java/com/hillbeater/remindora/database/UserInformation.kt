package com.hillbeater.remindora.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserInfoEntity")
data class UserInformation(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userName: String,
    val userEmail: String,
    val userImageUrl: String,
)
