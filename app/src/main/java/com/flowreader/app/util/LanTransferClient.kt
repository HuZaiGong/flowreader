package com.flowreader.app.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URI

/**
 * Downloads a FlowReader LAN backup from another device (v55). Strictly local-address oriented:
 * only `http://` URLs are accepted (the server never exposes anything else) and the download is
 * capped so a hostile peer cannot fill the disk.
 */
object LanTransferClient {

    private const val TIMEOUT_MS = 30_000
    private const val MAX_DOWNLOAD_BYTES = 200L * 1024 * 1024

    suspend fun download(url: String, target: File): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = URI(url)
            if (uri.scheme != "http") throw IllegalArgumentException("仅支持 http:// 链接")
            val connection = (uri.toURL().openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
            }
            try {
                val code = connection.responseCode
                if (code !in 200..299) throw IllegalStateException("接收失败: HTTP $code")
                val length = connection.contentLengthLong
                if (length > MAX_DOWNLOAD_BYTES) throw IllegalStateException("备份文件过大")
                target.outputStream().use { output ->
                    val input = connection.inputStream
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_DOWNLOAD_BYTES) throw IllegalStateException("备份文件超过 200MB 上限")
                        output.write(buffer, 0, read)
                    }
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
