# FlowReader

Offline-first Android e-book reader. Gradle modules are `:app`, `:core`, `:data`, `:domain`, `:feature:library`, and `:feature:reader`.

## Commands
- `./gradlew assembleDebug` builds the development APK.
- `./gradlew assembleRelease` builds the minified/shrunk release APK; release intentionally uses the debug signing config in `app/build.gradle.kts`.
- `./gradlew testDebugUnitTest` runs the JVM unit tests. Current focused test files include `app/src/test/java/com/flowreader/app/util/BookParserTest.kt` and domain model tests under `domain/src/test/java/`.
- `./gradlew testDebugUnitTest --tests com.flowreader.app.util.BookParserTest` runs the existing focused test class.
- `./gradlew verifyKotlinStyle` runs ktlint for non-app modules plus the lightweight whitespace gate.
- `./gradlew coverageSummary` enforces the 40% test breadth target across app/core/feature/domain (55.8% as of v52).
- `./gradlew clean` is available when generated/KSP state looks stale.
- CI is in `.github/workflows/ci.yml`; do not claim checks ran unless you ran a Gradle task or inspected CI results.

## Toolchain
- JDK 17 and Android SDK 35 are required by Gradle config.
- Gradle wrapper downloads Gradle `9.6.1` from `mirrors.cloud.tencent.com`; network issues may be mirror-related.
- AGP `8.6.0`, Kotlin `2.1.0`, Compose BOM `2024.12.01`, Hilt `2.55`, Room `2.6.1`.
- Room schemas are exported to `data/schemas/`; KSP generated output is under each module's `build/generated/ksp/`.

## Architecture
- Entry points: `MainActivity.kt`, `FlowReaderApplication.kt`, and root navigation/theme in `ui/Navigation.kt`.
- Flow is `Composable -> ViewModel -> domain repository interface -> data repository impl -> Room DAO`.
- `:app` is the current composition root; `:domain` owns models/contracts; `:data` owns Room local storage; `:core` owns the design system (tokens, `FlowTheme`, 12 reader palettes, Compose-free reader/format/contrast helpers) since v52; `:feature:*` are still empty migration boundaries.
- `ui/screens/<screen>/` pairs `*Screen.kt` with `*ViewModel.kt`; each ViewModel exposes immutable `StateFlow<XxxUiState>` backed by private `MutableStateFlow`.
- Domain repository interfaces are separate files in `domain/repository/`; implementations are in `data/repository/` and bound in `di/AppModule.kt`.
- `di/AppModule.kt` contains `DatabaseModule` DAO providers and `RepositoryModule` Hilt `@Binds` entries. Add bindings there for new repos.

## Navigation And Theme
- Routes live in sealed class `Screen` in `ui/Navigation.kt`: `library`, `stats`, `settings`, `wheel`, `book_detail/{bookId}`, `reader/{bookId}?chapterIndex={chapterIndex}`.
- `Screen.Reader.createRoute(bookId, chapterIndex = -1)` omits `chapterIndex` when resuming; non-negative values jump directly to a chapter.
- Bottom tabs are only Library, Stats, and Settings; the wheel is a secondary destination opened from the library top-bar overflow (demoted in v52).
- App theme is `AppThemeMode` (LIGHT/DARK/FOLLOW_SYSTEM) plus `ColorSource` (BRAND default, DYNAMIC opt-in). It is applied once in `FlowReaderNavHost` via `:core`'s `FlowTheme`; do not add per-screen theme wrappers.
- Reader colors are the 12 `ReaderPalette`s in `:core`, selected by `ReadingSettings.palette` / `nightPalette`. Contrast for all 12 is asserted against WCAG AA in `ReaderPaletteContrastTest`.
- `SettingsScreen` displays the app version from `BuildConfig.VERSION_NAME`.

## Room And Data
- `AppDatabase` is version `6`, has 6 entities/DAOs, and `exportSchema = true`.
- Room has explicit `MIGRATION_4_5` adding `books.tags` and `MIGRATION_5_6` adding the bookmark `(bookId, chapterIndex, position)` index; do not use `fallbackToDestructiveMigration()`.
- `BackupRepositoryImpl.importData()` uses `database.withTransaction`; keep backup import atomic.
- `ChapterRepositoryImpl` routes chapter metadata/content through `CacheManager`; avoid adding a second chapter-content cache.
- Code uses built-in `kotlin.Result` where needed; the old custom `AppException`/`Result` wrapper is gone.

