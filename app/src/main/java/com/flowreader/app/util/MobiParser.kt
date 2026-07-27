package com.flowreader.app.util

import org.jsoup.Jsoup
import java.nio.charset.Charset

/**
 * Read-only MOBI / PRC reader.
 *
 * A Palm database wraps a PalmDOC header, an optional MOBI header and a run of (usually LZ77
 * compressed) text records. Everything here is pure `ByteArray` arithmetic so it is unit-testable
 * on the JVM without an emulator.
 *
 * Explicitly out of scope: **DRM**. An encrypted book is reported as unreadable, never decrypted.
 * HUFF/CDIC compression is likewise declined rather than half-decoded into mojibake.
 */
object MobiParser {

    data class MobiBook(
        val title: String,
        val author: String,
        val description: String,
        val html: String
    )

    sealed class MobiFailure(val message: String) {
        object NotAMobiFile : MobiFailure("不是有效的 MOBI 文件")
        object Encrypted : MobiFailure("该 MOBI 文件受 DRM 保护，无法导入")
        object UnsupportedCompression : MobiFailure("暂不支持 HUFF/CDIC 压缩的 MOBI 文件")
        object NoText : MobiFailure("MOBI 文件中没有可读文本")
    }

    fun parse(bytes: ByteArray): Result<MobiBook> {
        if (bytes.size < PDB_HEADER_SIZE) return failure(MobiFailure.NotAMobiFile)

        val recordCount = readUShort(bytes, 76)
        if (recordCount <= 0) return failure(MobiFailure.NotAMobiFile)
        val recordListEnd = PDB_HEADER_SIZE + recordCount * 8
        if (bytes.size < recordListEnd) return failure(MobiFailure.NotAMobiFile)

        val offsets = IntArray(recordCount) { index -> readInt(bytes, PDB_HEADER_SIZE + index * 8) }
        val record0 = sliceRecord(bytes, offsets, 0, recordCount) ?: return failure(MobiFailure.NotAMobiFile)
        if (record0.size < 16) return failure(MobiFailure.NotAMobiFile)

        val compression = readUShort(record0, 0)
        val textLength = readInt(record0, 4)
        val textRecordCount = readUShort(record0, 8)
        val encryption = readUShort(record0, 12)

        if (encryption != 0) return failure(MobiFailure.Encrypted)
        if (compression == COMPRESSION_HUFF_CDIC) return failure(MobiFailure.UnsupportedCompression)
        if (compression != COMPRESSION_NONE && compression != COMPRESSION_PALMDOC) {
            return failure(MobiFailure.NotAMobiFile)
        }

        val hasMobiHeader = record0.size >= 20 && String(record0, 16, 4, Charsets.US_ASCII) == "MOBI"
        val mobiHeaderLength = if (hasMobiHeader) readInt(record0, 20) else 0
        val charset = if (hasMobiHeader) charsetFor(readInt(record0, 28)) else FALLBACK_CHARSET
        val extraDataFlags = if (hasMobiHeader && mobiHeaderLength >= 228 && record0.size >= 244) {
            readUShort(record0, 242)
        } else {
            0
        }

        val text = ByteArrayBuilder()
        for (index in 1..textRecordCount) {
            val record = sliceRecord(bytes, offsets, index, recordCount) ?: continue
            val trimmed = trimTrailingEntries(record, extraDataFlags)
            val chunk = if (compression == COMPRESSION_PALMDOC) decompressPalmDoc(trimmed) else trimmed
            text.append(chunk)
        }

        val html = String(text.trimmedTo(textLength), charset)
        if (html.isBlank()) return failure(MobiFailure.NoText)

        val fullName = if (hasMobiHeader) readFullName(record0, charset) else ""
        val exth = if (hasMobiHeader) readExth(record0, mobiHeaderLength, charset) else emptyMap()

        return Result.success(
            MobiBook(
                title = fullName.ifBlank { exth[EXTH_TITLE].orEmpty() },
                author = exth[EXTH_AUTHOR].orEmpty(),
                description = exth[EXTH_DESCRIPTION]?.let { Jsoup.parse(it).text() }.orEmpty(),
                html = html
            )
        )
    }

    /**
     * PalmDOC LZ77.
     *
     * Four token shapes: literal run (1..8), plain ASCII (0x09..0x7F), back-reference
     * (0x80..0xBF, 11-bit distance + 3-bit length) and space-plus-character (0xC0..0xFF).
     */
    fun decompressPalmDoc(data: ByteArray): ByteArray {
        val out = ByteArrayBuilder()
        var i = 0
        while (i < data.size) {
            val byte = data[i].toInt() and 0xFF
            i++
            when {
                byte == 0 -> out.append(0)
                byte < 0x09 -> {
                    val end = minOf(i + byte, data.size)
                    out.append(data, i, end - i)
                    i = end
                }
                byte < 0x80 -> out.append(byte.toByte())
                byte < 0xC0 -> {
                    if (i >= data.size) break
                    val pair = (byte shl 8) or (data[i].toInt() and 0xFF)
                    i++
                    val distance = (pair shr 3) and 0x07FF
                    val length = (pair and 0x0007) + 3
                    if (distance <= 0 || distance > out.size) break
                    out.appendBackReference(distance, length)
                }
                else -> {
                    out.append(' '.code.toByte())
                    out.append((byte xor 0x80).toByte())
                }
            }
        }
        return out.toByteArray()
    }

