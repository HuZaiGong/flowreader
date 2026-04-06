package com.flowreader.app.util

import kotlinx.coroutines.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookLoader @Inject constructor() {
    
    private val ioDispatcher = Dispatchers.IO.limitedParallelism(4)
    private val bookExecutor = Executors.newFixedThreadPool(2)
    private val bookDispatcher = Dispatchers.IO
    
    fun loadChapterAsync(
        bookId: Long,
        chapterIndex: Int,
        loadContent: suspend (Long, Int) -> String?
    ): Deferred<String?> = CoroutineScope(bookDispatcher).async {
        try {
            loadContent(bookId, chapterIndex)
        } catch (e: Exception) {
            null
        }
    }
    
    fun preloadChapters(
        bookId: Long,
        currentIndex: Int,
        totalChapters: Int,
        loadContent: suspend (Long, Int) -> String?
    ) {
        CoroutineScope(bookDispatcher + SupervisorJob()).launch {
            val preloadIndices = mutableListOf<Int>()
            
            if (currentIndex > 0) preloadIndices.add(currentIndex - 1)
            if (currentIndex < totalChapters - 1) preloadIndices.add(currentIndex + 1)
            if (currentIndex > 1) preloadIndices.add(currentIndex - 2)
            if (currentIndex < totalChapters - 2) preloadIndices.add(currentIndex + 2)
            
            preloadIndices.take(3).forEach { index ->
                launch {
                    loadContent(bookId, index)
                }
            }
        }
    }
    
    fun cancelAll() {
        bookExecutor.shutdownNow()
    }
}

@Singleton
class TextPaginator @Inject constructor() {
    
    fun paginateContent(
        content: String,
        pageHeightPx: Int,
        lineHeightPx: Int,
        charsPerLine: Int
    ): List<String> {
        if (content.isEmpty() || pageHeightPx <= 0) return listOf(content)
        
        val linesPerPage = (pageHeightPx / lineHeightPx).coerceAtLeast(1)
        val pages = mutableListOf<StringBuilder>()
        
        var currentPage = StringBuilder()
        var currentLineCount = 0
        
        val paragraphs = content.split("\n\n")
        
        for (paragraph in paragraphs) {
            val paragraphLines = (paragraph.length / charsPerLine).coerceAtLeast(1)
            
            if (currentLineCount + paragraphLines > linesPerPage && currentPage.isNotEmpty()) {
                pages.add(currentPage)
                currentPage = StringBuilder()
                currentLineCount = 0
            }
            
            if (paragraphLines > linesPerPage) {
                currentPage.append(paragraph).append("\n\n")
                currentLineCount += paragraphLines
            } else {
                if (currentPage.isNotEmpty()) {
                    currentPage.append("\n\n")
                }
                currentPage.append(paragraph)
                currentLineCount += paragraphLines
            }
        }
        
        if (currentPage.isNotEmpty()) {
            pages.add(currentPage)
        }
        
        return if (pages.isEmpty()) listOf(content) else pages.map { it.toString() }
    }
    
    fun estimatePageCount(
        content: String,
        pageHeightPx: Int,
        lineHeightPx: Int,
        charsPerLine: Int
    ): Int {
        return paginateContent(content, pageHeightPx, lineHeightPx, charsPerLine).size
    }
}
