package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.ReadingListDao
import com.flowreader.app.data.local.entity.ReadingListEntity
import com.flowreader.app.domain.model.ReadingList
import com.flowreader.app.domain.model.ReadingListBook
import com.flowreader.app.domain.repository.ReadingListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReadingListRepositoryImpl @Inject constructor(
    private val readingListDao: ReadingListDao
) : ReadingListRepository {

    override fun getAllLists(): Flow<List<ReadingList>> =
        readingListDao.getAllLists().map { rows ->
            rows.map { it.list.toDomain(bookCount = it.bookCount) }
        }

    override suspend fun getListById(listId: Long): ReadingList? {
        if (listId <= 0L) return null
        return readingListDao.getListById(listId)?.toDomain()
    }

    override fun getBooksInList(listId: Long): Flow<List<ReadingListBook>> =
        readingListDao.getBooksInList(listId).map { rows -> rows.map { it.toDomain() } }

    override suspend fun createList(name: String, description: String): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "书单名称不能为空" }
        val now = System.currentTimeMillis()
        return readingListDao.insertList(
            ReadingListEntity(
                name = trimmed,
                description = description.trim(),
                createdTime = now,
                updatedTime = now
            )
        )
    }

    override suspend fun renameList(listId: Long, name: String, description: String) {
        val trimmed = name.trim()
        if (listId <= 0L || trimmed.isEmpty()) return
        readingListDao.renameList(listId, trimmed, description.trim(), System.currentTimeMillis())
    }

    override suspend fun deleteList(listId: Long) {
        if (listId <= 0L) return
        readingListDao.deleteList(listId)
    }

    override suspend fun addBooks(listId: Long, bookIds: List<Long>) {
        if (listId <= 0L) return
        readingListDao.appendBooks(listId, bookIds.filter { it > 0L }, System.currentTimeMillis())
    }

    override suspend fun removeBook(listId: Long, bookId: Long) {
        if (listId <= 0L || bookId <= 0L) return
        readingListDao.removeBook(listId, bookId, System.currentTimeMillis())
    }

    override suspend fun reorder(listId: Long, orderedBookIds: List<Long>) {
        if (listId <= 0L || orderedBookIds.isEmpty()) return
        readingListDao.applyOrder(listId, orderedBookIds.distinct(), System.currentTimeMillis())
    }
}
