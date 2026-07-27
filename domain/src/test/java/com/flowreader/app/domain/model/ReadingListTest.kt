package com.flowreader.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingListTest {

    private val items = listOf("a", "b", "c", "d")

    @Test
    fun movingForwardsShiftsTheItemsInBetween() {
        assertEquals(listOf("b", "c", "a", "d"), ReadingListOrder.move(items, 0, 2))
    }

    @Test
    fun movingBackwardsShiftsTheItemsInBetween() {
        assertEquals(listOf("a", "d", "b", "c"), ReadingListOrder.move(items, 3, 1))
    }

    @Test
    fun movingToTheSameIndexIsANoOp() {
        assertEquals(items, ReadingListOrder.move(items, 2, 2))
    }

    @Test
    fun outOfRangeIndicesLeaveTheListUntouched() {
        // The up/down buttons on the first and last rows hit exactly this path.
        assertEquals(items, ReadingListOrder.move(items, 0, -1))
        assertEquals(items, ReadingListOrder.move(items, 3, 4))
        assertEquals(items, ReadingListOrder.move(items, 9, 0))
        assertEquals(emptyList<String>(), ReadingListOrder.move(emptyList<String>(), 0, 0))
    }

    @Test
    fun moveNeverLosesOrDuplicatesAnItem() {
        for (from in items.indices) {
            for (to in items.indices) {
                val moved = ReadingListOrder.move(items, from, to)
                assertEquals(items.size, moved.size)
                assertEquals(items.toSet(), moved.toSet())
            }
        }
    }

    @Test
    fun reindexDensifiesPositionsAfterARemoval() {
        val sparse = listOf(
            ReadingListEntry(id = 1, listId = 1, bookId = 10, position = 0),
            ReadingListEntry(id = 3, listId = 1, bookId = 30, position = 2),
            ReadingListEntry(id = 4, listId = 1, bookId = 40, position = 5)
        )
        assertEquals(listOf(0, 1, 2), ReadingListOrder.reindex(sparse).map { it.position })
    }

    @Test
    fun reindexKeepsIdentityWhenPositionsAreAlreadyDense() {
        val dense = listOf(
            ReadingListEntry(id = 1, listId = 1, bookId = 10, position = 0),
            ReadingListEntry(id = 2, listId = 1, bookId = 20, position = 1)
        )
        assertEquals(dense, ReadingListOrder.reindex(dense))
    }

    @Test
    fun listDefaultsToAnEmptyDescriptionAndZeroBooks() {
        val list = ReadingList(name = "2026 必读")
        assertEquals("", list.description)
        assertEquals(0, list.bookCount)
    }
}
