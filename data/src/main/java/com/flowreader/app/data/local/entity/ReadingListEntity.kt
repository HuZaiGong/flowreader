package com.flowreader.app.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.flowreader.app.domain.model.ReadingList
import com.flowreader.app.domain.model.ReadingListBook
import java.util.Date

@Entity(tableName = "reading_lists")
data class ReadingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdTime: Long = System.currentTimeMillis(),
    val updatedTime: Long = System.currentTimeMillis()
) {
    fun toDomain(bookCount: Int = 0): ReadingList = ReadingList(
        id = id,
        name = name,
        description = description,
        bookCount = bookCount,
        createdTime = Date(createdTime),
        updatedTime = Date(updatedTime)
    )
}

/**
 * Membership row. The `(listId, bookId)` index is unique so "add" is idempotent at the storage
 * layer — a double tap on 加入书单 cannot produce two entries that then fight over one position.
 */
@Entity(
    tableName = "reading_list_items",
    foreignKeys = [
        ForeignKey(
            entity = ReadingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["listId", "bookId"], unique = true),
        Index(value = ["bookId"])
    ]
)
data class ReadingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val bookId: Long,
    val position: Int,
    val addedTime: Long = System.currentTimeMillis()
)

/** Projection for the lists screen: the list plus how many books are on it. */
data class ReadingListWithCount(
    @Embedded val list: ReadingListEntity,
    val bookCount: Int
)

/** Projection for the detail screen: a book plus where it sits in the list. */
data class ReadingListBookRow(
    @Embedded val book: BookEntity,
    val entryId: Long,
    val entryPosition: Int
) {
    fun toDomain(): ReadingListBook = ReadingListBook(
        entryId = entryId,
        position = entryPosition,
        book = book.toDomain()
    )
}
