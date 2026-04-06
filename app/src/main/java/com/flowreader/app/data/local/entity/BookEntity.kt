package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import java.util.Date

@Entity(tableName = "books", indices = [
    Index(value = ["lastReadTime", "addedTime"]),
    Index(value = ["categoryId"]),
    Index(value = ["title"]),
    Index(value = ["author"])
])
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val author: String,
    val filePath: String,
    val coverPath: String? = null,
    val description: String = "",
    val fileSize: Long = 0,
    val format: String = "EPUB",
    val totalChapters: Int = 0,
    val currentChapter: Int = 0,
    val currentPosition: Int = 0,
    val readingProgress: Float = 0f,
    val lastReadTime: Long? = null,
    val addedTime: Long = System.currentTimeMillis(),
    val categoryId: Long? = null
) {
    fun toDomain(): Book = Book(
        id = id,
        title = title,
        author = author,
        filePath = filePath,
        coverPath = coverPath,
        description = description,
        fileSize = fileSize,
        format = try { BookFormat.valueOf(format) } catch (e: Exception) { BookFormat.UNKNOWN },
        totalChapters = totalChapters,
        currentChapter = currentChapter,
        currentPosition = currentPosition,
        readingProgress = readingProgress,
        lastReadTime = lastReadTime?.let { Date(it) },
        addedTime = Date(addedTime),
        categoryId = categoryId
    )

    companion object {
        fun fromDomain(book: Book): BookEntity = BookEntity(
            id = book.id,
            title = book.title,
            author = book.author,
            filePath = book.filePath,
            coverPath = book.coverPath,
            description = book.description,
            fileSize = book.fileSize,
            format = book.format.name,
            totalChapters = book.totalChapters,
            currentChapter = book.currentChapter,
            currentPosition = book.currentPosition,
            readingProgress = book.readingProgress,
            lastReadTime = book.lastReadTime?.time,
            addedTime = book.addedTime.time,
            categoryId = book.categoryId
        )
    }
}
