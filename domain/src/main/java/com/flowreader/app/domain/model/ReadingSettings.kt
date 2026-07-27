package com.flowreader.app.domain.model

/**
 * App-level theme mode. This only decides whether the Material surfaces *around* the reader are
 * light or dark; the reader body has its own palette ([ReaderPaletteId]) and never follows this.
 */
enum class AppThemeMode(val displayName: String) {
    LIGHT("浅色"),
    DARK("深色"),
    FOLLOW_SYSTEM("跟随系统");

    companion object {
        fun fromStoredName(raw: String?): AppThemeMode = entries.firstOrNull { it.name == raw } ?: FOLLOW_SYSTEM
    }
}

/**
 * Where the Material color scheme comes from. [BRAND] keeps the app's own visual identity,
 * [DYNAMIC] follows the wallpaper on Android 12+ and falls back to [BRAND] below it.
 */
enum class ColorSource(val displayName: String) {
    BRAND("品牌配色"),
    DYNAMIC("跟随壁纸");

    companion object {
        fun fromStoredName(raw: String?): ColorSource = entries.firstOrNull { it.name == raw } ?: BRAND
    }
}

/**
 * The 12 built-in reader palettes. Only the identity lives here — the actual color values are a
 * rendering concern and live in `:core` (`ReaderPalettes`).
 */
enum class ReaderPaletteId(val displayName: String, val isDark: Boolean) {
    PAPER("纸白", false),
    SEPIA("米黄", false),
    GREEN("护眼绿", false),
    LINEN("亚麻", false),
    MIST("晨雾", false),
    COOL_GRAY("冷灰", false),
    EINK("电子墨水", false),
    NIGHT("夜黑", true),
    INK_BLUE("墨蓝", true),
    DEEP_BROWN("深棕", true),
    OBSIDIAN("曜石", true),
    OLED("纯黑", true);

    companion object {
        val LIGHT_PALETTES: List<ReaderPaletteId> get() = entries.filter { !it.isDark }
        val DARK_PALETTES: List<ReaderPaletteId> get() = entries.filter { it.isDark }

        /**
         * Reads a persisted palette id, migrating the pre-v52 `reader_theme` values
         * (`LIGHT` / `DARK`) that shared this slot.
         */
        fun fromStoredName(raw: String?, fallback: ReaderPaletteId = PAPER): ReaderPaletteId = when (raw) {
            null -> fallback
            "LIGHT" -> PAPER
            "DARK" -> NIGHT
            else -> entries.firstOrNull { it.name == raw } ?: fallback
        }
    }
}

/**
 * Page-turn behaviour. Only the two modes the renderer actually implements are exposed:
 * [SLIDE] animates the chapter swap and the scroll reset, [NONE] jumps instantly.
 * The pre-v52 `SIMULATION` / `CURL` / `SLIDE_OVER` values were never rendered and are gone.
 */
enum class PageMode(val displayName: String) {
    SLIDE("滑动"),
    NONE("无动画");

    companion object {
        fun fromStoredName(raw: String?): PageMode = entries.firstOrNull { it.name == raw } ?: SLIDE
    }
}

/**
 * Reader font families. Every entry maps to a face the platform can actually resolve — anything
 * more specific (楷体 / 仿宋 …) is covered by importing a custom `.ttf`.
 */
enum class ReaderFontFamily(val displayName: String) {
    DEFAULT("系统默认"),
    SERIF("宋体 / 衬线"),
    SANS_SERIF("黑体 / 无衬线"),
    MONOSPACE("等宽");

    companion object {
        /** Maps the pre-v52 eight-value enum onto the four faces that actually render. */
        fun fromStoredName(raw: String?): ReaderFontFamily = when (raw) {
            null -> DEFAULT
            "SONG", "FANGSONG", "KAI" -> SERIF
            "HEI" -> SANS_SERIF
            else -> entries.firstOrNull { it.name == raw } ?: DEFAULT
        }
    }
}

enum class GestureAction(val displayName: String) {
    PREVIOUS_PAGE("上一章"),
    NEXT_PAGE("下一章"),
    TOGGLE_CONTROLS("显示/隐藏控制栏"),
    SHOW_SETTINGS("阅读设置"),
    SHOW_BOOKMARKS("书签列表"),
    SHOW_TOC("目录"),
    ADD_BOOKMARK("添加书签"),
    NONE("无");

    companion object {
        fun fromStoredName(raw: String?, fallback: GestureAction): GestureAction = entries.firstOrNull { it.name == raw } ?: fallback
    }
}

data class GestureSettings(
    val leftTapAction: GestureAction = GestureAction.PREVIOUS_PAGE,
    val middleTapAction: GestureAction = GestureAction.TOGGLE_CONTROLS,
    val rightTapAction: GestureAction = GestureAction.NEXT_PAGE,
    val swipeLeftAction: GestureAction = GestureAction.NEXT_PAGE,
    val swipeRightAction: GestureAction = GestureAction.PREVIOUS_PAGE,
    val doubleTapAction: GestureAction = GestureAction.SHOW_SETTINGS,
    val longPressAction: GestureAction = GestureAction.ADD_BOOKMARK,
    val edgeGestureEnabled: Boolean = true,
    val leftEdgeWidth: Int = 20,
    val rightEdgeWidth: Int = 20
)

data class ReadingSettings(
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.5f,
    /** Multiple of the font size, not a raw dp value. See `ReaderMetrics.paragraphSpacingDp`. */
    val paragraphSpacing: Float = 1.0f,
    val firstLineIndent: Boolean = true,
    val fontFamily: ReaderFontFamily = ReaderFontFamily.DEFAULT,
    val customFontPath: String? = null,
    val palette: ReaderPaletteId = ReaderPaletteId.PAPER,
    val nightPalette: ReaderPaletteId = ReaderPaletteId.NIGHT,
    val pageMode: PageMode = PageMode.SLIDE,
    val keepScreenOn: Boolean = true,
    val screenTimeoutMinutes: Int = 0,
    val eyeProtectionIntervalMinutes: Int = 20,
    val autoNightMode: Boolean = false,
    val tapZoneRatio: Float = 0.3f,
    val gestureSettings: GestureSettings = GestureSettings()
) {
    companion object {
        const val PARAGRAPH_SPACING_MIN = 0.25f
        const val PARAGRAPH_SPACING_MAX = 3.0f

        /**
         * Before v52 `paragraphSpacing` was fed straight into `Modifier.padding(...dp)`, so stored
         * values could be anything. Clamp legacy values back into the multiple domain on read.
         */
        fun normalizeParagraphSpacing(raw: Float): Float = when {
            raw.isNaN() -> 1.0f
            raw < PARAGRAPH_SPACING_MIN -> PARAGRAPH_SPACING_MIN
            raw > PARAGRAPH_SPACING_MAX -> 1.0f
            else -> raw
        }
    }
}

data class AppSettings(
    val themeMode: AppThemeMode = AppThemeMode.FOLLOW_SYSTEM,
    val colorSource: ColorSource = ColorSource.BRAND,
    val language: AppLanguage = AppLanguage.FOLLOW_SYSTEM,
    val defaultReadingSettings: ReadingSettings = ReadingSettings(),
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0
)
