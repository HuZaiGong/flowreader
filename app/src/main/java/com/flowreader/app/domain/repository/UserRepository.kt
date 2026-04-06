package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.User
import com.flowreader.app.domain.model.SyncData
import kotlinx.coroutines.flow.Flow

interface UserRepository {
    suspend fun getUserById(id: Long): User?
    suspend fun getUserByUsername(username: String): User?
    suspend fun getUserByEmail(email: String): User?
    suspend fun register(username: String, password: String, email: String? = null, phone: String? = null): Result<User>
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout()
    fun getCurrentUser(): Flow<User?>
    suspend fun updateSyncSettings(enabled: Boolean, token: String?)
    suspend fun exportSyncData(): SyncData
    suspend fun importSyncData(syncData: SyncData): Result<Unit>
}
