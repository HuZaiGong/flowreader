package com.flowreader.app.data.local.dao

import androidx.room.*
import com.flowreader.app.data.local.entity.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE phone = :phone")
    suspend fun getUserByPhone(phone: String): UserEntity?

    @Query("SELECT * FROM users WHERE syncToken = :token")
    suspend fun getUserBySyncToken(token: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("UPDATE users SET lastLoginAt = :time WHERE id = :userId")
    suspend fun updateLastLoginTime(userId: Long, time: Long = System.currentTimeMillis())

    @Query("UPDATE users SET syncToken = :token, syncEnabled = :enabled WHERE id = :userId")
    suspend fun updateSyncSettings(userId: Long, token: String?, enabled: Boolean)

    @Query("SELECT * FROM users LIMIT 1")
    fun getCurrentUser(): Flow<UserEntity?>
}
