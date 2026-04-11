package com.flowreader.app.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.AnnotationType
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.PageMode
import com.flowreader.app.domain.model.ReaderTheme
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.domain.repository.AnnotationRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.ReadingStatsRepository
import com.flowreader.app.data.repository.SettingsRepository
import com.flowreader.app.util.CacheManager
import com.flowreader.app.util.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

data class ReaderUiState(
    val book: Book? = null,
    val chapters: List<Chapter> = emptyList(),
    val currentChapter: Chapter? = null,
    val currentChapterIndex: Int = 0,
    val currentPosition: Int = 0,
    val readingSettings: ReadingSettings = ReadingSettings(),
    val bookmarks: List<Bookmark> = emptyList(),
    val annotations: List<Annotation> = emptyList(),
    val showControls: Boolean = true,
    val showChapterList: Boolean = false,
    val showSettings: Boolean = false,
    val showBookmarks: Boolean = false,
    val showAnnotations: Boolean = false,
    val isLoading: Boolean = true,
    val todayReadTime: Long = 0,
    val todayReadPages: Int = 0,
    val shareText: String? = null,
    val estimatedTimeRemaining: Long = 0,
    val readingSpeed: Float = 0f,
    val sessionReadTime: Long = 0,
    val showEyeProtectionReminder: Boolean = false,
    val dailyGoalProgress: Float = 0f,
    val suggestedBreakTime: Long = 0
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val annotationRepository: AnnotationRepository,
    private val settingsRepository: SettingsRepository,
    private val readingStatsRepository: ReadingStatsRepository,
    private val cacheManager: CacheManager,
    private val memoryManager: MemoryManager,
    private val bookLoader: com.flowreader.app.util.BookLoader,
    private val ttsManager: com.flowreader.app.util.TtsManager
) : ViewModel() {

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    val ttsState: StateFlow<com.flowreader.app.util.TtsState> = ttsManager.ttsState

    private var progressSaveJob: Job? = null
    private var statsUpdateJob: Job? = null
    private var eyeProtectionJob: Job? = null
    private var predictionJob: Job? = null
    private val progressDebounceMs = 3000L
    
    private var sessionStartTime: Long = 0
    private var sessionReadPages: Int = 0
    private var sessionCharactersRead: Int = 0
    private var lastPositionUpdateTime: Long = 0
    private var lastPosition: Int = 0
    
    private val eyeProtectionIntervalMs = 20 * 60 * 1000L
    private val defaultDailyGoalMinutes = 30

    init {
        loadBook()
        loadSettings()
        loadTodayStats()
        startEyeProtectionTimer()
    }

    private fun loadTodayStats() {
        viewModelScope.launch {
            val readTime = readingStatsRepository.getTodayReadTime()
            val readPages = readingStatsRepository.getTodayReadPages()
            val dailyGoal = settingsRepository.getDailyReadingGoal().first()
            val goalProgress = if (dailyGoal > 0) (readTime.toFloat() / (dailyGoal * 60)).coerceIn(0f, 1f) else 0f
            
            _uiState.update {
                it.copy(
                    todayReadTime = readTime, 
                    todayReadPages = readPages,
                    dailyGoalProgress = goalProgress
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        progressSaveJob?.cancel()
        eyeProtectionJob?.cancel()
        predictionJob?.cancel()
        saveProgressImmediately()
        saveReadingStats()
        ttsManager.shutdown()
    }

    private fun saveReadingStats() {
        val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000
        if (sessionTime > 0 && sessionReadPages > 0) {
            viewModelScope.launch {
                readingStatsRepository.updateTodayStats(
                    bookId = bookId,
                    readPages = sessionReadPages,
                    readTimeSeconds = sessionTime
                )
            }
        }
    }

    private fun saveProgressImmediately() {
        val state = _uiState.value
        if (state.chapters.isNotEmpty()) {
            val progress = (state.currentChapterIndex.toFloat() + 1) / state.chapters.size
            viewModelScope.launch {
                bookRepository.updateReadingProgress(
                    bookId, 
                    state.currentChapterIndex, 
                    state.currentPosition, 
                    progress
                )
            }
        }
    }

    private fun debouncedSaveProgress(chapterIndex: Int, position: Int, progress: Float) {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            delay(progressDebounceMs)
            bookRepository.updateReadingProgress(bookId, chapterIndex, position, progress)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsRepository.appSettings
                .first()
                .let { settings ->
                    _uiState.update {
                        it.copy(readingSettings = settings.defaultReadingSettings)
                    }
                }
        }
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val book = bookRepository.getBookById(bookId)
            val chapterMetadata = chapterRepository.getChapterMetadataList(bookId)
            val bookmarks = bookmarkRepository.getBookmarksListByBookId(bookId)
            val annotations = annotationRepository.getAnnotationsListByBookId(bookId)

            if (book != null && chapterMetadata.isNotEmpty()) {
                val currentChapterIndex = book.currentChapter.coerceIn(0, chapterMetadata.size - 1)
                val currentChapter = chapterMetadata.getOrNull(currentChapterIndex)?.let { meta ->
                    val content = chapterRepository.getChapterContent(bookId, currentChapterIndex) ?: ""
                    meta.copy(content = content)
                }

                sessionStartTime = System.currentTimeMillis()
                lastPositionUpdateTime = sessionStartTime

                _uiState.update {
                    it.copy(
                        book = book,
                        chapters = chapterMetadata,
                        currentChapter = currentChapter,
                        currentChapterIndex = currentChapterIndex,
                        currentPosition = book.currentPosition,
                        bookmarks = bookmarks,
                        annotations = annotations,
                        isLoading = false
                    )
                }
                
                calculateReadingPrediction()
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun loadChapterContent(index: Int) {
        viewModelScope.launch {
            val content = chapterRepository.getChapterContent(bookId, index)
            content?.let {
                val state = _uiState.value
                val updatedChapter = state.chapters.getOrNull(index)?.copy(content = it)
                if (updatedChapter != null) {
                    val updatedChapters = state.chapters.toMutableList().apply {
                        if (index < size) set(index, updatedChapter)
                    }
                    _uiState.update {
                        it.copy(
                            chapters = updatedChapters,
                            currentChapter = if (index == it.currentChapterIndex) updatedChapter else it.currentChapter
                        )
                    }
                }
            }
        }
    }

    fun goToNextChapter() {
        val state = _uiState.value
        if (state.currentChapterIndex < state.chapters.size - 1) {
            goToChapter(state.currentChapterIndex + 1)
        }
    }

    fun goToPreviousChapter() {
        val state = _uiState.value
        if (state.currentChapterIndex > 0) {
            goToChapter(state.currentChapterIndex - 1)
        }
    }

    fun goToChapter(index: Int) {
        val state = _uiState.value
        if (index in state.chapters.indices) {
            val existingChapter = state.chapters[index]
            val chapterToUse = if (existingChapter.content.isNotEmpty()) {
                existingChapter
            } else {
                viewModelScope.launch {
                    val content = chapterRepository.getChapterContent(bookId, index) ?: ""
                    val updated = existingChapter.copy(content = content)
                    val updatedChapters = state.chapters.toMutableList().apply {
                        if (index < size) set(index, updated)
                    }
                    _uiState.update {
                        it.copy(chapters = updatedChapters)
                    }
                }
                existingChapter
            }

            val progress = if (state.chapters.isNotEmpty()) {
                (index.toFloat() + 1) / state.chapters.size
            } else 0f

            viewModelScope.launch {
                bookRepository.updateReadingProgress(bookId, index, 0, progress)
            }

            _uiState.update {
                it.copy(
                    currentChapter = chapterToUse,
                    currentChapterIndex = index,
                    currentPosition = 0,
                    showChapterList = false
                )
            }
            
            calculateReadingPrediction()
            preloadAdjacentChapters(index, state.chapters.size)
        }
    }
    
    private fun preloadAdjacentChapters(currentIndex: Int, totalChapters: Int) {
        viewModelScope.launch {
            bookLoader.preloadChapters(
                bookId = bookId,
                currentIndex = currentIndex,
                totalChapters = totalChapters,
                loadContent = { bid, idx -> chapterRepository.getChapterContent(bid, idx) }
            )
        }
    }

    fun updatePosition(position: Int) {
        val state = _uiState.value
        val now = System.currentTimeMillis()
        
        if (lastPositionUpdateTime > 0) {
            val timeDelta = now - lastPositionUpdateTime
            val positionDelta = position - lastPosition
            
            if (timeDelta > 0 && positionDelta > 0) {
                val charsPerSecond = positionDelta.toFloat() / (timeDelta / 1000f)
                val charsPerMinute = charsPerSecond * 60
                
                val currentSpeed = state.readingSpeed
                val alpha = 0.3f
                val newSpeed = if (currentSpeed > 0) {
                    alpha * charsPerMinute + (1 - alpha) * currentSpeed
                } else {
                    charsPerMinute
                }
                
                _uiState.update { it.copy(readingSpeed = newSpeed) }
            }
        }
        
        lastPositionUpdateTime = now
        lastPosition = position
        
        sessionCharactersRead += position
        
        val progress = if (state.chapters.isNotEmpty()) {
            val currentChapter = state.chapters.getOrNull(state.currentChapterIndex)
            val contentLength = currentChapter?.content?.length?.coerceAtLeast(1) ?: 1
            (state.currentChapterIndex.toFloat() + position.toFloat() / contentLength) / state.chapters.size
        } else 0f

        _uiState.update { it.copy(currentPosition = position) }
        debouncedSaveProgress(state.currentChapterIndex, position, progress)
        
        calculateReadingPrediction()
    }

    private fun calculateReadingPrediction() {
        predictionJob?.cancel()
        predictionJob = viewModelScope.launch {
            val state = _uiState.value
            
            var remainingChars = 0
            for (i in state.currentChapterIndex until state.chapters.size) {
                val chapter = state.chapters.getOrNull(i) ?: continue
                if (i == state.currentChapterIndex) {
                    remainingChars += (chapter.content.length - state.currentPosition).coerceAtLeast(0)
                } else {
                    remainingChars += chapter.content.length
                }
            }
            
            val speed = state.readingSpeed.coerceAtLeast(100f)
            val estimatedMinutes = (remainingChars / speed).roundToInt()
            val sessionTime = (System.currentTimeMillis() - sessionStartTime) / 1000 / 60
            val suggestedBreakMinutes = if (sessionTime >= 45) 15 else if (sessionTime >= 30) 10 else 0
            
            _uiState.update {
                it.copy(
                    estimatedTimeRemaining = estimatedMinutes.toLong(),
                    sessionReadTime = sessionTime,
                    suggestedBreakTime = suggestedBreakMinutes.toLong()
                )
            }
        }
    }

    private fun startEyeProtectionTimer() {
        eyeProtectionJob?.cancel()
        eyeProtectionJob = viewModelScope.launch {
            while (true) {
                delay(eyeProtectionIntervalMs)
                _uiState.update { it.copy(showEyeProtectionReminder = true) }
            }
        }
    }

    fun dismissEyeProtectionReminder() {
        _uiState.update { it.copy(showEyeProtectionReminder = false) }
        startEyeProtectionTimer()
    }

    fun goToBookmark(bookmark: Bookmark) {
        goToChapter(bookmark.chapterIndex)
    }

    fun addBookmark(text: String) {
        viewModelScope.launch {
            val state = _uiState.value
            val bookmark = Bookmark(
                bookId = bookId,
                chapterIndex = state.currentChapterIndex,
                text = text,
                position = state.currentPosition
            )
            bookmarkRepository.insertBookmark(bookmark)
            _uiState.update { it.copy(bookmarks = it.bookmarks + bookmark) }
        }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(bookmark)
            _uiState.update { it.copy(bookmarks = it.bookmarks.filter { it.id != bookmark.id }) }
        }
    }

    fun addAnnotation(text: String, start: Int, end: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val annotation = Annotation(
                bookId = bookId,
                chapterIndex = state.currentChapterIndex,
                selectedText = text,
                startPosition = start,
                endPosition = end,
                color = AnnotationColor.YELLOW,
                note = ""
            )
            annotationRepository.insertAnnotation(annotation)
            _uiState.update { it.copy(annotations = it.annotations + annotation) }
        }
    }

    fun deleteAnnotation(annotation: Annotation) {
        viewModelScope.launch {
            annotationRepository.deleteAnnotation(annotation)
            _uiState.update { it.copy(annotations = it.annotations.filter { it.id != annotation.id }) }
        }
    }

    fun updateAnnotationNote(id: Long, note: String) {
        viewModelScope.launch {
            val annotation = _uiState.value.annotations.find { it.id == id }
            annotation?.let {
                annotationRepository.updateAnnotation(it.copy(note = note))
                _uiState.update { state ->
                    state.copy(
                        annotations = state.annotations.map { a ->
                            if (a.id == id) a.copy(note = note) else a
                        }
                    )
                }
            }
        }
    }

    fun showChapterList(show: Boolean) {
        _uiState.update { it.copy(showChapterList = show) }
    }

    fun showSettings(show: Boolean) {
        _uiState.update { it.copy(showSettings = show) }
    }

    fun showBookmarks(show: Boolean) {
        _uiState.update { it.copy(showBookmarks = show) }
    }

    fun showAnnotations(show: Boolean) {
        _uiState.update { it.copy(showAnnotations = show) }
    }

    fun toggleControls() {
        _uiState.update { it.copy(showControls = !it.showControls) }
    }

    fun updateFontSize(size: Int) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            val newSettings = currentSettings.copy(fontSize = size)
            settingsRepository.updateReadingSettings(newSettings)
            _uiState.update { it.copy(readingSettings = newSettings) }
        }
    }

    fun updateLineSpacing(spacing: Float) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            val newSettings = currentSettings.copy(lineSpacing = spacing)
            settingsRepository.updateReadingSettings(newSettings)
            _uiState.update { it.copy(readingSettings = newSettings) }
        }
    }

    fun updateReaderTheme(theme: ReaderTheme) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            val newSettings = currentSettings.copy(theme = theme)
            settingsRepository.updateReadingSettings(newSettings)
            _uiState.update { it.copy(readingSettings = newSettings) }
        }
    }

    fun updatePageMode(mode: PageMode) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            val newSettings = currentSettings.copy(pageMode = mode)
            settingsRepository.updateReadingSettings(newSettings)
            _uiState.update { it.copy(readingSettings = newSettings) }
        }
    }

    fun shareProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        val chapter = state.currentChapter ?: return
        
        val progress = if (state.chapters.isNotEmpty()) {
            ((state.currentChapterIndex + 1).toFloat() / state.chapters.size * 100).roundToInt()
        } else 0

        val shareText = "📚 正在阅读《${book.title}》\n" +
                "第 ${state.currentChapterIndex + 1} 章：${chapter.title}\n" +
                "进度：$progress%\n\n" +
                "#心流阅读 #FlowReader"

        _uiState.update { it.copy(shareText = shareText) }
    }

    fun clearShareText() {
        _uiState.update { it.copy(shareText = null) }
    }

    fun playTts() {
        val currentChapter = _uiState.value.currentChapter ?: return
        ttsManager.speak(currentChapter.content)
    }

    fun stopTts() {
        ttsManager.stop()
    }
}
