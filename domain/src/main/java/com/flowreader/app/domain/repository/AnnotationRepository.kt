package com.flowreader.app.domain.repository

import com.flowreader.app.domain.model.Annotation
import kotlinx.coroutines.flow.Flow

interface AnnotationRepository {
    fun getAnnotationsByBookId(bookId: Long): Flow<List<Annotation>>
    suspend fun getAnnotationsListByBookId(bookId: Long): List<Annotation>
    fun getAnnotationsByChapter(bookId: Long, chapterIndex: Int): Flow<List<Annotation>>
    suspend fun getAnnotationsListByChapter(bookId: Long, chapterIndex: Int): List<Annotation>
    suspend fun getAnnotationById(id: Long): Annotation?
    suspend fun insertAnnotation(annotation: Annotation): Long
    suspend fun updateAnnotation(annotation: Annotation)
    suspend fun deleteAnnotation(annotation: Annotation)
    suspend fun deleteAnnotationById(id: Long)
    suspend fun deleteAnnotationsByBookId(bookId: Long)
    suspend fun exportAnnotations(bookId: Long, format: AnnotationExportFormat): String

    /**
     * Cross-book notes (v53). Searching them is the caller's job: the notes screen holds the whole
     * list already and filters it case-insensitively, which SQLite's `LIKE` cannot do for non-ASCII
     * text. The two `LIKE` queries that used to back this had no caller.
     */
    fun getAllAnnotations(): Flow<List<Annotation>>
}

enum class AnnotationExportFormat { MARKDOWN, HTML, TEXT }
