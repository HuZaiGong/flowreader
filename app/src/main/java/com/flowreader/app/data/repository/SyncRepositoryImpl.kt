package com.flowreader.app.data.repository

import com.flowreader.app.domain.model.SyncBookmark
import com.flowreader.app.domain.model.SyncProgress
import com.flowreader.app.domain.model.SyncSettings
import com.flowreader.app.domain.repository.SyncData
import com.flowreader.app.domain.repository.SyncRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SyncRepository {

    private fun getUserDoc(userId: String) = firestore.collection("users").document(userId)

    override suspend fun syncReadingProgress(userId: String, progress: SyncProgress): Result<Unit> {
        return try {
            getUserDoc(userId)
                .collection("progress")
                .document(progress.bookId.toString())
                .set(mapOf(
                    "bookId" to progress.bookId,
                    "currentChapter" to progress.currentChapter,
                    "currentPosition" to progress.currentPosition,
                    "readingProgress" to progress.readingProgress,
                    "lastReadTime" to progress.lastReadTime
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRemoteProgress(userId: String, bookId: Long): Result<SyncProgress?> {
        return try {
            val doc = getUserDoc(userId)
                .collection("progress")
                .document(bookId.toString())
                .get()
                .await()

            if (doc.exists()) {
                val progress = SyncProgress(
                    bookId = doc.getLong("bookId") ?: bookId,
                    currentChapter = doc.getLong("currentChapter")?.toInt() ?: 0,
                    currentPosition = doc.getLong("currentPosition")?.toInt() ?: 0,
                    readingProgress = doc.getDouble("readingProgress")?.toFloat() ?: 0f,
                    lastReadTime = doc.getLong("lastReadTime") ?: 0
                )
                Result.success(progress)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncBookmarks(userId: String, bookmarks: List<SyncBookmark>): Result<Unit> {
        return try {
            val batch = firestore.batch()
            val bookmarksRef = getUserDoc(userId).collection("bookmarks")

            bookmarks.forEachIndexed { index, bookmark ->
                batch.set(bookmarksRef.document(bookmark.bookId.toString() + "_" + index), mapOf(
                    "bookId" to bookmark.bookId,
                    "chapterIndex" to bookmark.chapterIndex,
                    "position" to bookmark.position,
                    "text" to bookmark.text,
                    "createdTime" to bookmark.createdTime
                ))
            }

            batch.commit().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRemoteBookmarks(userId: String, bookId: Long): Result<List<SyncBookmark>> {
        return try {
            val snapshot = getUserDoc(userId)
                .collection("bookmarks")
                .whereEqualTo("bookId", bookId)
                .get()
                .await()

            val bookmarks = snapshot.documents.mapNotNull { doc ->
                try {
                    SyncBookmark(
                        bookId = doc.getLong("bookId") ?: bookId,
                        chapterIndex = doc.getLong("chapterIndex")?.toInt() ?: 0,
                        position = doc.getLong("position")?.toInt() ?: 0,
                        text = doc.getString("text") ?: "",
                        createdTime = doc.getLong("createdTime") ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(bookmarks)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncSettings(userId: String, settings: SyncSettings): Result<Unit> {
        return try {
            getUserDoc(userId)
                .collection("settings")
                .document("userSettings")
                .set(mapOf(
                    "fontSize" to settings.fontSize,
                    "lineSpacing" to settings.lineSpacing,
                    "theme" to settings.theme,
                    "pageMode" to settings.pageMode,
                    "keepScreenOn" to settings.keepScreenOn
                ))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRemoteSettings(userId: String): Result<SyncSettings?> {
        return try {
            val doc = getUserDoc(userId)
                .collection("settings")
                .document("userSettings")
                .get()
                .await()

            if (doc.exists()) {
                val settings = SyncSettings(
                    fontSize = doc.getLong("fontSize")?.toInt() ?: 18,
                    lineSpacing = doc.getDouble("lineSpacing")?.toFloat() ?: 1.5f,
                    theme = doc.getString("theme") ?: "LIGHT",
                    pageMode = doc.getString("pageMode") ?: "SLIDE",
                    keepScreenOn = doc.getBoolean("keepScreenOn") ?: true
                )
                Result.success(settings)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncAllData(
        userId: String,
        progressList: List<SyncProgress>,
        bookmarkList: List<SyncBookmark>,
        settings: SyncSettings
    ): Result<Unit> {
        return try {
            val batch = firestore.batch()

            progressList.forEach { progress ->
                val ref = getUserDoc(userId)
                    .collection("progress")
                    .document(progress.bookId.toString())
                batch.set(ref, mapOf(
                    "bookId" to progress.bookId,
                    "currentChapter" to progress.currentChapter,
                    "currentPosition" to progress.currentPosition,
                    "readingProgress" to progress.readingProgress,
                    "lastReadTime" to progress.lastReadTime
                ))
            }

            bookmarkList.forEachIndexed { index, bookmark ->
                val ref = getUserDoc(userId)
                    .collection("bookmarks")
                    .document(bookmark.bookId.toString() + "_" + index)
                batch.set(ref, mapOf(
                    "bookId" to bookmark.bookId,
                    "chapterIndex" to bookmark.chapterIndex,
                    "position" to bookmark.position,
                    "text" to bookmark.text,
                    "createdTime" to bookmark.createdTime
                ))
            }

            val settingsRef = getUserDoc(userId)
                .collection("settings")
                .document("userSettings")
            batch.set(settingsRef, mapOf(
                "fontSize" to settings.fontSize,
                "lineSpacing" to settings.lineSpacing,
                "theme" to settings.theme,
                "pageMode" to settings.pageMode,
                "keepScreenOn" to settings.keepScreenOn
            ))

            batch.commit().await()

            getUserDoc(userId)
                .update("lastSyncTime", com.google.firebase.Timestamp.now())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAllRemoteData(userId: String): Result<SyncData> {
        return try {
            val progressSnapshot = getUserDoc(userId)
                .collection("progress")
                .get()
                .await()

            val progressList = progressSnapshot.documents.mapNotNull { doc ->
                try {
                    SyncProgress(
                        bookId = doc.getLong("bookId") ?: 0,
                        currentChapter = doc.getLong("currentChapter")?.toInt() ?: 0,
                        currentPosition = doc.getLong("currentPosition")?.toInt() ?: 0,
                        readingProgress = doc.getDouble("readingProgress")?.toFloat() ?: 0f,
                        lastReadTime = doc.getLong("lastReadTime") ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val bookmarkSnapshot = getUserDoc(userId)
                .collection("bookmarks")
                .get()
                .await()

            val bookmarkList = bookmarkSnapshot.documents.mapNotNull { doc ->
                try {
                    SyncBookmark(
                        bookId = doc.getLong("bookId") ?: 0,
                        chapterIndex = doc.getLong("chapterIndex")?.toInt() ?: 0,
                        position = doc.getLong("position")?.toInt() ?: 0,
                        text = doc.getString("text") ?: "",
                        createdTime = doc.getLong("createdTime") ?: 0
                    )
                } catch (e: Exception) {
                    null
                }
            }

            val settings = getRemoteSettings(userId).getOrNull()

            Result.success(SyncData(progressList, bookmarkList, settings))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun enableSync(userId: String, enabled: Boolean): Result<Unit> {
        return try {
            getUserDoc(userId)
                .update("syncEnabled", enabled)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
