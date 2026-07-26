# FlowReader

Offline-first Android e-book reader. Single Gradle module: `:app`; all Kotlin app code is under `app/src/main/java/com/flowreader/app/`.

## Commands
- `./gradlew assembleDebug` builds the development APK.
- `./gradlew assembleRelease` builds the minified/shrunk release APK; release intentionally uses the debug signing config in `app/build.gradle.kts`.
- `./gradlew testDebugUnitTest` runs the JVM unit tests. Current focused test file: `app/src/test/java/com/flowreader/app/util/BookParserTest.kt`.
- `./gradlew testDebugUnitTest --tests com.flowreader.app.util.BookParserTest` runs the existing focused test class.
- `./gradlew clean` is available when generated/KSP state looks stale.
- There is no repo CI workflow, lint, detekt, or ktlint config at the moment; do not claim those checks ran unless you ran a Gradle task.

## Toolchain
- JDK 17 and Android SDK 35 are required by Gradle config.
- Gradle wrapper downloads Gradle `9.6.1` from `mirrors.cloud.tencent.com`; network issues may be mirror-related.
- AGP `8.6.0`, Kotlin `2.0.21`, Compose BOM `2024.12.01`, Hilt `2.50`, Room `2.6.1`.
- Room schemas are exported to `app/schemas/`; KSP generated output is under `app/build/generated/ksp/`.

## Architecture
- Entry points: `MainActivity.kt`, `FlowReaderApplication.kt`, and root navigation/theme in `ui/Navigation.kt`.
- Flow is `Composable -> ViewModel -> domain repository interface -> data repository impl -> Room DAO`.
- `ui/screens/<screen>/` pairs `*Screen.kt` with `*ViewModel.kt`; each ViewModel exposes immutable `StateFlow<XxxUiState>` backed by private `MutableStateFlow`.
- Domain repository interfaces are separate files in `domain/repository/`; implementations are in `data/repository/` and bound in `di/AppModule.kt`.
- `di/AppModule.kt` contains `DatabaseModule` DAO providers and `RepositoryModule` Hilt `@Binds` entries. Add bindings there for new repos.

## Navigation And Theme
- Routes live in sealed class `Screen` in `ui/Navigation.kt`: `library`, `wheel`, `stats`, `settings`, `book_detail/{bookId}`, `reader/{bookId}?chapterIndex={chapterIndex}`.
- `Screen.Reader.createRoute(bookId, chapterIndex = -1)` omits `chapterIndex` when resuming; non-negative values jump directly to a chapter.
- Bottom tabs are only Library, Wheel, Stats, and Settings.
- `ReaderTheme` has only `LIGHT` and `DARK`. The app theme is applied once in `FlowReaderNavHost` via `FlowReaderTheme`; do not add per-screen theme wrappers.
- `SettingsScreen` displays the app version from `BuildConfig.VERSION_NAME`.

## Room And Data
- `AppDatabase` is version `4`, has 6 entities/DAOs, and `exportSchema = true`.
- `Room.databaseBuilder(...).build()` currently has no `fallbackToDestructiveMigration()` and no explicit migrations. If bumping DB version, add real migrations and schema updates.
- `BackupRepositoryImpl.importData()` uses `database.withTransaction`; keep backup import atomic.
- `ChapterRepositoryImpl` routes chapter metadata/content through `CacheManager`; avoid adding a second chapter-content cache.
- Code uses built-in `kotlin.Result` where needed; the old custom `AppException`/`Result` wrapper is gone.

## Reader Gotchas
- `ReaderViewModel` reads `bookId` and optional `chapterIndex` from `SavedStateHandle`; validate `bookId > 0` before DB work.
- `goToChapter()` must load chapter content before setting `currentChapter`; setting metadata-only chapters causes blank reader content.
- Reading progress saves are debounced by 3 seconds in `debouncedSaveProgress()`.
- Reading stats are auto-saved every 30 seconds, on chapter change, and in `onCleared()`; page counts come from real chapter-character deltas, unfinished page characters carry across scroll updates, and pauses over 5 minutes split a new session.
- Eye protection reminder interval is persisted in `ReadingSettings.eyeProtectionIntervalMinutes` and exposed as 15/20/30/45/60 minute chips in `ReaderSettingsDialog`.
- Reader scroll position is remembered per chapter in `ReaderViewModel.chapterPositions`; keep `ReaderScreen` scroll restoration aligned with `uiState.currentPosition`.
- `FullTextSearch` is injected into `ReaderViewModel`; chapters are indexed after book load and `SearchDialog` navigates to matching chapters.
- Reader font selection uses `FontFamily` with 8 variants in `domain/model/ReadingSettings.kt` and persists through settings/DataStore.
- Bookmark entry points are hidden from current screens, but bookmark data/repository/ViewModel plumbing and some unused UI components still exist.

## Compose/UI Gotchas
- Never put a `LazyColumn` inside another `LazyColumn` item; use a plain `Column` for nested lists to avoid unbounded-height crashes.
- `WheelViewModel.spin()` is a regular function that launches its own coroutine; do not call it from a `LaunchedEffect` as if it were suspend.
- Wheel animation timing uses `System.nanoTime()` plus `delay(16L)`; preserve elapsed-time-based animation rather than reintroducing fixed-step loops.
- `WheelScreen` isolates `error` and `result` with `derivedStateOf` so 60 FPS `rotationAngle` updates do not recompose unrelated UI.
- Library filtering combines category chips, search query, and sort order in `LibraryViewModel`; keep `selectedCategoryId` reflected in `LibraryUiState` for UI chips.
