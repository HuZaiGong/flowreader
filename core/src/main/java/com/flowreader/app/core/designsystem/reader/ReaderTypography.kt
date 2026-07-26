package com.flowreader.app.core.designsystem.reader

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.flowreader.app.domain.model.ReaderFontFamily
import com.flowreader.app.domain.model.ReadingSettings
import java.io.File

/**
 * Turns [ReadingSettings] into the styles the reader body actually renders with.
 *
 * Before v52 none of this was wired: the font-family chips, the imported `.ttf` and the paragraph
 * spacing value all persisted correctly and were then ignored by `ReaderContent`.
 */

fun ReaderFontFamily.toComposeFontFamily(): FontFamily = when (this) {
    ReaderFontFamily.DEFAULT -> FontFamily.Default
    ReaderFontFamily.SERIF -> FontFamily.Serif
    ReaderFontFamily.SANS_SERIF -> FontFamily.SansSerif
    ReaderFontFamily.MONOSPACE -> FontFamily.Monospace
}

/**
 * Loads a user-imported font file. Returns `null` when the path is missing or unreadable so the
 * reader silently falls back to the selected built-in face instead of failing during layout.
 */
fun loadCustomFontFamily(path: String?): FontFamily? {
    if (path.isNullOrBlank()) return null
    return try {
        val file = File(path)
        if (!file.isFile) null else FontFamily(Typeface.createFromFile(file))
    } catch (e: Exception) {
        null
    }
}

/** The imported font wins over the built-in face; both are honoured, neither is decorative. */
@Composable
fun rememberReaderFontFamily(settings: ReadingSettings): FontFamily {
    val customPath = settings.customFontPath
    val selected = settings.fontFamily
    return remember(customPath, selected) {
        loadCustomFontFamily(customPath) ?: selected.toComposeFontFamily()
    }
}

fun readerBodyStyle(settings: ReadingSettings, fontFamily: FontFamily, palette: ReaderPalette): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontSize = settings.fontSize.sp,
    lineHeight = ReaderMetrics.lineHeightSp(settings.fontSize, settings.lineSpacing).sp,
    letterSpacing = 0.sp,
    textAlign = TextAlign.Justify,
    color = palette.text,
    lineBreak = LineBreak.Paragraph,
    hyphens = Hyphens.None
)

fun readerChapterTitleStyle(settings: ReadingSettings, fontFamily: FontFamily, palette: ReaderPalette): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontSize = ReaderMetrics.chapterTitleSizeSp(settings.fontSize).sp,
    lineHeight = (ReaderMetrics.chapterTitleSizeSp(settings.fontSize) * 1.4f).sp,
    letterSpacing = 0.sp,
    fontWeight = FontWeight.Medium,
    color = palette.text
)

fun readerHeadingStyle(settings: ReadingSettings, fontFamily: FontFamily, palette: ReaderPalette): TextStyle = TextStyle(
    fontFamily = fontFamily,
    fontSize = ReaderMetrics.headingSizeSp(settings.fontSize).sp,
    lineHeight = (ReaderMetrics.headingSizeSp(settings.fontSize) * 1.4f).sp,
    letterSpacing = 0.sp,
    fontWeight = FontWeight.Bold,
    color = palette.text
)

/** Paragraph gap, already converted out of the multiplier domain. */
fun paragraphSpacing(settings: ReadingSettings): Dp = ReaderMetrics.paragraphSpacingDp(settings.fontSize, settings.paragraphSpacing).dp
