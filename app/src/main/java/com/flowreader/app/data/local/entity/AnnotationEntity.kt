package com.flowreader.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.model.AnnotationColor
import com.flowreader.app.domain.model.AnnotationType
import java.util.Date

@Entity(
    tableName = "annotations",
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
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val startPosition: Int,
    val endPosition: Int,
    val selectedText: String,
    val note: String = "",
    val color: String = "YELLOW",
    val type: String = "HIGHLIGHT",
    val createdTime: Long = System.currentTimeMillis(),
    val modifiedTime: Long = System.currentTimeMillis()
) {
    fun toDomain(): Annotation = Annotation(
        id = id,
        bookId = bookId,
        chapterIndex = chapterIndex,
        startPosition = startPosition,
        endPosition = endPosition,
        selectedText = selectedText,
        note = note,
        color = try { AnnotationColor.valueOf(color) } catch (e: Exception) { AnnotationColor.YELLOW },
        type = try { AnnotationType.valueOf(type) } catch (e: Exception) { AnnotationType.HIGHLIGHT },
        createdTime = Date(createdTime),
        modifiedTime = Date(modifiedTime)
    )

    companion object {
        fun fromDomain(annotation: Annotation): AnnotationEntity = AnnotationEntity(
            id = annotation.id,
            bookId = annotation.bookId,
            chapterIndex = annotation.chapterIndex,
            startPosition = annotation.startPosition,
            endPosition = annotation.endPosition,
            selectedText = annotation.selectedText,
            note = annotation.note,
            color = annotation.color.name,
            type = annotation.type.name,
            createdTime = annotation.createdTime.time,
            modifiedTime = annotation.modifiedTime.time
        )
    }
}