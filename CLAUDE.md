# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

FlowReader (心流阅读) is an offline-first Android e-book reader — Jetpack Compose + Material 3, Clean Architecture + MVVM, no network/account layer. Supports EPUB / TXT / PDF / Markdown. Docs and user-facing strings are Chinese; code and identifiers are English.

`AGENTS.md` holds the full running list of behavioral gotchas (reader, Compose, Room). Read it alongside this file; when the two disagree, verify against the source.

## Commands

Toolchain: JDK 17, Android SDK 35 (compileSdk 35 / minSdk 26). The Gradle wrapper pulls Gradle 9.6.1 from a Tencent Cloud mirror, so wrapper download failures are usually mirror-related, not project-related.

```bash
./gradlew assembleDebug            # dev APK
./gradlew assembleRelease          # R8 full-mode minify + resource shrink (signs with the DEBUG config on purpose)
./gradlew testDebugUnitTest        # all JVM unit tests (:app and :domain)
./gradlew verifyKotlinStyle        # ktlint (non-:app modules) + whitespace gate
./gradlew coverageSummary          # enforces the 40% test-breadth file ratio
./gradlew clean                    # when KSP/generated state looks stale
```

Single test class or module:

```bash
./gradlew :app:testDebugUnitTest --tests com.flowreader.app.util.BookParserTest
./gradlew :domain:testDebugUnitTest
```

CI (`.github/workflows/ci.yml`) runs exactly: `verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug`. Never claim a check passed unless you actually ran the Gradle task.

### Two verification gates that trip people up

- **ktlint is applied to every module except `:app`** (see the `subprojects` block in the root `build.gradle.kts`). App-module Kotlin is only checked by the whitespace gate in `verifyKotlinStyle` — which fails the whole build on *any* tab character or trailing whitespace in any `.kt`/`.kts` file in the repo. `.editorconfig` sets 4-space indent, LF, max line 140, `android_studio` ktlint style.
- **`coverageSummary` is a file-count ratio, not line coverage**: `(test files in app + core + feature + domain test source sets) / (app repository impls + app ViewModels + feature ViewModels + every `:core` main source file + domain repository interfaces + domain models)` must be ≥ 40% (currently 55.8%). Adding a new domain model, ViewModel or `:core` file without a test can break the build even though nothing else changed.

## Module layout and its current reality

`:app`, `:core`, `:data`, `:domain`, `:feature:library`, `:feature:reader`.

Allowed dependency direction: `feature:* → core/domain`, `data → core/domain`, `app → core/data/domain/feature:*`. Features must not depend on `:app`; `:data` must not depend on features; `:domain` stays free of app/Room/Compose/Hilt.

**Important: `:feature:library` and `:feature:reader` currently contain no source files.** They are compiled, linted, and tested placeholders. All screen/ViewModel code still lives in `:app`. `:core` became real in v52 and now holds the design system. Modules holding code today:

- `:domain` — `domain/model/` (data classes, enums like `AppThemeMode`/`ColorSource`/`ReaderPaletteId`/`BookFormat`/`ReaderFontFamily`) and `domain/repository/` (9 repository interfaces, one file each). `domain/usecase/` is an empty leftover directory; business logic lives in ViewModels by deliberate choice.
- `:core` — the design system (v52). `core/designsystem/token/` (`FlowSpacing`/`FlowRadius`/`FlowElevation`/`FlowMotion`/`FlowShapes`/`FlowTypography`/`FlowBrandColors`), `core/designsystem/theme/FlowTheme.kt`, `core/designsystem/reader/` (12 `ReaderPalette`s + `ReaderMetrics`/`ReaderTypography`), `core/designsystem/component/FlowStateHost.kt`, and `core/util/` (`ColorContrast`, `FlowFormatters`, `ReadingProgress`, `ReaderBehavior`). Everything under `core/util/` and `ReaderMetrics` is deliberately Compose-free so it is JVM-testable.
- `:data` — Room only: `AppDatabase` plus 6 DAOs and 6 entities under `data/local/`.
- `:app` — composition root: `MainActivity`, `FlowReaderApplication`, `di/AppModule.kt`, `ui/`, `util/`, `widget/`, and **all repository implementations** in `data/repository/`.

Note the package/namespace mismatch: modules use namespaces like `com.flowreader.domain` and `com.flowreader.data`, but source packages are all `com.flowreader.app.*`. Keep new files in the `com.flowreader.app.*` package tree to match.

When moving code into `:feature:*`, remember those modules have minimal dependency blocks (Compose UI + Material3 + `:domain` only) — Hilt, Room, navigation, and lifecycle deps are declared in `:app` and must be added explicitly if the moved code needs them. `:core` carries Compose UI/foundation/animation/material3 + `:domain` + `core-ktx`, but no Hilt, Room, navigation or Coil.

## Data flow

`Composable → ViewModel → domain repository interface → data repository impl → Room DAO / DataStore`.

Each `ui/screens/<screen>/` directory pairs `*Screen.kt` with `*ViewModel.kt`. Every ViewModel exposes an immutable `StateFlow<XxxUiState>` backed by a private `MutableStateFlow`. Errors use built-in `kotlin.Result`; the old custom `AppException`/`Result` wrapper has been removed.

All Hilt wiring lives in one file, `app/src/main/java/com/flowreader/app/di/AppModule.kt`:
- `DatabaseModule` — builds `AppDatabase` and `@Provides` each DAO.
- `RepositoryModule` — `@Binds` each impl to its domain interface.

A new repository means: interface in `domain/repository/`, impl in `app/data/repository/`, `@Binds` entry in `RepositoryModule`, and (for the coverage gate) a test.

