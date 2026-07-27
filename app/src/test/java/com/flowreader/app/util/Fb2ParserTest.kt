package com.flowreader.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class Fb2ParserTest {

    @Test
    fun metadataAndSectionsAreExtracted() {
        val book = Fb2Parser.parse(fb2(SECTIONS)).getOrThrow()

        assertEquals("心流", book.title)
        assertEquals("米哈里 契克森米哈赖", book.author)
        assertEquals("关于专注的书", book.description)
        assertEquals(listOf("第一章", "第二章"), book.sections.map { it.title })
        assertTrue(book.sections[0].content.contains("第一段"))
        assertTrue(book.sections[0].content.contains("第二段"))
    }

    @Test
    fun footnoteBodiesAreNotTurnedIntoChapters() {
        val xml = fb2(SECTIONS + """<body name="notes"><section><title><p>注释</p></title><p>脚注</p></section></body>""")
        val book = Fb2Parser.parse(xml).getOrThrow()

        assertEquals(2, book.sections.size)
        assertTrue(book.sections.none { it.content.contains("脚注") })
    }

    @Test
    fun theCoverBinaryIsDecoded() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val encoded = Base64.getEncoder().encodeToString(payload)
        val xml = fb2(
            sections = SECTIONS,
            coverpage = """<coverpage><image l:href="#cover.jpg"/></coverpage>""",
            binaries = """<binary id="cover.jpg" content-type="image/jpeg">$encoded</binary>"""
        )

        val book = Fb2Parser.parse(xml).getOrThrow()

        assertNotNull(book.coverImage)
        assertTrue(payload.contentEquals(book.coverImage))
    }

    @Test
    fun aMissingCoverIsNotAnError() {
        val book = Fb2Parser.parse(fb2(SECTIONS)).getOrThrow()
        assertNull(book.coverImage)
    }

    @Test
    fun aBodyWithoutSectionsStillProducesOneChapter() {
        val book = Fb2Parser.parse(fb2("<body><p>只有正文</p></body>")).getOrThrow()

        assertEquals(1, book.sections.size)
        assertTrue(book.sections.first().content.contains("只有正文"))
    }

    @Test
    fun anEmptyBookIsRejectedRatherThanImportedBlank() {
        val result = Fb2Parser.parse(fb2("<body></body>"))
        assertTrue(result.isFailure)
    }

    @Test
    fun malformedInputFails() {
        assertTrue(Fb2Parser.parse("not xml at all").isFailure)
    }

    private fun fb2(sections: String, coverpage: String = "", binaries: String = ""): String = """
        <?xml version="1.0" encoding="utf-8"?>
        <FictionBook xmlns:l="http://www.w3.org/1999/xlink">
          <description>
            <title-info>
              <book-title>心流</book-title>
              <author><first-name>米哈里</first-name><last-name>契克森米哈赖</last-name></author>
              <annotation><p>关于专注的书</p></annotation>
              $coverpage
            </title-info>
          </description>
          $sections
          $binaries
        </FictionBook>
    """.trimIndent()

    private companion object {
        const val SECTIONS = """
            <body>
              <section><title><p>第一章</p></title><p>第一段</p><p>第二段</p></section>
              <section><title><p>第二章</p></title><p>第三段</p></section>
            </body>
        """
    }
}
