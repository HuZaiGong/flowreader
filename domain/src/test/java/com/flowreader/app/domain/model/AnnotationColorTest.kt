package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AnnotationColorTest {
    @Test
    fun annotationColorsUseStableArgbValues() {
        assertEquals(0xFFFFFF00, AnnotationColor.YELLOW.colorValue)
        assertEquals(0xFF90EE90, AnnotationColor.GREEN.colorValue)
        assertEquals(0xFFADD8E6, AnnotationColor.BLUE.colorValue)
        assertEquals(0xFFFFB6C1, AnnotationColor.PINK.colorValue)
        assertEquals(0xFFFFA500, AnnotationColor.ORANGE.colorValue)
    }
}
