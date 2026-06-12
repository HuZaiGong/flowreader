package com.flowreader.app.util

import java.util.Locale

object SafeFileNames {
    private const val DEFAULT_BASENAME = "book"
    private const val MAX_BASENAME_CHARS = 80

    fun forInternalBook(originalName: String, fallbackExtension: String = ""): String {
        val cleanedName = originalName.substringAfterLast('/').substringAfterLast('\\')
        val extension = cleanedName.substringAfterLast('.', missingDelimiterValue = "")
            .lowercase(Locale.ROOT)
            .takeIf { it.isSupportedExtension() }
            ?: fallbackExtension.trimStart('.').lowercase(Locale.ROOT).takeIf { it.isSupportedExtension() }

        val baseName = cleanedName.substringBeforeLast('.', missingDelimiterValue = cleanedName)
            .map { if (it.isSafeFileChar()) it else '_' }
            .joinToString(separator = "")
            .trim('_', '.', ' ')
            .take(MAX_BASENAME_CHARS)
            .ifEmpty { DEFAULT_BASENAME }

        return if (extension == null) baseName else "$baseName.$extension"
    }

    fun forCover(bookTitle: String): String = "${forInternalBook(bookTitle, "jpg").substringBeforeLast('.')}.jpg"

    private fun String.isSupportedExtension(): Boolean = when (this) {
        "epub", "txt", "pdf", "md", "markdown", "jpg", "jpeg", "png" -> true
        else -> false
    }

    private fun Char.isSafeFileChar(): Boolean = isLetterOrDigit() || this == '-' || this == '_' || this == ' '
}
