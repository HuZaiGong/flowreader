package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WheelItemTest {
    @Test
    fun defaultItems_arePureDomainValues() {
        val items = WheelItem.defaultItems()

        assertEquals(6, items.size)
        assertTrue(items.all { it.colorValue != 0L })
    }
}
