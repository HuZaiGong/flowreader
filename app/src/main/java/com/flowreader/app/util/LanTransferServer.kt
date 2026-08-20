package com.flowreader.app.util

import java.io.BufferedOutputStream
import java.io.File
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.security.SecureRandom
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.concurrent.thread

/**
 * Minimal LAN backup server (v55): serves one exported backup file over plain HTTP on a random
 * local port. The URL carries a random 16-hex token so casual peers on the same network cannot
 * guess it; the server only ever answers the exact `/backup/<token>` path.
 *
 * Offline-first by design: no internet involvement, works on a shared WiFi with no router
 * configuration.
 */
class LanTransferServer(private val file: File) {

    @Volatile
    private var serverSocket: ServerSocket? = null

    @Volatile
    private var token: String = ""

    @Volatile
    private var active = false

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    val isRunning: Boolean get() = active

    /** The serving URL once [start] succeeded. */
    @Volatile
    var url: String? = null
        private set

    fun start(): String? {
        if (active) return url
        val bytes = file.readBytesOrNull() ?: return null
        val ip = localIpv4Address() ?: return null
        token = TOKEN_CHARS.generateToken(16)

        return try {
            // Bind the discovered LAN interface only — a cellular/other-interface peer cannot
            // reach the server even with the token.
            val socket = ServerSocket()
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(ip, 0))
            serverSocket = socket
            active = true
            val port = socket.localPort
            url = "http://$ip:$port/backup/$token"
            executor.execute { serve(socket, bytes) }
            url
        } catch (e: Exception) {
            active = false
            serverSocket = null
            null
        }
    }

    fun stop() {
        active = false
        serverSocket?.close()
        serverSocket = null
        url = null
        executor.shutdownNow()
    }

    private fun serve(socket: ServerSocket, payload: ByteArray) {
        while (active) {
            val client = try {
                socket.accept()
            } catch (e: Exception) {
                break
            }
            try {
                // One worker thread serves every peer, so a connection that never sends a request
                // line would hold the server for as long as the dialog stays open. A read timeout
                // bounds that to five seconds per peer instead of forever.
                client.soTimeout = CLIENT_TIMEOUT_MS
                client.use { connection ->
                    val reader = connection.getInputStream().bufferedReader()
                    val requestLine = reader.readLine() ?: return@use
                    val accepted = isBackupRequest(requestLine, token)
                    val output = BufferedOutputStream(connection.getOutputStream())
                    if (accepted) {
                        val headers = buildString {
                            append("HTTP/1.1 200 OK\r\n")
                            append("Content-Type: application/json\r\n")
                            append("Content-Length: ${payload.size}\r\n")
                            append("Connection: close\r\n")
                            append("X-FlowReader-Backup: 1\r\n")
                            append("\r\n")
                        }
                        output.write(headers.toByteArray(Charsets.US_ASCII))
                        output.write(payload)
                    } else {
                        val body = "404 Not Found".toByteArray(Charsets.US_ASCII)
                        val headers = buildString {
                            append("HTTP/1.1 404 Not Found\r\n")
                            append("Content-Length: ${body.size}\r\n")
                            append("Connection: close\r\n")
                            append("\r\n")
                        }
                        output.write(headers.toByteArray(Charsets.US_ASCII))
                        output.write(body)
                    }
                    output.flush()
                }
            } catch (e: Exception) {
                // A single broken or timed-out peer must not kill the server; `client.use` above
                // has already closed the connection by the time we get here.
            }
        }
        runCatching { socket.close() }
    }

    private fun File.readBytesOrNull(): ByteArray? =
        try {
            if (length() > MAX_PAYLOAD_BYTES) null else readBytes()
        } catch (e: Exception) {
            null
        }

    companion object {
        private const val MAX_PAYLOAD_BYTES = 200L * 1024 * 1024
        private const val TOKEN_CHARS = "0123456789abcdef"
        private const val CLIENT_TIMEOUT_MS = 5_000

        /**
         * Exact match on the request line, which is what the class KDoc has always claimed.
         *
         * `requestLine.contains("/backup/$token")` accepted `GET /anything/backup/<token>` and
         * `GET /backup/<token>extra` as well. Not a token leak on its own — you still need the
         * token — but the guarantee documented above should be the one the code actually enforces,
         * and a loose match here is the kind of thing a later path-handling change builds on.
         *
         * HTTP/1.0 is accepted because `HttpURLConnection` may downgrade, and a bare
         * `GET <path>` (HTTP/0.9 style) is not.
         */
        internal fun isBackupRequest(requestLine: String, token: String): Boolean {
            if (token.isEmpty()) return false
            val expected = "/backup/$token"
            return requestLine == "GET $expected HTTP/1.1" || requestLine == "GET $expected HTTP/1.0"
        }

        fun localIpv4Address(): String? = runCatching {
            NetworkInterface.getNetworkInterfaces()
                .asSequence()
                .filter { it.isUp && !it.isLoopback }
                .flatMap { it.inetAddresses.asSequence() }
                .filterIsInstance<Inet4Address>()
                .firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        }.getOrNull()

        private fun String.generateToken(length: Int): String {
            val random = SecureRandom()
            val charset = this
            return buildString {
                repeat(length) { append(charset[random.nextInt(charset.length)]) }
            }
        }
    }
}
