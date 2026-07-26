package com.flowreader.app.domain.model

enum class ReaderTheme {
    LIGHT,
    DARK
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
    val longPressAction: GestureAction = GestureAction.ADD_BOOKMARK,
    val edgeGestureEnabled: Boolean = true,
    val leftEdgeWidth: Int = 20,
    val rightEdgeWidth: Int = 20
)

data class ReadingSettings(
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.5f,
    val paragraphSpacing: Float = 1.0f,
    val fontFamily: FontFamily = FontFamily.DEFAULT,
    val customFontPath: String? = null,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val pageMode: PageMode = PageMode.SLIDE,
    val keepScreenOn: Boolean = true,
    val screenTimeoutMinutes: Int = 0,
    val eyeProtectionIntervalMinutes: Int = 20,
    val autoNightMode: Boolean = false,
    val tapZoneRatio: Float = 0.3f,
    val gestureSettings: GestureSettings = GestureSettings()
)

data class AppSettings(
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val defaultReadingSettings: ReadingSettings = ReadingSettings(),
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0
)
