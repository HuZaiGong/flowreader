package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.Bookmark
import java.util.Date

@Entity(
    tableName = "bookmarks",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("bookId")]
)
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val text: String,
    val createdTime: Long = System.currentTimeMillis()
) {
    fun toDomain(): Bookmark = Bookmark(
        id = id,
        bookId = bookId,
        chapterIndex = chapterIndex,
        position = position,
        text = text,
        createdTime = Date(createdTime)
    )

    companion object {
        fun fromDomain(bookmark: Bookmark): BookmarkEntity = BookmarkEntity(
            id = bookmark.id,
            bookId = bookmark.bookId,
            chapterIndex = bookmark.chapterIndex,
            position = bookmark.position,
            text = bookmark.text,
            createdTime = bookmark.createdTime.time
        )
    }
}
