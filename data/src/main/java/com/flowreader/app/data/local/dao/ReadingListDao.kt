package com.flowreader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.flowreader.app.data.local.entity.ReadingListBookRow
import com.flowreader.app.data.local.entity.ReadingListEntity
import com.flowreader.app.data.local.entity.ReadingListItemEntity
import com.flowreader.app.data.local.entity.ReadingListWithCount
import kotlinx.coroutines.flow.Flow

/**
 * Abstract class rather than an interface so the multi-statement `@Transaction` helpers below are
 * generated as real transactions — a half-applied reorder leaves duplicate positions behind.
 */
@Dao
abstract class ReadingListDao {

    @Query(
        "SELECT l.*, (SELECT COUNT(*) FROM reading_list_items i WHERE i.listId = l.id) AS bookCount " +
            "FROM reading_lists l ORDER BY l.updatedTime DESC"
    )
    abstract fun getAllLists(): Flow<List<ReadingListWithCount>>

    @Query("SELECT * FROM reading_lists WHERE id = :listId")
    abstract suspend fun getListById(listId: Long): ReadingListEntity?

    @Query(
        "SELECT b.*, i.id AS entryId, i.position AS entryPosition FROM reading_list_items i " +
            "INNER JOIN books b ON b.id = i.bookId WHERE i.listId = :listId ORDER BY i.position ASC"
    )
    abstract fun getBooksInList(listId: Long): Flow<List<ReadingListBookRow>>

    @Query("SELECT bookId FROM reading_list_items WHERE listId = :listId")
    abstract suspend fun getBookIdsInList(listId: Long): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertList(list: ReadingListEntity): Long

    @Query("UPDATE reading_lists SET name = :name, description = :description, updatedTime = :updatedTime WHERE id = :listId")
    abstract suspend fun renameList(listId: Long, name: String, description: String, updatedTime: Long)

    @Query("DELETE FROM reading_lists WHERE id = :listId")
    abstract suspend fun deleteList(listId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun insertItems(items: List<ReadingListItemEntity>)

    @Query("DELETE FROM reading_list_items WHERE listId = :listId AND bookId = :bookId")
    abstract suspend fun deleteItem(listId: Long, bookId: Long)

    @Query("UPDATE reading_list_items SET position = :position WHERE listId = :listId AND bookId = :bookId")
    abstract suspend fun setPosition(listId: Long, bookId: Long, position: Int)

    @Query("SELECT COALESCE(MAX(position), -1) FROM reading_list_items WHERE listId = :listId")
    abstract suspend fun maxPosition(listId: Long): Int

    @Query("UPDATE reading_lists SET updatedTime = :updatedTime WHERE id = :listId")
    abstract suspend fun touch(listId: Long, updatedTime: Long)

    @Transaction
    open suspend fun appendBooks(listId: Long, bookIds: List<Long>, now: Long) {
        if (bookIds.isEmpty()) return
        val existing = getBookIdsInList(listId).toSet()
        val fresh = bookIds.distinct().filterNot { it in existing }
        if (fresh.isEmpty()) return
        var position = maxPosition(listId)
        insertItems(
            fresh.map { bookId ->
                position += 1
                ReadingListItemEntity(listId = listId, bookId = bookId, position = position, addedTime = now)
            }
        )
        touch(listId, now)
    }

    @Transaction
    open suspend fun removeBook(listId: Long, bookId: Long, now: Long) {
        deleteItem(listId, bookId)
        compact(listId)
        touch(listId, now)
    }

    @Transaction
    open suspend fun applyOrder(listId: Long, orderedBookIds: List<Long>, now: Long) {
        orderedBookIds.forEachIndexed { index, bookId -> setPosition(listId, bookId, index) }
        touch(listId, now)
    }

    /** Re-densifies positions to `0..n-1` after a removal so later reorders stay unambiguous. */
    @Transaction
    open suspend fun compact(listId: Long) {
        val ordered = getOrderedBookIds(listId)
        ordered.forEachIndexed { index, bookId -> setPosition(listId, bookId, index) }
    }

    @Query("SELECT bookId FROM reading_list_items WHERE listId = :listId ORDER BY position ASC")
    abstract suspend fun getOrderedBookIds(listId: Long): List<Long>
}
