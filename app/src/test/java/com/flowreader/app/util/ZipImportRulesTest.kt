package com.flowreader.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ZipImportRulesTest {

    @Test
    fun supportedBooksAreFlattenedToTheirFileName() {
        assertEquals("book.epub", ZipImportRules.safeBookName("book.epub", isDirectory = false))
        assertEquals("book.epub", ZipImportRules.safeBookName("nested/dir/book.epub", isDirectory = false))
        assertEquals("book.txt", ZipImportRules.safeBookName("nested\\windows\\book.txt", isDirectory = false))
        assertEquals("novel.fb2", ZipImportRules.safeBookName("novel.fb2", isDirectory = false))
        assertEquals("novel.mobi", ZipImportRules.safeBookName("novel.mobi", isDirectory = false))
    }

    @Test
    fun pathTraversalIsRejected() {
        assertNull(ZipImportRules.safeBookName("../../etc/passwd.txt", isDirectory = false))
        assertNull(ZipImportRules.safeBookName("a/../../b.epub", isDirectory = false))
        assertNull(ZipImportRules.safeBookName("/absolute/book.epub", isDirectory = false))
    }

    @Test
    fun directoriesAndNoiseAreSkipped() {
        assertNull(ZipImportRules.safeBookName("chapters/", isDirectory = true))
        assertNull(ZipImportRules.safeBookName("__MACOSX/._book.epub", isDirectory = false))
        assertNull(ZipImportRules.safeBookName(".hidden.epub", isDirectory = false))
    }

    @Test
    fun unsupportedExtensionsAreSkipped() {
        assertNull(ZipImportRules.safeBookName("cover.jpg", isDirectory = false))
        assertNull(ZipImportRules.safeBookName("notes", isDirectory = false))
        assertNull(ZipImportRules.safeBookName("payload.apk", isDirectory = false))
    }

    @Test
    fun theEntryAndSizeCapsAreSane() {
        // A zip bomb should be stopped by these, not by the device running out of storage.
        assert(ZipImportRules.MAX_ENTRIES in 1..2000)
        assert(ZipImportRules.MAX_ENTRY_BYTES > 0)
    }
}
