# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

FlowReader (心流阅读) is an offline-first Android e-book reader — Jetpack Compose + Material 3, Clean Architecture + MVVM, no network/account layer. Supports EPUB / TXT / PDF / Markdown / FB2 / MOBI / comic archives (JPG/PNG/WebP/ZIP/CBZ). Docs and user-facing strings are Chinese; code and identifiers are English.

`AGENTS.md` holds the full running list of behavioral gotchas (reader, Compose, Room, security). Read it alongside this file; when the two disagree, verify against the source.

## Commands

Toolchain: JDK 17, Android SDK 35 (compileSdk 35 / minSdk 26). The Gradle wrapper pulls Gradle 9.6.1 from a Tencent Cloud mirror, so wrapper download failures are usually mirror-related, not project-related.

```bash
./gradlew assembleDebug            # dev APK
./gradlew assembleRelease          # R8 full-mode minify + resource shrink (signs with the DEBUG config on purpose)
./gradlew testDebugUnitTest        # all JVM unit tests (:app, :core, :domain, :feature:reader)
./gradlew verifyKotlinStyle        # ktlint (non-:app modules) + whitespace gate
./gradlew coverageSummary          # enforces the 40% test-breadth file ratio (currently 71.0%)
./gradlew verifyRoborazziDebug     # screenshot regression gate
./gradlew performanceBaseline      # APK size tracking vs baseline/apk-size.properties
./gradlew clean                    # when KSP/generated state looks stale
```

Single test class or module:

```bash
./gradlew :app:testDebugUnitTest --tests com.flowreader.app.util.BookParserTest
./gradlew :domain:testDebugUnitTest
```

CI (`.github/workflows/ci.yml`) runs exactly: `verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug` → `verifyRoborazziDebug` → `performanceBaseline`. Never claim a check passed unless you actually ran the Gradle task.

### Two verification gates that trip people up

- **ktlint is applied to every module except `:app`** (see the `subprojects` block in the root `build.gradle.kts`). App-module Kotlin is only checked by the whitespace gate in `verifyKotlinStyle` — which fails the whole build on *any* tab character or trailing whitespace in any `.kt`/`.kts` file in the repo. `.editorconfig` sets 4-space indent, LF, max line 140, `android_studio` ktlint style.
- **`coverageSummary` is a file-count ratio, not line coverage**: `(test files in app + core + feature + domain test source sets) / (app repository impls + app ViewModels + feature ViewModels + every :core main source file + domain repository interfaces + domain models)` must be ≥ 40% (currently 71.0%). Adding a new domain model, ViewModel or `:core` file without a test can break the build even though nothing else changed.

## Module layout and its current reality

`:app`, `:core`, `:data`, `:domain`, `:feature:library`, `:feature:reader`.

Allowed dependency direction: `feature:* → core/domain`, `data → core/domain`, `app → core/data/domain/feature:*`. Features must not depend on `:app`; `:data` must not depend on features; `:domain` stays free of app/Room/Compose/Hilt.

**Important: `:feature:library` contains no source files yet.** It is a compiled, linted, and tested placeholder. `:feature:reader` now holds `ChapterPaginator`, `ReaderProgressEngine`, `ReaderSessionTracker` (all unit-tested). All screen/ViewModel code still lives in `:app`. `:core` became real in v52 and now holds the design system. Modules holding code today:

- `:domain` — `domain/model/` (data classes, enums like `AppThemeMode`/`ColorSource`/`ReaderPaletteId`/`BookFormat`/`ReaderFontFamily`/`PageMode`) and `domain/repository/` (10 repository interfaces, one file each). `domain/usecase/` is an empty leftover directory; business logic lives in ViewModels by deliberate choice.
- `:core` — the design system (v52). `core/designsystem/token/` (`FlowTokens`/`FlowBrandColors`/`FlowTypography`), `core/designsystem/theme/FlowTheme.kt`, `core/designsystem/reader/` (12 `ReaderPalette`s + `ReaderMetrics`/`ReaderTypography`), `core/designsystem/component/` (`BookCover`, `FlowScaffold`, `FlowTopBar`, `FlowStateHost`, `SkeletonBox`), and `core/util/` (`ColorContrast`, `FlowFormatters`, `ReadingProgress`, `ReaderBehavior`, `ReaderCustomTheme`, `CoverArt`, exporters). Everything under `core/util/` and `ReaderMetrics` is deliberately Compose-free so it is JVM-testable.
- `:data` — Room only: `AppDatabase` plus 7 DAOs and 8 entities under `data/local/`.
- `:app` — composition root: `MainActivity`, `FlowReaderApplication`, `di/AppModule.kt`, `ui/`, `util/`, `widget/`, and **all repository implementations** in `data/repository/`.

