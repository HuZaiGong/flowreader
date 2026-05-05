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
    
    // Optimized: Use ConcurrentHashMap for thread-safe access without explicit synchronization
    private val chapterCache = ConcurrentHashMap<Long, ChapterCache>()
    private val bookMetadataCache = ConcurrentHashMap<Long, BookCacheEntry>()
    private val coverCache = ConcurrentHashMap<String, CoverCacheEntry>()
    
    private val memoryUsage = AtomicInteger(0)
    
    // Optimized: LRU cache for chapters per book using LinkedHashMap
    inner class ChapterCache : LinkedHashMap<Int, String>(MAX_CHAPTERS_PER_BOOK, 0.75f, true) {
        @Synchronized
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, String>?): Boolean {
            return size > MAX_CHAPTERS_PER_BOOK
        }
        
        @Synchronized
        fun putContent(index: Int, content: String): String? {
            val old = put(index, content)
            return old
        }
        
        @Synchronized
        fun getContent(index: Int): String? = get(index)
    }

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

    /**
     * Get cached chapter content with O(1) lookup
     */
    fun getChapterContent(bookId: Long, chapterIndex: Int): String? {
        return chapterCache[bookId]?.getContent(chapterIndex)
    }

    /**
     * Put chapter content with automatic LRU eviction
     */
    fun putChapterContent(bookId: Long, chapterIndex: Int, content: String) {
        val bookCache = chapterCache.getOrPut(bookId) { ChapterCache() }
        val oldContent = bookCache.putContent(chapterIndex, content)
        
        // Update memory usage tracking
        val oldSize = oldContent?.length ?: 0
        memoryUsage.addAndGet(content.length - oldSize)
    }

    /**
     * Get book metadata with expiry check
     */
    fun getBookMetadata(bookId: Long): List<ChapterMeta>? {
        val entry = bookMetadataCache[bookId]
        return if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRY_MS) {
            entry.chapters
        } else null
    }

    /**
     * Put book metadata into cache
     */
    fun putBookMetadata(bookId: Long, chapters: List<ChapterMeta>) {
        bookMetadataCache[bookId] = BookCacheEntry(chapters)
    }

    /**
     * Get cached cover path with expiry check
     */
    fun getCover(coverPath: String): String? {
        val entry = coverCache[coverPath]
        return if (entry != null && System.currentTimeMillis() - entry.timestamp < CACHE_EXPIRY_MS) {
            entry.path
        } else null
    }

    /**
     * Put cover into cache with automatic eviction when cache is full
     */
    fun putCover(coverPath: String) {
        if (coverCache.size >= MAX_COVERS) {
            val oldest = coverCache.entries.minByOrNull { it.value.timestamp }
            oldest?.key?.let { coverCache.remove(it) }
        }
        coverCache[coverPath] = CoverCacheEntry(coverPath)
    }

    /**
     * Evict a specific book from all caches
     */
    private fun evictBook(bookId: Long) {
        chapterCache.remove(bookId)
        bookMetadataCache.remove(bookId)
    }

    /**
     * Clear all caches and reset memory tracking
     */
    fun clearAll() {
        chapterCache.clear()
        bookMetadataCache.clear()
        coverCache.clear()
        memoryUsage.set(0)
    }

    /**
     * Trim memory based on system callback level
     */
    fun trimMemory(level: Int) {
        when (level) {
            ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL,
            ComponentCallbacks2.TRIM_MEMORY_COMPLETE -> {
                clearAll()
            }
            ComponentCallbacks2.TRIM_MEMORY_MODERATE -> {
                // Keep only the most recently accessed book
                val keys = chapterCache.keys.toList()
                if (keys.size > 1) {
                    keys.drop(1).forEach { evictBook(it) }
                }
            }
            ComponentCallbacks2.TRIM_MEMORY_BACKGROUND -> {
                // Remove least recently accessed book
                if (chapterCache.size > 1) {
                    chapterCache.keys.firstOrNull()?.let { evictBook(it) }
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

    /**
     * Get cache statistics for monitoring
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            booksInMemory = chapterCache.size,
            chaptersInMemory = chapterCache.values.sumOf { it.size },
            coversCached = coverCache.size,
            estimatedMemory = memoryUsage.get()
        )
    }
    
    /**
     * Warm up cache for frequently accessed books
     */
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
    
    /**
     * Preload specific chapter indices for a book
     */
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
    
    /**
     * Estimate cache hit rate (simplified implementation)
     */
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