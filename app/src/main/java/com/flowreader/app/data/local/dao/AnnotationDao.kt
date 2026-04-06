package com.flowreader.app.data.local.dao

import androidx.room.*
import com.flowreader.app.data.local.entity.AnnotationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdTime DESC")
    fun getAnnotationsByBookId(bookId: Long): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId ORDER BY createdTime DESC")
    suspend fun getAnnotationsListByBookId(bookId: Long): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND chapterIndex = :chapterIndex ORDER BY startPosition ASC")
    fun getAnnotationsByChapter(bookId: Long, chapterIndex: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND chapterIndex = :chapterIndex ORDER BY startPosition ASC")
    suspend fun getAnnotationsListByChapter(bookId: Long, chapterIndex: Int): List<AnnotationEntity>

    @Query("SELECT * FROM annotations WHERE id = :id")
    suspend fun getAnnotationById(id: Long): AnnotationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnotation(annotation: AnnotationEntity): Long

    @Update
    suspend fun updateAnnotation(annotation: AnnotationEntity)

    @Delete
    suspend fun deleteAnnotation(annotation: AnnotationEntity)

    @Query("DELETE FROM annotations WHERE id = :id")
    suspend fun deleteAnnotationById(id: Long)

    @Query("DELETE FROM annotations WHERE bookId = :bookId")
    suspend fun deleteAnnotationsByBookId(bookId: Long)

    @Query("SELECT * FROM annotations WHERE bookId = :bookId AND selectedText LIKE '%' || :query || '%'")
    suspend fun searchAnnotations(bookId: Long, query: String): List<AnnotationEntity>
}