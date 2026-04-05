package com.flowreader.app.domain.model

enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA,
    PAPER,
    AMOLED,
    SYSTEM,
    MORNING,
    NOON,
    EVENING,
    NIGHT
}

enum class PageMode {
    SLIDE,
    SIMULATION,
    NONE,
    CURL,
    SLIDE_OVER
}

enum class FontFamily(val displayName: String) {
    DEFAULT("默认"),
    SERIF("衬线体"),
    SANS_SERIF("无衬线"),
    MONOSPACE("等宽"),
    SONG("宋体"),
    HEI("黑体"),
    KAI("楷体"),
    FANGSONG("仿宋")
}

enum class ParagraphMode(val displayName: String) {
    COMPACT("紧凑"),
    STANDARD("标准"),
    RELAXED("宽松"),
    IMMERSIVE("沉浸")
}

enum class GestureAction {
    PREVIOUS_PAGE,
    NEXT_PAGE,
    TOGGLE_CONTROLS,
    SHOW_SETTINGS,
    SHOW_BOOKMARKS,
    SHOW_TOC,
    ADD_BOOKMARK,
    NONE
}

data class GestureSettings(
    val leftTapAction: GestureAction = GestureAction.PREVIOUS_PAGE,
    val middleTapAction: GestureAction = GestureAction.TOGGLE_CONTROLS,
    val rightTapAction: GestureAction = GestureAction.NEXT_PAGE,
    val swipeLeftAction: GestureAction = GestureAction.NEXT_PAGE,
    val swipeRightAction: GestureAction = GestureAction.PREVIOUS_PAGE,
    val doubleTapAction: GestureAction = GestureAction.SHOW_SETTINGS,
    val longPressAction: GestureAction = GestureAction.ADD_BOOKMARK
)

enum class BackgroundTexture(val displayName: String) {
    NONE("无"),
    PAPER("纸张"),
    CANVAS("画布"),
    WOOD("木纹"),
    MARBLE("大理石"),
    GRADIENT("渐变")
}

enum class AmbientSound(val displayName: String) {
    NONE("无"),
    RAIN("雨声"),
    WIND("风声"),
    FIREPLACE("柴火"),
    CAFE("咖啡馆"),
    OCEAN("海浪")
}

data class ReadingSettings(
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.5f,
    val paragraphSpacing: Float = 1.0f,
    val paragraphMode: ParagraphMode = ParagraphMode.STANDARD,
    val fontFamily: FontFamily = FontFamily.DEFAULT,
    val customFontPath: String? = null,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val pageMode: PageMode = PageMode.SLIDE,
    val keepScreenOn: Boolean = true,
    val screenTimeoutMinutes: Int = 0,
    val tapZoneRatio: Float = 0.3f,
    val gestureSettings: GestureSettings = GestureSettings(),
    val backgroundTexture: BackgroundTexture = BackgroundTexture.NONE,
    val backgroundColor: Long = 0xFFF5F5DC,
    val textColor: Long = 0xFF000000,
    val autoHideControls: Boolean = true,
    val controlsHideDelay: Long = 3000L,
    val fullScreenMode: Boolean = true,
    val ambientSound: AmbientSound = AmbientSound.NONE,
    val ambientSoundVolume: Float = 0.5f,
    val firstLineIndent: Boolean = true,
    val justifyText: Boolean = true,
    val simplifiedChinese: Boolean = true
)

data class AppSettings(
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val defaultReadingSettings: ReadingSettings = ReadingSettings(),
    val autoTimeTheme: Boolean = false,
    val timeThemeStartHour: Int = 20,
    val timeThemeEndHour: Int = 6,
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0
)
