package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ChapterMetadata
import com.flowreader.app.domain.model.Chapter
import com.flowreader.app.domain.repository.ChapterRepository
import com.flowreader.app.util.CacheManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChapterRepositoryImpl @Inject constructor(
    private val chapterDao: ChapterDao,
    private val cacheManager: CacheManager
) : ChapterRepository {

    override fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>> {
        return chapterDao.getChaptersByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChaptersListByBookId(bookId: Long): List<Chapter> {
        return chapterDao.getChaptersListByBookId(bookId).map { it.toDomain() }
    }

    override suspend fun getChapterMetadataList(bookId: Long): List<Chapter> {
        val metadataList = chapterDao.getChapterMetadataList(bookId)
        return metadataList.map { meta ->
            Chapter(
                id = meta.id,
                bookId = meta.bookId,
                index = meta.index,
                title = meta.title,
                content = "",
                startPosition = meta.startPosition,
                endPosition = meta.endPosition
            )
        }
    }

    override suspend fun getChapter(bookId: Long, index: Int): Chapter? {
        val meta = chapterDao.getChapterMetadataList(bookId).firstOrNull { it.index == index }

        if (meta == null) {
            val entity = chapterDao.getChapter(bookId, index) ?: return null
            return entity.toDomain()
        }

        val content = cacheManager.getChapterContent(bookId, index)
            ?: chapterDao.getChapterContent(bookId, index)
            ?: ""

        if (content.isNotEmpty()) {
            cacheManager.putChapterContent(bookId, index, content)
        }

        return Chapter(
            id = meta.id,
            bookId = meta.bookId,
            index = meta.index,
            title = meta.title,
            content = content,
            startPosition = meta.startPosition,
            endPosition = meta.endPosition
        )
    }

    override suspend fun getChapterContent(bookId: Long, index: Int): String? {
        val cached = cacheManager.getChapterContent(bookId, index)
        if (cached != null) return cached

        val content = chapterDao.getChapterContent(bookId, index)
        if (content != null) {
            cacheManager.putChapterContent(bookId, index, content)
        }
        return content
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
