package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.User
import java.util.Date

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val email: String? = null,
    val phone: String? = null,
    val passwordHash: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val syncToken: String? = null,
    val syncEnabled: Boolean = false
) {
    fun toDomain(): User = User(
        id = id,
        username = username,
        email = email,
        phone = phone,
        createdAt = Date(createdAt),
        lastLoginAt = lastLoginAt?.let { Date(it) },
        syncEnabled = syncEnabled
    )

    companion object {
        fun fromDomain(user: User, passwordHash: String): UserEntity = UserEntity(
            id = user.id,
            username = user.username,
            email = user.email,
            phone = user.phone,
            passwordHash = passwordHash,
            createdAt = user.createdAt.time,
            lastLoginAt = user.lastLoginAt?.time,
            syncEnabled = user.syncEnabled
        )
    }
}
