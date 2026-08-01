package com.flowreader.app.domain.model

/** How the library shelf lays out its books (v55): cover grid or detail list. */
enum class LibraryViewMode(val displayName: String) {
    GRID("网格"),
    LIST("列表");

    companion object {
        fun fromStoredName(raw: String?): LibraryViewMode = entries.firstOrNull { it.name == raw } ?: LIST
    }
}
