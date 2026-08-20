package com.flowreader.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URI

class LanTransferServerTest {

    private lateinit var payload: File
    private var server: LanTransferServer? = null

    @Before
    fun setUp() {
        payload = File.createTempFile("lan_test", ".json")
        payload.writeText("""{"books":[]}""")
    }

    @After
    fun tearDown() {
        server?.stop()
        payload.delete()
    }

    private fun fetch(url: String): Pair<Int, String> {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.readText().orEmpty()
            code to body
        } catch (e: java.io.IOException) {
            0 to ""
        } finally {
            connection.disconnect()
        }
    }

    @Test
    fun servesPayloadAtTokenPath() {
        server = LanTransferServer(payload)
        val url = server!!.start()
        assertTrue(url != null && url.contains("/backup/"))

        val (code, body) = fetch(url!!)
        assertEquals(200, code)
        assertEquals("""{"books":[]}""", body)
    }

    @Test
    fun wrongPathGets404() {
        server = LanTransferServer(payload)
        val url = server!!.start()!!

        val (code, _) = fetch(url.replaceAfterLast("/", "deadbeef"))
        assertEquals(404, code)
    }

    @Test
    fun stopRejectsFurtherConnections() {
        server = LanTransferServer(payload)
        val url = server!!.start()!!
        server!!.stop()

        val (code, _) = fetch(url)
        assertTrue(code != 200)
    }

    @Test
    fun missingPayloadRefusesToStart() {
        val missing = File(payload.parentFile, "does_not_exist.json")
        server = LanTransferServer(missing)
        assertNull(server!!.start())
    }

    @Test
    fun everyInstanceUsesAFreshToken() {
        val first = LanTransferServer(payload).start()!!
        server = LanTransferServer(payload)
        val second = server!!.start()!!
        assertTrue(first != second)
        LanTransferServer(payload).stop()
    }

    /**
     * Security regression test for CVE-LOCAL-001 (v56.4.1 fix).
     *
     * Validates that token generation uses the full charset length, not the desired token length.
     * The bug was: `random.nextInt(length)` instead of `random.nextInt(charset.length)`.
     */
    @Test
    fun tokenGenerationUsesFullCharsetEntropy() {
        // Generate many servers and extract tokens from URLs
        val tokens = (1..100).map {
            val srv = LanTransferServer(payload)
            val url = srv.start() ?: return@map null
            srv.stop()
            // Extract token from "http://IP:PORT/backup/TOKEN"
            url.substringAfterLast("/")
        }.filterNotNull()

        assertTrue("Should generate at least 90 valid tokens", tokens.size >= 90)

        // All tokens should be 16 characters (hex)
        tokens.forEach { token ->
            assertEquals("Token should be 16 chars", 16, token.length)
            assertTrue(
                "Token should be all hex chars",
                token.all { it in '0'..'9' || it in 'a'..'f' }
            )
        }

        // Aggregate all characters used across all tokens
        val allCharsUsed = tokens.flatMap { it.toList() }.toSet()

        // Over 100 tokens, we should see most or all of the 16 hex characters
        assertTrue(
            "Expected to see at least 14 of 16 hex chars across 100 tokens, got ${allCharsUsed.size}",
            allCharsUsed.size >= 14
        )
    }

    @Test
    fun tokenUniquenessAcrossMultipleInstances() {
        val tokens = (1..50).map {
            val srv = LanTransferServer(payload)
            val url = srv.start()
            srv.stop()
            url?.substringAfterLast("/")
        }.filterNotNull()

        val uniqueTokens = tokens.toSet()
        // With 16^16 space, all 50 tokens should be unique
        assertEquals(
            "All generated tokens should be unique",
            tokens.size,
            uniqueTokens.size
        )
    }

    // --- v56.6.2, review finding #4: exact request-line matching ---

    @Test
    fun onlyTheExactBackupRequestLineIsAccepted() {
        val token = "0123456789abcdef"

        assertTrue(LanTransferServer.isBackupRequest("GET /backup/$token HTTP/1.1", token))
        // HttpURLConnection may downgrade, and the server's own response is version-agnostic.
        assertTrue(LanTransferServer.isBackupRequest("GET /backup/$token HTTP/1.0", token))
    }

    @Test
    fun aPathThatMerelyContainsTheTokenIsRefused() {
        // This is the actual fix: the old check was `requestLine.contains("/backup/$token")`, so
        // every line below answered 200 despite the KDoc promising "the exact /backup/<token> path".
        val token = "0123456789abcdef"

        assertFalse(LanTransferServer.isBackupRequest("GET /anything/backup/$token HTTP/1.1", token))
        assertFalse(LanTransferServer.isBackupRequest("GET /backup/${token}extra HTTP/1.1", token))
        assertFalse(LanTransferServer.isBackupRequest("GET /../backup/$token HTTP/1.1", token))
        assertFalse(LanTransferServer.isBackupRequest("GET /backup/$token?x=1 HTTP/1.1", token))
    }

    @Test
    fun otherMethodsAndMalformedLinesAreRefused() {
        val token = "0123456789abcdef"

        assertFalse(LanTransferServer.isBackupRequest("POST /backup/$token HTTP/1.1", token))
        assertFalse(LanTransferServer.isBackupRequest("HEAD /backup/$token HTTP/1.1", token))
        assertFalse(LanTransferServer.isBackupRequest("GET /backup/$token", token))
        assertFalse(LanTransferServer.isBackupRequest("GET  /backup/$token  HTTP/1.1", token))
        assertFalse(LanTransferServer.isBackupRequest("", token))
    }

    @Test
    fun aWrongTokenIsRefusedAndAnEmptyTokenMatchesNothing() {
        assertFalse(LanTransferServer.isBackupRequest("GET /backup/deadbeefdeadbeef HTTP/1.1", "0123456789abcdef"))
        // An empty token means the server never started. Without this guard, `/backup/` alone would
        // match the built expected path and serve the payload to anyone.
        assertFalse(LanTransferServer.isBackupRequest("GET /backup/ HTTP/1.1", ""))
    }

    // --- v56.6.2, review finding #4: one stalled peer cannot hold the server ---

    @Test
    fun aPeerThatNeverSendsARequestDoesNotBlockTheServerForever() {
        // One worker thread serves every peer, so a socket that connects and stays silent used to
        // park that thread on `readLine()` for as long as the dialog was open. The 5s `soTimeout`
        // bounds it. The client timeout below is what makes this a failing test rather than a
        // hanging one if the fix is reverted.
        server = LanTransferServer(payload)
        val url = server!!.start() ?: return
        val uri = URI(url)

        val stalled = java.net.Socket()
        try {
            stalled.connect(InetSocketAddress(uri.host, uri.port), 5_000)

            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.connectTimeout = 20_000
            connection.readTimeout = 20_000
            try {
                assertEquals("stalled peer still holding the only worker thread", 200, connection.responseCode)
                assertEquals("""{"books":[]}""", connection.inputStream.bufferedReader().readText())
            } finally {
                connection.disconnect()
            }
        } finally {
            runCatching { stalled.close() }
        }
    }
}