    /**
     * Text records may carry trailing index entries that are *not* part of the book text. Feeding
     * them to the decompressor is what produces the classic block of garbage at every 4 KB
     * boundary, so they are stripped before decompression.
     */
    fun trimTrailingEntries(record: ByteArray, extraDataFlags: Int): ByteArray {
        if (extraDataFlags == 0) return record
        var end = record.size
        var flags = extraDataFlags shr 1
        while (flags > 0) {
            if (flags and 1 == 1) {
                val size = trailingEntrySize(record, end)
                if (size <= 0 || size > end) return record.copyOfRange(0, end)
                end -= size
            }
            flags = flags shr 1
        }
        if (extraDataFlags and 1 == 1 && end > 0) {
            val overlap = (record[end - 1].toInt() and 0x03) + 1
            end = (end - overlap).coerceAtLeast(0)
        }
        return record.copyOfRange(0, end)
    }

    /** Backwards variable-width integer: the terminating byte is the one with the high bit set. */
    private fun trailingEntrySize(data: ByteArray, end: Int): Int {
        var bitpos = 0
        var result = 0
        var cursor = end
        while (cursor > 0) {
            val value = data[cursor - 1].toInt() and 0xFF
            result = result or ((value and 0x7F) shl bitpos)
            bitpos += 7
            cursor--
            if (value and 0x80 != 0 || bitpos >= 28) break
        }
        return result
    }

    private fun readFullName(record0: ByteArray, charset: Charset): String {
        if (record0.size < 92) return ""
        val offset = readInt(record0, 84)
        val length = readInt(record0, 88)
        if (offset <= 0 || length <= 0 || offset + length > record0.size) return ""
        return String(record0, offset, length, charset).trim()
    }

    private fun readExth(record0: ByteArray, mobiHeaderLength: Int, charset: Charset): Map<Int, String> {
        if (mobiHeaderLength <= 0 || record0.size < 132) return emptyMap()
        val flags = readInt(record0, 128)
        if (flags and 0x40 == 0) return emptyMap()

        val start = 16 + mobiHeaderLength
        if (start + 12 > record0.size) return emptyMap()
        if (String(record0, start, 4, Charsets.US_ASCII) != "EXTH") return emptyMap()

        val count = readInt(record0, start + 8)
        if (count <= 0 || count > MAX_EXTH_RECORDS) return emptyMap()

        val result = mutableMapOf<Int, String>()
        var cursor = start + 12
        repeat(count) {
            if (cursor + 8 > record0.size) return result
            val type = readInt(record0, cursor)
            val length = readInt(record0, cursor + 4)
            if (length < 8 || cursor + length > record0.size) return result
            if (type in EXTH_TEXT_TYPES) {
                result[type] = String(record0, cursor + 8, length - 8, charset).trim()
            }
            cursor += length
        }
        return result
    }

    private fun sliceRecord(bytes: ByteArray, offsets: IntArray, index: Int, recordCount: Int): ByteArray? {
        if (index !in offsets.indices) return null
        val start = offsets[index]
        val end = if (index + 1 < recordCount) offsets[index + 1] else bytes.size
        if (start < 0 || end > bytes.size || end <= start) return null
        return bytes.copyOfRange(start, end)
    }

    private fun charsetFor(code: Int): Charset = when (code) {
        65001 -> Charsets.UTF_8
        else -> FALLBACK_CHARSET
    }

    private fun failure(reason: MobiFailure): Result<MobiBook> = Result.failure(IllegalArgumentException(reason.message))

    private fun readInt(data: ByteArray, offset: Int): Int {
        if (offset + 4 > data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    private fun readUShort(data: ByteArray, offset: Int): Int {
        if (offset + 2 > data.size) return 0
        return ((data[offset].toInt() and 0xFF) shl 8) or (data[offset + 1].toInt() and 0xFF)
    }

    /** Growable byte buffer with the back-reference copy the LZ77 decoder needs. */
    private class ByteArrayBuilder {
        private var buffer = ByteArray(INITIAL_CAPACITY)
        var size: Int = 0
            private set

        fun append(byte: Byte) {
            ensure(size + 1)
            buffer[size] = byte
            size++
        }

        fun append(value: Int) = append(value.toByte())

        fun append(source: ByteArray, from: Int = 0, length: Int = source.size) {
            if (length <= 0) return
            ensure(size + length)
            System.arraycopy(source, from, buffer, size, length)
            size += length
        }

        fun appendBackReference(distance: Int, length: Int) {
            ensure(size + length)
            var cursor = size - distance
            repeat(length) {
                buffer[size] = buffer[cursor]
                size++
                cursor++
            }
        }

        fun toByteArray(): ByteArray = buffer.copyOf(size)

        fun trimmedTo(limit: Int): ByteArray = buffer.copyOf(if (limit in 1..size) limit else size)

        private fun ensure(capacity: Int) {
            if (capacity <= buffer.size) return
            var next = buffer.size
            while (next < capacity) next *= 2
            buffer = buffer.copyOf(next)
        }
    }

    private const val PDB_HEADER_SIZE = 78
    private const val COMPRESSION_NONE = 1
    private const val COMPRESSION_PALMDOC = 2
    private const val COMPRESSION_HUFF_CDIC = 17480
    private const val MAX_EXTH_RECORDS = 1024
    private const val INITIAL_CAPACITY = 8192
    private const val EXTH_AUTHOR = 100
    private const val EXTH_DESCRIPTION = 103
    private const val EXTH_TITLE = 503
    private val EXTH_TEXT_TYPES = setOf(EXTH_AUTHOR, EXTH_DESCRIPTION, EXTH_TITLE)
    private val FALLBACK_CHARSET: Charset =
        runCatching { Charset.forName("windows-1252") }.getOrDefault(Charsets.ISO_8859_1)
}
