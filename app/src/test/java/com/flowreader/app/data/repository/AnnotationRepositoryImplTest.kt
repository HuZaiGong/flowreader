package com.flowreader.app.data.repository

import com.flowreader.app.data.local.dao.AnnotationDao
import com.flowreader.app.data.local.entity.AnnotationEntity
import com.flowreader.app.domain.repository.AnnotationExportFormat
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AnnotationRepositoryImplTest {
    private val dao = mockk<AnnotationDao>()
    private val repository = AnnotationRepositoryImpl(dao)

    @Test
    fun exportAnnotations_asMarkdown_includesNotes() = runTest {
        coEvery { dao.getAnnotationsListByBookId(1L) } returns listOf(
            AnnotationEntity(bookId = 1L, chapterIndex = 0, startPosition = 0, endPosition = 5, selectedText = "hello", note = "note")
        )

        val markdown = repository.exportAnnotations(1L, AnnotationExportFormat.MARKDOWN)

        assertTrue(markdown.contains("hello"))
        assertTrue(markdown.contains("note"))
    }
}
