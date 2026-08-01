package com.flowreader.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.flowreader.app.domain.model.Book
import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.Chapter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import java.io.*
import java.util.Locale
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

sealed class ParseProgress {
    object Starting : ParseProgress()
    data class Reading(val bytesRead: Long, val totalBytes: Long) : ParseProgress()
    object Parsing : ParseProgress()
    object Saving : ParseProgress()
    object Complete : ParseProgress()
    data class Error(val message: String) : ParseProgress()
}

data class BookParseResult(
    val book: Book,
    val chapters: List<Chapter>,
    val pdfPageCount: Int = 0,
    val pdfFilePath: String? = null
)

@Singleton
class BookParser @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val bufferSize = 8192

    // Keep chapter rows safely below Android CursorWindow limits. Large TXT/Markdown
    // files without headings used to be stored as one huge chapter and could crash
    // when Room read the content back for the reader screen.
    private val maxChapterChars = 8_000

    /** Single EPUB XHTML entry cap; a pathological 100MB+ book must not blow up memory. */
    private val maxChapterReadBytes = 16L * 1024 * 1024

    /** Single embedded image cap; oversized art is skipped instead of loaded. */
    private val maxSingleImageBytes = 24L * 1024 * 1024

    /** Whole-document cap for TXT / Markdown / FB2 / MOBI single-file formats. */
    private val maxWholeFileBytes = 128L * 1024 * 1024

    suspend fun parseBook(uri: Uri): Result<BookParseResult> = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("无法打开文件"))

            val fileName = getFileName(uri)
            val format = detectFormat(fileName)
            val fileSize = getFileSize(uri)

            when (format) {
                BookFormat.EPUB -> {
                    val coverInputStream = context.contentResolver.openInputStream(uri)
                        ?: inputStream
                    val coverPath = extractEpubCover(coverInputStream, fileName.removeSuffix(".epub"))
                    val result = parseEpubStream(inputStream, fileName, fileSize)
                    result.map { it.copy(book = it.book.copy(coverPath = coverPath)) }
                }
                BookFormat.TXT -> parseTxtStream(inputStream, fileName, fileSize)
                BookFormat.MARKDOWN -> parseMarkdownStream(inputStream, fileName, fileSize)
                BookFormat.PDF -> {
                    val result = parsePdfStream(uri, fileName, fileSize)
                    result.map { it.copy(book = it.book.copy(coverPath = extractPdfCover(uri))) }
                }
                BookFormat.FB2 -> parseFb2Stream(inputStream, fileName, fileSize)
                BookFormat.MOBI -> parseMobiStream(inputStream, fileName, fileSize)
                BookFormat.COMIC -> parseComicImageStream(inputStream, fileName, fileSize)
                BookFormat.UNKNOWN -> {
                    if (fileName.endsWith(".zip", ignoreCase = true) || fileName.endsWith(".cbz", ignoreCase = true)) {
                        parseComicZipStream(inputStream, fileName, fileSize)
                    } else {
                        Result.failure(Exception("不支持的格式: $format"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun parseBookWithProgress(uri: Uri) = flow {
        emit(ParseProgress.Starting)

        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val fileName = getFileName(uri)
                val format = detectFormat(fileName)
                val fileSize = getFileSize(uri)

                when (format) {
                    BookFormat.EPUB -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = parseEpubStream(inputStream, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.TXT -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = parseTxtStream(inputStream, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.PDF -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        inputStream.close()
                        val result = parsePdfStream(uri, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.MARKDOWN -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = parseMarkdownStream(inputStream, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.FB2 -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = parseFb2Stream(inputStream, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.MOBI -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = parseMobiStream(inputStream, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.COMIC -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = parseComicImageStream(inputStream, fileName, fileSize)
                        emit(ParseProgress.Parsing)
                        result
                    }
                    BookFormat.UNKNOWN -> {
                        emit(ParseProgress.Reading(0, fileSize))
                        val result = if (fileName.endsWith(".zip", ignoreCase = true) || fileName.endsWith(".cbz", ignoreCase = true)) {
                            parseComicZipStream(inputStream, fileName, fileSize)
                        } else {
                            Result.failure(Exception("不支持的格式: $format"))
                        }
                        emit(ParseProgress.Parsing)
                        result
                    }
                }.onSuccess {
                    emit(ParseProgress.Complete)
                }.onFailure {
                    emit(ParseProgress.Error(it.message ?: "解析失败"))
                }
            } ?: throw Exception("无法打开文件")
        } catch (e: Exception) {
            emit(ParseProgress.Error(e.message ?: "解析失败"))
        }
    }.flowOn(Dispatchers.IO)

    private fun detectFormat(fileName: String): BookFormat {
        return detectFormatStatic(fileName)
    }

    companion object {
        fun detectFormatStatic(fileName: String): BookFormat {
            return when {
                fileName.endsWith(".epub", ignoreCase = true) -> BookFormat.EPUB
                fileName.endsWith(".txt", ignoreCase = true) -> BookFormat.TXT
                fileName.endsWith(".pdf", ignoreCase = true) -> BookFormat.PDF
                fileName.endsWith(".md", ignoreCase = true) || fileName.endsWith(".markdown", ignoreCase = true) -> BookFormat.MARKDOWN
                fileName.endsWith(".fb2", ignoreCase = true) || fileName.endsWith(".fb2.zip", ignoreCase = true) -> BookFormat.FB2
                // .azw is DRM-free MOBI; an encrypted one is rejected by MobiParser, not decrypted.
                fileName.endsWith(".mobi", ignoreCase = true) ||
                    fileName.endsWith(".prc", ignoreCase = true) ||
                    fileName.endsWith(".azw", ignoreCase = true) -> BookFormat.MOBI
                isComicImageName(fileName) -> BookFormat.COMIC
                fileName.endsWith(".cbz", ignoreCase = true) -> BookFormat.UNKNOWN
                else -> BookFormat.UNKNOWN
            }
        }

        fun isComicImageName(fileName: String): Boolean {
            val lower = fileName.lowercase(Locale.ROOT)
            return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") || lower.endsWith(".webp")
        }

        fun naturalComicSortKey(name: String): List<String> =
            Regex("\\d+|\\D+").findAll(name.lowercase(Locale.ROOT)).map { match ->
                val value = match.value
                value.toLongOrNull()?.toString()?.padStart(12, '0') ?: value
            }.toList()
    }

    /**
     * The display name the picker reports, resolved off the main thread. Callers need this before
     * parsing to tell a book apart from a `.zip` of books.
     */
    suspend fun displayName(uri: Uri): String = withContext(Dispatchers.IO) { getFileName(uri) }

    /**
     * `ContentResolver.query` returns nothing for `file://` URIs, which is how batch-extracted ZIP
     * entries and OPDS downloads arrive. Falling back to the path keeps their titles and sizes
     * real instead of every one of them importing as "未知书籍" with size 0.
     */
    private fun getFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex >= 0) {
                    cursor.getString(displayNameIndex)?.takeIf { it.isNotBlank() }?.let { return it }
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "未知书籍"
    }

    private fun getFileSize(uri: Uri): Long {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {
                    val size = cursor.getLong(sizeIndex)
                    if (size > 0) return size
                }
            }
        }
        return uri.path?.let { path -> File(path).takeIf { it.isFile }?.length() } ?: 0L
    }

    private fun parseEpubStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val tempFile = File(context.cacheDir, "temp_epub_parse_${System.currentTimeMillis()}.zip")
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            val title = fileName.removeSuffix(".epub")
            val chapters = mutableListOf<Chapter>()
            var author = "未知作者"
            var description = ""

            ZipFile(tempFile).use { zip ->
                val containerEntry = zip.getEntry("META-INF/container.xml")
                    ?: return@use
                val containerXml = Jsoup.parse(zip.getInputStream(containerEntry).bufferedReader().readText())
                val opfPath = containerXml.select("rootfile").attr("full-path")
                if (opfPath.isBlank()) return@use

                val opfEntry = zip.getEntry(opfPath) ?: return@use
                val opfDoc = Jsoup.parse(zip.getInputStream(opfEntry).bufferedReader().readText())

                opfDoc.select("creator").firstOrNull()?.let { author = it.text() }
                opfDoc.select("description").firstOrNull()?.let { description = it.text() }

                val opfBaseDir = opfPath.substringBeforeLast("/", "")
                val itemMap = mutableMapOf<String, String>()
                opfDoc.select("manifest > item").forEach { item ->
                    val itemId = item.attr("id")
                    val href = item.attr("href")
                    itemMap[itemId] = if (opfBaseDir.isNotEmpty()) "$opfBaseDir/$href" else href
                }

                val spineRefs = mutableListOf<String>()
                opfDoc.select("spine > itemref").forEach { ref ->
                    val refId = ref.attr("idref")
                    itemMap[refId]?.let { spineRefs.add(it) }
                }

                val imageDir = File(context.cacheDir, "epub_images_${System.currentTimeMillis()}")
                imageDir.mkdirs()
                val imageHrefs = mutableMapOf<String, String>()
                opfDoc.select("manifest > item").forEach { item ->
                    val href = item.attr("href")
                    val mediaType = item.attr("media-type")
                    if (mediaType.startsWith("image/")) {
                        val entryPath = if (opfBaseDir.isNotEmpty()) "$opfBaseDir/$href" else href
                        val imageEntry = zip.getEntry(entryPath)
                        if (imageEntry != null) {
                            val imageBytes = readCappedBytes(zip.getInputStream(imageEntry), maxSingleImageBytes) ?: return@forEach
                            val fileName = "img_${href.replace("/", "_").replace("\\", "_")}"
                            val imageFile = File(imageDir, fileName)
                            FileOutputStream(imageFile).use { it.write(imageBytes) }
                            imageHrefs[href] = imageFile.absolutePath
                        }
                    }
                }

                var chapterIndex = 0
                for (href in spineRefs) {
                    val entry = zip.getEntry(href) ?: continue
                    val htmlText = readCappedText(zip.getInputStream(entry), maxChapterReadBytes) ?: continue
                    val htmlDoc = Jsoup.parse(htmlText)
                    htmlDoc.outputSettings().syntax(org.jsoup.nodes.Document.OutputSettings.Syntax.xml)
                    htmlDoc.select("script, style, nav").remove()

                    val bodyText = htmlToFormattedText(htmlDoc.body(), imageHrefs).trim()
                    if (bodyText.isBlank()) continue

                    val chapterTitle = htmlDoc.select("title").firstOrNull()?.text()
                        ?: "第 ${chapterIndex + 1} 章"

                    chapterIndex = appendChunkedChapter(
                        chapters = chapters,
                        title = chapterTitle,
                        content = bodyText,
                        startPosition = 0,
                        startIndex = chapterIndex
                    )
                }
            }

            if (tempFile.exists()) {
                tempFile.delete()
            }

            if (chapters.isEmpty()) {
                return Result.failure(Exception("未找到可解析的章节内容"))
            }

            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = author,
                        filePath = "",
                        coverPath = null,
                        description = description,
                        fileSize = fileSize,
                        format = BookFormat.EPUB,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractEpubCover(inputStream: InputStream, bookTitle: String): String? {
        return try {
            val tempFile = File(context.cacheDir, "temp_epub_${System.currentTimeMillis()}.zip")
            inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }

            ZipInputStream(FileInputStream(tempFile)).use { zipInput ->
                var entry = zipInput.nextEntry
                var coverImage: ByteArray? = null
                var coverName: String? = null

                while (entry != null) {
                    val lowerName = entry.name.lowercase()
                    if (lowerName.contains("cover") && (lowerName.endsWith(".jpg") || lowerName.endsWith(".png") || lowerName.endsWith(".jpeg"))) {
                        coverImage = zipInput.readBytes()
                        coverName = entry.name
                        entry = zipInput.nextEntry
                    } else if (lowerName.endsWith(".opf")) {
                        val opfContent = zipInput.bufferedReader().readText()
                        val coverMeta = Regex("item[^>]*href=\"([^\"]+cover[^\"]*\\.(jpg|png|jpeg))\"", RegexOption.IGNORE_CASE)
                            .find(opfContent)
                        if (coverMeta != null) {
                            val coverPath = coverMeta.groupValues[1]
                            val zip = java.util.zip.ZipFile(tempFile)
                            val coverEntry = zip.getEntry(coverPath)
                            if (coverEntry != null) {
                                coverImage = zip.getInputStream(coverEntry).readBytes()
                            }
                            zip.close()
                        }
                    }
                    entry = zipInput.nextEntry
                }

                if (tempFile.exists()) {
                    tempFile.delete()
                }

                coverImage?.let { saveCoverImage(it, bookTitle) }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun extractPdfCover(uri: Uri): String? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val bitmap = Bitmap.createBitmap(
                                page.width * 2,
                                page.height * 2,
                                Bitmap.Config.ARGB_8888
                            )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            val coversDir = File(context.filesDir, "covers")
                            if (!coversDir.exists()) {
                                coversDir.mkdirs()
                            }

                            val fileName = "pdf_cover_${System.currentTimeMillis()}.jpg"
                            val file = File(coversDir, fileName)

                            FileOutputStream(file).use { fos ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                            }

                            bitmap.recycle()
                            file.absolutePath
                        }
                    } else null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun htmlToFormattedText(
        element: Element,
        imageHrefs: Map<String, String>
    ): String {
        val sb = StringBuilder()
        for (node in element.childNodes()) {
            when (node) {
                is TextNode -> {
                    val text = node.text()
                    if (text.isNotBlank()) sb.append(text)
                }
                is Element -> {
                    val tag = node.tagName()
                    when {
                        tag == "br" -> sb.appendLine()
                        tag.startsWith("h") && tag.length == 2 && tag[1] in '1'..'6' -> {
                            sb.appendLine()
                            sb.append("## ")
                            sb.append(htmlToFormattedText(node, imageHrefs))
                            sb.appendLine()
                        }
                        tag == "b" || tag == "strong" -> {
                            sb.append("**")
                            sb.append(htmlToFormattedText(node, imageHrefs))
                            sb.append("**")
                        }
                        tag == "i" || tag == "em" -> {
                            sb.append("*")
                            sb.append(htmlToFormattedText(node, imageHrefs))
                            sb.append("*")
                        }
                        tag == "img" -> {
                            val src = node.attr("src")
                            val savedPath = imageHrefs[src]
                            if (savedPath != null) {
                                sb.appendLine()
                                sb.append("[IMG:$savedPath]")
                                sb.appendLine()
                            }
                        }
                        tag == "p" || tag == "div" -> {
                            val inner = htmlToFormattedText(node, imageHrefs).trim()
                            if (inner.isNotBlank()) {
                                sb.appendLine(inner)
                                sb.appendLine()
                            }
                        }
                        tag == "span" || tag == "a" || tag == "li" -> {
                            sb.append(htmlToFormattedText(node, imageHrefs))
                        }
                        tag == "ul" || tag == "ol" -> {
                            sb.appendLine()
                            sb.append(htmlToFormattedText(node, imageHrefs))
                            sb.appendLine()
                        }
                        else -> {
                            sb.append(htmlToFormattedText(node, imageHrefs))
                        }
                    }
                }
            }
        }
        return sb.toString()
    }

    private fun parseTxtStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val text = inputStream.readCappedTextNotNull(maxWholeFileBytes)

            val title = fileName.removeSuffix(".txt")
            val chapters = mutableListOf<Chapter>()

            val lines = text.lines()
            val chapterPattern = Regex("^(第[一二三四五六七八九十\\d]+[章节卷部分篇]|#|Chapter|CHAPTER)\\s*.*")

            var currentChapterTitle = "前言"
            var currentContent = StringBuilder()
            var chapterIndex = 0

            for (line in lines) {
                val trimmedLine = line.trim()
                if (chapterPattern.containsMatchIn(trimmedLine) && trimmedLine.length < 100) {
                    if (currentContent.isNotBlank()) {
                        chapterIndex = appendChunkedChapter(
                            chapters = chapters,
                            title = currentChapterTitle,
                            content = currentContent.toString().trim(),
                            startPosition = 0,
                            startIndex = chapterIndex
                        )
                    }
                    currentChapterTitle = trimmedLine
                    currentContent = StringBuilder()
                } else {
                    currentContent.appendLine(line)
                }
            }

            if (currentContent.isNotBlank()) {
                appendChunkedChapter(
                    chapters = chapters,
                    title = currentChapterTitle,
                    content = currentContent.toString().trim(),
                    startPosition = 0,
                    startIndex = chapterIndex
                )
            }

            if (chapters.isEmpty()) {
                appendChunkedChapter(
                    chapters = chapters,
                    title = "全部内容",
                    content = text,
                    startPosition = 0
                )
            }

            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = "未知作者",
                        filePath = "",
                        fileSize = fileSize,
                        format = BookFormat.TXT,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun saveCoverImage(imageData: ByteArray, bookTitle: String): String? {
        return try {
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }

            val fileName = bookTitle.replace(Regex("[^a-zA-Z0-9]"), "_") + ".jpg"
            val file = File(coversDir, fileName)

            FileOutputStream(file).use { fos ->
                fos.write(imageData)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun copyFileToInternal(uri: Uri): String? {
        return try {
            val booksDir = File(context.filesDir, "books")
            if (!booksDir.exists()) {
                booksDir.mkdirs()
            }

            val fileName = getFileName(uri)
            val file = File(booksDir, fileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun parsePdfStream(uri: Uri, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val internalPath = copyFileToInternal(uri)
                ?: return Result.failure(Exception("无法保存PDF文件"))

            val pageCount = context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    renderer.pageCount
                }
            } ?: 0

            val title = fileName.removeSuffix(".pdf")

            val chapters = (0 until pageCount).map { pageIndex ->
                Chapter(
                    bookId = 0,
                    index = pageIndex,
                    title = "第 ${pageIndex + 1} 页",
                    content = "",
                    startPosition = pageIndex,
                    endPosition = pageIndex
                )
            }

            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = "未知作者",
                        filePath = internalPath,
                        fileSize = fileSize,
                        format = BookFormat.PDF,
                        totalChapters = pageCount
                    ),
                    chapters = chapters,
                    pdfPageCount = pageCount,
                    pdfFilePath = internalPath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseComicImageStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val title = fileName.substringBeforeLast('.')
            val directory = File(context.filesDir, "comics/${System.currentTimeMillis()}_${sanitizeFileName(title)}")
            if (!directory.mkdirs() && !directory.isDirectory) {
                return Result.failure(Exception("无法创建漫画目录"))
            }
            val image = File(directory, uniqueComicFileName(fileName, 0))
            inputStream.use { input -> FileOutputStream(image).use { output -> input.copyTo(output) } }

            val chapter = comicChapter(index = 0, title = "第 1 页", imagePath = image.absolutePath)
            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = "未知作者",
                        filePath = directory.absolutePath,
                        coverPath = image.absolutePath,
                        fileSize = fileSize,
                        format = BookFormat.COMIC,
                        totalChapters = 1
                    ),
                    chapters = listOf(chapter),
                    pdfFilePath = directory.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseComicZipStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        val tempFile = File(context.cacheDir, "temp_comic_${System.currentTimeMillis()}.zip")
        return try {
            inputStream.use { input -> FileOutputStream(tempFile).use { output -> input.copyTo(output) } }

            val title = fileName.removeSuffixIgnoreCase(".zip").removeSuffixIgnoreCase(".cbz")
            val directory = File(context.filesDir, "comics/${System.currentTimeMillis()}_${sanitizeFileName(title)}")
            if (!directory.mkdirs() && !directory.isDirectory) {
                return Result.failure(Exception("无法创建漫画目录"))
            }

            val imageEntries = ZipFile(tempFile).use { zip ->
                zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .mapNotNull { entry -> ZipImportRules.safeComicImageName(entry.name, isDirectory = false)?.let { entry.name to it } }
                    .sortedWith { left, right -> compareNatural(left.second, right.second) }
                    .take(ZipImportRules.MAX_ENTRIES)
                    .toList()
            }
            if (imageEntries.isEmpty()) {
                directory.deleteRecursively()
                return Result.failure(Exception("压缩包中没有支持的图片"))
            }

            val images = mutableListOf<File>()
            ZipFile(tempFile).use { zip ->
                imageEntries.forEachIndexed { index, (entryName, name) ->
                    val target = File(directory, uniqueComicFileName(name, index))
                    val entry = zip.getEntry(entryName) ?: return@forEachIndexed
                    val written = zip.getInputStream(entry).use { input -> copyCapped(input, target, ZipImportRules.MAX_ENTRY_BYTES) }
                    if (written > 0) images.add(target) else target.delete()
                }
            }
            if (images.isEmpty()) {
                directory.deleteRecursively()
                return Result.failure(Exception("压缩包中的图片超过大小限制"))
            }

            val chapters = images.mapIndexed { index, image -> comicChapter(index, "第 ${index + 1} 页", image.absolutePath) }
            Result.success(
                BookParseResult(
                    book = Book(
                        title = title.ifBlank { fileName.substringBeforeLast('.') },
                        author = "未知作者",
                        filePath = directory.absolutePath,
                        coverPath = images.firstOrNull()?.absolutePath,
                        fileSize = fileSize,
                        format = BookFormat.COMIC,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters,
                    pdfFilePath = directory.absolutePath
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tempFile.delete()
        }
    }

    private fun comicChapter(index: Int, title: String, imagePath: String): Chapter =
        Chapter(
            bookId = 0,
            index = index,
            title = title,
            content = "[COMIC:$imagePath]",
            startPosition = index,
            endPosition = index + 1
        )

    /**
     * Reads the whole stream into memory up to [limit] bytes; null when the stream is larger
     * (the caller then skips the entry or fails the parse with a clear message).
     */
    private fun readCappedBytes(stream: InputStream, limit: Long): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(bufferSize)
        var total = 0L
        while (true) {
            val read = stream.read(buffer)
            if (read <= 0) break
            total += read
            if (total > limit) return null
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun readCappedText(stream: InputStream, limit: Long): String? =
        readCappedBytes(stream, limit)?.toString(Charsets.UTF_8)

    private fun InputStream.readCappedTextNotNull(limit: Long): String =
        readCappedText(this, limit) ?: throw IllegalStateException("文件过大：超过 ${limit / 1024 / 1024}MB 上限")

    private fun InputStream.readCappedBytesNotNull(limit: Long): ByteArray =
        readCappedBytes(this, limit) ?: throw IllegalStateException("文件过大：超过 ${limit / 1024 / 1024}MB 上限")

    private fun copyCapped(source: InputStream, target: File, limit: Long): Long {
        var total = 0L
        val buffer = ByteArray(bufferSize)
        FileOutputStream(target).use { output ->
            while (true) {
                val read = source.read(buffer)
                if (read <= 0) break
                total += read
                if (total > limit) return -1
                output.write(buffer, 0, read)
            }
        }
        return total
    }

    private fun uniqueComicFileName(name: String, index: Int): String {
        val extension = name.substringAfterLast('.', "jpg").lowercase(Locale.ROOT).takeIf { it.length <= 5 } ?: "jpg"
        return index.toString().padStart(5, '0') + ".$extension"
    }

    private fun sanitizeFileName(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]+"), "_").trim('_').ifBlank { "comic" }

    private fun String.removeSuffixIgnoreCase(suffix: String): String =
        if (endsWith(suffix, ignoreCase = true)) dropLast(suffix.length) else this

    private fun compareNatural(left: String, right: String): Int {
        val leftKey = naturalComicSortKey(left)
        val rightKey = naturalComicSortKey(right)
        val size = minOf(leftKey.size, rightKey.size)
        for (i in 0 until size) {
            val result = leftKey[i].compareTo(rightKey[i])
            if (result != 0) return result
        }
        return leftKey.size.compareTo(rightKey.size)
    }

    private fun appendChunkedChapter(
        chapters: MutableList<Chapter>,
        title: String,
        content: String,
        startPosition: Int,
        startIndex: Int = chapters.size
    ): Int {
        if (content.isBlank()) return startIndex

        var nextIndex = startIndex
        var offset = 0
        val trimmedContent = content.trim()
        while (offset < trimmedContent.length) {
            val rawEnd = minOf(offset + maxChapterChars, trimmedContent.length)
            val splitEnd = if (rawEnd < trimmedContent.length) {
                trimmedContent.lastIndexOf('\n', rawEnd - 1).takeIf { it > offset + maxChapterChars / 2 } ?: rawEnd
            } else {
                rawEnd
            }
            val chunk = trimmedContent.substring(offset, splitEnd).trim()
            if (chunk.isNotEmpty()) {
                val partNumber = nextIndex - startIndex + 1
                chapters.add(
                    Chapter(
                        bookId = 0,
                        index = nextIndex,
                        title = if (offset == 0 && splitEnd >= trimmedContent.length) title else "$title ($partNumber)",
                        content = chunk,
                        startPosition = startPosition + offset,
                        endPosition = startPosition + splitEnd
                    )
                )
                nextIndex++
            }
            offset = splitEnd
            while (offset < trimmedContent.length && trimmedContent[offset].isWhitespace()) {
                offset++
            }
        }
        return nextIndex
    }

    /**
     * FB2 (v53). `.fb2.zip` is the common distribution form, so the archive wrapper is unwrapped
     * here rather than pushed onto the user as "unsupported format".
     */
    private fun parseFb2Stream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val xml = inputStream.use { stream ->
                if (fileName.endsWith(".zip", ignoreCase = true)) {
                    readFirstZipEntryText(stream)
                } else {
                    readCappedBytes(stream, maxWholeFileBytes)?.decodeXml()
                        ?: return Result.failure(Exception("文件过大：FB2 超过 ${maxWholeFileBytes / 1024 / 1024}MB 上限"))
                }
            } ?: return Result.failure(Exception("无法读取 FB2 内容"))

            Fb2Parser.parse(xml).map { book ->
                val fallbackTitle = fileName.removeSuffix(".zip").removeSuffix(".fb2")
                val chapters = mutableListOf<Chapter>()
                var index = 0
                book.sections.forEach { section ->
                    index = appendChunkedChapter(
                        chapters = chapters,
                        title = section.title,
                        content = section.content,
                        startPosition = 0,
                        startIndex = index
                    )
                }
                BookParseResult(
                    book = Book(
                        title = book.title.ifBlank { fallbackTitle },
                        author = book.author.ifBlank { "未知作者" },
                        filePath = "",
                        coverPath = book.coverImage?.let { saveCoverImage(it, book.title.ifBlank { fallbackTitle }) },
                        description = book.description,
                        fileSize = fileSize,
                        format = BookFormat.FB2,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** MOBI / PRC / AZW (v53), read-only. DRM-protected files are declined, never decrypted. */
    private fun parseMobiStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val bytes = inputStream.use { it.readCappedBytesNotNull(maxWholeFileBytes) }
            MobiParser.parse(bytes).map { book ->
                val fallbackTitle = fileName.substringBeforeLast('.')
                val document = Jsoup.parse(book.html)
                document.select("script, style").remove()
                val text = htmlToFormattedText(document.body(), emptyMap()).trim()

                val chapters = mutableListOf<Chapter>()
                appendChunkedChapter(
                    chapters = chapters,
                    title = book.title.ifBlank { fallbackTitle },
                    content = text,
                    startPosition = 0
                )

                BookParseResult(
                    book = Book(
                        title = book.title.ifBlank { fallbackTitle },
                        author = book.author.ifBlank { "未知作者" },
                        filePath = "",
                        description = book.description,
                        fileSize = fileSize,
                        format = BookFormat.MOBI,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun readFirstZipEntryText(stream: InputStream): String? {
        ZipInputStream(stream.buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".fb2", ignoreCase = true)) {
                    return readCappedBytes(zip, maxWholeFileBytes)?.decodeXml()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }

    /** FB2 files ship in UTF-8 or windows-1251; trust the XML declaration over a blind UTF-8 read. */
    private fun ByteArray.decodeXml(): String {
        val head = String(this, 0, minOf(size, 200), Charsets.ISO_8859_1)
        val declared = Regex("encoding=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(head)?.groupValues?.get(1)
        val charset = declared?.let { runCatching { java.nio.charset.Charset.forName(it) }.getOrNull() } ?: Charsets.UTF_8
        return String(this, charset)
    }

    private fun parseMarkdownStream(inputStream: InputStream, fileName: String, fileSize: Long): Result<BookParseResult> {
        return try {
            val text = inputStream.readCappedTextNotNull(maxWholeFileBytes)

            val title = fileName.removeSuffix(".md")
            val chapters = mutableListOf<Chapter>()

            val lines = text.lines()
            val chapterPattern = Regex("^#{1,6}\\s+(.+)|^##?\\s+.+")

            var currentChapterTitle = "前言"
            var currentContent = StringBuilder()
            var chapterIndex = 0

            for (line in lines) {
                val trimmedLine = line.trim()
                if (chapterPattern.containsMatchIn(trimmedLine) && trimmedLine.length < 100) {
                    if (currentContent.isNotBlank()) {
                        chapterIndex = appendChunkedChapter(
                            chapters = chapters,
                            title = currentChapterTitle,
                            content = currentContent.toString().trim(),
                            startPosition = 0,
                            startIndex = chapterIndex
                        )
                    }
                    currentChapterTitle = trimmedLine.replace(Regex("^#{1,6}\\s*"), "")
                    currentContent = StringBuilder()
                } else {
                    currentContent.appendLine(line)
                }
            }

            if (currentContent.isNotBlank()) {
                appendChunkedChapter(
                    chapters = chapters,
                    title = currentChapterTitle,
                    content = currentContent.toString().trim(),
                    startPosition = 0,
                    startIndex = chapterIndex
                )
            }

            if (chapters.isEmpty()) {
                appendChunkedChapter(
                    chapters = chapters,
                    title = "全部内容",
                    content = text,
                    startPosition = 0
                )
            }

            Result.success(
                BookParseResult(
                    book = Book(
                        title = title,
                        author = "未知作者",
                        filePath = "",
                        fileSize = fileSize,
                        format = BookFormat.MARKDOWN,
                        totalChapters = chapters.size
                    ),
                    chapters = chapters
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
