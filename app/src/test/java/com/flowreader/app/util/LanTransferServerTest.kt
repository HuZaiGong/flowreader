package com.flowreader.app.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.net.HttpURLConnection
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
}
