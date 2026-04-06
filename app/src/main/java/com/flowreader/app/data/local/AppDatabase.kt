package com.flowreader.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flowreader.app.data.local.dao.AnnotationDao
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.dao.ReadingStatsDao
import com.flowreader.app.data.local.dao.UserDao
import com.flowreader.app.data.local.entity.AnnotationEntity
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ReadingStatsEntity
import com.flowreader.app.data.local.entity.UserEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        AnnotationEntity::class,
        CategoryEntity::class,
        ReadingStatsEntity::class,
        UserEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun categoryDao(): CategoryDao
    abstract fun readingStatsDao(): ReadingStatsDao
    abstract fun userDao(): UserDao

    companion object {
        const val DATABASE_NAME = "flowreader_db"
    }
}
