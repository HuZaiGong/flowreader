package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the search segmentation. These pin the *shape* of the emitted streams; the
 * end-to-end proof that these expressions actually match in SQLite lives in
 * `app/src/test/.../FullTextSearchEngineTest.kt`, which runs them against a real FTS5 table.
 */
class CjkTokenizerTest {

    @Test
    fun foldKeepsOnlyLettersAndDigits() {
        assertEquals("这是一个故事", CjkTokenizer.fold("这是一个，故事。"))
        assertEquals("hello2024", CjkTokenizer.fold("Hello, 2024!"))
        assertEquals("", CjkTokenizer.fold("  —— ，。！?  "))
    }

    @Test
    fun foldLowercasesLatin() {
        assertEquals("flowreader", CjkTokenizer.fold("FlowReader"))
    }

    /** Structural escaping: nothing an FTS5 operator is made of can survive folding. */
    @Test
    fun foldStripsEveryFtsOperatorCharacter() {
        assertEquals("abc", CjkTokenizer.fold("\"a^b*c\""))
        assertEquals("ab", CjkTokenizer.fold("a - b"))
        assertEquals("ab", CjkTokenizer.fold("(a) {b}"))
        assertEquals("", CjkTokenizer.fold("\"\"\"*^-+~(){}[]:"))
    }

    @Test
    fun indexStreamEmitsOverlappingBigrams() {
        assertEquals("这是 是一 一个", CjkTokenizer.indexStream("这是一个"))
    }

    @Test
    fun indexStreamBridgesPunctuationAndScriptChanges() {
        // 「事，江」 and 「流Flow」 are only reachable because folding happens before segmentation.
        assertTrue(CjkTokenizer.indexStream("故事，江南").contains("事江"))
        assertTrue(CjkTokenizer.indexStream("心流Flow").contains("流f"))
        assertTrue(CjkTokenizer.indexStream("上海1949").contains("海1"))
    }

    @Test
    fun indexStreamHandlesShortAndEmptyText() {
        assertEquals("", CjkTokenizer.indexStream(""))
        assertEquals("", CjkTokenizer.indexStream("，。！"))
        // A single code point has no bigram, so it is indexed on its own.
        assertEquals("柔", CjkTokenizer.indexStream("柔"))
        assertEquals("心流", CjkTokenizer.indexStream("心流"))
    }

    @Test
    fun indexStreamTruncatesAtTheLimit() {
        val stream = CjkTokenizer.indexStream("abcdef", limit = 3)
        assertEquals("ab bc", stream)
    }

    @Test
    fun indexStreamKeepsSurrogatePairsIntact() {
        // U+1F600 is a surrogate pair and not a letter or digit, so it folds away entirely.
        assertEquals("ab", CjkTokenizer.fold("a😀b"))
        // U+20000 (𠀀) is a Han extension-B letter: it must survive as one code point, not two chars.
        val stream = CjkTokenizer.indexStream("𠀀心")
        assertEquals("𠀀心", stream)
    }

    @Test
    fun matchExpressionIsAPhraseOfBigrams() {
        assertEquals("\"心流\"", CjkTokenizer.matchExpression("心流"))
        assertEquals("\"这是 是一 一个\"", CjkTokenizer.matchExpression("这是一个"))
    }

    /** A lone code point has no bigram, so it must be matched as a prefix instead. */
    @Test
    fun matchExpressionUsesPrefixFormForASingleCodePoint() {
        assertEquals("\"心\"*", CjkTokenizer.matchExpression("心"))
        assertEquals("\"a\"*", CjkTokenizer.matchExpression("A"))
    }

    @Test
    fun matchExpressionIsNullWhenNothingSearchableRemains() {
        assertNull(CjkTokenizer.matchExpression(""))
        assertNull(CjkTokenizer.matchExpression("   "))
        assertNull(CjkTokenizer.matchExpression("\"*^-+~(){}[]:"))
    }

    @Test
    fun matchExpressionFoldsTheQueryTheSameWayAsTheIndex() {
        // Query and index must agree, or punctuation in either one desynchronizes them.
        assertEquals(CjkTokenizer.matchExpression("事江"), CjkTokenizer.matchExpression("事，江"))
        assertEquals(CjkTokenizer.matchExpression("flow"), CjkTokenizer.matchExpression("FLOW"))
    }

    /**
     * The UI used to require two characters, which silently made every single-character Chinese
     * search a no-op. One Han character is a word; one Latin letter is a prefix over the library.
     */
    @Test
    fun singleCharacterCjkQueriesAreSearchable() {
        for (query in listOf("爱", "山", "读", "𠀀")) {
            assertTrue("'$query' must be searchable", CjkTokenizer.isSearchableQuery(query))
        }
    }

    @Test
    fun singleAsciiCharacterQueriesAreNot() {
        for (query in listOf("a", "Z", "7", " a ")) {
            assertFalse("'$query' must not be searchable", CjkTokenizer.isSearchableQuery(query))
        }
    }

    @Test
    fun twoOrMoreCharactersAreAlwaysSearchable() {
        for (query in listOf("ab", "心流", "a1", "a-b")) {
            assertTrue("'$query' must be searchable", CjkTokenizer.isSearchableQuery(query))
        }
    }

    @Test
    fun aQueryThatFoldsAwayIsNotSearchable() {
        // Must agree with matchExpression returning null, or the UI would call a search that cannot run.
        for (query in listOf("", "   ", "\"*^-+~(){}[]:", "😀")) {
            assertFalse("'$query' must not be searchable", CjkTokenizer.isSearchableQuery(query))
            assertNull(CjkTokenizer.matchExpression(query))
        }
    }
}
