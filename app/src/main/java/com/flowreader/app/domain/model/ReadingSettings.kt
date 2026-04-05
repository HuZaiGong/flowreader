package com.flowreader.app.domain.model

import java.util.Date

/**
 * 阅读主题 - 支持更多主题
 */
enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA,
    PAPER,
    AMOLED,
    SYSTEM,
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT,
    EINK_PURE,
    EINK_GRAY,
    EINK_WARM
}

enum class PageMode {
    SLIDE,
    SIMULATION,
    NONE
}

enum class FontFamily(val displayName: String) {
    DEFAULT("默认"),
    SERIF("思源宋体"),
    SANS_SERIF("思源黑体"),
    MONOSPACE("等宽"),
    KAITI("楷体")
}

enum class ParagraphSpacing(val displayName: String, val value: Float) {
    COMPACT("紧凑", 0.5f),
    STANDARD("标准", 1.0f),
    RELAXED("宽松", 1.5f),
    IMMERSIVE("沉浸", 2.0f)
}

enum class TapZone {
    LEFT,
    MIDDLE,
    RIGHT
}

enum class GestureAction {
    PREVIOUS_PAGE,
    NEXT_PAGE,
    SHOW_MENU,
    SHOW_SETTINGS,
    SHOW_BOOKMARKS
}

data class TapZoneConfig(
    val leftAction: GestureAction = GestureAction.PREVIOUS_PAGE,
    val middleAction: GestureAction = GestureAction.SHOW_MENU,
    val rightAction: GestureAction = GestureAction.NEXT_PAGE,
    val leftRatio: Float = 0.3f,
    val rightRatio: Float = 0.3f
)

data class DoubleFingerGestureConfig(
    val swipeUpAction: GestureAction = GestureAction.NEXT_PAGE,
    val swipeDownAction: GestureAction = GestureAction.PREVIOUS_PAGE,
    val pinchAction: GestureAction = GestureAction.SHOW_SETTINGS,
    val expandAction: GestureAction = GestureAction.SHOW_SETTINGS
)

enum class BackgroundTexture(val displayName: String) {
    SOLID("纯色"),
    PAPER("纸张"),
    FABRIC("布艺"),
    MARBLE("大理石"),
    GRADIENT("渐变")
}

enum class SoundEffect(val displayName: String) {
    NONE("静音"),
    RAIN("雨声"),
    WIND("风声"),
    FIREPLACE("柴火声"),
    CAFE("咖啡馆")
}

data class ReadingSettings(
    val fontSize: Int = 18,
    val fontFamily: FontFamily = FontFamily.DEFAULT,
    val lineSpacing: Float = 1.5f,
    val paragraphSpacing: Float = 1.0f,
    val paragraphSpacingPreset: ParagraphSpacing = ParagraphSpacing.STANDARD,
    val firstLineIndent: Boolean = true,
    val justifyText: Boolean = true,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val autoTimeTheme: Boolean = false,
    val pageMode: PageMode = PageMode.SLIDE,
    val keepScreenOn: Boolean = true,
    val screenTimeoutMinutes: Int = 0,
    val tapZoneConfig: TapZoneConfig = TapZoneConfig(),
    val doubleFingerGesture: Boolean = true,
    val longPressGesture: Boolean = true,
    val controlsAutoHide: Boolean = true,
    val controlsHideDelaySeconds: Int = 3,
    val backgroundTexture: BackgroundTexture = BackgroundTexture.SOLID,
    val soundEffect: SoundEffect = SoundEffect.NONE,
    val wordCount: Int = 0,
    val einkMode: Boolean = false,
    val einkRefreshMode: EinkRefreshMode = EinkRefreshMode.AUTO,
    val accessibilityMode: AccessibilityMode = AccessibilityMode.NONE,
    val highContrastMode: Boolean = false,
    val largeFontMode: Boolean = false,
    val simplifyMode: Boolean = false,
    val dailyReadingGoalMinutes: Int = 30,
    val weeklyReadingGoalBooks: Int = 1,
    val enableReadingReminder: Boolean = false,
    val reminderTime: String = "20:00",
    val autoNightMode: Boolean = false,
    val nightModeStartHour: Int = 20,
    val nightModeEndHour: Int = 6
)

enum class EinkRefreshMode {
    AUTO,
    FULL,
    PARTIAL,
    FAST
}

enum class AccessibilityMode {
    NONE,
    LARGE_FONT,
    HIGH_CONTRAST,
    SIMPLIFIED
}

data class AppSettings(
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val defaultReadingSettings: ReadingSettings = ReadingSettings(),
    val enableSplashScreen: Boolean = true,
    val enableBackupReminder: Boolean = true
)
