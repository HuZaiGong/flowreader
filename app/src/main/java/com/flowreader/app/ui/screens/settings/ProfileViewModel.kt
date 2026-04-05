package com.flowreader.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.SyncBookmark
import com.flowreader.app.domain.model.SyncProgress
import com.flowreader.app.domain.model.SyncSettings
import com.flowreader.app.domain.model.User
import com.flowreader.app.domain.repository.AuthRepository
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.BookmarkRepository
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val syncEnabled: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: String? = null,
    val isLoggedOut: Boolean = false,
    val showDeleteDialog: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val syncRepository: SyncRepository,
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _navigateToAuth = MutableSharedFlow<Unit>()
    val navigateToAuth: SharedFlow<Unit> = _navigateToAuth.asSharedFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            authRepository.currentUser.collect { user ->
                _uiState.update {
                    it.copy(
                        user = user,
                        syncEnabled = user?.syncEnabled ?: false
                    )
                }
            }
        }
    }

    fun navigateToAuth() {
        viewModelScope.launch {
            _navigateToAuth.emit(Unit)
        }
    }

    fun toggleSync(enabled: Boolean) {
        val user = _uiState.value.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            syncRepository.enableSync(user.id, enabled)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            syncEnabled = enabled
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }

    fun syncNow() {
        val user = _uiState.value.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true) }

            try {
                val books = bookRepository.getAllBooks().first()
                val progressList = mutableListOf<SyncProgress>()

                books.forEach { book ->
                    progressList.add(
                        SyncProgress(
                            bookId = book.id,
                            currentChapter = book.currentChapter,
                            currentPosition = book.currentPosition,
                            readingProgress = book.readingProgress,
                            lastReadTime = book.lastReadTime?.time ?: System.currentTimeMillis()
                        )
                    )
                }

                val allBookmarks = mutableListOf<SyncBookmark>()
                books.forEach { book ->
                    val bookmarks = bookmarkRepository.getBookmarksListByBookId(book.id).first()
                    bookmarks.forEach { bookmark ->
                        allBookmarks.add(
                            SyncBookmark(
                                bookId = bookmark.bookId,
                                chapterIndex = bookmark.chapterIndex,
                                position = bookmark.position,
                                text = bookmark.text,
                                createdTime = bookmark.createdTime.time
                            )
                        )
                    }
                }

                val settings = SyncSettings()

                syncRepository.syncAllData(user.id, progressList, allBookmarks, settings)
                    .onSuccess {
                        val now = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            .format(Date())
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                lastSyncTime = now
                            )
                        }
                    }
                    .onFailure { e ->
                        _uiState.update {
                            it.copy(
                                isSyncing = false,
                                error = e.message
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update {
                ProfileUiState(isLoggedOut = true)
            }
        }
    }

    fun showDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun hideDeleteConfirmation() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteDialog = false) }

            authRepository.deleteAccount()
                .onSuccess {
                    _uiState.update {
                        ProfileUiState(isLoggedOut = true)
                    }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message
                        )
                    }
                }
        }
    }
}