## Reader Gotchas
- `ReaderViewModel` reads `bookId` and optional `chapterIndex` from `SavedStateHandle`; validate `bookId > 0` before DB work.
- `goToChapter()` must load chapter content before setting `currentChapter`; setting metadata-only chapters causes blank reader content.
- Reading progress saves are debounced by 3 seconds in `debouncedSaveProgress()`.
- Reading stats are auto-saved every 30 seconds, on chapter change, and in `onCleared()`; page counts come from real chapter-character deltas, unfinished page characters carry across scroll updates, and pauses over 5 minutes split a new session.
- `ReadingStatsRepository.getRecentDailyStats()` must aggregate rows by date before charting; `getReadingReport(days)` powers weekly/monthly reports and goals.
- Eye protection reminder interval is persisted in `ReadingSettings.eyeProtectionIntervalMinutes` and exposed as 15/20/30/45/60 minute chips in `ReaderSettingsSheet`.
- Every reader preference is written through the single `ReaderViewModel.updateReadingSettings(ReadingSettings)`; per-field mutators were removed in v52.
- Reader text styles must come from `:core` (`readerBodyStyle`, `paragraphSpacing`, `ReaderMetrics`). Hard-coding `bodyLarge` is what made font/custom-font/paragraph-spacing settings inert before v52.
- `PageMode` is `SLIDE` + `NONE` only, and both are actually rendered (`SLIDE` animates the chapter scroll, `NONE` jumps). Never add a mode ahead of its implementation.
- Tap zones, swipes, double tap and long press are resolved by `ReaderBehavior` in `:core` from `GestureSettings`; auto night mode is driven by a one-minute ticker, not a single composition-time `Calendar` read.
- Reader scroll position is remembered per chapter in `ReaderViewModel.chapterPositions`; keep `ReaderScreen` scroll restoration aligned with `uiState.currentPosition`.
- `FullTextSearch` is injected into `ReaderViewModel`; chapters are indexed after book load and `SearchDialog` navigates to matching chapters.
- `TtsManager` wraps Android `TextToSpeech` and exposes `StateFlow<TtsState>`; `ReaderViewModel` observes it for button state, speaks from `currentPosition`, and calls `shutdown()` in `onCleared()`.
- Reading progress Widget uses DataStore keys `widget_book_title` and `widget_progress_percent`, updated from `ReaderViewModel.updateWidgetSnapshot()`.
- `ReadingSettings.autoNightMode` is time-based in `ReaderScreen` (19:00-07:00 dark); it does not change the global app theme.
- Reader font selection uses `ReaderFontFamily` with 4 resolvable faces in `domain/model/ReadingSettings.kt`; imported `.ttf/.otf` files win over the built-in face and fall back silently when unreadable.
- Bookmark entry points are active in `ReaderControls`; long-pressing a paragraph opens `ParagraphActionSheet`, which highlights exactly that paragraph, copies it, or adds a bookmark note. The v51 `HighlightMenu` that asked the user to type the highlight text is gone.
- Bookmark repository normalizes text, validates positive IDs, stores via `addBookmark()`, and bookmarks are sorted by chapter/position for stable navigation.

## Compose/UI Gotchas
- Never put a `LazyColumn` inside another `LazyColumn` item; use a plain `Column` for nested lists to avoid unbounded-height crashes.
- `WheelViewModel.spin()` is a regular function that launches its own coroutine; do not call it from a `LaunchedEffect` as if it were suspend.
- Wheel animation timing uses `System.nanoTime()` plus `delay(16L)`; preserve elapsed-time-based animation rather than reintroducing fixed-step loops.
- `WheelScreen` isolates `error` and `result` with `derivedStateOf` so 60 FPS `rotationAngle` updates do not recompose unrelated UI.
- Library filtering combines category chips, search query, and sort order in `LibraryViewModel`; keep `selectedCategoryId` reflected in `LibraryUiState` for UI chips.
- Global search uses `SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()` from the library search bar; results must include source book title.
