package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.repository.ChapterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val chapterDao: ChapterDao
) : ChapterRepository {

    override fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>> {
        return chapterDao.getChaptersByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChaptersListByBookId(bookId: Long): List<Chapter> {
        return chapterDao.getChaptersListByBookId(bookId).map { it.toDomain() }
    }

    override suspend fun getChapter(bookId: Long, index: Int): Chapter? {
        return chapterDao.getChapter(bookId, index)?.toDomain()
    }

    override suspend fun getChapterById(id: Long): Chapter? {
        return chapterDao.getChapterById(id)?.toDomain()
    }

    override suspend fun insertChapter(chapter: Chapter): Long {
        return chapterDao.insertChapter(ChapterEntity.fromDomain(chapter))
    }

    override suspend fun insertChapters(chapters: List<Chapter>) {
        chapterDao.insertChapters(chapters.map { ChapterEntity.fromDomain(it) })
    }

    override suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(ChapterEntity.fromDomain(chapter))
    }

    override suspend fun deleteChaptersByBookId(bookId: Long) {
        chapterDao.deleteChaptersByBookId(bookId)
    }

    override suspend fun getChapterCount(bookId: Long): Int {
        return chapterDao.getChapterCount(bookId)
    }
}
