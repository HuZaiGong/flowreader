package com.flowreader.app.core.util

import com.flowreader.app.core.designsystem.reader.ReaderPalette

/**
 * Custom reader colors (v55): the user can pick their own text and background colors, which
 * override the active palette. The pair is validated against WCAG AA body-text contrast — an
 * unreadable pair silently falls back to the palette side that would restore contrast.
 */
object ReaderCustomTheme {

    fun resolve(palette: ReaderPalette, customTextArgb: Long?, customBackgroundArgb: Long?): ReaderPalette {
        if (customTextArgb == null && customBackgroundArgb == null) return palette

        var text = customTextArgb ?: palette.textArgb
        var background = customBackgroundArgb ?: palette.backgroundArgb

        if (!ColorContrast.meetsAaBodyText(text, background)) {
            val onlyTextCustom = customTextArgb != null && customBackgroundArgb == null
            val onlyBackgroundCustom = customBackgroundArgb != null && customTextArgb == null
            text = when {
                onlyTextCustom && ColorContrast.meetsAaBodyText(palette.textArgb, background) -> palette.textArgb
                else -> text
            }
            background = when {
                onlyBackgroundCustom && ColorContrast.meetsAaBodyText(text, palette.backgroundArgb) -> palette.backgroundArgb
                else -> background
            }
            if (!ColorContrast.meetsAaBodyText(text, background)) {
                text = palette.textArgb
                background = palette.backgroundArgb
            }
        }
        return palette.copy(textArgb = text, backgroundArgb = background)
    }
}
