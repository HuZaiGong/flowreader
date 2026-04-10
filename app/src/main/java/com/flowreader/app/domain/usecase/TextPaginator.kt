package com.flowreader.app.domain.usecase

data class TextPage(
    val pageIndex: Int,
    val content: String,
    val isFirstPage: Boolean = false,
    val isLastPage: Boolean = false
)

class TextPaginator(
    private val chapterContent: String,
    private val charsPerPage: Int = 3000
) {
    private val pages: List<TextPage>
    
    init {
        pages = generatePages()
    }
    
    private fun generatePages(): List<TextPage> {
        if (chapterContent.isEmpty()) return emptyList()
        
        val paragraphs = chapterContent.split("\n\n")
        val result = mutableListOf<String>()
        var currentPage = StringBuilder()
        var currentLength = 0
        
        for (paragraph in paragraphs) {
            val paraLen = paragraph.length
            if (currentLength + paraLen > charsPerPage && currentLength > 0) {
                result.add(currentPage.toString())
                currentPage = StringBuilder()
                currentLength = 0
            }
            currentPage.append(paragraph)
            currentLength += paraLen
        }
        
        if (currentPage.isNotEmpty()) {
            result.add(currentPage.toString())
        }
        
        return result.mapIndexed { index, content ->
            TextPage(
                pageIndex = index,
                content = content,
                isFirstPage = index == 0,
                isLastPage = index == result.lastIndex
            )
        }
    }
    
    fun getPage(index: Int): TextPage? = pages.getOrNull(index)
    
    fun getPages(): List<TextPage> = pages
    
    fun getPageCount(): Int = pages.size
    
    fun preloadIndices(currentPage: Int, preloadCount: Int = 2): List<Int> {
        val indices = mutableListOf<Int>()
        for (i in 1..preloadCount) {
            if (currentPage + i < pages.size) indices.add(currentPage + i)
            if (currentPage - i >= 0) indices.add(currentPage - i)
        }
        return indices.distinct()
    }
}