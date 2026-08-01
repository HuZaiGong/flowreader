package com.flowreader.app.ui.screens.settings

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flowreader.app.domain.model.AppLanguage
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import com.flowreader.app.domain.model.GestureSettings
import com.flowreader.app.domain.model.ReadingSettings
import com.flowreader.app.domain.repository.BookRepository
import com.flowreader.app.domain.repository.CategoryRepository
import com.flowreader.app.domain.repository.ReadingListRepository
import com.flowreader.app.domain.repository.SettingsRepository
import com.flowreader.app.domain.repository.BackupRepository
import com.flowreader.app.core.util.ShelfExporter
import com.flowreader.app.util.LanTransferClient
import com.flowreader.app.util.LanTransferServer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shelf export target format (v55): CSV table or JSON dump. */
enum class ShelfExportFormat { CSV, JSON }

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val colorSource: ColorSource = ColorSource.BRAND,
    val language: AppLanguage = AppLanguage.FOLLOW_SYSTEM,
    val readingSettings: ReadingSettings = ReadingSettings(),
    val isLoading: Boolean = true,
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0,
    val dailyReadingGoal: Int = 30,
    val exportResult: String? = null,
    val importResult: String? = null,
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isOnboardingCompleted: Boolean = false,
    val customFontPath: String? = null,
    val shelfExportFormat: ShelfExportFormat? = null,
    val lanServerUrl: String? = null,
    val lanTransferMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val bookRepository: BookRepository,
    private val categoryRepository: CategoryRepository,
    private val readingListRepository: ReadingListRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            combine(
                settingsRepository.appSettings,
                settingsRepository.getDailyReadingGoal(),
                settingsRepository.isOnboardingCompleted(),
                settingsRepository.getCustomFontPath()
            ) { settings, goal, onboardingCompleted, fontPath ->
                SettingsUiState(
                    themeMode = settings.themeMode,
                    colorSource = settings.colorSource,
                    language = settings.language,
                    readingSettings = settings.defaultReadingSettings,
                    isLoading = false,
                    readingReminderEnabled = settings.readingReminderEnabled,
                    readingReminderHour = settings.readingReminderHour,
                    readingReminderMinute = settings.readingReminderMinute,
                    dailyReadingGoal = goal,
                    isOnboardingCompleted = onboardingCompleted,
                    customFontPath = fontPath
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun checkOnboardingStatus() {
        viewModelScope.launch {
            settingsRepository.isOnboardingCompleted().collect { completed ->
                _uiState.update { it.copy(isOnboardingCompleted = completed) }
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted()
        }
    }

    fun exportData() {
        _uiState.update { it.copy(isExporting = true, exportResult = null) }
    }

    fun onExportReady(uri: Uri) {
        viewModelScope.launch {
            backupRepository.exportData(uri)
                .onSuccess {
                    _uiState.update { it.copy(isExporting = false, exportResult = "备份成功") }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isExporting = false, exportResult = "备份失败: ${e.message}") }
                }
        }
    }

    fun importData() {
        _uiState.update { it.copy(isImporting = true, importResult = null) }
    }

    private var lanServer: LanTransferServer? = null

    fun startLanServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(lanTransferMessage = null) }
            val backupFile = java.io.File(context.cacheDir, "lan_backup_${System.currentTimeMillis()}.json")
            backupRepository.exportDataToFile(backupFile)
                .onSuccess {
                    val server = LanTransferServer(backupFile)
                    val url = server.start()
                    if (url != null) {
                        lanServer?.stop()
                        lanServer = server
                        _uiState.update { it.copy(lanServerUrl = url, lanTransferMessage = "服务已启动，同一 WiFi 下的设备可接收") }
                    } else {
                        _uiState.update { it.copy(lanTransferMessage = "启动失败：无法绑定端口或获取局域网地址") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(lanTransferMessage = "生成备份失败: ${e.message}") }
                }
        }
    }

    fun stopLanServer() {
        lanServer?.stop()
        lanServer = null
        _uiState.update { it.copy(lanServerUrl = null, lanTransferMessage = "服务已停止") }
    }

    fun importFromLanUrl(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(lanTransferMessage = "请输入接收链接") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(lanTransferMessage = "正在接收…") }
            val target = java.io.File(context.cacheDir, "lan_import_${System.currentTimeMillis()}.json")
            LanTransferClient.download(trimmed, target)
                .onSuccess {
                    backupRepository.importDataFromFile(target)
                        .onSuccess { result ->
                            _uiState.update {
                                it.copy(
                                    lanTransferMessage = "接收并导入成功：${result.booksImported} 本书，${result.bookmarksImported} 个书签"
                                )
                            }
                        }
                        .onFailure { e ->
                            _uiState.update { it.copy(lanTransferMessage = "导入失败: ${e.message}") }
                        }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(lanTransferMessage = "接收失败: ${e.message}") }
                }
            target.delete()
        }
    }

    fun clearLanTransferMessage() {
        _uiState.update { it.copy(lanTransferMessage = null) }
    }

    fun requestShelfExport(format: ShelfExportFormat) {
        _uiState.update { it.copy(shelfExportFormat = format, isExporting = true, exportResult = null) }
    }

    fun onShelfExportReady(uri: Uri) {
        val format = _uiState.value.shelfExportFormat ?: return
        viewModelScope.launch {
            runCatching {
                val books = bookRepository.getAllBooks().first()
                val categories = categoryRepository.getAllCategories().first()
                val lists = readingListRepository.getAllLists().first()
                val listsByBook = buildMap<Long, String> {
                    lists.forEach { list ->
                        readingListRepository.getBooksInList(list.id).first().forEach { entry ->
                            put(entry.book.id, list.name)
                        }
                    }
                }
                val content = when (format) {
                    ShelfExportFormat.CSV -> ShelfExporter.toCsv(books, categories, lists, listsByBook)
                    ShelfExportFormat.JSON -> ShelfExporter.toJson(books, categories, lists, listsByBook)
                }
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(content.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                _uiState.update { it.copy(isExporting = false, shelfExportFormat = null, exportResult = "书架信息已导出") }
            }.onFailure { e ->
                _uiState.update {
                    it.copy(isExporting = false, shelfExportFormat = null, exportResult = "导出失败: ${e.message}")
                }
            }
        }
    }

    fun onImportReady(uri: Uri) {
        viewModelScope.launch {
            backupRepository.importData(uri)
                .onSuccess { result ->
                    _uiState.update {
                        it.copy(
                            isImporting = false,
                            importResult = "导入成功: ${result.booksImported}本书, ${result.bookmarksImported}个书签"
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isImporting = false, importResult = "导入失败: ${e.message}") }
                }
        }
    }

    fun clearExportResult() {
        _uiState.update { it.copy(exportResult = null) }
    }

    fun clearImportResult() {
        _uiState.update { it.copy(importResult = null) }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(mode)
        }
    }

    fun updateColorSource(source: ColorSource) {
        viewModelScope.launch {
            settingsRepository.updateColorSource(source)
        }
    }

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.updateLanguage(language)
        }
    }

    fun updateKeepScreenOn(keepOn: Boolean) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(keepScreenOn = keepOn))
        }
    }

    fun updateReadingReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0) {
        viewModelScope.launch {
            settingsRepository.updateReadingReminder(enabled, hour, minute)
        }
    }

    fun updateDailyReadingGoal(minutes: Int) {
        viewModelScope.launch {
            settingsRepository.updateDailyReadingGoal(minutes)
        }
    }

    fun updateGestureSettings(gestureSettings: GestureSettings) {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateReadingSettings(currentSettings.copy(gestureSettings = gestureSettings))
        }
    }

    fun onCustomFontSelected(uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: return@launch
                val fontDir = java.io.File(context.filesDir, "fonts")
                if (!fontDir.exists()) fontDir.mkdirs()
                val fileName = "custom_font_${System.currentTimeMillis()}.ttf"
                val targetFile = java.io.File(fontDir, fileName)
                java.io.FileOutputStream(targetFile).use { output ->
                    inputStream.copyTo(output)
                }
                inputStream.close()
                settingsRepository.updateCustomFontPath(targetFile.absolutePath)
                val currentSettings = _uiState.value.readingSettings
                settingsRepository.updateReadingSettings(
                    currentSettings.copy(customFontPath = targetFile.absolutePath)
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(exportResult = "导入字体失败: ${e.message}") }
            }
        }
    }

    fun clearCustomFont() {
        viewModelScope.launch {
            val currentSettings = _uiState.value.readingSettings
            settingsRepository.updateCustomFontPath(null)
            settingsRepository.updateReadingSettings(
                currentSettings.copy(customFontPath = null)
            )
        }
    }
}
