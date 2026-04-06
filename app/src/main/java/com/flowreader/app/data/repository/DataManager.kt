package com.flowreader.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataCleaner @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("data_cleaner", Context.MODE_PRIVATE)
    
    private val _lastCleanupTime = MutableStateFlow(prefs.getLong("last_cleanup", 0))
    val lastCleanupTime: Flow<Long> = _lastCleanupTime.asStateFlow()
    
    fun shouldCleanup(): Boolean {
        val lastCleanup = prefs.getLong("last_cleanup", 0)
        val currentTime = System.currentTimeMillis()
        val interval = 24 * 60 * 60 * 1000L
        return currentTime - lastCleanup > interval
    }
    
    fun recordCleanup() {
        prefs.edit { putLong("last_cleanup", System.currentTimeMillis()) }
        _lastCleanupTime.value = System.currentTimeMillis()
    }
}

@Singleton
class DataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataCleaner: DataCleaner
) {
    companion object {
        private const val PREFS_NAME = "data_manager"
        private const val KEY_BOOK_COUNT = "book_count"
        private const val KEY_LAST_SYNC = "last_sync"
        private const val KEY_SESSION_COUNT = "session_count"
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val _bookCount = MutableStateFlow(0)
    val bookCount: Flow<Int> = _bookCount.asStateFlow()
    
    init {
        _bookCount.value = prefs.getInt(KEY_BOOK_COUNT, 0)
    }
    
    fun incrementBookCount() {
        val newCount = _bookCount.value + 1
        _bookCount.value = newCount
        prefs.edit { putInt(KEY_BOOK_COUNT, newCount) }
        
        if (dataCleaner.shouldCleanup()) {
            dataCleaner.recordCleanup()
        }
    }
    
    fun resetBookCount() {
        _bookCount.value = 0
        prefs.edit { putInt(KEY_BOOK_COUNT, 0) }
    }
    
    fun getSessionCount(): Int = prefs.getInt(KEY_SESSION_COUNT, 0)
    
    fun incrementSessionCount() {
        val count = getSessionCount() + 1
        prefs.edit { putInt(KEY_SESSION_COUNT, count) }
    }
    
    fun getLastSyncTime(): Long = prefs.getLong(KEY_LAST_SYNC, 0)
    
    fun setLastSyncTime(time: Long) {
        prefs.edit { putLong(KEY_LAST_SYNC, time) }
    }
}
