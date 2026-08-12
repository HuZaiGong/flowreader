package com.flowreader.feature.reader

import com.flowreader.app.domain.model.BookFormat
import com.flowreader.app.domain.model.PageMode

/**
 * What a reader position actually counts.
 *
 * The reader reports a single `Int` position for every mode, but it means two different things:
 * a character offset while scrolling, or an index of a rendered page. Feeding a page index into
 * the character-based reading-stats math costs a page turn about one character, so a session never
 * reaches one full page and the `pages > 0` persistence gate throws the whole session away — no
 * reading time and no pages are recorded. Keep this decision in one tested place.
 */
enum class ReaderPositionUnit {
    /** Character offset (approximated by scroll pixels) — `SLIDE` and `NONE` text reading. */
    CHARACTERS,

    /** Index of a rendered page — `PAGED` text reading and comic pages. */
    PAGE_INDEX;

    val isPageIndex: Boolean get() = this == PAGE_INDEX

    companion object {
        /**
         * Comics render one image per "chapter" and report the visible image index, whatever the
         * page mode; text reading only pages when [PageMode.PAGED] is selected.
         */
        fun of(pageMode: PageMode, format: BookFormat?): ReaderPositionUnit = when {
            format == BookFormat.COMIC -> PAGE_INDEX
            pageMode == PageMode.PAGED -> PAGE_INDEX
            else -> CHARACTERS
        }
    }
}
