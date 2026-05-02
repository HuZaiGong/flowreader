# FlowReader — Agent Guide

Android ebook reader (EPUB/TXT/PDF/Markdown). Jetpack Compose + MVVM + Hilt. Offline-first, no network features.

## Build & Test

```bash
./gradlew assembleDebug           # Debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease         # Release APK (R8 minified, needs signing config)
./gradlew :app:testDebugUnitTest  # Unit tests (JUnit 4)
./gradlew clean                   # Clean build
```

- JDK 17 required. compileSdk/targetSdk = 35, minSdk = 26.
- Release build enables `isMinifyEnabled` + `isShrinkResources` (see `app/build.gradle.kts:30-36`).
- No lint/detekt/ktlint configured. Only `.editorconfig` for style.

## Versions (verify in `build.gradle.kts` / `app/build.gradle.kts`)

| Component | Version |
|-----------|---------|
| AGP | 8.6.0 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.27 |
| Hilt | 2.50 |
| Gradle wrapper | 8.7 |
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 |
| app versionName | 41.0.0 |

## Structure

Single module: `:app`. Source root: `app/src/main/java/com/flowreader/app/`

```
├── MainActivity.kt              # Entry point, sets Compose content
├── FlowReaderApplication.kt     # @HiltAndroidApp (NOT FlowReaderApp)
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room DB v4, name="flowreader_db"
│   │   ├── dao/                 # BookDao, ChapterDao, BookmarkDao, AnnotationDao, CategoryDao, ReadingStatsDao
│   │   └── entity/              # Matching entities for each DAO
│   └── repository/              # Impl classes + SettingsRepository, DataManager, BackupRepository
├── di/
│   └── AppModule.kt             # Contains BOTH DatabaseModule (provides) AND RepositoryModule (binds)
├── domain/
│   ├── model/                   # Book, ReadingSettings, Annotation, AppException, WheelItem, etc.
│   ├── repository/              # Interfaces: BookRepository, BackupRepository, ReadingStatsRepository
│   └── usecase/                 # GetBookUseCase, SaveProgressUseCase, TextPaginator
├── ui/
│   ├── FlowReaderApp.kt         # Root composable is FlowReaderRoot() (NOT FlowReaderApp)
│   ├── Navigation.kt            # Sealed Screen class, NavHost, bottom nav (4 tabs)
│   ├── theme/                   # Color.kt, Theme.kt, Typography.kt
│   └── screens/
│       ├── library/             # LibraryScreen + ViewModel
│       ├── bookdetail/          # BookDetailScreen + ViewModel
│       ├── reader/              # ReaderScreen + ViewModel + components/
│       ├── settings/            # SettingsScreen + ViewModel
│       ├── stats/               # StatsScreen + ViewModel
│       └── wheel/               # WheelScreen + ViewModel + components/WheelSpinner
└── util/                        # BookParser, BookLoader, TtsManager, FullTextSearch, MemoryManager, CacheManager
```

## Architecture Notes

- **Naming collision resolved (v35)**: Application = `FlowReaderApplication`, root composable = `FlowReaderRoot` (in `ui/FlowReaderApp.kt`). The old name `FlowReaderApp` is gone.
- **Hilt modules**: Both `DatabaseModule` (provides DB + DAOs) and `RepositoryModule` (binds interfaces) live in the single file `di/AppModule.kt`.
- **Navigation**: 4 bottom tabs — Library, Wheel, Stats, Settings. Reader and BookDetail are non-tab routes. See `Navigation.kt:29-40`.
- **ReadingSettings** (`domain/model/ReadingSettings.kt`) defines 10 `ReaderTheme` values (LIGHT, DARK, SEPIA, PAPER, AMOLED, SYSTEM, MORNING, NOON, EVENING, NIGHT), 5 `PageMode` values, 8 `FontFamily` values, gesture/texture/sound enums.
- **3-second debounce** for progress save in ReaderViewModel.
- **Test framework**: JUnit 4 (not JUnit 5). Tests at `app/src/test/java/com/flowreader/app/util/BookParserTest.kt`.
- **No preloaded books**: `app/src/main/assets/books/` exists but is empty. Users import their own.
- **PDF rendering**: Android native `PdfRenderer`, no external PDF library.
- **EPUB rendering**: Readium Kotlin Toolkit 3.1.2 (readium-shared, readium-streamer, readium-navigator, readium-opds).

## CI (`.github/workflows/build.yml`)

- PR → `build` job: unit tests + debug APK upload.
- Push to main → `build-and-release` job: tests + debug + release APK + GitHub release.
- `build-and-release` uses `permissions: contents: write` (broad permission).
- Release tag/name is **dynamically read** from `app/build.gradle.kts` `versionName` via grep step.
- Uses `actions/cache@v3` (not v4).

## Gotchas

- `gradle.properties` declares `org.gradle.jvmargs` **twice** (line 2 and line 23); the second overrides the first.
- README.md and index.html contain outdated info (version badge says 34.0.0, claims 5 themes, says Kotlin 1.9+). Trust `build.gradle.kts` over docs.
- `coreLibraryDesugaring` is enabled (`app/build.gradle.kts:45,62`) for java.time API backport.
