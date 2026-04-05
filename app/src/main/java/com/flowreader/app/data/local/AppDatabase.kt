package com.flowreader.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.flowreader.app.data.local.dao.BookDao
import com.flowreader.app.data.local.dao.BookmarkDao
import com.flowreader.app.data.local.dao.CategoryDao
import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.entity.BookEntity
import com.flowreader.app.data.local.entity.BookmarkEntity
import com.flowreader.app.data.local.entity.CategoryEntity
import com.flowreader.app.data.local.entity.ChapterEntity

@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        BookmarkEntity::class,
        CategoryEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun chapterDao(): ChapterDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        const val DATABASE_NAME = "flowreader_db"
    }
}
