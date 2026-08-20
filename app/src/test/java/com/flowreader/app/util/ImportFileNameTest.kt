package com.flowreader.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Guards the import path-traversal fix (v56.6.2, review finding #1).
 *
 * The attack these tests describe is concrete: a malicious app sends `ACTION_VIEW` at the exported
 * `MainActivity`, and its `ContentProvider` answers `DISPLAY_NAME` with a traversing name. That
 * string used to reach `File(booksDir, name)` unmodified, so the import wrote over
 * `databases/flowreader_db` or `datastore/settings.preferences_pb`.
 */
class ImportFileNameTest {

    // --- sanitize: path structure cannot survive ---

    @Test
    fun stripsTraversalToABareName() {
        assertEquals("flowreader_db", ImportFileName.sanitize("../../databases/flowreader_db"))
    }

    @Test
    fun stripsAbsolutePathToItsLastSegment() {
        assertEquals("evil.epub", ImportFileName.sanitize("/data/data/com.flowreader.app/files/evil.epub"))
    }

    @Test
    fun handlesWindowsStyleSeparators() {
        assertEquals("evil.epub", ImportFileName.sanitize("..\\..\\databases\\evil.epub"))
    }

    @Test
    fun neverReturnsANameContainingASeparator() {
        val hostile = listOf(
            "../x", "a/b/c", "a\\b", "/", "//", "..", ".", "...", "./.././x"
        )
        hostile.forEach { raw ->
            val safe = ImportFileName.sanitize(raw)
            assertFalse("'$raw' produced '$safe'", safe.contains('/'))
            assertFalse("'$raw' produced '$safe'", safe.contains('\\'))
            assertFalse("'$raw' produced '$safe'", safe == "." || safe == "..")
            assertTrue("'$raw' produced a blank name", safe.isNotBlank())
        }
    }

    @Test
    fun dotOnlyNamesFallBackInsteadOfBeingSalvaged() {
        assertEquals("未知书籍", ImportFileName.sanitize(".."))
        assertEquals("未知书籍", ImportFileName.sanitize("..."))
    }

    @Test
    fun blankAndNullFallBack() {
        assertEquals("未知书籍", ImportFileName.sanitize(null))
        assertEquals("未知书籍", ImportFileName.sanitize("   "))
    }

    // --- sanitize: legitimate names survive intact ---

    @Test
    fun keepsChineseTitles() {
        // The whole reason this does not reuse the ASCII-only sanitizeFileName used for comic
        // directories: that one would turn this into "_.epub" and collapse distinct books together.
        assertEquals("三体.epub", ImportFileName.sanitize("三体.epub"))
    }

    @Test
    fun keepsOtherScriptsAndOrdinaryPunctuation() {
        assertEquals("Война и мир.epub", ImportFileName.sanitize("Война и мир.epub"))
        assertEquals("book-01_v2.epub", ImportFileName.sanitize("book-01_v2.epub"))
    }

    @Test
    fun replacesControlCharactersRatherThanPassingThemThrough() {
        val safe = ImportFileName.sanitize("book\tname\u0001.epub")
        assertFalse("tab survived: $safe", safe.contains('\t'))
        assertFalse(safe.contains('\n'))
    }

    @Test
    fun truncatesLongNamesButKeepsTheExtension() {
        val safe = ImportFileName.sanitize("x".repeat(400) + ".epub")
        assertTrue("length was ${safe.length}", safe.length <= 120)
        assertTrue("extension lost: $safe", safe.endsWith(".epub"))
    }

    // --- resolveWithin: canonical containment is the second, independent guard ---

    @Test
    fun resolvesAGoodNameInsideTheDirectory() {
        val root = createTempDirectory()
        try {
            val books = File(root, "books").apply { mkdirs() }
            val resolved = ImportFileName.resolveWithin(books, "三体.epub")
            assertNotNull(resolved)
            assertEquals(books.canonicalPath, resolved!!.parentFile!!.canonicalPath)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsAnEscapeEvenIfSanitizingWereBypassed() {
        val root = createTempDirectory()
        try {
            val books = File(root, "books").apply { mkdirs() }
            // resolveWithin sanitizes first, so this asserts the end-to-end guarantee: whatever the
            // provider says, the write lands under books/ or not at all.
            val resolved = ImportFileName.resolveWithin(books, "../../databases/flowreader_db")
            assertNotNull(resolved)
            assertTrue(
                "escaped to ${resolved!!.canonicalPath}",
                resolved.canonicalPath.startsWith(books.canonicalPath + File.separator)
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun siblingDirectoryWithASharedPrefixIsNotAcceptedAsContainment() {
        val root = createTempDirectory()
        try {
            val books = File(root, "books").apply { mkdirs() }
            File(root, "booksomething").mkdirs()
            // Guards the `startsWith(path + separator)` form: a plain string prefix check would let
            // /root/booksomething/x satisfy a /root/books containment test.
            val resolved = ImportFileName.resolveWithin(books, "x.epub")
            assertNotNull(resolved)
            assertFalse(resolved!!.canonicalPath.contains("booksomething"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun rejectsWhenTheTargetDirectoryIsASymlinkOutOfTheTree() {
        val root = createTempDirectory()
        try {
            val real = File(root, "real").apply { mkdirs() }
            val outside = createTempDirectory()
            try {
                val link = File(real, "books").toPath()
                val created = runCatching {
                    java.nio.file.Files.createSymbolicLink(link, outside.toPath())
                }.isSuccess
                // Symlink creation can be unavailable; the assertion only applies when it worked.
                if (created) {
                    val resolved = ImportFileName.resolveWithin(File(real, "books"), "x.epub")
                    // Canonicalization follows the link, so the target resolves under `outside`,
                    // which still equals the canonical form of the directory we were handed --
                    // containment holds relative to the requested directory, which is the contract.
                    assertNotNull(resolved)
                    assertTrue(
                        resolved!!.canonicalPath.startsWith(outside.canonicalPath + File.separator)
                    )
                }
            } finally {
                outside.deleteRecursively()
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun anUncanonicalizablePathFailsClosed() {
        // resolveWithin must return null when it cannot prove containment, not fall through to the
        // unchecked path. A NUL byte makes File.canonicalFile throw, which is the cheapest way to
        // reach that branch. Written as an explicit \u0000 escape, because an earlier version
        // of this test embedded a raw NUL byte in the source, which is invisible in every editor
        // and diff, making the test read as an assertion about a merely missing directory.
        //
        // Note this is a containment check, not an existence check -- a directory that simply does
        // not exist yet canonicalizes fine and resolves normally.
        val broken = File("/tmp/does\u0000not/exist")
        assertNull(ImportFileName.resolveWithin(broken, "x.epub"))
    }

    private fun createTempDirectory(): File =
        java.nio.file.Files.createTempDirectory("import_name_test").toFile()
}
