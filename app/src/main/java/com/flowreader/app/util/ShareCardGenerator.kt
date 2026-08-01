package com.flowreader.app.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import java.io.File

/**
 * Draws a reading-progress share card (v55): book title, current chapter, big progress percent
 * and a progress bar over the active reader palette, exported as a PNG into the cache dir.
 *
 * Plain `android.graphics` so the card renders off the main composition and reuses the exact
 * palette ARGB values the reader uses.
 */
object ShareCardGenerator {

    fun generate(
        cacheDir: File,
        bookTitle: String,
        chapterTitle: String,
        progressPercent: Int,
        backgroundArgb: Long,
        textArgb: Long,
        accentArgb: Long
    ): File {
        val width = 1080
        val height = 1440
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        canvas.drawColor(backgroundArgb.toInt())

        val backgroundPaint = Paint().apply { color = backgroundArgb.toInt() }
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textArgb.toInt() }
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accentArgb.toInt() }
        val subtlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = textArgb.toInt(); alpha = 90 }

        val margin = 96f

        // Book title (wrapped to two lines max)
        textPaint.textSize = 84f
        textPaint.isFakeBoldText = true
        val titleLines = wrap(textPaint, bookTitle, width - margin * 2)
        var y = 320f
        titleLines.take(2).forEach { line ->
            canvas.drawText(line, margin, y, textPaint)
            y += 104f
        }

        // Chapter line
        textPaint.textSize = 48f
        textPaint.isFakeBoldText = false
        canvas.drawText(chapterTitle, margin, y + 40f, textPaint)

        // Big percent
        textPaint.textSize = 220f
        textPaint.isFakeBoldText = true
        val percentText = "$progressPercent%"
        val percentWidth = textPaint.measureText(percentText)
        canvas.drawText(percentText, (width - percentWidth) / 2f, 900f, textPaint)

        // Progress bar
        val barRect = RectF(margin, 1040f, width - margin, 1088f)
        subtlePaint.color = textArgb.toInt()
        subtlePaint.alpha = 40
        canvas.drawRoundRect(barRect, 24f, 24f, subtlePaint)
        if (progressPercent > 0) {
            val filled = RectF(
                barRect.left,
                barRect.top,
                barRect.left + (barRect.width() * progressPercent / 100f).coerceAtMost(barRect.width()),
                barRect.bottom
            )
            canvas.drawRoundRect(filled, 24f, 24f, accentPaint)
        }

        // Footer
        textPaint.textSize = 40f
        textPaint.isFakeBoldText = false
        subtlePaint.alpha = 160
        canvas.drawText("#FlowReader 心流阅读", margin, height - 120f, subtlePaint)

        val file = File(cacheDir, "share_card_${System.currentTimeMillis()}.png")
        file.outputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        }
        bitmap.recycle()
        return file
    }

    private fun wrap(paint: Paint, text: String, maxWidth: Float): List<String> {
        val lines = mutableListOf<String>()
        var current = StringBuilder()
        text.split(Regex("(?<=[，。！？；、\\s])|(?=[，。！？；、])")).forEach { token ->
            if (paint.measureText(current.toString() + token) > maxWidth && current.isNotEmpty()) {
                lines.add(current.toString())
                current = StringBuilder()
            }
            current.append(token)
        }
        if (current.isNotEmpty()) lines.add(current.toString())
        return lines.ifEmpty { listOf(text) }
    }
}
