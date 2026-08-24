package com.flowreader.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SqlLikeTest {

    @Test
    fun escapesWildcards() {
        assertEquals("\\%", SqlLike.escape("%"))
        assertEquals("\\_", SqlLike.escape("_"))
        assertEquals("100\\%\\_off", SqlLike.escape("100%_off"))
    }

    /** The escape character itself must be escaped first, or `\%` would consume the `%`. */
    @Test
    fun escapesTheEscapeCharacter() {
        assertEquals("\\\\", SqlLike.escape("\\"))
        assertEquals("\\\\\\%", SqlLike.escape("\\%"))
    }

    @Test
    fun leavesOrdinaryTextAlone() {
        assertEquals("", SqlLike.escape(""))
        assertEquals("心流阅读", SqlLike.escape("心流阅读"))
        assertEquals("Flow Reader", SqlLike.escape("Flow Reader"))
    }

    @Test
    fun escapeCharIsASingleCharacterAsSqliteRequires() {
        assertEquals(1, SqlLike.ESCAPE_CHAR.length)
    }
}
