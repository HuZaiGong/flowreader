package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchHistoryTest {

    // ---- encoding ------------------------------------------------------------------------------

    @Test
    fun plainEntriesRoundTrip() {
        val entries = listOf("心流", "flow reader", "1949")

        assertEquals(entries, SearchHistory.decode(SearchHistory.encode(entries)))
    }

    /**
     * The delimiter inside a query is the bug this encoding exists for: `"|"`-joining split
     * 「a|b」 into two entries on the next read, and re-encoding made that permanent.
     */
    @Test
    fun anEntryContainingTheDelimiterSurvives() {
        val entries = listOf("a|b", "c")
        val stored = SearchHistory.encode(entries)

        assertEquals(entries, SearchHistory.decode(stored))
        assertEquals(2, SearchHistory.decode(stored).size)
    }

    @Test
    fun anEntryContainingTheEscapeCharacterSurvives() {
        val entries = listOf("""C:\path""", """back\\slash""", """trailing\""")

        assertEquals(entries, SearchHistory.decode(SearchHistory.encode(entries)))
    }

    @Test
    fun anEntryOfNothingButDelimitersSurvives() {
        val entries = listOf("|||", """\|\""")

        assertEquals(entries, SearchHistory.decode(SearchHistory.encode(entries)))
    }

    @Test
    fun anEmptyListEncodesToAnEmptyStringAndBack() {
        assertEquals("", SearchHistory.encode(emptyList()))
        assertEquals(emptyList<String>(), SearchHistory.decode(""))
        assertEquals(emptyList<String>(), SearchHistory.decode(null))
    }

    /** Values written before escaping existed have no backslashes, so they decode unchanged. */
    @Test
    fun theLegacyUnescapedFormatStillReads() {
        assertEquals(listOf("心流", "阅读", "flow"), SearchHistory.decode("心流|阅读|flow"))
    }

    @Test
    fun blankEntriesAreDropped() {
        assertEquals(listOf("flow"), SearchHistory.decode("|flow|"))
        assertEquals(listOf("flow"), SearchHistory.decode("   |flow"))
    }

    /** A hand-corrupted trailing escape must not swallow the character before it. */
    @Test
    fun aTrailingLoneEscapeIsTakenLiterally() {
        assertEquals(listOf("""flow\"""), SearchHistory.decode("""flow\"""))
    }

    // ---- ordering ------------------------------------------------------------------------------

    @Test
    fun theNewestQueryGoesFirst() {
        val history = SearchHistory.withQuery(listOf("旧", "更旧"), "新")

        assertEquals(listOf("新", "旧", "更旧"), history)
    }

    /**
     * Re-running an old search moves it up. The old rule skipped the update entirely when the query
     * was already present, so the most recent search could sit at the bottom of the list.
     */
    @Test
    fun aRepeatedQueryIsPromotedRatherThanIgnored() {
        val history = SearchHistory.withQuery(listOf("a", "b", "c"), "c")

        assertEquals(listOf("c", "a", "b"), history)
    }

    @Test
    fun promotionIsCaseInsensitiveSoNoNearDuplicateAppears() {
        val history = SearchHistory.withQuery(listOf("flow", "reader"), "Flow")

        assertEquals(listOf("Flow", "reader"), history)
    }

    @Test
    fun theQueryIsTrimmedBeforeItIsStored() {
        assertEquals(listOf("心流"), SearchHistory.withQuery(emptyList(), "  心流  "))
    }

    @Test
    fun aBlankQueryIsNotRecorded() {
        val existing = listOf("心流")

        assertEquals(existing, SearchHistory.withQuery(existing, "   "))
        assertEquals(existing, SearchHistory.withQuery(existing, ""))
    }

    @Test
    fun theListIsCappedAndDropsTheOldestEntry() {
        var history = emptyList<String>()
        for (i in 1..SearchHistory.MAX_ENTRIES + 3) {
            history = SearchHistory.withQuery(history, "query $i")
        }

        assertEquals(SearchHistory.MAX_ENTRIES, history.size)
        assertEquals("query ${SearchHistory.MAX_ENTRIES + 3}", history.first())
        assertTrue("the oldest entries must fall off", history.none { it == "query 1" })
    }

    @Test
    fun aCappedListStillRoundTripsThroughStorage() {
        var history = emptyList<String>()
        for (i in 1..SearchHistory.MAX_ENTRIES) {
            history = SearchHistory.withQuery(history, "q|$i")
        }

        assertEquals(history, SearchHistory.decode(SearchHistory.encode(history)))
    }
}
