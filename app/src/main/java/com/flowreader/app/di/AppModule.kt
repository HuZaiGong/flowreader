package com.flowreader.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.flowreader.app.data.local.AppDatabase
import com.flowreader.app.data.local.dao.AnnotationDao
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.repository.*
import com.flowreader.app.domain.repository.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE books ADD COLUMN tags TEXT NOT NULL DEFAULT ''")
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE INDEX IF NOT EXISTS index_bookmarks_bookId_chapterIndex_position ON bookmarks(bookId, chapterIndex, position)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addMigrations(MIGRATION_4_5, MIGRATION_5_6).build()
    }

    @Provides
    @Singleton
    fun provideBookDao(database: AppDatabase): BookDao = database.bookDao()

    @Provides
    @Singleton
    fun provideChapterDao(database: AppDatabase): ChapterDao = database.chapterDao()

    @Provides
    @Singleton
    fun provideBookmarkDao(database: AppDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    @Singleton
    fun provideAnnotationDao(database: AppDatabase): AnnotationDao = database.annotationDao()

    @Provides
    @Singleton
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    @Singleton
    fun provideReadingStatsDao(database: AppDatabase): ReadingStatsDao = database.readingStatsDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds
    @Singleton
    abstract fun bindChapterRepository(impl: ChapterRepositoryImpl): ChapterRepository

    @Binds
    @Singleton
    abstract fun bindBookmarkRepository(impl: BookmarkRepositoryImpl): BookmarkRepository

    @Binds
    @Singleton
    abstract fun bindAnnotationRepository(impl: AnnotationRepositoryImpl): AnnotationRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindReadingStatsRepository(impl: ReadingStatsRepositoryImpl): ReadingStatsRepository

    @Binds
    @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
