package com.flowreader.app.util

import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookLoader @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun loadChapterAsync(
        bookId: Long,
        chapterIndex: Int,
        loadContent: suspend (Long, Int) -> String?
    ): Deferred<String?> = scope.async {
        try {
            loadContent(bookId, chapterIndex)
        } catch (e: Exception) {
            null
        }
    }

    fun preloadChapters(
        bookId: Long,
        currentIndex: Int,
        totalChapters: Int,
        loadContent: suspend (Long, Int) -> String?
    ) {
        scope.launch {
            val preloadIndices = mutableListOf<Int>()

            if (currentIndex > 0) preloadIndices.add(currentIndex - 1)
            if (currentIndex < totalChapters - 1) preloadIndices.add(currentIndex + 1)
            if (currentIndex > 1) preloadIndices.add(currentIndex - 2)
            if (currentIndex < totalChapters - 2) preloadIndices.add(currentIndex + 2)

            preloadIndices.take(3).forEach { index ->
                launch {
                    loadContent(bookId, index)
                }
            }
        }
    }

    fun cancelAll() {
        scope.cancel()
    }
}
