package com.flowreader.app.domain.model

import java.util.Date

/**
 * A user-curated, manually ordered shelf ("2026 必读书单").
 *
 * Distinct from [Category]: a book has at most one category but can sit in any number of lists,
 * and a list keeps an explicit [ReadingListEntry.position] because the whole point is the order.
 */
data class ReadingList(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val bookCount: Int = 0,
    val createdTime: Date = Date(),
    val updatedTime: Date = Date()
)

/** One book's membership in a [ReadingList]. [position] is dense and zero-based after any edit. */
data class ReadingListEntry(
    val id: Long = 0,
    val listId: Long,
    val bookId: Long,
    val position: Int,
    val addedTime: Date = Date()
)

/** A [ReadingListEntry] resolved against its book, which is what the detail screen renders. */
data class ReadingListBook(
    val entryId: Long,
    val position: Int,
    val book: Book
)

/**
 * Drag-reorder arithmetic, kept out of the composable so it can be tested without a gesture.
 *
 * Reordering by hand is exactly the kind of index maths that silently drops or duplicates an item
 * at the list edges, so [move] is total: out-of-range indices return the input unchanged.
 */
object ReadingListOrder {

    fun <T> move(items: List<T>, from: Int, to: Int): List<T> {
        if (from == to) return items
        if (from !in items.indices || to !in items.indices) return items
        val mutable = items.toMutableList()
        mutable.add(to, mutable.removeAt(from))
        return mutable
    }

    /** Renumbers entries to a dense `0..n-1` sequence in their current order. */
    fun reindex(entries: List<ReadingListEntry>): List<ReadingListEntry> =
        entries.mapIndexed { index, entry -> if (entry.position == index) entry else entry.copy(position = index) }
}
