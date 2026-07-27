package com.flowreader.app.util

import android.content.Context
import android.net.Uri
import com.flowreader.app.domain.model.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Batch import from a `.zip` of books.
 *
 * The rules are deliberately paranoid: a zip is untrusted input. [ZipImportRules] rejects absolute
 * and `..` paths (zip-slip), caps the entry count and the per-entry size, and only lets through
 * extensions the app can actually parse — an archive of 4000 JPEGs should import zero books, not
 * fill internal storage first and fail afterwards.
 */
@Singleton
class ZipImporter @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun extract(uri: Uri): Result<List<File>> = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "zip_import_${System.currentTimeMillis()}")
        if (!target.mkdirs() && !target.isDirectory) {
            return@withContext Result.failure(IllegalStateException("无法创建解压目录"))
        }
        val input = context.contentResolver.openInputStream(uri)
            ?: return@withContext Result.failure(IllegalStateException("无法打开压缩包"))

        try {
            val extracted = mutableListOf<File>()
            ZipInputStream(input.buffered()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (extracted.size >= ZipImportRules.MAX_ENTRIES) break
                    val safeName = ZipImportRules.safeBookName(entry.name, entry.isDirectory)
                    if (safeName != null) {
                        val file = File(target, uniqueName(target, safeName))
                        val written = copyCapped(zip, file, ZipImportRules.MAX_ENTRY_BYTES)
                        if (written > 0) extracted.add(file) else file.delete()
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            if (extracted.isEmpty()) {
                target.deleteRecursively()
                Result.failure(IllegalArgumentException("压缩包中没有支持的书籍文件"))
            } else {
                Result.success(extracted)
            }
        } catch (e: Exception) {
            target.deleteRecursively()
            Result.failure(e)
        }
    }

    /** Returns the byte count written, or `-1` when the entry blew past [limit] and was discarded. */
    private fun copyCapped(source: InputStream, target: File, limit: Long): Long {
        var total = 0L
        val buffer = ByteArray(BUFFER_SIZE)
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

    private fun uniqueName(directory: File, name: String): String {
        if (!File(directory, name).exists()) return name
        val base = name.substringBeforeLast('.')
        val extension = name.substringAfterLast('.', "")
        var index = 1
        while (true) {
            val candidate = if (extension.isEmpty()) "$base-$index" else "$base-$index.$extension"
            if (!File(directory, candidate).exists()) return candidate
            index++
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8192
    }
}

/** Pure zip-entry policy, split out so the guards are testable without touching the filesystem. */
object ZipImportRules {
    const val MAX_ENTRIES = 500
    const val MAX_ENTRY_BYTES = 512L * 1024 * 1024

    /**
     * Flattens an entry path to a bare filename and returns `null` when the entry must be skipped:
     * directories, path traversal, hidden `__MACOSX` noise, and anything the parser cannot read.
     */
    fun safeBookName(rawName: String, isDirectory: Boolean): String? {
        if (isDirectory) return null
        val normalized = rawName.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.split('/').any { it == ".." }) return null
        if (normalized.startsWith("__MACOSX/")) return null

        val fileName = normalized.substringAfterLast('/')
        if (fileName.isBlank() || fileName.startsWith(".")) return null
        if (BookParser.detectFormatStatic(fileName) == BookFormat.UNKNOWN) return null
        return fileName
    }
}
