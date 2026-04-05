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

    private val chapterCache = mutableMapOf<Long, MutableMap<Int, Chapter>>()
    private val metadataCache = mutableMapOf<Long, List<Chapter>>()

    override fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>> {
        return chapterDao.getChaptersByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChaptersListByBookId(bookId: Long): List<Chapter> {
        return metadataCache.getOrPut(bookId) {
            chapterDao.getChaptersListByBookId(bookId).map { it.toDomain() }
        }
    }

    override suspend fun getChapter(bookId: Long, index: Int): Chapter? {
        val bookCache = chapterCache.getOrPut(bookId) { mutableMapOf() }
        return bookCache.getOrPut(index) {
            chapterDao.getChapter(bookId, index)?.toDomain() ?: return null
        }
    }

    override suspend fun getChapterById(id: Long): Chapter? {
        return chapterDao.getChapterById(id)?.toDomain()
    }

    override suspend fun insertChapter(chapter: Chapter): Long {
        val id = chapterDao.insertChapter(ChapterEntity.fromDomain(chapter))
        chapterCache[chapter.bookId]?.remove(chapter.index)
        metadataCache.remove(chapter.bookId)
        return id
    }

    override suspend fun insertChapters(chapters: List<Chapter>) {
        chapterDao.insertChapters(chapters.map { ChapterEntity.fromDomain(it) })
        chapters.firstOrNull()?.let { metadataCache.remove(it.bookId) }
    }

    override suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(ChapterEntity.fromDomain(chapter))
        chapterCache[chapter.bookId]?.remove(chapter.index)
        metadataCache.remove(chapter.bookId)
    }

    override suspend fun deleteChaptersByBookId(bookId: Long) {
        chapterDao.deleteChaptersByBookId(bookId)
        chapterCache.remove(bookId)
        metadataCache.remove(bookId)
    }

    override suspend fun getChapterCount(bookId: Long): Int {
        return chapterDao.getChapterCount(bookId)
    }
}