Note the package/namespace mismatch: modules use namespaces like `com.flowreader.domain` and `com.flowreader.data`, but source packages are all `com.flowreader.app.*`. Keep new files in the `com.flowreader.app.*` package tree to match.

When moving code into `:feature:*`, remember those modules have minimal dependency blocks (Compose UI + Material3 + `:domain` only) — Hilt, Room, navigation, and lifecycle deps are declared in `:app` and must be added explicitly if the moved code needs them. `:core` carries Compose UI/foundation/animation/material3 + `:domain` + `core-ktx`, but no Hilt, Room, navigation or Coil.

## Data flow

`Composable → ViewModel → domain repository interface → data repository impl → Room DAO / DataStore`.

Each `ui/screens/<screen>/` directory pairs `*Screen.kt` with `*ViewModel.kt`. Every ViewModel exposes an immutable `StateFlow<XxxUiState>` backed by a private `MutableStateFlow`. Errors use built-in `kotlin.Result`; the old custom `AppException`/`Result` wrapper has been removed.

All Hilt wiring lives in one file, `app/src/main/java/com/flowreader/app/di/AppModule.kt`:
- `DatabaseModule` — builds `AppDatabase` and `@Provides` each DAO.
- `RepositoryModule` — `@Binds` each impl to its domain interface (10 bindings).

A new repository means: interface in `domain/repository/`, impl in `app/data/repository/`, `@Binds` entry in `RepositoryModule`, and (for the coverage gate) a test.

### Three separate persistence stores

1. **Room** (`flowreader_db`) — `AppDatabase` is version 7, `exportSchema = true`, schemas land in `data/schemas/`. Explicit `MIGRATION_4_5` (adds `books.tags`), `MIGRATION_5_6` (adds the bookmark `(bookId, chapterIndex, position)` index), and `MIGRATION_6_7` (adds `reading_lists` + `reading_list_items` tables) are registered in `DatabaseModule`. There is intentionally **no** `fallbackToDestructiveMigration()` — every schema change needs a hand-written migration. (`app/schemas/` is an empty leftover from before `AppDatabase` moved to `:data`.)
2. **A second raw SQLite database** (`flowreader_fts.db`) managed by `util/FullTextSearch.kt` via `openOrCreateDatabase` — an FTS5 virtual table (`book_content_fts`) shadowing a `book_content` table, entirely outside Room. This powers both in-book search (injected into `ReaderViewModel`) and global library search (`SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()`). `SearchRepositoryImpl` serializes index rebuilds with a `Mutex` (v56.4) so concurrent searches cannot interleave `deleteAllContent`/`indexChapter`. Don't try to fold it into `AppDatabase`.
3. **DataStore Preferences** (`settings`) — `SettingsRepositoryImpl` owns the `Context.dataStore` extension and all preference keys. Since v52 `Navigation.kt` no longer parses the raw `theme` key itself; it consumes `AppShellViewModel.appSettings`. The `reader_theme` key was repurposed to hold a `ReaderPaletteId`, with the old `LIGHT`/`DARK` values migrated in `ReaderPaletteId.fromStoredName`.

`util/CacheManager.kt` is the single chapter-content/metadata/cover cache; it sizes itself from `MemoryManager.getRecommendedCacheSize()` and implements `ComponentCallbacks2` for memory pressure. `ChapterRepositoryImpl` routes chapter reads through it — do not add a second chapter cache. Since v54.5 it adapts per-book chapter capacity (2–12) to hit rate sampled every 50 accesses.

## Navigation and theme

