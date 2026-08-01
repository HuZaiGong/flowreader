package com.flowreader.app.domain.model

/**
 * The in-app display language.
 *
 * [FOLLOW_SYSTEM] carries a `null` [tag], which is the signal to leave resource resolution to the
 * platform rather than pinning a locale. Every other entry maps to a `values-<qualifier>` folder
 * that actually exists — adding a value here without shipping the translations makes the picker
 * offer a language that silently falls back to Chinese.
 */
enum class AppLanguage(val tag: String?, val displayName: String) {
    FOLLOW_SYSTEM(null, "跟随系统"),
    CHINESE("zh-CN", "简体中文"),
    ENGLISH("en", "English"),
    JAPANESE("ja", "日本語"),
    KOREAN("ko", "한국어"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    SPANISH("es", "Español"),
    PORTUGUESE("pt", "Português"),
    RUSSIAN("ru", "Русский");

    companion object {
        fun fromStoredName(raw: String?): AppLanguage = entries.firstOrNull { it.name == raw } ?: FOLLOW_SYSTEM
    }
}
