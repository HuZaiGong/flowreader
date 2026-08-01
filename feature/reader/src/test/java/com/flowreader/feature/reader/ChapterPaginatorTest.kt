package com.flowreader.feature.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChapterPaginatorTest {

    /** Fake measurement: height = text.length * 10px (titles count 2x). */
    private fun paginator() = ChapterPaginator { text, _, isTitle ->
        text.length * 10 * (if (isTitle) 2 else 1)
    }

    private fun block(text: String, offset: Int = 0, title: Boolean = false, indent: Boolean = false) =
        Block(text = text, offset = offset, isTitle = title, indent = indent)

    @Test
    fun shortParagraphsShareOnePage() {
        val pages = paginator().paginate(
            blocks = listOf(block("a".repeat(5)), block("b".repeat(5), offset = 5)),
            pageHeightPx = 100,
            gapPx = 0
        )
        assertEquals(1, pages.size)
        assertEquals(2, pages[0].size)
        assertEquals(0, pages[0][0].paragraphStart)
        assertEquals(5, pages[0][1].paragraphStart)
    }

    @Test
    fun longParagraphSplitsAcrossPages() {
        val pages = paginator().paginate(
            blocks = listOf(block("x".repeat(60))),
            pageHeightPx = 100,
            gapPx = 0
        )
        // 10 chars per page (100px): 6 full pages
        assertEquals(6, pages.size)
        pages.forEach { page ->
            assertEquals(10, page[0].text.length)
        }
        // absolute offsets stay monotonic across the fragments
        assertEquals(0, pages[0][0].paragraphStart)
        assertEquals(10, pages[1][0].paragraphStart)
        assertEquals(50, pages[5][0].paragraphStart)
    }

    @Test
    fun blockTallerThanPageSplitsAndOverflowFlows() {
        val pages = paginator().paginate(
            blocks = listOf(block("x".repeat(13))),
            pageHeightPx = 100,
            gapPx = 0
        )
        assertEquals(2, pages.size)
        assertEquals(10, pages[0][0].text.length)
        assertEquals(3, pages[1][0].text.length)
        assertEquals(10, pages[1][0].paragraphStart)
    }

    @Test
    fun fragmentThatDoesNotFitRemainingHeightFlowsToNextPage() {
        val pages = paginator().paginate(
            blocks = listOf(block("a".repeat(6)), block("b".repeat(6), offset = 6)),
            pageHeightPx = 70,
            gapPx = 0
        )
        // a = 60px fits; b needs 60 but only 10 remain -> next page
        assertEquals(2, pages.size)
        assertEquals("a".repeat(6), pages[0][0].text)
        assertEquals("b".repeat(6), pages[1][0].text)
    }

    @Test
    fun titleSitsOnFirstPageWithFollowingContent() {
        val pages = paginator().paginate(
            blocks = listOf(
                block("章节", title = true),
                block("c".repeat(5), offset = 2, indent = true)
            ),
            pageHeightPx = 100,
            gapPx = 0
        )
        assertEquals(1, pages.size)
        assertTrue(pages[0][0].isTitle)
        assertTrue(pages[0][1].indent)
        assertEquals(2, pages[0][1].paragraphStart)
    }

    @Test
    fun emptyBlocksProduceNoPages() {
        assertTrue(paginator().paginate(emptyList(), 100, 0).isEmpty())
        assertTrue(paginator().paginate(listOf(Block("", 0)), 100, 0).isEmpty())
    }

    @Test
    fun zeroPageHeightProducesNoPages() {
        assertTrue(paginator().paginate(listOf(block("abc")), 0, 0).isEmpty())
    }

    @Test
    fun gapReducesHowMuchFitsPerPage() {
        val blocks = listOf(
            block("a".repeat(3)),
            block("b".repeat(3), offset = 3),
            block("c".repeat(3), offset = 6)
        )
        // 30px per block: without gap all three fit on one page; with 20px gap only two do
        assertEquals(1, paginator().paginate(blocks, 100, 0).size)
        assertEquals(2, paginator().paginate(blocks, 100, 20).size)
    }
}