### Three separate persistence stores

1. **Room** (`flowreader_db`) — `AppDatabase` is version 6, `exportSchema = true`, schemas land in `data/schemas/`. Explicit `MIGRATION_4_5` (adds `books.tags`) and `MIGRATION_5_6` (adds the bookmark `(bookId, chapterIndex, position)` index) are registered in `DatabaseModule`. There is intentionally **no** `fallbackToDestructiveMigration()` — every schema change needs a hand-written migration. (`app/schemas/` is an empty leftover from before `AppDatabase` moved to `:data`.)
2. **A second raw SQLite database** (`flowreader_fts.db`) managed by `util/FullTextSearch.kt` via `openOrCreateDatabase` — an FTS5 virtual table (`book_content_fts`) shadowing a `book_content` table, entirely outside Room. This powers both in-book search (injected into `ReaderViewModel`) and global library search (`SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()`). Don't try to fold it into `AppDatabase`.
3. **DataStore Preferences** (`settings`) — `SettingsRepositoryImpl` owns the `Context.dataStore` extension and all preference keys. Since v52 `Navigation.kt` no longer parses the raw `theme` key itself; it consumes `AppShellViewModel.appSettings`. The `reader_theme` key was repurposed to hold a `ReaderPaletteId`, with the old `LIGHT`/`DARK` values migrated in `ReaderPaletteId.fromStoredName`.

`util/CacheManager.kt` is the single chapter-content/metadata/cover cache; it sizes itself from `MemoryManager.getRecommendedCacheSize()` and implements `ComponentCallbacks2` for memory pressure. `ChapterRepositoryImpl` routes chapter reads through it — do not add a second chapter cache.

## Navigation and theme

Routes are the sealed class `Screen` in `ui/Navigation.kt`: `library`, `stats`, `settings`, `wheel`, `book_detail/{bookId}`, `reader/{bookId}?chapterIndex={chapterIndex}`. **Bottom tabs are only Library / Stats / Settings** — the wheel was demoted in v52 to a secondary destination reached from the library top-bar overflow.

`Screen.Reader.createRoute(bookId, chapterIndex = -1)` omits `chapterIndex` entirely when resuming; a non-negative value jumps straight to that chapter. `ReaderScreen` takes no `bookId` parameter — `ReaderViewModel` pulls both args out of `SavedStateHandle` (validate `bookId > 0` before touching the DB, since the default is `0L`).

`FlowTheme` (from `:core`) is applied once, in `FlowReaderNavHost`. Do not add per-screen theme wrappers. The app theme is `AppThemeMode` (`LIGHT`/`DARK`/`FOLLOW_SYSTEM`) plus `ColorSource` (`BRAND` default / `DYNAMIC` wallpaper). The reader's 12 `ReaderPalette`s and its time-based `autoNightMode` (19:00–07:00, re-evaluated every minute) are separate and never change the app theme.

## Highest-value behavioral constraints

Reader (`ReaderViewModel` is ~780 lines and holds most of this):
- `goToChapter()` must load chapter *content* before assigning `currentChapter`; assigning a metadata-only chapter renders a blank reader.
- Every reader preference goes through the single `ReaderViewModel.updateReadingSettings(ReadingSettings)`; do not reintroduce per-field mutators.
- Reader styling must come from `:core`'s `readerBodyStyle`/`paragraphSpacing`/`ReaderMetrics`. Hard-coding `MaterialTheme.typography.bodyLarge` in `ReaderContent` is what made the font, custom-font and paragraph-spacing settings dead before v52.
- `PageMode` has exactly two values (`SLIDE`, `NONE`) and both are rendered. Do not add a mode without implementing it — `SIMULATION`/`CURL`/`SLIDE_OVER` were removed for exactly that reason.
- Progress saves are debounced 3s (`debouncedSaveProgress()`). Reading stats auto-save every 30s, on chapter change, and in `onCleared()`; page counts come from real chapter-character deltas, partial-page characters carry across scroll updates, and a pause over 5 minutes starts a new session.
- Per-chapter scroll positions live in `ReaderViewModel.chapterPositions`; keep `ReaderScreen` restoration aligned with `uiState.currentPosition`.
- `TtsManager` wraps Android `TextToSpeech`, exposes `StateFlow<TtsState>`, is lazily initialized (eager init used to crash reader launch), and must be `shutdown()` in `onCleared()`.
- The home-screen widget reads DataStore keys `widget_book_title` / `widget_progress_percent`, written by `ReaderViewModel.updateWidgetSnapshot()`.

Compose:
- Never nest a `LazyColumn` inside another `LazyColumn` item — use a plain `Column` (unbounded-height crash).
- `WheelViewModel.spin()` is a plain function that launches its own coroutine; don't call it from `LaunchedEffect` as if it were suspending.
- Wheel animation is elapsed-time based (`System.nanoTime()` + `delay(16L)`); `WheelScreen` isolates `error`/`result` with `derivedStateOf` so 60 FPS `rotationAngle` updates don't recompose unrelated UI. Preserve both.

Data:
- `BackupRepositoryImpl.importData()` runs inside `database.withTransaction` — keep backup import atomic.

## Version bookkeeping

`versionCode`/`versionName` live in `app/build.gradle.kts` (currently 5200 / "52.0.0") and `SettingsScreen` surfaces `BuildConfig.VERSION_NAME`. Releases are one commit per version with a matching `CHANGELOG.md` entry (`vNN.N.N: summary`); `ROADMAP.md` tracks the longer arc and the acknowledged tech debt.
