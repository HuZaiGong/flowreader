package com.flowreader.app.ui.screens.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Bookmark
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.domain.repository.AnnotationRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.ReadingStatsRepository
import com.flowreader.app.domain.repository.SettingsRepository
import com.flowreader.feature.reader.ReaderPositionUnit
import com.flowreader.feature.reader.ReaderProgressEngine
import com.flowreader.feature.reader.ReaderSessionTracker
import com.flowreader.app.util.CacheManager
import com.flowreader.app.util.FullTextSearch
import com.flowreader.app.util.FtsSearchResult
import com.flowreader.app.util.TtsManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs
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
    val showSearch: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FtsSearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val todayReadTime: Long = 0,
    val todayReadPages: Int = 0,
    val shareText: String? = null,
    val estimatedTimeRemaining: Long = 0,
    val readingSpeed: Float = 0f,
    val sessionReadTime: Long = 0,
    val showEyeProtectionReminder: Boolean = false,
    val dailyGoalProgress: Float = 0f,
    val suggestedBreakTime: Long = 0,
    val isTtsPlaying: Boolean = false,
    val isImmersiveMode: Boolean = false,
    val scrollRequestVersion: Long = 0L
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
    private val bookLoader: com.flowreader.app.util.BookLoader,
    private val fullTextSearch: FullTextSearch,
    private val ttsManager: TtsManager,
    private val backgroundImporter: com.flowreader.app.util.ReaderBackgroundImporter
) : ViewModel() {

    private val progressEngine = ReaderProgressEngine()
    private val sessionTracker = ReaderSessionTracker()
    private val ttsCoordinator = ReaderTtsCoordinator(ttsManager)

    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: 0L
    private val initialChapterIndex: Int = savedStateHandle.get<Int>("chapterIndex") ?: -1

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    private var progressSaveJob: Job? = null
    private var statsUpdateJob: Job? = null
    private var periodicStatsSaveJob: Job? = null
    private var eyeProtectionJob: Job? = null
    private var predictionJob: Job? = null
    private val progressDebounceMs = 3000L

    private var lastPositionUpdateTime: Long = 0
    private var lastUiPositionUpdateTime: Long = 0
    private var lastPredictionUpdateTime: Long = 0
    private var lastWidgetProgressPercent: Int = -1
    private var chapterFraction: Float = 0f
    private val chapterPositions = mutableMapOf<Int, Int>()

    /** Latest fraction through the open chapter; the controls layer reads it for the slider. */
    val currentChapterFraction: Float
        get() = chapterFraction

    private val positionUpdateIntervalMs = 250L
    private val predictionUpdateIntervalMs = 1_500L

    init {
        loadBook()
        loadSettings()
        loadTodayStats()
        observeTtsState()
        startEyeProtectionTimer()
        startPeriodicStatsSave()
    }

    private fun observeTtsState() {
        viewModelScope.launch {
            ttsManager.state.collect { ttsState ->
                _uiState.update { it.copy(isTtsPlaying = ttsState.isSpeaking) }
            }
        }
    }

    private fun startPeriodicStatsSave() {
        periodicStatsSaveJob?.cancel()
        periodicStatsSaveJob = viewModelScope.launch {
            while (true) {
                delay(30_000L)
                saveReadingStats()
            }
        }
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
        periodicStatsSaveJob?.cancel()
        ttsCoordinator.shutdown()
        // viewModelScope is already cancelled here, so a plain launch would never run and the
        // final progress/stats would be lost on exit. Use an independent scope for the flush.
        val flushScope = CoroutineScope(Dispatchers.IO)
        val state = _uiState.value
        if (state.chapters.isNotEmpty()) {
            val position = chapterPositions[state.currentChapterIndex] ?: state.currentPosition
            val progress = progressEngine.fraction(state.currentChapterIndex, chapterFraction, state.chapters.size)
            flushScope.launch {
                bookRepository.updateReadingProgress(bookId, state.currentChapterIndex, position, progress)
                val (pages, seconds) = sessionTracker.takeSnapshotAndReset()
                if (seconds > 0 && pages > 0) {
                    readingStatsRepository.updateTodayStats(bookId, pages, seconds)
                }
            }
        }
    }

    private fun saveReadingStats() {
        val (pages, seconds) = sessionTracker.takeSnapshotAndReset()
        if (seconds > 0 && pages > 0) {
            viewModelScope.launch {
                try {
                    readingStatsRepository.updateTodayStats(
                        bookId = bookId,
                        readPages = pages,
                        readTimeSeconds = seconds
                    )
                    loadTodayStats()
                } catch (e: Exception) {
                    android.util.Log.e("ReaderViewModel", "Failed to save reading stats", e)
                }
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
                .collect { settings ->
                    _uiState.update {
                        it.copy(readingSettings = settings.defaultReadingSettings)
                    }
                    startEyeProtectionTimer()
                }
        }
    }

    fun retryLoadBook() {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                if (bookId <= 0L) {
                    _uiState.update { it.copy(isLoading = false, error = "无效的书籍 ID") }
                    return@launch
                }

                val book = bookRepository.getBookById(bookId)
                val chapterMetadata = chapterRepository.getChapterMetadataList(bookId)
                val bookmarks = bookmarkRepository.getBookmarksListByBookId(bookId)
                val annotations = annotationRepository.getAnnotationsListByBookId(bookId)

                if (book != null && chapterMetadata.isNotEmpty()) {
                    val chapters = if (book.format == BookFormat.COMIC) {
                        chapterMetadata.map { meta ->
                            meta.copy(content = chapterRepository.getChapterContent(bookId, meta.index) ?: "")
                        }
                    } else {
                        chapterMetadata
                    }
                    val currentChapterIndex = book.currentChapter.coerceIn(0, chapterMetadata.size - 1)
                    val currentChapter = chapters.getOrNull(currentChapterIndex)?.let { meta ->
                        if (meta.content.isNotEmpty()) meta else meta.copy(content = chapterRepository.getChapterContent(bookId, currentChapterIndex) ?: "")
                    }

                    sessionTracker.startSession()
                    lastPositionUpdateTime = System.currentTimeMillis()

                    _uiState.update {
                        it.copy(
                            book = book,
                            chapters = chapters,
                            currentChapter = currentChapter,
                            currentChapterIndex = currentChapterIndex,
                            currentPosition = book.currentPosition,
                            bookmarks = bookmarks,
                            annotations = annotations,
                            isLoading = false,
                            scrollRequestVersion = it.scrollRequestVersion + 1
                        )
                    }

                    calculateReadingPrediction()
                    if (book.format != BookFormat.COMIC) indexBookForSearch(book, chapterMetadata)

                    if (initialChapterIndex >= 0 && initialChapterIndex < chapterMetadata.size) {
                        goToChapter(initialChapterIndex)
                    }
                } else {
                    val errorMsg = if (book == null) "未找到书籍" else "书籍暂无章节内容"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "加载书籍失败: ${e.localizedMessage ?: "未知错误"}") }
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
        goToChapter(index, null)
    }

    fun setCurrentComicPage(index: Int) {
        val state = _uiState.value
        if (state.book?.format != BookFormat.COMIC || index !in state.chapters.indices || index == state.currentChapterIndex) return
        val progress = progressEngine.fraction(index, 0f, state.chapters.size)
        chapterFraction = 0f
        _uiState.update {
            it.copy(
                currentChapterIndex = index,
                currentChapter = state.chapters[index],
                currentPosition = 0
            )
        }
        debouncedSaveProgress(index, 0, progress)
        updateWidgetSnapshot(progress)
    }

    private fun goToChapter(index: Int, positionOverride: Int?) {
        if (index !in _uiState.value.chapters.indices) return

        val previousState = _uiState.value
        // `chapterPositions` is written on every updatePosition() call, before the UI throttle, so
        // it is always at least as fresh as uiState.currentPosition (which lags up to 250ms).
        // Only seed it when the chapter was never scrolled, or leaving rewinds the saved spot.
        chapterPositions.getOrPut(previousState.currentChapterIndex) { previousState.currentPosition }
        ttsCoordinator.stop()
        saveReadingStats()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val state = _uiState.value
            val existingChapter = state.chapters[index]
            val content = if (existingChapter.content.isNotEmpty()) {
                existingChapter.content
            } else {
                chapterRepository.getChapterContent(bookId, index) ?: ""
            }
            val chapter = existingChapter.copy(content = content)
            val updatedChapters = state.chapters.toMutableList().apply {
                if (index < size) set(index, chapter)
            }

            val restoredPosition = (positionOverride ?: chapterPositions[index] ?: if (index == state.book?.currentChapter) state.book.currentPosition else 0)
                .coerceAtLeast(0)
            chapterFraction = 0f
            val progress = progressEngine.fraction(index, 0f, state.chapters.size)

            bookRepository.updateReadingProgress(bookId, index, restoredPosition, progress)

            _uiState.update {
                it.copy(
                    chapters = updatedChapters,
                    currentChapter = chapter,
                    currentChapterIndex = index,
                    currentPosition = restoredPosition,
                    isLoading = false,
                    showChapterList = false,
                    scrollRequestVersion = it.scrollRequestVersion + 1
                )
            }

            calculateReadingPrediction()
            preloadAdjacentChapters(index, _uiState.value.chapters.size)
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

    /**
     * True when [updatePosition] is fed a rendered page index instead of a character offset. The
     * two units need different reading-stats math — see [ReaderPositionUnit].
     */
    private val positionIsPageIndex: Boolean
        get() {
            val state = _uiState.value
            return ReaderPositionUnit.of(state.readingSettings.pageMode, state.book?.format).isPageIndex
        }

    fun updatePosition(position: Int, chapterScrollFraction: Float = chapterFraction) {
        val state = _uiState.value
        if (position < 0) return
        chapterFraction = if (chapterScrollFraction.isNaN()) 0f else chapterScrollFraction.coerceIn(0f, 1f)
        val now = System.currentTimeMillis()
        chapterPositions[state.currentChapterIndex] = position
        val pageUnits = positionIsPageIndex

        // A page index moves by 1 per turn, so the 200-character scroll threshold can never fire;
        // any page change is significant on its own.
        val moveThreshold = if (pageUnits) 1 else 200
        val movedEnough = abs(position - sessionTracker.lastPosition) >= moveThreshold
        val waitedEnough = now - lastPositionUpdateTime >= positionUpdateIntervalMs
        if (lastPositionUpdateTime > 0 && !movedEnough && !waitedEnough) return

        if (sessionTracker.recordInteraction(position)) {
            saveReadingStats()
        }
        val charsPerPage = progressEngine.estimateCharsPerPage(
            state.readingSettings.fontSize,
            state.readingSettings.lineSpacing
        )
        if (pageUnits) {
            sessionTracker.recordPageProgress(pageIndex = position, charsPerPage = charsPerPage)
        } else {
            sessionTracker.recordProgress(
                position = position,
                content = state.currentChapter?.content,
                charsPerPage = charsPerPage
            )
        }
        _uiState.update { it.copy(readingSpeed = sessionTracker.readingSpeed) }

        lastPositionUpdateTime = now

        val progress = progressEngine.fraction(state.currentChapterIndex, chapterFraction, state.chapters.size)

        if (now - lastUiPositionUpdateTime >= positionUpdateIntervalMs) {
            lastUiPositionUpdateTime = now
            _uiState.update { it.copy(currentPosition = position) }
        }
        debouncedSaveProgress(state.currentChapterIndex, position, progress)
        updateWidgetSnapshot(progress)

        if (now - lastPredictionUpdateTime >= predictionUpdateIntervalMs) {
            lastPredictionUpdateTime = now
            calculateReadingPrediction()
        }
    }

    private fun updateWidgetSnapshot(progress: Float) {
        val title = _uiState.value.book?.title ?: return
        val percent = (progress * 100).roundToInt().coerceIn(0, 100)
        if (percent == lastWidgetProgressPercent) return
        lastWidgetProgressPercent = percent
        viewModelScope.launch {
            settingsRepository.updateReadingWidgetSnapshot(title, percent)
        }
    }

    private fun calculateReadingPrediction() {
        predictionJob?.cancel()
        predictionJob = viewModelScope.launch {
            val state = _uiState.value

            val estimatedMinutes = progressEngine.remainingMinutes(
                chapters = state.chapters,
                currentIndex = state.currentChapterIndex,
                currentPosition = state.currentPosition,
                speed = sessionTracker.readingSpeed
            )
            val sessionTime = sessionTracker.elapsedSeconds / 60

            _uiState.update {
                it.copy(
                    estimatedTimeRemaining = estimatedMinutes.toLong(),
                    sessionReadTime = sessionTime,
                    suggestedBreakTime = progressEngine.suggestedBreakMinutes(sessionTime)
                )
            }
        }
    }

    private fun startEyeProtectionTimer() {
        eyeProtectionJob?.cancel()
        eyeProtectionJob = viewModelScope.launch {
            while (true) {
                val intervalMinutes = _uiState.value.readingSettings.eyeProtectionIntervalMinutes.coerceAtLeast(15)
                delay(intervalMinutes * 60 * 1000L)
                _uiState.update { it.copy(showEyeProtectionReminder = true) }
            }
        }
    }

    private fun indexBookForSearch(book: Book, chapters: List<Chapter>) {
        viewModelScope.launch {
            try {
                fullTextSearch.initialize()
                // Delete-then-reindex must be atomic against SearchRepositoryImpl's global
                // rebuild, or this book can be wiped from the index after the rebuild already
                // recorded it as indexed — global search would then silently miss it.
                fullTextSearch.withIndexLock {
                    fullTextSearch.deleteBookContent(bookId)
                    chapters.forEachIndexed { index, chapter ->
                        chapterRepository.getChapterContent(bookId, index)?.let { content ->
                            fullTextSearch.indexChapter(bookId, index, chapter.title, content)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to index book for search", e)
            }
        }
    }

    fun showSearch(show: Boolean) {
        _uiState.update { it.copy(showSearch = show, searchQuery = "", searchResults = emptyList(), hasSearched = false) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun searchInBook() {
        val query = _uiState.value.searchQuery
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, hasSearched = true) }
            try {
                val results = fullTextSearch.search(bookId, query)
                settingsRepository.addSearchHistory(query)
                _uiState.update { it.copy(searchResults = results, isSearching = false) }
            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "FTS search failed", e)
                _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
            }
        }
    }

    fun goToSearchResult(chapterIndex: Int) {
        goToChapter(chapterIndex)
        _uiState.update { it.copy(showSearch = false) }
    }

    fun dismissEyeProtectionReminder() {
        _uiState.update { it.copy(showEyeProtectionReminder = false) }
        startEyeProtectionTimer()
    }

    fun goToBookmark(bookmark: Bookmark) {
        if (bookmark.bookId != bookId) return
        goToChapter(bookmark.chapterIndex, bookmark.position)
        _uiState.update { it.copy(showBookmarks = false) }
    }

    /**
     * A bookmark's [Bookmark.position] is always the reader's own position, because that is the
     * only unit [goToBookmark] can restore: `goToChapter(index, position)` writes it back into
     * `currentPosition`, which `ReaderScreen` feeds to `ScrollState.scrollTo()` (scroll pixels in
     * SLIDE/NONE) or `PagedReader` uses as a page index. A selection's raw character offset is
     * none of those and nothing can convert it back without the layout, so bookmarks made from
     * selected text deliberately keep only the [text] — passing the character offset through used
     * to scroll to an arbitrary pixel, or in PAGED mode get clamped to the last page of the
     * chapter. Highlights are unaffected: `Annotation` positions really are character offsets.
     */
    fun addBookmark(text: String) {
        viewModelScope.launch {
            val state = _uiState.value
            if (bookId <= 0L || state.currentChapterIndex !in state.chapters.indices) return@launch
            val bookmark = Bookmark(
                bookId = bookId,
                chapterIndex = state.currentChapterIndex,
                text = text,
                position = state.currentPosition.coerceAtLeast(0)
            )
            try {
                val savedBookmark = bookmarkRepository.addBookmark(bookmark)
                _uiState.update { it.copy(bookmarks = (it.bookmarks + savedBookmark).sortedWith(bookmarkComparator)) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "添加书签失败: ${e.localizedMessage ?: "未知错误"}") }
            }
        }
    }

    fun toggleTts() {
        val state = _uiState.value
        if (state.isTtsPlaying) {
            ttsCoordinator.pause()
            _uiState.update { it.copy(isTtsPlaying = false) }
            return
        }

        val chapter = state.currentChapter ?: return
        // PAGED / comic positions are page indices, so speaking "from position" would start a few
        // characters in; only a character offset is a meaningful start point.
        val start = if (positionIsPageIndex) 0 else state.currentPosition
        ttsCoordinator.speakFrom(chapter.content, start)
    }

    fun stopTts() {
        ttsCoordinator.stop()
        _uiState.update { it.copy(isTtsPlaying = false) }
    }

    fun toggleImmersiveMode() {
        _uiState.update { it.copy(isImmersiveMode = !it.isImmersiveMode) }
    }

    fun deleteBookmark(bookmark: Bookmark) {
        viewModelScope.launch {
            if (bookmark.id <= 0L || bookmark.bookId != bookId) return@launch
            try {
                bookmarkRepository.deleteBookmarkById(bookmark.id)
                _uiState.update { it.copy(bookmarks = it.bookmarks.filter { it.id != bookmark.id }) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "删除书签失败: ${e.localizedMessage ?: "未知错误"}") }
            }
        }
    }

    fun addAnnotation(text: String, start: Int, end: Int, color: AnnotationColor = AnnotationColor.YELLOW) {
        viewModelScope.launch {
            val state = _uiState.value
            val annotation = Annotation(
                bookId = bookId,
                chapterIndex = state.currentChapterIndex,
                selectedText = text,
                startPosition = start,
                endPosition = end,
                color = color,
                note = ""
            )
            val newId = annotationRepository.insertAnnotation(annotation)
            _uiState.update { it.copy(annotations = it.annotations + annotation.copy(id = newId)) }
        }
    }

    /** PDF region highlight (v55): the region is packed into start/end positions. */
    fun addPdfAnnotation(page: Int, x0: Int, y0: Int, x1: Int, y1: Int) {
        viewModelScope.launch {
            val state = _uiState.value
            val packedStart = com.flowreader.app.ui.screens.reader.components.PdfRegionCodec.pack(x0, y0)
            val packedEnd = com.flowreader.app.ui.screens.reader.components.PdfRegionCodec.pack(x1, y1)
            val annotation = Annotation(
                bookId = bookId,
                chapterIndex = page,
                startPosition = packedStart,
                endPosition = packedEnd,
                selectedText = "PDF 标注",
                color = AnnotationColor.YELLOW,
                note = ""
            )
            val newId = annotationRepository.insertAnnotation(annotation)
            _uiState.update { it.copy(annotations = it.annotations + annotation.copy(id = newId)) }
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

    /**
     * Single entry point for every reader preference. The v51 ViewModel carried six near-identical
     * mutators; the settings sheet now hands back a whole [ReadingSettings].
     */
    fun updateReadingSettings(settings: ReadingSettings) {
        val previousInterval = _uiState.value.readingSettings.eyeProtectionIntervalMinutes
        _uiState.update { it.copy(readingSettings = settings) }
        viewModelScope.launch {
            settingsRepository.updateReadingSettings(settings)
        }
        if (settings.eyeProtectionIntervalMinutes != previousInterval) {
            startEyeProtectionTimer()
        }
    }

    /**
     * Imports a picked image as the reader background. Decoding and re-encoding happen off the main
     * thread; the resulting path is applied through [updateReadingSettings] so the background is
     * persisted by the same single entry point as every other reader preference.
     */
    fun onBackgroundImageSelected(uri: android.net.Uri) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { backgroundImporter.import(uri) }
            result.fold(
                onSuccess = { path ->
                    // The importer prunes older files, so the previous background is already gone.
                    updateReadingSettings(_uiState.value.readingSettings.copy(backgroundImagePath = path))
                },
                onFailure = { error ->
                    // Keep the existing background on failure rather than clearing to a blank state.
                    _uiState.update { it.copy(error = "导入背景图失败: ${error.message}") }
                }
            )
        }
    }

    /** Removes the reader background image and deletes the stored file. */
    fun clearBackgroundImage() {
        val current = _uiState.value.readingSettings
        val path = current.backgroundImagePath
        updateReadingSettings(current.copy(backgroundImagePath = null))
        viewModelScope.launch(Dispatchers.IO) { backgroundImporter.clear(path) }
    }

    /** Jumps to whichever chapter the reader progress slider was released over. */
    fun goToProgress(fraction: Float) {
        val state = _uiState.value
        if (state.chapters.isEmpty()) return
        goToChapter(progressEngine.chapterAt(fraction, state.chapters.size))
    }

    fun shareProgress() {
        val state = _uiState.value
        val book = state.book ?: return
        val chapter = state.currentChapter ?: return

        val progress = (progressEngine.fraction(state.currentChapterIndex, chapterFraction, state.chapters.size) * 100).roundToInt()

        val shareText = "📚 正在阅读《${book.title}》\n" +
                "第 ${state.currentChapterIndex + 1} 章：${chapter.title}\n" +
                "进度：$progress%\n\n" +
                "#心流阅读 #FlowReader"

        _uiState.update { it.copy(shareText = shareText) }
    }

    fun clearShareText() {
        _uiState.update { it.copy(shareText = null) }
    }

    private companion object {
        val bookmarkComparator = compareBy<Bookmark> { it.chapterIndex }
            .thenBy { it.position }
            .thenByDescending { it.createdTime.time }
    }

}
