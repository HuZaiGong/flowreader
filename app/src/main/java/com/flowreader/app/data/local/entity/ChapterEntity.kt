package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.Chapter

@Entity(
    tableName = "chapters",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "index"], unique = true)
    ]
)
data class ChapterEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val index: Int,
    val title: String,
    val content: String = "",
    val startPosition: Int = 0,
    val endPosition: Int = 0
) {
    fun toDomain(): Chapter = Chapter(
        id = id,
        bookId = bookId,
        index = index,
        title = title,
        content = content,
        startPosition = startPosition,
        endPosition = endPosition
    )

    companion object {
        fun fromDomain(chapter: Chapter): ChapterEntity = ChapterEntity(
            id = chapter.id,
            bookId = chapter.bookId,
            index = chapter.index,
            title = chapter.title,
            content = chapter.content,
            startPosition = chapter.startPosition,
            endPosition = chapter.endPosition
        )
    }
}

data class ChapterMetadata(
    val id: Long,
    val bookId: Long,
    val index: Int,
    val title: String,
    val startPosition: Int,
    val endPosition: Int
)