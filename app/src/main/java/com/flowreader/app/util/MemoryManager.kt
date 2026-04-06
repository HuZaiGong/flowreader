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

    fun getAvailableMemoryMB(): Long {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024)
    }

    fun getTotalMemoryMB(): Long {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024)
    }

    fun getMemoryClassMB(): Int {
        return activityManager.memoryClass
    }

    fun isLowMemory(): Boolean {
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }

    fun getMemoryUsagePercent(): Float {
        activityManager.getMemoryInfo(memoryInfo)
        return ((memoryInfo.totalMem - memoryInfo.availMem).toFloat() / memoryInfo.totalMem * 100)
    }

    fun getMemoryPressureLevel(): MemoryPressureLevel {
        val available = getAvailableMemoryMB()
        val usage = getMemoryUsagePercent()
        
        return when {
            available < 100 || usage > 85 -> MemoryPressureLevel.CRITICAL
            available < 200 || usage > 75 -> MemoryPressureLevel.HIGH
            available < 400 || usage > 60 -> MemoryPressureLevel.MODERATE
            else -> MemoryPressureLevel.NORMAL
        }
    }

    fun getRecommendedCacheSize(): Int {
        val available = getAvailableMemoryMB()
        return when {
            available < 100 -> 1
            available < 200 -> 2
            available < 400 -> 3
            else -> 5
        }
    }

    enum class MemoryPressureLevel {
        NORMAL,
        MODERATE,
        HIGH,
        CRITICAL
    }

    fun getMemoryStatus(): MemoryStatus {
        return MemoryStatus(
            availableMB = getAvailableMemoryMB(),
            totalMB = getTotalMemoryMB(),
            usagePercent = getMemoryUsagePercent(),
            memoryClassMB = getMemoryClassMB(),
            isLowMemory = isLowMemory(),
            pressureLevel = when {
                getMemoryUsagePercent() > 85 -> MemoryPressureLevel.CRITICAL
                getMemoryUsagePercent() > 75 -> MemoryPressureLevel.HIGH
                getMemoryUsagePercent() > 60 -> MemoryPressureLevel.MODERATE
                else -> MemoryPressureLevel.NORMAL
            }
        )
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