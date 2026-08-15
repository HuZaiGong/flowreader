package com.flowreader.app.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.flowreader.app.core.util.ReaderBackgroundImage
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wraps an [InputStream] to throw once [maxBytes] have been read.
 *
 * Used when a content URI returns unknown length — the declared-size check cannot gate it, so the
 * stream itself must enforce the cap before decode work begins.
 */
private class BoundedInputStream(
    private val source: InputStream,
    private val maxBytes: Long
) : InputStream() {
    private var bytesRead = 0L

    override fun read(): Int {
        if (bytesRead >= maxBytes) error("stream exceeded ${maxBytes}-byte limit")
        val b = source.read()
        if (b >= 0) bytesRead++
        return b
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (bytesRead >= maxBytes) error("stream exceeded ${maxBytes}-byte limit")
        val remaining = (maxBytes - bytesRead).coerceAtMost(length.toLong()).toInt()
        if (remaining <= 0) return -1
        val count = source.read(buffer, offset, remaining)
        if (count > 0) bytesRead += count
        return count
    }

    override fun close() = source.close()
}

/**
 * Copies a user-picked image into `filesDir/backgrounds/` for use as the reader background.
 *
 * Mirrors the custom-font import in `SettingsViewModel`: the file is copied into app-private
 * storage and referenced by absolute path, so no long-lived `content://` permission is held and the
 * picked file can move or vanish afterwards without breaking the reader.
 *
 * Two limits, both matching existing project conventions rather than inventing new ones:
 * [ReaderBackgroundImage.MAX_IMAGE_BYTES] equals `BookParser`'s single-image cap, and the bitmap is
 * downsampled to [ReaderBackgroundImage.MAX_EDGE_PX] before being written, so a 12000px photo does
 * not sit in memory at full size on every chapter render.
 *
 * No FileProvider grant is added for this directory — backgrounds never leave the app, and
 * `file_paths.xml` deliberately exposes only `share_cards/`.
 */
@Singleton
class ReaderBackgroundImporter @Inject constructor(
    @ApplicationContext context: Context
) {
    private val appContext = context.applicationContext

    private val backgroundDir: File
        get() = File(appContext.filesDir, ReaderBackgroundImage.BACKGROUND_DIR)

    /**
     * Imports [uri] and returns the stored absolute path, or a failure describing why not.
     *
     * Decodes bounds first to pick a sample size, then decodes for real — the two-pass read is what
     * keeps an oversized image from being fully materialised just to be scaled down.
     */
    fun import(uri: Uri): Result<String> = runCatching {
        val declaredSize = declaredSizeOf(uri)
        if (declaredSize != null && declaredSize > ReaderBackgroundImage.MAX_IMAGE_BYTES) {
            error("image is ${declaredSize} bytes, over the ${ReaderBackgroundImage.MAX_IMAGE_BYTES} limit")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundStream = if (declaredSize != null) {
            appContext.contentResolver.openInputStream(uri)
        } else {
            // No declared size — wrap the stream to enforce the cap during decoding.
            appContext.contentResolver.openInputStream(uri)?.let { raw ->
                BoundedInputStream(raw, ReaderBackgroundImage.MAX_IMAGE_BYTES.toLong())
            }
        }
        boundStream?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        } ?: error("cannot open the selected image")

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            error("the selected file is not a decodable image")
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = ReaderBackgroundImage.sampleSizeFor(bounds.outWidth, bounds.outHeight)
        }
        val decodeStream = if (declaredSize != null) {
            appContext.contentResolver.openInputStream(uri)
        } else {
            appContext.contentResolver.openInputStream(uri)?.let { raw ->
                BoundedInputStream(raw, ReaderBackgroundImage.MAX_IMAGE_BYTES.toLong())
            }
        }
        val bitmap = decodeStream?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: error("cannot decode the selected image")

        try {
            if (!backgroundDir.exists() && !backgroundDir.mkdirs()) {
                error("cannot create the backgrounds directory")
            }
            // WEBP keeps the file small without the generation loss of re-encoding to JPEG, and is
            // supported on every API level this app targets (minSdk 26).
            val target = File(backgroundDir, "reader_background_${System.currentTimeMillis()}.webp")
            target.outputStream().use { output ->
                @Suppress("DEPRECATION")
                val compressed = bitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP, 90, output)
                if (!compressed) error("cannot re-encode the selected image")
            }
            pruneOldBackgrounds(keep = target)
            target.absolutePath
        } finally {
            bitmap.recycle()
        }
    }

    /** Deletes the stored background, if any. Safe to call when none is set. */
    fun clear(path: String?) {
        val file = path?.takeIf { ReaderBackgroundImage.isActive(it) }?.let(::File) ?: return
        if (file.parentFile?.absolutePath == backgroundDir.absolutePath) {
            file.delete()
        }
    }

    /**
     * Only one background is ever referenced, so previous imports are dead weight — without this,
     * every re-import would leak another copy into app storage.
     */
    private fun pruneOldBackgrounds(keep: File) {
        backgroundDir.listFiles()?.forEach { file ->
            if (file.isFile && file.absolutePath != keep.absolutePath) {
                file.delete()
            }
        }
    }

    private fun declaredSizeOf(uri: Uri): Long? = runCatching {
        appContext.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0 }
        }
    }.getOrNull()
}
