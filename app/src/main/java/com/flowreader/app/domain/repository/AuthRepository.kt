package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.SyncBookmark
import com.flowreader.app.domain.model.SyncProgress
import com.flowreader.app.domain.model.SyncSettings
import com.flowreader.app.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>
    val isLoggedIn: Flow<Boolean>
    
    suspend fun signInWithEmail(email: String, password: String): Result<User>
    suspend fun signUpWithEmail(email: String, password: String): Result<User>
    suspend fun signInWithGoogle(idToken: String): Result<User>
    suspend fun signOut()
    suspend fun getCurrentUser(): User?
    suspend fun updateUserProfile(displayName: String): Result<Unit>
    suspend fun deleteAccount(): Result<Unit>
    suspend fun resetPassword(email: String): Result<Unit>
}

interface SyncRepository {
    suspend fun syncReadingProgress(userId: String, progress: SyncProgress): Result<Unit>
    suspend fun getRemoteProgress(userId: String, bookId: Long): Result<SyncProgress?>
    suspend fun syncBookmarks(userId: String, bookmarks: List<SyncBookmark>): Result<Unit>
    suspend fun getRemoteBookmarks(userId: String, bookId: Long): Result<List<SyncBookmark>>
    suspend fun syncSettings(userId: String, settings: SyncSettings): Result<Unit>
    suspend fun getRemoteSettings(userId: String): Result<SyncSettings?>
    suspend fun syncAllData(
        userId: String,
        progressList: List<SyncProgress>,
        bookmarkList: List<SyncBookmark>,
        settings: SyncSettings
    ): Result<Unit>
    suspend fun getAllRemoteData(userId: String): Result<SyncData>
    suspend fun enableSync(userId: String, enabled: Boolean): Result<Unit>
}

data class SyncData(
    val progressList: List<SyncProgress> = emptyList(),
    val bookmarkList: List<SyncBookmark> = emptyList(),
    val settings: SyncSettings? = null
)
