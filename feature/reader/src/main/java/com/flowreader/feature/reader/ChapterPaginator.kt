package com.flowreader.feature.reader

/**
 * Greedy text pagination for the `PAGED` page-turn mode.
 *
 * A chapter is split into blocks (title / paragraph / heading) and every block is measured
 * through an injected [measureHeight] callback so the algorithm stays pure Kotlin and JVM
 * testable; the composable side implements the callback with a real `TextMeasurer`.
 *
 * A block that does not fit the remaining page height is split on raw character boundaries —
 * the fragment keeps its absolute chapter offset, so highlights and bookmarks computed against
 * one fragment map back to the same raw text regardless of how the chapter was paged.
 */
class ChapterPaginator(private val measureHeight: (text: String, indent: Boolean, isTitle: Boolean) -> Int) {

    /** A renderable chunk of a block. [paragraphStart] is the absolute chapter offset. */
    data class Fragment(
        val text: String,
        val paragraphStart: Int,
        val indent: Boolean,
        val isTitle: Boolean,
        val isHeading: Boolean
    )

    /**
     * @param blocks raw chapter blocks with their absolute offsets ([title] is offset 0).
     * @param pageHeightPx usable content height of one page.
     * @param gapPx vertical gap between consecutive fragments inside a page.
     */
    fun paginate(blocks: List<Block>, pageHeightPx: Int, gapPx: Int): List<List<Fragment>> {
        if (blocks.isEmpty() || pageHeightPx <= 0) return emptyList()

        val pages = mutableListOf<List<Fragment>>()
        var current = mutableListOf<Fragment>()
        var remainingHeight = pageHeightPx

        fun flushPage() {
            if (current.isNotEmpty()) {
                pages.add(current)
                current = mutableListOf()
            }
            remainingHeight = pageHeightPx
        }

        blocks.forEach { block ->
            var text = block.text
            var offset = block.offset
            var firstFragment = true

            while (text.isNotEmpty()) {
                val indent = firstFragment && block.paragraph && block.indent
                val measured = measureHeight(text, indent, block.isTitle)
                val needed = measured + if (current.isEmpty()) 0 else gapPx

                if (needed <= remainingHeight) {
                    current.add(Fragment(text, offset, indent, block.isTitle, block.isHeading))
                    remainingHeight -= needed
                    text = ""
                } else if (current.isEmpty()) {
                    // Block is taller than a whole page: split it and let the overflow flow.
                    val split = findSplit(text, indent, block.isTitle, remainingHeight)
                    if (split <= 0) {
                        current.add(Fragment(text, offset, indent, block.isTitle, block.isHeading))
                        text = ""
                    } else {
                        current.add(Fragment(text.substring(0, split), offset, indent, block.isTitle, block.isHeading))
                        offset += split
                        text = text.substring(split)
                        firstFragment = false
                        flushPage()
                    }
                } else {
                    flushPage()
                }
            }
            firstFragment = false
        }

        if (current.isNotEmpty()) pages.add(current)
        return pages
    }

    /**
     * Largest raw split point whose measured height still fits [availableHeight]. Binary search
     * over the raw text length — measurement is monotonic in raw length, and display length
     * (after markdown stripping) is monotonic in raw length too.
     */
    private fun findSplit(text: String, indent: Boolean, isTitle: Boolean, availableHeight: Int): Int {
        var low = 1
        var high = text.length
        var best = 0
        while (low <= high) {
            val mid = (low + high) / 2
            if (measureHeight(text.substring(0, mid), indent, isTitle) <= availableHeight) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return best
    }
}

/** A raw chapter block: the optional title, a paragraph or a markdown heading. */
data class Block(
    val text: String,
    val offset: Int,
    val isTitle: Boolean = false,
    val isHeading: Boolean = false,
    val paragraph: Boolean = true,
    val indent: Boolean = false
)
