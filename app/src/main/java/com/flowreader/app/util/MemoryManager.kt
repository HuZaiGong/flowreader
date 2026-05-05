package com.flowreader.app.util

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    private val memoryInfo = ActivityManager.MemoryInfo()

    /**
     * Get available memory in MB with single system call
     */
    fun getAvailableMemoryMB(): Long {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    /**
     * Get total memory in MB with single system call
     */
    fun getTotalMemoryMB(): Long {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }

    fun getMemoryClassMB(): Int = activityManager.memoryClass

    /**
     * Check if device is in low memory state with single system call
     */
    fun isLowMemory(): Boolean {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }

    /**
     * Calculate memory usage percentage with single system call
     */
    fun getMemoryUsagePercent(): Float {
        activityManager.getMemoryInfo(memoryInfo)
        return ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem * 100)
    }

    /**
     * Get memory pressure level based on available memory and usage percentage
     * Optimized to use cached memoryInfo from previous calls when possible
     */
    fun getMemoryPressureLevel(): MemoryPressureLevel {
        activityManager.getMemoryInfo(memoryInfo)
        val available = memoryInfo.availMem / (1024 * 1024)
        val usage = ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem * 100)
        
        return when {
            available < 100 || usage > 85 -> MemoryPressureLevel.CRITICAL
            available < 200 || usage > 75 -> MemoryPressureLevel.HIGH
            available < 400 || usage > 60 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NORMAL
        }
    }

    /**
     * Get recommended cache size based on available memory
     */
    fun getRecommendedCacheSize(): Int {
        val available = getAvailableMemoryMB()
        return when {
            available < 100 -> 1
            available < 200 -> 2
            available < 400 -> 3
            else -> 5
        }
    }

    /**
     * Get comprehensive memory status in a single call
     * Optimized to minimize system calls by reusing memoryInfo
     */
    fun getMemoryStatus(): MemoryStatus {
        activityManager.getMemoryInfo(memoryInfo)
        val availableMB = memoryInfo.availMem / (1024 * 1024)
        val totalMB = memoryInfo.totalMem / (1024 * 1024)
        val usagePercent = ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem * 100)
        
        return MemoryStatus(
            availableMB = availableMB,
            totalMB = totalMB,
            usagePercent = usagePercent,
            memoryClassMB = activityManager.memoryClass,
            isLowMemory = memoryInfo.lowMemory,
            pressureLevel = when {
                usagePercent > 85 -> MemoryPressureLevel.CRITICAL
                usagePercent > 75 -> MemoryPressureLevel.HIGH
                usagePercent > 60 -> MemoryPressureLevel.MODERATE
                else -> MemoryPressureLevel.NORMAL
            }
        )
    }

    enum class MemoryPressureLevel {
        NORMAL,
        MODERATE,
        HIGH,
        CRITICAL
    }

    data class MemoryStatus(
        val availableMB: Long,
        val totalMB: Long,
        val usagePercent: Float,
        val memoryClassMB: Int,
        val isLowMemory: Boolean,
        val pressureLevel: MemoryPressureLevel
    )
}