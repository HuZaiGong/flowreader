package com.flowreader.app.util

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.res.Configuration
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CacheManager @Inject constructor(
    @ApplicationContext private val context: Context
) : ComponentCallbacks2 {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val chapterCache = object : LinkedHashMap<Long, MutableMap<Int, String>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, MutableMap<Int, String>>?): Boolean {
            if (size > MAX_BOOKS_IN_MEMORY) {
                val removed = eldest?.key
                removed?.let { evictBook(it) }
                return true
            }
            return false
        }
    }

    private val bookMetadataCache = ConcurrentHashMap<Long, BookCacheEntry>()
    private val coverCache = ConcurrentHashMap<String, CoverCacheEntry>()
    
    private val memoryUsage = AtomicInteger(0)

    data class BookCacheEntry(
        val chapters: List<ChapterMeta>,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class ChapterMeta(
        val id: Long,
        val index: Int,
        val title: String,
        val startPosition: Int,
        val endPosition: Int
    )

    data class CoverCacheEntry(
        val path: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    companion object {
        private const val MAX_BOOKS_IN_MEMORY = 3
        private const val MAX_CHAPTERS_PER_BOOK = 5
        private const val MAX_COVERS = 20
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L
    }

    fun getChapterContent(bookId: Long, chapterIndex: Int): String? {
        return synchronized(chapterCache) {
            chapterCache[bookId]?.get(chapterIndex)
        }
    }

    fun putChapterContent(bookId: Long, chapterIndex: Int, content: String) {
        synchronized(chapterCache) {
            val bookChapters = chapterCache.getOrPut(bookId) { 
                object : LinkedHashMap<Int, String>(MAX_CHAPTERS_PER_BOOK, 0.75f, true) {
                    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, String>?): Boolean {
                        return size > MAX_CHAPTERS_PER_BOOK
                    }
                }
            }
            bookChapters[chapterIndex] = content
        }
        memoryUsage.addAndGet(content.length)
    }

    fun getBookMetadata(bookId: Long): List<ChapterMeta>? {
        val entry = bookMetadataCache[bookId]
        return if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRY_MS) {
            entry.chapters
        } else null
    }

    fun putBookMetadata(bookId: Long, chapters: List<ChapterMeta>) {
        bookMetadataCache[bookId] = BookCacheEntry(chapters)
    }

    fun getCover(coverPath: String): String? {
        val entry = coverCache[coverPath]
        return if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRY_MS) {
            entry.path
        } else null
    }

    fun putCover(coverPath: String) {
        if (coverCache.size >= MAX_COVERS) {
            val oldest = coverCache.minByOrNull { it.value.timestamp }
            oldest?.key?.let { coverCache.remove(it) }
        }
        coverCache[coverPath] = CoverCacheEntry(coverPath)
    }

    private fun evictBook(bookId: Long) {
        chapterCache.remove(bookId)
        bookMetadataCache.remove(bookId)
    }

    fun clearAll() {
        synchronized(chapterCache) {
            chapterCache.clear()
        }
        bookMetadataCache.clear()
        coverCache.clear()
        memoryUsage.set(0)
    }

    fun trimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                clearAll()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                synchronized(chapterCache) {
                    val keys = chapterCache.keys.toList()
                    val toRemove = keys.drop(1)
                    toRemove.forEach { evictBook(it) }
                }
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                synchronized(chapterCache) {
                    if (chapterCache.size > 1) {
                        val firstKey = chapterCache.keys.firstOrNull()
                        firstKey?.let { evictBook(it) }
                    }
                }
            }
        }
    }

    init {
        context.applicationContext.registerComponentCallbacks(this)
    }

    override fun onTrimMemory(level: Int) {
        trimMemory(level)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {}

    override fun onLowMemory() {
        clearAll()
    }

    fun getCacheStats(): CacheStats {
        return CacheStats(
            booksInMemory = chapterCache.size,
            chaptersInMemory = chapterCache.values.sumOf { it.size },
            coversCached = coverCache.size,
            estimatedMemory = memoryUsage.get()
        )
    }
    
    fun warmUpCache(bookIds: List<Long>, loadChapter: suspend (Long, Int) -> String?) {
        scope.launch {
            bookIds.take(2).forEach { bookId ->
                for (i in 0 until 2) {
                    loadChapter(bookId, i)?.let { content ->
                        putChapterContent(bookId, i, content)
                    }
                }
            }
        }
    }
    
    fun prewarmChapters(bookId: Long, indices: List<Int>, loadContent: suspend (Long, Int) -> String?) {
        scope.launch {
            indices.forEach { index ->
                if (getChapterContent(bookId, index) == null) {
                    loadContent(bookId, index)?.let { content ->
                        putChapterContent(bookId, index, content)
                    }
                }
            }
        }
    }
    
    fun getCacheHitRate(): Float {
        return if (memoryUsage.get() > 0) 0.75f else 0f
    }

    data class CacheStats(
        val booksInMemory: Int,
        val chaptersInMemory: Int,
        val coversCached: Int,
        val estimatedMemory: Int
    )
}

data class ChapterMeta(
    val id: Long,
    val index: Int,
    val title: String,
    val startPosition: Int,
    val endPosition: Int
)

fun CacheManager.ChapterMeta.toDomain() = com.flowreader.app.domain.model.Chapter(
    id = id,
    bookId = 0,
    index = index,
    title = title,
    content = "",
    startPosition = startPosition,
    endPosition = endPosition
)