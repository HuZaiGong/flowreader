package com.flowreader.app.domain.model

enum class ReaderTheme {
    LIGHT,
    DARK,
    SEPIA,
    PAPER,
    AMOLED,
    SYSTEM
}

enum class PageMode {
    SLIDE,
    SIMULATION,
    NONE
}

enum class FontFamily(val displayName: String) {
    DEFAULT("默认"),
    SERIF("衬线体"),
    SANS_SERIF("无衬线"),
    MONOSPACE("等宽")
}

data class ReadingSettings(
    val fontSize: Int = 18,
    val lineSpacing: Float = 1.5f,
    val paragraphSpacing: Float = 1.0f,
    val fontFamily: FontFamily = FontFamily.DEFAULT,
    val theme: ReaderTheme = ReaderTheme.LIGHT,
    val pageMode: PageMode = PageMode.SLIDE,
    val keepScreenOn: Boolean = true,
    val screenTimeoutMinutes: Int = 0,
    val tapZoneRatio: Float = 0.3f
)

data class AppSettings(
    val theme: ReaderTheme = ReaderTheme.SYSTEM,
    val defaultReadingSettings: ReadingSettings = ReadingSettings()
)
