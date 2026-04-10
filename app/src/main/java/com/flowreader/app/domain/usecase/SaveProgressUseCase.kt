package com.flowreader.app.domain.usecase

import com.flowreader.app.domain.repository.BookRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class SaveProgressUseCase @Inject constructor(
    private val bookRepository: BookRepository
) {
    private var saveJob: kotlinx.coroutines.Job? = null
    
    private val _lastSaveTime = MutableStateFlow(0L)
    val lastSaveTime: StateFlow<Long> = _lastSaveTime.asStateFlow()
    
    companion object {
        const val DEBOUNCE_MS = 3000L
    }
    
    suspend operator fun invoke(
        bookId: Long,
        chapterIndex: Int,
        position: Int,
        progress: Float
    ) {
        delay(DEBOUNCE_MS)
        bookRepository.updateReadingProgress(bookId, chapterIndex, position, progress)
        _lastSaveTime.value = System.currentTimeMillis()
    }
    
    fun cancelPending() {
        saveJob?.cancel()
        saveJob = null
    }
}