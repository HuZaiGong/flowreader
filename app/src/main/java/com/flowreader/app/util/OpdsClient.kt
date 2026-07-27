package com.flowreader.app.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class OpdsLink(val href: String, val rel: String, val type: String)

data class OpdsEntry(
    val title: String,
    val author: String,
    val summary: String,
    val acquisitionUrl: String?,
    val acquisitionType: String?,
    val navigationUrl: String?
) {
    val isNavigation: Boolean get() = acquisitionUrl == null && navigationUrl != null
}

data class OpdsFeed(
    val title: String,
    val url: String,
    val entries: List<OpdsEntry>,
    val nextUrl: String?
)

/**
 * The LAN boundary for OPDS.
 *
 * FlowReader has no network layer and no account system, and the roadmap's OPDS item is scoped to
 * "仅局域网" for exactly that reason. This object is what makes that scope real rather than a
 * README promise: only loopback, RFC1918 / RFC4193 literals and `.local`-style names are allowed,
 * and every redirect hop is re-checked, so a LAN URL cannot bounce the app onto the open internet.
 */
object OpdsAddress {

    fun normalize(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        return if (isLanUrl(withScheme)) withScheme else null
    }

    fun isLanUrl(raw: String): Boolean {
        val uri = runCatching { URI(raw.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase() ?: return false
        if (scheme != "http" && scheme != "https") return false
        val host = uri.host ?: return false
        return isPrivateHost(host)
    }

    fun isPrivateHost(rawHost: String): Boolean {
        val host = rawHost.trim().lowercase().removeSurrounding("[", "]")
        if (host.isEmpty()) return false
        if (host == "localhost") return true
        if (host.contains(':')) return isPrivateIpv6(host)

        val octets = host.split('.')
        if (octets.size == 4 && octets.all { it.isNotEmpty() && it.all(Char::isDigit) }) {
            val values = octets.map { it.toInt() }
            if (values.any { it > 255 }) return false
            return isPrivateIpv4(values)
        }
        return PRIVATE_SUFFIXES.any { host.endsWith(it) }
    }

    /** Resolves a feed-relative `href` against the feed it came from, then re-applies the guard. */
    fun resolve(baseUrl: String, href: String): String? {
        val resolved = runCatching { URI(baseUrl).resolve(href.trim()).toString() }.getOrNull() ?: return null
        return resolved.takeIf { isLanUrl(it) }
    }

    private fun isPrivateIpv4(octets: List<Int>): Boolean = when {
        octets[0] == 10 -> true
        octets[0] == 127 -> true
        octets[0] == 192 && octets[1] == 168 -> true
        octets[0] == 172 && octets[1] in 16..31 -> true
        octets[0] == 169 && octets[1] == 254 -> true
        else -> false
    }

    private fun isPrivateIpv6(host: String): Boolean = when {
        host == "::1" -> true
        host.startsWith("fc") || host.startsWith("fd") -> true
        host.startsWith("fe8") || host.startsWith("fe9") || host.startsWith("fea") || host.startsWith("feb") -> true
        else -> false
    }

    private val PRIVATE_SUFFIXES = listOf(".local", ".lan", ".home", ".internal", ".localdomain")
}

/**
 * Minimal OPDS 1.x (Atom) client, restricted to the local network by [OpdsAddress].
 *
 * Deliberately built on `HttpURLConnection` and Jsoup rather than pulling in an HTTP stack: the
 * app ships no other network code and this must not become the wedge that adds one.
 */
@Singleton
class OpdsClient @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun loadCatalog(rawUrl: String): Result<OpdsFeed> = withContext(Dispatchers.IO) {
        val url = OpdsAddress.normalize(rawUrl)
            ?: return@withContext Result.failure(IllegalArgumentException(LAN_ONLY_MESSAGE))

        try {
            val (finalUrl, body) = readText(url)
            Result.success(parseFeed(body, finalUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun download(rawUrl: String, suggestedName: String): Result<File> = withContext(Dispatchers.IO) {
        if (!OpdsAddress.isLanUrl(rawUrl)) {
            return@withContext Result.failure(IllegalArgumentException(LAN_ONLY_MESSAGE))
        }

        val directory = File(context.cacheDir, "opds")
        if (!directory.exists()) directory.mkdirs()
        val target = File(directory, sanitizeFileName(suggestedName))

        val connection = openConnection(rawUrl)
        try {
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("下载失败: HTTP ${connection.responseCode}")
            }
            var total = 0L
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read <= 0) break
                        total += read
                        if (total > MAX_DOWNLOAD_BYTES) throw IllegalStateException("文件超过 200MB 上限")
                        output.write(buffer, 0, read)
                    }
                }
            }
            Result.success(target)
        } catch (e: Exception) {
            target.delete()
            Result.failure(e)
        } finally {
            connection.disconnect()
        }
    }

