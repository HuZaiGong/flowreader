package com.flowreader.app.data.local.dao

import androidx.room.*
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ChapterMetadata
import kotlinx.coroutines.flow.Flow

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index`")
    fun getChaptersByBookId(bookId: Long): Flow<List<ChapterEntity>>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId ORDER BY `index`")
    suspend fun getChaptersListByBookId(bookId: Long): List<ChapterEntity>

    @Query("SELECT id, bookId, `index`, title, startPosition, endPosition FROM chapters WHERE bookId = :bookId ORDER BY `index`")
    suspend fun getChapterMetadataList(bookId: Long): List<ChapterMetadata>

    @Query("SELECT * FROM chapters WHERE bookId = :bookId AND `index` = :index")
    suspend fun getChapter(bookId: Long, index: Int): ChapterEntity?

    @Query("SELECT content FROM chapters WHERE bookId = :bookId AND `index` = :index")
    suspend fun getChapterContent(bookId: Long, index: Int): String?

    @Query("SELECT * FROM chapters WHERE id = :id")
    suspend fun getChapterById(id: Long): ChapterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapter(chapter: ChapterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(chapters: List<ChapterEntity>)

    @Update
    suspend fun updateChapter(chapter: ChapterEntity)

    @Delete
    suspend fun deleteChapter(chapter: ChapterEntity)

    @Query("DELETE FROM chapters WHERE bookId = :bookId")
    suspend fun deleteChaptersByBookId(bookId: Long)

    @Query("SELECT COUNT(*) FROM chapters WHERE bookId = :bookId")
    suspend fun getChapterCount(bookId: Long): Int

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCount(): Int
}
