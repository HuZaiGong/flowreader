package com.flowreader.app.util

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Guards the LAN boundary on the receiving side (v56.6.2, review finding #3).
 *
 * Before v56.6.2 [LanTransferClient.download] checked the scheme and nothing else, so a pasted
 * `http://evil.example.com/backup/...` link would be fetched from the open internet and handed to
 * the backup importer — 200MB of attacker-chosen content overwriting a user's library, through the
 * one `INTERNET` permission that is supposed to exist only for the LAN.
 *
 * Every rejection below happens before a socket is opened, so these tests do no networking. That is
 * itself the property worth having: a refused URL must not be contacted at all, not even to fail.
 */
class LanTransferClientTest {

    private lateinit var target: File

    @Before
    fun setUp() {
        target = File.createTempFile("lan_client_test", ".json")
        target.delete()
    }

    @After
    fun tearDown() {
        target.delete()
    }

    private fun attempt(url: String): Throwable? = runBlocking {
        LanTransferClient.download(url, target).exceptionOrNull()
    }

    private fun assertRejected(url: String, because: String) {
        val error = attempt(url)
        assertTrue("accepted $url", error is IllegalArgumentException)
        assertEquals("wrong reason for $url", because, error!!.message)
        assertFalse("$url was refused but still wrote a file", target.exists())
    }

    // --- the host check, which is the actual v56.6.2 fix ---

    @Test
    fun aPublicHostIsRefused() {
        assertRejected("http://evil.example.com/backup/0123456789abcdef", "仅支持局域网地址")
    }

    @Test
    fun aPublicIpLiteralIsRefused() {
        // Bypassing DNS does not help: the check is on the address, not the name.
        assertRejected("http://93.184.216.34/backup/0123456789abcdef", "仅支持局域网地址")
    }

    @Test
    fun aPublicHostIsStillRefusedWhenTheRestOfTheUrlLooksPerfect() {
        // The path and token shape are exactly what the server serves. Only the host is wrong, and
        // that alone has to be disqualifying — otherwise the token format becomes the access control.
        assertRejected("http://cdn.example.org/backup/deadbeefdeadbeef", "仅支持局域网地址")
    }

    @Test
    fun aPrivateLookingSubdomainOfAPublicNameIsRefused() {
        // `isPrivateHost` matches `.local` as a *suffix*, so this must not slip through on the
        // substring alone.
        assertRejected("http://192.168.1.5.evil.com/backup/0123456789abcdef", "仅支持局域网地址")
        assertRejected("http://local.evil.com/backup/0123456789abcdef", "仅支持局域网地址")
    }

    // --- the checks that were already there, pinned so a refactor cannot drop them ---

    @Test
    fun httpsAndOtherSchemesAreRefused() {
        // Not a security hole, a capability statement: the server only ever speaks plain HTTP, so
        // anything else means the URL did not come from a FlowReader peer.
        assertRejected("https://192.168.1.5/backup/0123456789abcdef", "仅支持 http:// 链接")
        assertRejected("file:///etc/passwd", "仅支持 http:// 链接")
    }

    @Test
    fun aMalformedPathIsRefusedEvenOnALanHost() {
        assertRejected("http://192.168.1.5/etc/passwd", "链接格式不正确")
        assertRejected("http://192.168.1.5/backup/../../secret", "链接格式不正确")
        assertRejected("http://192.168.1.5/backup/short", "链接格式不正确")
        assertRejected("http://192.168.1.5/backup/0123456789ABCDEF", "链接格式不正确")
    }

    @Test
    fun garbageInputFailsAsAResultRatherThanThrowing() {
        // The caller is a dialog handing over whatever was pasted; `download` must always come back
        // as a Result. `URI("not a url")` throws, so this pins that the runCatching wrapper stays.
        val error = attempt("not a url at all")
        assertTrue("expected a failure, got success", error != null)
        assertFalse(target.exists())
    }

    // --- the positive case, so the guard cannot be "reject everything" ---

    @Test
    fun aLanUrlPassesValidationAndReachesTheNetworkStage() {
        // Port 1 on loopback has nothing listening, so this fails at connect — which is precisely
        // the proof: validation let it through. Any IllegalArgumentException here would mean a real
        // peer-to-peer transfer is broken.
        val error = attempt("http://127.0.0.1:1/backup/0123456789abcdef")
        assertFalse(
            "a loopback LAN URL was rejected by validation: ${error?.message}",
            error is IllegalArgumentException
        )
    }
}