Routes are the sealed class `Screen` in `ui/Navigation.kt`: `library`, `stats`, `settings` (bottom tabs / navigation rail when `screenWidthDp >= 600`), `wheel`, `notes`, `reading_lists`, `opds` (secondary destinations reached from library overflow), `search?query={query}`, `book_detail/{bookId}`, `reader/{bookId}?chapterIndex={chapterIndex}`. **Bottom tabs are only Library / Stats / Settings** — the wheel was demoted in v52 to a secondary destination reached from the library top-bar overflow.

`Screen.Reader.createRoute(bookId, chapterIndex = -1)` omits `chapterIndex` entirely when resuming; a non-negative value jumps straight to that chapter. `ReaderScreen` takes no `bookId` parameter — `ReaderViewModel` pulls both args out of `SavedStateHandle` (validate `bookId > 0` before touching the DB, since the default is `0L`).

`FlowTheme` (from `:core`) is applied once, in `FlowReaderNavHost`. Do not add per-screen theme wrappers. The app theme is `AppThemeMode` (`LIGHT`/`DARK`/`FOLLOW_SYSTEM`) plus `ColorSource` (`BRAND` default / `DYNAMIC` wallpaper). The reader's 12 `ReaderPalette`s and its time-based `autoNightMode` (19:00–07:00, re-evaluated every minute) are separate and never change the app theme.

In-app language switch (`AppLanguage`: zh default, en, ja, ko, de, es, fr, pt, ru): `FlowLocaleProvider` wraps `LocalContext`/`LocalConfiguration`/`LocalLayoutDirection` with a `ContextWrapper` — a bare `createConfigurationContext()` result is not an Activity and makes `hiltViewModel()`'s `findActivity()` throw. Route titles in `Screen` are `@StringRes`, never string literals — the v53 language switch freezes string-bearing statics at first composition.

## Highest-value behavioral constraints

### Reader

`ReaderViewModel` is ~780 lines and holds most of this logic:

- `goToChapter()` must load chapter *content* before assigning `currentChapter`; assigning a metadata-only chapter renders a blank reader.
- Every reader preference goes through the single `ReaderViewModel.updateReadingSettings(ReadingSettings)`; do not reintroduce per-field mutators.
- Reader styling must come from `:core`'s `readerBodyStyle`/`paragraphSpacing`/`ReaderMetrics`. Hard-coding `MaterialTheme.typography.bodyLarge` in `ReaderContent` is what made the font, custom-font and paragraph-spacing settings dead before v52.
- `PageMode` has exactly three values (`SLIDE`, `PAGED`, `NONE`) and all three are rendered. `SLIDE` animates chapter scroll, `PAGED` renders measured pages in a `HorizontalPager` (`PagedReader`; `ChapterPaginator` splits oversized paragraphs on raw offsets so highlight/bookmark ranges survive re-pagination), `NONE` jumps. Comic books use `ComicReader` (SLIDE = swipe per page, NONE = vertical `LazyColumn` virtualized list). Do not add a mode without implementing it — `SIMULATION`/`CURL`/`SLIDE_OVER` were removed for exactly that reason, and `docs/page_turn_evaluation.md` (v56) decided simulated page curl stays unimplemented.
- Progress saves are debounced 3s (`debouncedSaveProgress()`). Reading stats auto-save every 30s, on chapter change, and in `onCleared()` (since v56.3 `onCleared()` runs its save in an independent IO scope, not `viewModelScope` which is already cancelled); page counts come from real chapter-character deltas, partial-page characters carry across scroll updates, and a pause over 5 minutes starts a new session.
- Per-chapter scroll positions live in `ReaderViewModel.chapterPositions`; keep `ReaderScreen` restoration aligned with `uiState.currentPosition`.
- `TtsManager` wraps Android `TextToSpeech`, exposes `StateFlow<TtsState>`, is lazily initialized (eager init used to crash reader launch), and must be `shutdown()` in `onCleared()`. `ReaderTtsCoordinator` handles speak-from-position logic.
- The home-screen widget reads DataStore keys `widget_book_title` / `widget_progress_percent`, written by `ReaderViewModel.updateWidgetSnapshot()`.
- Native text selection (v54): long-press opens the in-house engine in `ReaderContent` (`ReaderParagraph` + `ReaderSelectionBar`), drag-extend and draggable handles; the action bar highlights / copies / bookmarks the exact range. `ReaderTextMapping` maps display offsets back to raw chapter offsets (pure, tested). Do NOT use the platform `SelectionContainer` hoisted-selection overload — it is `internal` through Compose 1.9.x.
- Tap zones, swipes, double tap and long press are resolved by `ReaderBehavior` in `:core` from `GestureSettings`. Reader progress math lives in `ReaderProgressEngine` (feature:reader), session timing/EMA speed in `ReaderSessionTracker` (injectable clock).

