package com.hillbeater.remindora.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserInfoDao {
    @Insert
    suspend fun insertUserInformation(user: UserInformation)

    @Query("SELECT * FROM UserInfoEntity LIMIT 1")
    suspend fun getUserInfo(): UserInformation?

    @Query("DELETE FROM UserInfoEntity")
    suspend fun clearAllData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(userInfo: UserInformation)

}