    /** Follows redirects by hand so every hop is re-checked against the LAN guard. */
    private fun readText(startUrl: String): Pair<String, String> {
        var current = startUrl
        repeat(MAX_REDIRECTS) {
            val connection = openConnection(current)
            try {
                val code = connection.responseCode
                if (code !in 300..399) {
                    if (code !in 200..299) throw IllegalStateException("请求失败: HTTP $code")
                    val text = connection.inputStream.bufferedReader().use { reader -> reader.readText() }
                    return current to text
                }
                val location = connection.getHeaderField("Location")
                    ?: throw IllegalStateException("重定向缺少目标地址")
                current = OpdsAddress.resolve(current, location)
                    ?: throw IllegalArgumentException(LAN_ONLY_MESSAGE)
            } finally {
                connection.disconnect()
            }
        }
        throw IllegalStateException("重定向次数过多")
    }

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Accept", "application/atom+xml, application/xml;q=0.9, */*;q=0.5")
            setRequestProperty("User-Agent", "FlowReader/53 (offline; LAN OPDS)")
        }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.replace(Regex("[^\\p{L}\\p{N}._-]"), "_").trim('_', '.')
        return cleaned.ifBlank { "opds_${System.currentTimeMillis()}.epub" }.take(120)
    }

    companion object {
        const val LAN_ONLY_MESSAGE = "只允许局域网地址（10./172.16-31./192.168./127. 或 .local 等）"

        private const val TIMEOUT_MS = 10_000
        private const val MAX_REDIRECTS = 3
        private const val BUFFER_SIZE = 8192
        private const val MAX_DOWNLOAD_BYTES = 200L * 1024 * 1024

        private val ACQUISITION_RELS = listOf(
            "http://opds-spec.org/acquisition",
            "http://opds-spec.org/acquisition/open-access"
        )

        fun parseFeed(xml: String, feedUrl: String): OpdsFeed {
            val document = Jsoup.parse(xml, feedUrl, Parser.xmlParser())
            val feedTitle = document.selectFirst("feed > title")?.text()?.trim().orEmpty()

            val nextUrl = document.select("feed > link")
                .firstOrNull { it.attr("rel") == "next" }
                ?.let { OpdsAddress.resolve(feedUrl, it.attr("href")) }

            val entries = document.select("entry").mapNotNull { entry -> parseEntry(entry, feedUrl) }
            return OpdsFeed(title = feedTitle, url = feedUrl, entries = entries, nextUrl = nextUrl)
        }

        private fun parseEntry(entry: Element, feedUrl: String): OpdsEntry? {
            val title = entry.selectFirst("title")?.text()?.trim().orEmpty()
            if (title.isEmpty()) return null

            val links = entry.select("link").map {
                OpdsLink(href = it.attr("href"), rel = it.attr("rel"), type = it.attr("type"))
            }

            val acquisition = links.firstOrNull { link ->
                ACQUISITION_RELS.any { link.rel.startsWith(it) }
            }
            val navigation = links.firstOrNull { it.type.contains("application/atom+xml") }

            return OpdsEntry(
                title = title,
                author = entry.selectFirst("author > name")?.text()?.trim().orEmpty(),
                summary = (entry.selectFirst("summary") ?: entry.selectFirst("content"))?.text()?.trim().orEmpty(),
                acquisitionUrl = acquisition?.href?.let { OpdsAddress.resolve(feedUrl, it) },
                acquisitionType = acquisition?.type,
                navigationUrl = navigation?.href?.let { OpdsAddress.resolve(feedUrl, it) }
            )
        }
    }
}
