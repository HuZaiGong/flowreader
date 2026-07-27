package com.flowreader.app.core.util

/**
 * Deterministic placeholder cover art.
 *
 * TXT / Markdown / FB2 imports rarely carry an embedded image, and every one of them used to
 * render the same grey book glyph — a shelf of twenty was indistinguishable at a glance. These are
 * pure functions of the title so a book keeps the same generated cover forever, across reinstalls.
 */
object CoverArt {

    /** Stable 32-bit FNV-1a hash. Deterministic across processes, unlike [String.hashCode] seeds. */
    fun hash(seed: String): Int {
        var value = FNV_OFFSET_BASIS
        for (char in seed) {
            value = value xor char.code
            value *= FNV_PRIME
        }
        return value
    }

    /** Index into a palette of [paletteSize] entries. Always inside `0 until paletteSize`. */
    fun paletteIndex(seed: String, paletteSize: Int): Int {
        if (paletteSize <= 0) return 0
        val raw = hash(seed) % paletteSize
        return if (raw < 0) raw + paletteSize else raw
    }

    /**
     * The glyphs stamped on a generated cover: one character for CJK titles, up to two Latin
     * initials otherwise. Never returns an empty string — a blank cover reads as a render bug.
     */
    fun initials(title: String): String {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return "?"
        val first = trimmed.first()
        if (first.code > LATIN_MAX) return first.toString()
        val words = trimmed.split(' ', '_', '-', '.').filter { it.isNotBlank() }
        if (words.isEmpty()) return first.uppercaseChar().toString()
        return words.take(2).map { it.first().uppercaseChar() }.joinToString(separator = "")
    }

    private const val FNV_OFFSET_BASIS = -0x7ee3623b
    private const val FNV_PRIME = 16777619

    /** Everything above the Latin/Greek/Cyrillic blocks is treated as ideographic. */
    private const val LATIN_MAX = 0x2E80
}
