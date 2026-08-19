package com.flowreader.app.domain.model

/**
 * The 12 built-in app color presets (v56.6). Selecting one re-seeds the whole Material scheme —
 * it is not a single accent swap.
 *
 * Only the identity lives here; the seed values and the generated tonal roles are a rendering
 * concern and live in `:core` (`FlowColorPresets` / `SeedColorScheme`), exactly like
 * [ReaderPaletteId] and `ReaderPalettes`.
 *
 * [VIOLET] is the historical brand seed and stays the default. It is special-cased in `:core` to
 * return the hand-tuned brand scheme verbatim, so existing installs see no color shift.
 */
enum class AppColorPreset(val displayName: String) {
    VIOLET("经典紫"),
    INDIGO("靛蓝"),
    AZURE("晴空蓝"),
    TEAL("青碧"),
    EMERALD("翠绿"),
    MOSS("苔绿"),
    AMBER("琥珀"),
    TANGERINE("橘橙"),
    CRIMSON("绯红"),
    ROSE("玫瑰"),
    PLUM("梅紫"),
    GRAPHITE("石墨");

    companion object {
        val DEFAULT: AppColorPreset = VIOLET

        fun fromStoredName(raw: String?): AppColorPreset = entries.firstOrNull { it.name == raw } ?: DEFAULT
    }
}
