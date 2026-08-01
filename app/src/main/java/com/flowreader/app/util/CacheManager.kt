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
    @ApplicationContext private val context: Context,
    private val memoryManager: MemoryManager
) : ComponentCallbacks2 {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Dynamic chapter-cache capacity: starts from the memory recommendation and adapts to the
     * observed hit rate — high hit rate grows the cache (up to [MAX_CHAPTERS_PER_BOOK]), low
     * hit rate shrinks it (down to [MIN_CHAPTERS_PER_BOOK]). Written under [chapterCache] lock.
     */
    private var maxBooks: Int
    private var maxChaptersPerBook: Int

    init {
        val recommended = memoryManager.getRecommendedCacheSize()
        maxBooks = recommended.coerceIn(1, 5)
        maxChaptersPerBook = (recommended + 2).coerceIn(MIN_CHAPTERS_PER_BOOK, MAX_CHAPTERS_PER_BOOK)
    }

    private val chapterCache = object : LinkedHashMap<Long, MutableMap<Int, String>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, MutableMap<Int, String>>?): Boolean {
            if (size > maxBooks) {
                eldest?.key?.let { bookId ->
                    bookMetadataCache.remove(bookId)
                }
                return true
            }
            return false
        }
    }

    private val bookMetadataCache = ConcurrentHashMap<Long, BookCacheEntry>()
    private val coverCache = ConcurrentHashMap<String, CoverCacheEntry>()

    private val memoryUsage = AtomicInteger(0)
    private val bookUsage = ConcurrentHashMap<Long, AtomicInteger>()
    private val accessWindow = AtomicInteger(0)

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
        private const val MAX_COVERS = 20
        private const val CACHE_EXPIRY_MS = 30 * 60 * 1000L
        private const val MIN_CHAPTERS_PER_BOOK = 2
        private const val MAX_CHAPTERS_PER_BOOK = 12
        private const val ADAPT_WINDOW = 50
        private const val ADAPT_GROW_HIT_RATE = 0.6f
        private const val ADAPT_SHRINK_HIT_RATE = 0.3f
    }

    fun getChapterContent(bookId: Long, chapterIndex: Int): String? {
        val content = synchronized(chapterCache) {
            chapterCache[bookId]?.get(chapterIndex)
        }
        if (content != null) {
            recordCacheHit()
            bookUsage.getOrPut(bookId) { AtomicInteger(0) }.incrementAndGet()
        } else {
            recordCacheMiss()
        }
        maybeAdaptCapacity()
        return content
    }

    fun putChapterContent(bookId: Long, chapterIndex: Int, content: String) {
        synchronized(chapterCache) {
            val bookChapters = chapterCache.getOrPut(bookId) {
                object : LinkedHashMap<Int, String>(maxChaptersPerBook, 0.75f, true) {
                    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, String>?): Boolean {
                        return size > maxChaptersPerBook
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
        bookUsage.remove(bookId)
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
                    // Evict the least-used books first, keeping the hottest one in memory.
                    val ranked = chapterCache.keys
                        .sortedBy { bookId -> bookUsage[bookId]?.get() ?: 0 }
                    ranked.dropLast(1).forEach { evictBook(it) }
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
        val bookCount: Int
        val chapterCount: Int
        synchronized(chapterCache) {
            bookCount = chapterCache.size
            chapterCount = chapterCache.values.sumOf { it.size }
        }
        return CacheStats(
            booksInMemory = bookCount,
            chaptersInMemory = chapterCount,
            coversCached = coverCache.size,
            estimatedMemory = memoryUsage.get(),
            chaptersCapacity = synchronized(chapterCache) { maxChaptersPerBook }
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

    private var cacheHits = AtomicInteger(1)
    private var cacheMisses = AtomicInteger(1)

    fun recordCacheHit() = cacheHits.incrementAndGet()
    fun recordCacheMiss() = cacheMisses.incrementAndGet()

    /** Samples the hit rate every [ADAPT_WINDOW] accesses and tunes the chapter capacity. */
    private fun maybeAdaptCapacity() {
        if (accessWindow.incrementAndGet() % ADAPT_WINDOW != 0) return
        val hitRate = getCacheHitRate()
        synchronized(chapterCache) {
            when {
                hitRate >= ADAPT_GROW_HIT_RATE && maxChaptersPerBook < MAX_CHAPTERS_PER_BOOK -> maxChaptersPerBook++
                hitRate <= ADAPT_SHRINK_HIT_RATE && maxChaptersPerBook > MIN_CHAPTERS_PER_BOOK -> maxChaptersPerBook--
            }
        }
    }

    fun getCacheHitRate(): Float {
        val hits = cacheHits.get()
        val total = hits + cacheMisses.get()
        return if (total > 0) hits.toFloat() / total else 0f
    }

    data class CacheStats(
        val booksInMemory: Int,
        val chaptersInMemory: Int,
        val coversCached: Int,
        val estimatedMemory: Int,
        val chaptersCapacity: Int
    )
}

fun CacheManager.ChapterMeta.toDomain(bookId: Long) = com.flowreader.app.domain.model.Chapter(
    id = id,
    bookId = bookId,
    index = index,
    title = title,
    content = "",
    startPosition = startPosition,
    endPosition = endPosition
)