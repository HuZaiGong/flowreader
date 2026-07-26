package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnotationModelTest {
    @Test
    fun annotationDefaultsToYellowHighlightWithoutNote() {
        val annotation = Annotation(
            bookId = 1L,
            chapterIndex = 3,
            startPosition = 10,
            endPosition = 20,
            selectedText = "selected"
        )

        assertEquals(AnnotationColor.YELLOW, annotation.color)
        assertEquals(AnnotationType.HIGHLIGHT, annotation.type)
        assertEquals("", annotation.note)
    }
}
