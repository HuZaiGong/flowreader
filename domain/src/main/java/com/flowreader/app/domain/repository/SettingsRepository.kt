import com.flowreader.app.domain.model.AppLanguage
import com.flowreader.app.domain.model.AppSettings
import com.flowreader.app.domain.model.AppThemeMode
import com.flowreader.app.domain.model.ColorSource
import com.flowreader.app.domain.model.LibraryViewMode
import com.flowreader.app.domain.model.ReadingSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    companion object {
        const val THEME_KEY = "theme"
    }

    val appSettings: Flow<AppSettings>

    suspend fun updateThemeMode(mode: AppThemeMode)
    suspend fun updateColorSource(source: ColorSource)
    suspend fun updateLanguage(language: AppLanguage)
    suspend fun updateReadingSettings(settings: ReadingSettings)
    suspend fun updateReadingReminder(enabled: Boolean, hour: Int = 20, minute: Int = 0)
    suspend fun addSearchHistory(query: String)
    fun getSearchHistory(): Flow<List<String>>
    fun getLibraryViewMode(): Flow<LibraryViewMode>
    suspend fun setLibraryViewMode(mode: LibraryViewMode)
    suspend fun clearSearchHistory()
    suspend fun updateDailyReadingGoal(minutes: Int)
    fun getDailyReadingGoal(): Flow<Int>
    suspend fun updateWeeklyReadingGoal(minutes: Int)
    fun getWeeklyReadingGoal(): Flow<Int>
    suspend fun updateMonthlyReadingGoal(minutes: Int)
    fun getMonthlyReadingGoal(): Flow<Int>
    suspend fun updateReadingWidgetSnapshot(bookTitle: String, progressPercent: Int)
    fun isOnboardingCompleted(): Flow<Boolean>
    suspend fun setOnboardingCompleted()
    suspend fun updateCustomFontPath(path: String?)
    fun getCustomFontPath(): Flow<String?>
}