### Compose

- Never nest a `LazyColumn` inside another `LazyColumn` item — use a plain `Column` (unbounded-height crash).
- `WheelViewModel.spin()` is a plain function that launches its own coroutine; don't call it from `LaunchedEffect` as if it were suspending.
- Wheel animation is elapsed-time based (`System.nanoTime()` + `delay(16L)`); `WheelScreen` isolates `error`/`result` with `derivedStateOf` so 60 FPS `rotationAngle` updates don't recompose unrelated UI. Preserve both.

### Data

- `BackupRepositoryImpl.importData()` runs inside `database.withTransaction` — keep backup import atomic. Backup import/export capped at 200MB.
- `BookParser` caps reads: 16MB per EPUB chapter, 24MB per embedded image, 128MB for whole TXT/MD/FB2/MOBI files; oversized entries are skipped or fail with a clear message.
- `ZipImporter` / `ZipImportRules` guard ZIP/CBZ imports: absolute paths and `..` rejected (zip-slip), `__MACOSX` and hidden entries skipped, entry count and per-entry size capped, only parser-supported extensions let through.

## Security considerations

The project is offline-first and privacy-minded — keep it that way. v56.3/v56.4 were dedicated security audits; the following are hard constraints:

- **Single `INTERNET` permission**, used only by the LAN OPDS client. No accounts, analytics, crash reporting, or sync.
- **OPDS is LAN-only**: `OpdsAddress` restricts reachability to loopback / RFC1918 / RFC4193 / `.local`-style names and re-validates **every redirect hop**; public hosts are unreachable. Catalog reads capped at 2MB, downloads at 200MB, acquisition links MIME-filtered.
- **Import caps everywhere**: `BookParser` read limits, `ZipImportRules` zip-slip/entry caps, backup import 200MB on both SAF and LAN paths, `ACTION_VIEW` ("open with FlowReader") imports run through the same capped pipeline and are consumed once (`intent.data`/`EXTRA_STREAM` cleared) so configuration changes cannot re-import.
- **No DRM circumvention**: DRM-protected MOBI/AZW files are rejected outright; HUFF/CDIC-compressed files are rejected rather than half-decoded.
- **Backup/export contains no executables**: backup import is a single atomic transaction; cloud backup includes shared prefs only — the DB never leaves the device via Android Backup.
- **ContentProvider** (`com.flowreader.app.provider`) exposes read-only book metadata and progress — no file paths, no book text; writes are refused.
- **LAN transfer** (`LanTransferServer`/`LanTransferClient`): random-token-protected, binds the discovered LAN interface (never `0.0.0.0` since v56.4), and the HTTP server stops when the dialog closes.
- **Sanitization**: FTS queries escaped (`FullTextSearch.escapeFtsQuery()`), HTML/Markdown annotation exports escaped, `OpdsClient` filters MIME types.
- No WebView anywhere. The v56.3 audit gate requires **no new `!!`** (the only remaining one is the ContentProvider's standard `context!!` in `onCreate`).

## Version bookkeeping

`versionCode`/`versionName` live in `app/build.gradle.kts` (currently 5630 / "56.3.0") and `SettingsScreen` surfaces `BuildConfig.VERSION_NAME`. The newest `CHANGELOG.md` entry is v56.4.0 — the v56.4.0 commit did not bump `versionName` in the build file. Releases are one commit per version with a matching `CHANGELOG.md` entry (`vNN.N.N: summary`); `ROADMAP.md` tracks the longer arc and the acknowledged tech debt.

**v56.4.1 security fixes (2026-08-07)**: Fixed CVE-LOCAL-001 (token generation algorithm bug in `LanTransferServer`), removed `runBlocking` from `ContentProvider` by adding sync DAO methods, narrowed `FileProvider` paths from root to subdirectories, and documented backup rules. See `SECURITY_AUDIT_REPORT.md` and `SECURITY_FIX_SUMMARY.md` for full details.
