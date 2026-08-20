package com.flowreader.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/**
 * Guards the read/copy caps in [BookParser] (v56.6.2, review finding #2).
 *
 * The three helpers under test are the only thing standing between an attacker-supplied stream and
 * unbounded memory or disk use: `copyCapped` writes a container into `filesDir/books`, and
 * `readCappedBytes` / `readCappedText` pull EPUB entries (cover art, `container.xml`, the OPF) fully
 * into memory. All three used to be plain `copyTo()` / `readText()` calls.
 *
 * The real limits are 256MB / 24MB / 4MB, which is why the helpers are `internal`: these tests drive
 * them with a handful of bytes so the boundary is actually exercised. What they cannot check is that
 * every call site passes a limit at all — that stays a reading job.
 */
class BookParserCapsTest {

    private fun stream(size: Int): InputStream = ByteArrayInputStream(ByteArray(size) { 'a'.code.toByte() })

    // --- copyCapped ---

    @Test
    fun copyUnderTheLimitReportsTheByteCount() {
        val target = File.createTempFile("caps", ".bin")
        try {
            assertEquals(500L, BookParser.copyCapped(stream(500), target, limit = 1_000))
            assertEquals(500L, target.length())
        } finally {
            target.delete()
        }
    }

    @Test
    fun copyExactlyAtTheLimitStillSucceeds() {
        // The guard is `total > limit`, so a file of exactly the limit is legal. Worth pinning:
        // flipping it to `>=` would reject a book whose size happens to land on the boundary.
        val target = File.createTempFile("caps", ".bin")
        try {
            assertEquals(1_000L, BookParser.copyCapped(stream(1_000), target, limit = 1_000))
        } finally {
            target.delete()
        }
    }

    @Test
    fun copyOverTheLimitReturnsMinusOne() {
        val target = File.createTempFile("caps", ".bin")
        try {
            assertEquals(-1L, BookParser.copyCapped(stream(1_001), target, limit = 1_000))
        } finally {
            target.delete()
        }
    }

    @Test
    fun aBreachedCopyStopsWritingInsteadOfDrainingTheStream() {
        // The point of the cap is that an 80GB stream costs one buffer of disk, not 80GB. The write
        // stops within a buffer of the limit; it is not byte-exact and does not need to be.
        val target = File.createTempFile("caps", ".bin")
        try {
            assertEquals(-1L, BookParser.copyCapped(stream(4 * 1024 * 1024), target, limit = 1_000))
            assertTrue("wrote ${target.length()} bytes past a 1000-byte cap", target.length() < 64 * 1024)
        } finally {
            target.delete()
        }
    }

    @Test
    fun aBreachedCopyLeavesThePartialFileForTheCallerToDelete() {
        // Documented contract, not an accident: `copyCapped` does not delete. `copyFileToInternal`
        // deletes explicitly on -1 and the EPUB/comic paths delete in `finally`. A future caller
        // that trusts the cap to clean up would leave junk in `filesDir/books`.
        val target = File.createTempFile("caps", ".bin")
        try {
            BookParser.copyCapped(stream(9_000), target, limit = 1_000)
            assertTrue("cap deleted the partial file; call sites rely on doing that themselves", target.exists())
        } finally {
            target.delete()
        }
    }

    // --- readCappedBytes / readCappedText ---

    @Test
    fun readUnderTheLimitReturnsEverything() {
        val bytes = BookParser.readCappedBytes(stream(500), limit = 1_000)
        assertNotNull(bytes)
        assertEquals(500, bytes!!.size)
    }

    @Test
    fun readExactlyAtTheLimitStillSucceeds() {
        assertEquals(1_000, BookParser.readCappedBytes(stream(1_000), limit = 1_000)?.size)
    }

    @Test
    fun readOverTheLimitReturnsNullRatherThanATruncatedBuffer() {
        // Null, not a short array: a truncated `container.xml` would parse as a valid-looking EPUB
        // with a missing OPF path, so callers must be able to tell "too big" from "empty".
        assertNull(BookParser.readCappedBytes(stream(1_001), limit = 1_000))
    }

    @Test
    fun readCappedTextDecodesUtf8AndInheritsTheSameLimit() {
        val text = "书名：三体"
        val encoded = text.toByteArray(Charsets.UTF_8)

        assertEquals(text, BookParser.readCappedText(ByteArrayInputStream(encoded), limit = 1_000))
        assertNull(BookParser.readCappedText(ByteArrayInputStream(encoded), limit = (encoded.size - 1).toLong()))
    }

    @Test
    fun anEmptyStreamIsNotConfusedWithABreach() {
        assertEquals(0, BookParser.readCappedBytes(stream(0), limit = 1_000)?.size)

        val target = File.createTempFile("caps", ".bin")
        try {
            assertEquals(0L, BookParser.copyCapped(stream(0), target, limit = 1_000))
        } finally {
            target.delete()
        }
    }
}
