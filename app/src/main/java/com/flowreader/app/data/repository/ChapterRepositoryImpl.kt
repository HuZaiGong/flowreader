package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.ChapterDao
import com.flowreader.app.data.local.entity.ChapterEntity
import com.flowreader.app.data.local.entity.ChapterMetadata
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

    private val contentCache = mutableMapOf<Long, MutableMap<Int, String>>()

    override fun getChaptersByBookId(bookId: Long): Flow<List<Chapter>> {
        return chapterDao.getChaptersByBookId(bookId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getChaptersListByBookId(bookId: Long): List<Chapter> {
        val chapters = chapterDao.getChaptersListByBookId(bookId).map { it.toDomain() }
        return chapters
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
        val cachedContent = contentCache[bookId]?.get(index)
        
        val meta = chapterDao.getChapterMetadataList(bookId).getOrNull(index)
        
        if (meta == null) {
            val entity = chapterDao.getChapter(bookId, index) ?: return null
            return entity.toDomain()
        }
        
        val content = cachedContent ?: chapterDao.getChapterContent(bookId, index) ?: ""
        
        contentCache.getOrPut(bookId) { mutableMapOf() }[index] = content
        
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
        val cached = contentCache[bookId]?.get(index)
        if (cached != null) return cached
        
        val content = chapterDao.getChapterContent(bookId, index)
        if (content != null) {
            contentCache.getOrPut(bookId) { mutableMapOf() }[index] = content
        }
        return content
    }

    override suspend fun getChapterById(id: Long): Chapter? {
        return chapterDao.getChapterById(id)?.toDomain()
    }

    override suspend fun insertChapter(chapter: Chapter): Long {
        val id = chapterDao.insertChapter(ChapterEntity.fromDomain(chapter))
        contentCache[chapter.bookId]?.remove(chapter.index)
        return id
    }

    override suspend fun insertChapters(chapters: List<Chapter>) {
        chapterDao.insertChapters(chapters.map { ChapterEntity.fromDomain(it) })
        chapters.firstOrNull()?.let { contentCache.remove(it.bookId) }
    }

    override suspend fun updateChapter(chapter: Chapter) {
        chapterDao.updateChapter(ChapterEntity.fromDomain(chapter))
        contentCache[chapter.bookId]?.remove(chapter.index)
    }

    override suspend fun deleteChaptersByBookId(bookId: Long) {
        chapterDao.deleteChaptersByBookId(bookId)
        contentCache.remove(bookId)
    }

    override suspend fun getChapterCount(bookId: Long): Int {
        return chapterDao.getChapterCount(bookId)
    }
}
