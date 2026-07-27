package com.flowreader.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MobiParserTest {

    @Test
    fun palmDocLiteralRunIsCopiedVerbatim() {
        // 0x04 = "the next 4 bytes are literal"
        val compressed = byteArrayOf(0x04, 'A'.code.toByte(), 'B'.code.toByte(), 'C'.code.toByte(), 'D'.code.toByte())
        assertEquals("ABCD", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun palmDocPlainAsciiPassesThrough() {
        val compressed = "hello".toByteArray(Charsets.US_ASCII)
        assertEquals("hello", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun palmDocSpaceCharacterPairExpandsToTwoBytes() {
        // 0xC0..0xFF => a space followed by (byte xor 0x80).
        val compressed = byteArrayOf(('a'.code or 0x80).toByte())
        assertEquals(" a", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun palmDocBackReferenceRepeatsEarlierOutput() {
        // Emit "abcd", then a back-reference of distance 4 and length 4 => "abcdabcd".
        val distance = 4
        val length = 4
        val pair = (0x80 shl 8) or ((distance shl 3) and 0x3FF8) or (length - 3)
        val compressed = byteArrayOf(
            'a'.code.toByte(),
            'b'.code.toByte(),
            'c'.code.toByte(),
            'd'.code.toByte(),
            ((pair shr 8) and 0xFF).toByte(),
            (pair and 0xFF).toByte()
        )
        assertEquals("abcdabcd", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun palmDocStopsCleanlyOnATruncatedBackReference() {
        // A trailing high byte with no companion must not throw; a corrupt import should degrade,
        // not crash the import coroutine.
        val compressed = byteArrayOf('a'.code.toByte(), 0x80.toByte())
        assertEquals("a", String(MobiParser.decompressPalmDoc(compressed), Charsets.US_ASCII))
    }

    @Test
    fun trailingEntriesAreStrippedBeforeDecompression() {
        // flags bit 1 set => one trailing entry whose size is encoded backwards in the last byte.
        val payload = "text".toByteArray(Charsets.US_ASCII)
        val record = payload + byteArrayOf(0x00, 0x00, 0x83.toByte()) // 3-byte trailing entry
        assertEquals("text", String(MobiParser.trimTrailingEntries(record, 0b10), Charsets.US_ASCII))
    }

    @Test
    fun multibyteOverlapBytesAreStripped() {
        // flags bit 0 set => the last byte's low 2 bits + 1 overlap bytes belong to the next record.
        val record = "text".toByteArray(Charsets.US_ASCII) + byteArrayOf(0x01)
        assertEquals("tex", String(MobiParser.trimTrailingEntries(record, 0b1), Charsets.US_ASCII))
    }

    @Test
    fun zeroFlagsLeaveTheRecordAlone() {
        val record = "text".toByteArray(Charsets.US_ASCII)
        assertTrue(record.contentEquals(MobiParser.trimTrailingEntries(record, 0)))
    }

    @Test
    fun garbageIsRejectedRatherThanParsed() {
        val result = MobiParser.parse(ByteArray(10))
        assertTrue(result.isFailure)
    }

    @Test
    fun drmProtectedBooksAreDeclined() {
        val bytes = buildMobi(encryption = 2)
        val result = MobiParser.parse(bytes)

        assertTrue(result.isFailure)
        val message = result.exceptionOrNull()?.message.orEmpty()
        assertTrue("expected a DRM message, got '$message'", message.contains("DRM"))
    }

    @Test
    fun huffCdicCompressionIsDeclinedInsteadOfDecodedIntoMojibake() {
        val result = MobiParser.parse(buildMobi(compression = 17480))

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("HUFF"))
    }

    @Test
    fun aWellFormedBookYieldsTitleAuthorAndText() {
        val book = MobiParser.parse(buildMobi()).getOrThrow()

        assertEquals("心流", book.title)
        assertEquals("米哈里", book.author)
        assertTrue(book.html.contains("第一章"))
        assertTrue("text must be decoded as UTF-8, not cp1252", book.html.startsWith("<html>"))
    }

    /**
     * Builds a minimal but structurally real MOBI: PDB header, record offsets, a record 0 carrying
     * the PalmDOC + MOBI + EXTH headers, and one uncompressed text record.
     */
    private fun buildMobi(
        compression: Int = 1,
        encryption: Int = 0,
        title: String = "心流",
        author: String = "米哈里",
        text: String = "<html><body><h1>第一章</h1><p>正文</p></body></html>"
    ): ByteArray {
        val titleBytes = title.toByteArray(Charsets.UTF_8)
        val authorBytes = author.toByteArray(Charsets.UTF_8)
        val textBytes = text.toByteArray(Charsets.UTF_8)

        val record0 = ByteArray(RECORD0_SIZE)
        writeShort(record0, 0, compression)
        writeInt(record0, 4, textBytes.size)
        writeShort(record0, 8, 1) // one text record
        writeShort(record0, 10, 4096)
        writeShort(record0, 12, encryption)

        "MOBI".toByteArray(Charsets.US_ASCII).copyInto(record0, 16)
        writeInt(record0, 20, MOBI_HEADER_LENGTH)
        writeInt(record0, 24, 2)
        writeInt(record0, 28, 65001) // UTF-8
        writeInt(record0, 84, FULL_NAME_OFFSET)
        writeInt(record0, 88, titleBytes.size)
        writeInt(record0, 128, 0x40) // EXTH present
        writeShort(record0, 242, 0) // no trailing entry flags
        titleBytes.copyInto(record0, FULL_NAME_OFFSET)

        val exthStart = 16 + MOBI_HEADER_LENGTH
        "EXTH".toByteArray(Charsets.US_ASCII).copyInto(record0, exthStart)
        writeInt(record0, exthStart + 4, 12 + 8 + authorBytes.size)
        writeInt(record0, exthStart + 8, 1)
        writeInt(record0, exthStart + 12, 100) // author
        writeInt(record0, exthStart + 16, 8 + authorBytes.size)
        authorBytes.copyInto(record0, exthStart + 20)

        val recordListSize = RECORD_COUNT * 8
        val dataStart = PDB_HEADER_SIZE + recordListSize
        val bytes = ByteArray(dataStart + record0.size + textBytes.size)

        "BOOK".toByteArray(Charsets.US_ASCII).copyInto(bytes, 60)
        "MOBI".toByteArray(Charsets.US_ASCII).copyInto(bytes, 64)
        writeShort(bytes, 76, RECORD_COUNT)
        writeInt(bytes, PDB_HEADER_SIZE, dataStart)
        writeInt(bytes, PDB_HEADER_SIZE + 8, dataStart + record0.size)

        record0.copyInto(bytes, dataStart)
        textBytes.copyInto(bytes, dataStart + record0.size)
        return bytes
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value shr 24) and 0xFF).toByte()
        target[offset + 1] = ((value shr 16) and 0xFF).toByte()
        target[offset + 2] = ((value shr 8) and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }

    private fun writeShort(target: ByteArray, offset: Int, value: Int) {
        target[offset] = ((value shr 8) and 0xFF).toByte()
        target[offset + 1] = (value and 0xFF).toByte()
    }

    private companion object {
        const val PDB_HEADER_SIZE = 78
        const val RECORD_COUNT = 2
        const val MOBI_HEADER_LENGTH = 232
        const val RECORD0_SIZE = 512
        const val FULL_NAME_OFFSET = 400
    }
}
