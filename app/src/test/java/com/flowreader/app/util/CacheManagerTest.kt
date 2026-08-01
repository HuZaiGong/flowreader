package com.flowreader.app.util

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheManagerTest {

    private val context = mockk<Context>(relaxed = true)
    private val memoryManager = mockk<MemoryManager>(relaxed = true)

    private fun cacheManager(recommended: Int = 3): CacheManager {
        every { memoryManager.getRecommendedCacheSize() } returns recommended
        return CacheManager(context, memoryManager)
    }

    @Test
    fun initialCapacityFollowsMemoryRecommendation() {
        val cm = cacheManager(recommended = 3)
        assertEquals(5, cm.getCacheStats().chaptersCapacity)
    }

    @Test
    fun highHitRateGrowsCapacity() {
        val cm = cacheManager(recommended = 1)
        val initial = cm.getCacheStats().chaptersCapacity

        // Simulate 50 window accesses with mostly hits.
        repeat(50) { i ->
            cm.putChapterContent(1L, i % 3, "content-$i")
            cm.getChapterContent(1L, i % 3)
        }
        cm.getChapterContent(1L, 0) // ensure hits > 60%
        cm.getChapterContent(1L, 0)
        cm.getChapterContent(1L, 0)

        assertTrue("capacity ${cm.getCacheStats().chaptersCapacity} should exceed initial $initial",
            cm.getCacheStats().chaptersCapacity > initial)
    }

    @Test
    fun lowHitRateShrinksCapacity() {
        val cm = cacheManager(recommended = 5)
        val initial = cm.getCacheStats().chaptersCapacity

        // 50 accesses, nearly all misses (content never cached before lookup).
        repeat(50) { i ->
            cm.getChapterContent(100L, i)
        }
        cm.getChapterContent(101L, 0)
        cm.getChapterContent(101L, 1)

        assertTrue(cm.getCacheStats().chaptersCapacity < initial)
    }

    @Test
    fun capacityNeverDropsBelowFloor() {
        val cm = cacheManager(recommended = 5)
        repeat(300) { i ->
            cm.getChapterContent(900L, i)
        }
        assertTrue(cm.getCacheStats().chaptersCapacity >= 2)
    }

    @Test
    fun evictBookClearsUsageTracking() {
        val cm = cacheManager(recommended = 5)
        repeat(5) { i ->
            cm.putChapterContent(1L, i, "c$i")
            cm.getChapterContent(1L, i)
        }
        assertEquals(5, cm.getCacheStats().chaptersInMemory)

        cm.trimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_COMPLETE)
        assertEquals(0, cm.getCacheStats().chaptersInMemory)
        assertNull(cm.getChapterContent(1L, 0))
    }

    @Test
    fun leastUsedBookIsEvictedFirstOnModerateTrim() {
        val cm = cacheManager(recommended = 5)
        cm.putChapterContent(1L, 0, "a")
        cm.putChapterContent(2L, 0, "b")
        cm.putChapterContent(3L, 0, "c")

        // Book 1 is used most, book 3 least.
        repeat(9) { cm.getChapterContent(1L, 0) }
        repeat(3) { cm.getChapterContent(2L, 0) }
        cm.getChapterContent(3L, 0)

        cm.trimMemory(android.content.ComponentCallbacks2.TRIM_MEMORY_MODERATE)

        // Only the hottest book survives.
        assertEquals(1, cm.getCacheStats().booksInMemory)
        assertEquals("a", cm.getChapterContent(1L, 0))
        assertNull(cm.getChapterContent(2L, 0))
        assertNull(cm.getChapterContent(3L, 0))
    }
}
