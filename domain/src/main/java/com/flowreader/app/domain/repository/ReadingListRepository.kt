package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.ReadingList
import com.flowreader.app.domain.model.ReadingListBook
import kotlinx.coroutines.flow.Flow

interface ReadingListRepository {
    fun getAllLists(): Flow<List<ReadingList>>
    suspend fun getListById(listId: Long): ReadingList?
    fun getBooksInList(listId: Long): Flow<List<ReadingListBook>>
    suspend fun createList(name: String, description: String = ""): Long
    suspend fun renameList(listId: Long, name: String, description: String)
    suspend fun deleteList(listId: Long)

    /** Appends to the end of the list. Adding a book already in the list is a no-op. */
    suspend fun addBooks(listId: Long, bookIds: List<Long>)
    suspend fun removeBook(listId: Long, bookId: Long)

    /** Persists a full reordering. [orderedBookIds] is the new front-to-back order. */
    suspend fun reorder(listId: Long, orderedBookIds: List<Long>)
}
