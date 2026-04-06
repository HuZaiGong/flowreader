package com.flowreader.app.di

import android.content.Context
import androidx.room.Room
import com.flowreader.app.data.local.AppDatabase
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ReadingStatsEntity
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

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).build()
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
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindReadingStatsRepository(impl: ReadingStatsRepositoryImpl): ReadingStatsRepository
}
