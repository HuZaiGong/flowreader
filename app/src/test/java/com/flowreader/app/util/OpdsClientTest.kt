package com.flowreader.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OpdsAddressTest {

    @Test
    fun rfc1918LiteralsAreAllowed() {
        listOf(
            "http://192.168.1.10:8080/opds",
            "http://10.0.0.5/catalog",
            "http://172.16.3.4/opds",
            "http://172.31.255.255/opds",
            "http://127.0.0.1:8080/opds",
            "http://169.254.1.1/opds",
            "https://localhost:8443/opds"
        ).forEach { url ->
            assertTrue("$url should be treated as LAN", OpdsAddress.isLanUrl(url))
        }
    }

    @Test
    fun publicHostsAreRejected() {
        listOf(
            "http://example.com/opds",
            "https://opds.example.org/catalog",
            "http://8.8.8.8/opds",
            "http://172.32.0.1/opds",
            "http://172.15.0.1/opds",
            "http://11.0.0.1/opds",
            "http://192.169.1.1/opds"
        ).forEach { url ->
            assertFalse("$url must not be reachable", OpdsAddress.isLanUrl(url))
        }
    }

    @Test
    fun hostnamesThatMerelyLookLikeIpv6PrefixesAreRejected() {
        // "fc"/"fd" only mean a unique-local address when the host is actually an IPv6 literal.
        assertFalse(OpdsAddress.isLanUrl("http://fcbooks.com/opds"))
        assertFalse(OpdsAddress.isLanUrl("http://fdrive.io/opds"))
        assertTrue(OpdsAddress.isLanUrl("http://[fd00::1]/opds"))
        assertTrue(OpdsAddress.isLanUrl("http://[::1]:8080/opds"))
    }

    @Test
    fun localSuffixesAreAllowed() {
        assertTrue(OpdsAddress.isLanUrl("http://nas.local:8080/opds"))
        assertTrue(OpdsAddress.isLanUrl("http://calibre.lan/opds"))
        assertFalse(OpdsAddress.isLanUrl("http://notlocal.example/opds"))
    }

    @Test
    fun nonHttpSchemesAreRejected() {
        assertFalse(OpdsAddress.isLanUrl("file:///etc/passwd"))
        assertFalse(OpdsAddress.isLanUrl("ftp://192.168.1.1/books"))
        assertFalse(OpdsAddress.isLanUrl("javascript:alert(1)"))
        assertFalse(OpdsAddress.isLanUrl(""))
    }

    @Test
    fun normalizeAddsTheSchemeButKeepsTheGuard() {
        assertEquals("http://192.168.1.10:8080/opds", OpdsAddress.normalize("192.168.1.10:8080/opds"))
        assertEquals("http://192.168.1.10/opds", OpdsAddress.normalize("  192.168.1.10/opds  "))
        assertNull(OpdsAddress.normalize("example.com/opds"))
        assertNull(OpdsAddress.normalize(""))
    }

    @Test
    fun resolveKeepsRelativeLinksInsideTheLan() {
        assertEquals(
            "http://192.168.1.10/opds/new",
            OpdsAddress.resolve("http://192.168.1.10/opds/root", "new")
        )
        // An absolute redirect off the LAN must not survive resolution.
        assertNull(OpdsAddress.resolve("http://192.168.1.10/opds", "http://evil.example/steal"))
    }
}

class OpdsFeedParsingTest {

    private val feedUrl = "http://192.168.1.10:8080/opds"

    @Test
    fun acquisitionEntriesExposeADownloadUrl() {
        val feed = OpdsClient.parseFeed(FEED, feedUrl)

        assertEquals("我的书库", feed.title)
        val book = feed.entries.first { it.title == "心流" }
        assertEquals("米哈里", book.author)
        assertEquals("http://192.168.1.10:8080/get/1.epub", book.acquisitionUrl)
        assertEquals("application/epub+zip", book.acquisitionType)
        assertFalse(book.isNavigation)
    }

    @Test
    fun navigationEntriesExposeAFollowUrl() {
        val feed = OpdsClient.parseFeed(FEED, feedUrl)

        val folder = feed.entries.first { it.title == "按作者" }
        assertTrue(folder.isNavigation)
        assertEquals("http://192.168.1.10:8080/opds/authors", folder.navigationUrl)
    }

    @Test
    fun paginationIsFollowedOnlyWhenItStaysOnTheLan() {
        assertEquals("http://192.168.1.10:8080/opds?page=2", OpdsClient.parseFeed(FEED, feedUrl).nextUrl)

        val offLan = FEED.replace("/opds?page=2", "http://cdn.example.com/page2")
        assertNull(OpdsClient.parseFeed(offLan, feedUrl).nextUrl)
    }

    @Test
    fun untitledEntriesAreDropped() {
        val feed = OpdsClient.parseFeed(
            """<feed xmlns="http://www.w3.org/2005/Atom"><entry><id>1</id></entry></feed>""",
            feedUrl
        )
        assertTrue(feed.entries.isEmpty())
    }

    @Test
    fun garbageDoesNotThrow() {
        val feed = OpdsClient.parseFeed("this is not a feed", feedUrl)
        assertTrue(feed.entries.isEmpty())
    }

    private companion object {
        const val FEED = """<?xml version="1.0" encoding="utf-8"?>
        <feed xmlns="http://www.w3.org/2005/Atom">
          <title>我的书库</title>
          <link rel="next" href="/opds?page=2" type="application/atom+xml"/>
          <entry>
            <title>心流</title>
            <author><name>米哈里</name></author>
            <summary>关于专注</summary>
            <link rel="http://opds-spec.org/acquisition" href="/get/1.epub" type="application/epub+zip"/>
          </entry>
          <entry>
            <title>按作者</title>
            <link href="/opds/authors" type="application/atom+xml;profile=opds-catalog"/>
          </entry>
        </feed>"""
    }
}
