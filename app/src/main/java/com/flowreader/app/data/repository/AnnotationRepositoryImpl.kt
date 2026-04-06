package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.AnnotationDao
import com.flowreader.app.data.local.entity.AnnotationEntity
import com.flowreader.app.domain.model.Annotation
import com.flowreader.app.domain.repository.AnnotationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnnotationRepositoryImpl @Inject constructor(
    private val annotationDao: AnnotationDao
) : AnnotationRepository {

    override fun getAnnotationsByBookId(bookId: Long): Flow<List<Annotation>> {
        return annotationDao.getAnnotationsByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAnnotationsListByBookId(bookId: Long): List<Annotation> {
        return annotationDao.getAnnotationsListByBookId(bookId).map { it.toDomain() }
    }

    override fun getAnnotationsByChapter(bookId: Long, chapterIndex: Int): Flow<List<Annotation>> {
        return annotationDao.getAnnotationsByChapter(bookId, chapterIndex).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAnnotationsListByChapter(bookId: Long, chapterIndex: Int): List<Annotation> {
        return annotationDao.getAnnotationsListByChapter(bookId, chapterIndex).map { it.toDomain() }
    }

    override suspend fun getAnnotationById(id: Long): Annotation? {
        return annotationDao.getAnnotationById(id)?.toDomain()
    }

    override suspend fun insertAnnotation(annotation: Annotation): Long {
        return annotationDao.insertAnnotation(AnnotationEntity.fromDomain(annotation))
    }

    override suspend fun updateAnnotation(annotation: Annotation) {
        annotationDao.updateAnnotation(AnnotationEntity.fromDomain(annotation))
    }

    override suspend fun deleteAnnotation(annotation: Annotation) {
        annotationDao.deleteAnnotation(AnnotationEntity.fromDomain(annotation))
    }

    override suspend fun deleteAnnotationById(id: Long) {
        annotationDao.deleteAnnotationById(id)
    }

    override suspend fun deleteAnnotationsByBookId(bookId: Long) {
        annotationDao.deleteAnnotationsByBookId(bookId)
    }

    override suspend fun searchAnnotations(bookId: Long, query: String): List<Annotation> {
        return annotationDao.searchAnnotations(bookId, query).map { it.toDomain() }
    }
}