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
                client.use { connection ->
                    val reader = connection.getInputStream().bufferedReader()
                    val requestLine = reader.readLine() ?: return@use
                    val expectedPath = "/backup/$token"
                    val accepted = requestLine.startsWith("GET ") && requestLine.contains(expectedPath)
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
                // A single broken peer must not kill the server.
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
            return buildString {
                repeat(length) { append(this@generateToken[random.nextInt(length)]) }
            }
        }
    }
}
