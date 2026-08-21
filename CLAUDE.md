# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

FlowReader (心流阅读) is an offline-first Android e-book reader — Jetpack Compose + Material 3, Clean Architecture + MVVM, no account or cloud-sync layer. The only network clients are LAN-constrained OPDS and backup transfer. Supports EPUB / TXT / PDF / Markdown / FB2 / MOBI / comic archives (JPG/PNG/WebP/ZIP/CBZ). Docs and user-facing strings are Chinese; code and identifiers are English.

`AGENTS.md` holds the full running list of behavioral gotchas (reader, Compose, Room, security). Read it alongside this file; when the two disagree, verify against the source.

## Commands

Toolchain: JDK 17, Android SDK 35 (compileSdk 35 / minSdk 26). The Gradle wrapper pulls Gradle 9.6.1 from a Tencent Cloud mirror, so wrapper download failures are usually mirror-related, not project-related.

```bash
./gradlew assembleDebug            # dev APK
./gradlew assembleRelease          # R8 full-mode minify + resource shrink (signs with the release keystore when keystore.properties exists, else falls back to debug)
./gradlew testDebugUnitTest        # all JVM unit tests (:app, :core, :domain, :feature:reader)
./gradlew verifyKotlinStyle        # ktlint (non-:app modules) + whitespace gate
./gradlew coverageSummary          # enforces the 40% test-breadth file ratio (currently 85.3%)
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
- **`coverageSummary` is a file-count ratio, not line coverage**: `(test files in app + core + feature + domain test source sets) / (app repository impls + app ViewModels + feature ViewModels + every :core main source file + domain repository interfaces + domain models)` must be ≥ 40% (currently 85.3%). Adding a new domain model, ViewModel or `:core` file without a test can break the build even though nothing else changed.

## Module layout and its current reality

`:app`, `:core`, `:data`, `:domain`, `:feature:library`, `:feature:reader`.

Allowed dependency direction: `feature:* → core/domain`, `data → core/domain`, `app → core/data/domain/feature:*`. Features must not depend on `:app`; `:data` must not depend on features; `:domain` stays free of app/Room/Compose/Hilt.

**Important: `:feature:library` contains no source files yet.** It is a compiled, linted, and tested placeholder. `:feature:reader` now holds `ChapterPaginator`, `ReaderProgressEngine`, `ReaderSessionTracker` (all unit-tested). All screen/ViewModel code still lives in `:app`. `:core` became real in v52 and now holds the design system. Modules holding code today:

- `:domain` — `domain/model/` (data classes, enums like `AppThemeMode`/`ColorSource`/`AppColorPreset`/`ReaderPaletteId`/`BookFormat`/`ReaderFontFamily`/`PageMode`) and `domain/repository/` (10 repository interfaces, one file each). There is no `domain/usecase/` directory (it was deleted, not left empty); business logic lives in ViewModels by deliberate choice.
- `:core` — the design system (v52). `core/designsystem/token/` (`FlowTokens`/`FlowBrandColors`/`FlowColorPresets`/`FlowTypography`), `core/designsystem/theme/FlowTheme.kt`, `core/designsystem/reader/` (18 `ReaderPalette`s + `ReaderMetrics`/`ReaderTypography`), `core/designsystem/component/` (`BookCover`, `FlowScaffold`, `FlowTopBar`, `FlowStateHost`, `SkeletonBox`), and `core/util/` (`ColorContrast`, `ColorSpaces`, `SeedColorScheme`, `ColorWheelMath`, `FlowFormatters`, `ReadingProgress`, `ReaderBehavior`, `ReaderCustomTheme`, `CoverArt`, exporters). Everything under `core/util/` and `ReaderMetrics` is deliberately Compose-free so it is JVM-testable.
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
2. **A second raw SQLite database** (`flowreader_fts.db`) managed by `util/FullTextSearch.kt` via `openOrCreateDatabase` — an FTS5 virtual table (`book_content_fts`) shadowing a `book_content` table, entirely outside Room. This powers both in-book search (injected into `ReaderViewModel`) and global library search (`SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()`). Index mutations are serialized by `FullTextSearch.withIndexLock()` — the lock lives on the shared object (v56.4.3) because the reader's per-book re-index bypassed the one `SearchRepositoryImpl` held in v56.4; it is not re-entrant. Book-scoped queries must compare `book_id` through `CAST(? AS INTEGER)` (FTS5 columns have no affinity — see the version notes). Don't try to fold it into `AppDatabase`.
3. **DataStore Preferences** (`settings`) — `SettingsRepositoryImpl` owns the `Context.dataStore` extension and all preference keys. Since v52 `Navigation.kt` no longer parses the raw `theme` key itself; it consumes `AppShellViewModel.appSettings`. The `reader_theme` key was repurposed to hold a `ReaderPaletteId`, with the old `LIGHT`/`DARK` values migrated in `ReaderPaletteId.fromStoredName`.

`util/CacheManager.kt` is the single chapter-content/metadata/cover cache; it sizes itself from `MemoryManager.getRecommendedCacheSize()` and implements `ComponentCallbacks2` for memory pressure. `ChapterRepositoryImpl` routes chapter reads through it — do not add a second chapter cache. Since v54.5 it adapts per-book chapter capacity (2–12) to hit rate sampled every 50 accesses.

## Navigation and theme

Routes are the sealed class `Screen` in `ui/Navigation.kt`: `library`, `stats`, `settings` (bottom tabs / navigation rail when `screenWidthDp >= 600`), `wheel`, `notes`, `reading_lists`, `opds` (secondary destinations reached from library overflow), `search?query={query}`, `book_detail/{bookId}`, `reader/{bookId}?chapterIndex={chapterIndex}`. **Bottom tabs are only Library / Stats / Settings** — the wheel was demoted in v52 to a secondary destination reached from the library top-bar overflow.

`Screen.Reader.createRoute(bookId, chapterIndex = -1)` omits `chapterIndex` entirely when resuming; a non-negative value jumps straight to that chapter. `ReaderScreen` takes no `bookId` parameter — `ReaderViewModel` pulls both args out of `SavedStateHandle` (validate `bookId > 0` before touching the DB, since the default is `0L`).

`FlowTheme` (from `:core`) is applied once, in `FlowReaderNavHost`. Do not add per-screen theme wrappers. The app theme is `AppThemeMode` (`LIGHT`/`DARK`/`FOLLOW_SYSTEM`) plus `ColorSource` (`BRAND` default / `DYNAMIC` wallpaper / `CUSTOM` seed). The reader's 18 `ReaderPalette`s and its time-based `autoNightMode` (19:00–07:00, re-evaluated every minute) are separate and never change the app theme.

App color (v56.6): `BRAND` renders one of 12 `AppColorPreset`s, `CUSTOM` generates a scheme from `AppSettings.customSeedArgb`. `AppColorPreset.VIOLET` is the default and is special-cased in `FlowColorPresets` to return the hand-tuned brand scheme verbatim — everything else goes through `SeedColorScheme`, a Compose-free HSL generator in `:core/util` that guarantees WCAG AA on every text pair for any seed. `ColorStudioDialog` (`:app`) is the hue-ring / spectrum-bar picker; its geometry lives in `:core` `ColorWheelMath`. See `AGENTS.md` for the constraints that are easy to break.

In-app language switch (`AppLanguage`: zh default, en, ja, ko, de, es, fr, pt, ru): `FlowLocaleProvider` wraps `LocalContext`/`LocalConfiguration`/`LocalLayoutDirection` with a `ContextWrapper` — a bare `createConfigurationContext()` result is not an Activity and makes `hiltViewModel()`'s `findActivity()` throw. Route titles in `Screen` are `@StringRes`, never string literals — the v53 language switch freezes string-bearing statics at first composition.

## Highest-value behavioral constraints

### Reader

`ReaderViewModel` is ~780 lines and holds most of this logic:

- `goToChapter()` must load chapter *content* before assigning `currentChapter`; assigning a metadata-only chapter renders a blank reader.
- Every reader preference goes through the single `ReaderViewModel.updateReadingSettings(ReadingSettings)`; do not reintroduce per-field mutators.
- Reader styling must come from `:core`'s `readerBodyStyle`/`paragraphSpacing`/`ReaderMetrics`. Hard-coding `MaterialTheme.typography.bodyLarge` in `ReaderContent` is what made the font, custom-font and paragraph-spacing settings dead before v52.
- `PageMode` has exactly three values (`SLIDE`, `PAGED`, `NONE`) and all three are rendered. `SLIDE` animates chapter scroll, `PAGED` renders measured pages in a `HorizontalPager` (`PagedReader`; `ChapterPaginator` splits oversized paragraphs on raw offsets so highlight/bookmark ranges survive re-pagination), `NONE` jumps. Comic books use `ComicReader` (SLIDE = swipe per page, NONE = vertical `LazyColumn` virtualized list). Do not add a mode without implementing it — `SIMULATION`/`CURL`/`SLIDE_OVER` were removed for exactly that reason, and `docs/page_turn_evaluation.md` (v56) decided simulated page curl stays unimplemented.
- Progress saves are debounced 3s (`debouncedSaveProgress()`). Reading stats auto-save every 30s, on chapter change, and in `onCleared()` (since v56.3 `onCleared()` runs its save in an independent IO scope, not `viewModelScope` which is already cancelled); page counts come from real chapter-character deltas, partial-page characters carry across scroll updates, and a pause over 5 minutes starts a new session.
- `currentPosition` carries **two different units**: a character offset in `SLIDE`/`NONE`, a rendered page index in `PAGED` and comics. Route through `ReaderPositionUnit.of(pageMode, format)` rather than re-deriving it; page indices belong in `ReaderSessionTracker.recordPageProgress()`, not `recordProgress()`, and cannot be used as a TTS start offset. Mixing them up silently discards whole reading sessions (v56.4.3).
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

- **Single network permission, `INTERNET`**, used only by the LAN OPDS client and LAN backup transfer. Legacy storage permissions remain maxSdk-capped for old Android releases. No accounts, analytics, crash reporting, or sync.
- **OPDS is LAN-only**: `OpdsAddress` restricts reachability to loopback / RFC1918 / RFC4193 / `.local`-style names and re-validates **every redirect hop**; public hosts are unreachable. Catalog reads capped at 2MB, downloads at 200MB, acquisition links MIME-filtered.
- **Import caps everywhere**: `BookParser` read limits, `ZipImportRules` zip-slip/entry caps, backup import 200MB on both SAF and LAN paths, `ACTION_VIEW` ("open with FlowReader") imports run through the same capped pipeline and are consumed once (`intent.data`/`EXTRA_STREAM` cleared) so configuration changes cannot re-import.
- **No DRM circumvention**: DRM-protected MOBI/AZW files are rejected outright; HUFF/CDIC-compressed files are rejected rather than half-decoded.
- **Backup/export contains no executables**: backup import is a single atomic transaction; cloud backup includes shared prefs only — the DB never leaves the device via Android Backup.
- **ContentProvider** (`com.flowreader.app.provider`) exposes read-only book metadata and progress — no file paths, no book text; writes are refused. Since v56.6.2 it is gated by the custom `dangerous` permission `com.flowreader.app.permission.READ_LIBRARY`, so a caller needs a declaration plus a runtime grant.
- **LAN transfer** (`LanTransferServer`/`LanTransferClient`): random-token-protected, binds the discovered LAN interface (never `0.0.0.0` since v56.4), and the HTTP server stops when the dialog closes.
- **Sanitization**: FTS queries escaped (`FullTextSearch.escapeFtsQuery()`), HTML/Markdown annotation exports escaped, `OpdsClient` filters MIME types.
- No WebView anywhere. The v56.3 audit gate requires **no new `!!`** (the only remaining one is the ContentProvider's standard `context!!` in `onCreate`).

## Version bookkeeping

`versionCode`/`versionName` live in `app/build.gradle.kts` (currently 5662 / "56.6.2") and `SettingsScreen` surfaces `BuildConfig.VERSION_NAME`. Releases are one commit per version with a matching `CHANGELOG.md` entry (`vNN.N.N: summary`); `ROADMAP.md` tracks the longer arc and the acknowledged tech debt.

**v56.6.2 static-review findings #1-#4 (2026-08-20)** — from `SECURITY_REVIEW_2026-08-20.md`, a third-party static review baselined on v56.6.1:
- **A `ContentResolver` `DISPLAY_NAME` is attacker-controlled input, not a file name.** `getFileName()` returned it verbatim and `copyFileToInternal()` put it straight into `File(booksDir, fileName)`. A malicious app sends `ACTION_VIEW`, its own provider answers `../../databases/flowreader_db`, and the import overwrites the Room DB — or `datastore/settings.preferences_pb`. Nothing in between ever doubted the string.
  - New `util/ImportFileName.kt`. `sanitize()`: last path segment only (`/` and `\` both count as separators), everything outside `[letter digit . _ - space]` replaced, leading dots stripped, truncated to 120 chars keeping the extension. `resolveWithin()`: re-checks containment through `canonicalFile` and returns null when it cannot prove it, so an unresolvable path fails closed rather than falling through.
  - **Sanitization keeps Unicode deliberately.** The ASCII-only `sanitizeFileName()` that already existed is for comic *directory* names; reusing it here would reduce `三体.epub` to `_.epub`. The two are not interchangeable.
  - The same change closed a TOCTOU. `copyFileToInternal` used to re-query the resolver for the name, and two queries on one Uri can legitimately return two different answers — the one that passed the check need not be the one written to disk. The caller (`LibraryViewModel`/`OpdsViewModel`) now resolves it once and passes it in.
- **Unbounded reads on attacker-supplied bytes.** EPUB `container.xml` and the OPF were read with plain `readText()`, and containers and images were copied with `copyTo()` — all of it controlled by whoever made the file, not by whoever opens it.
  - Everything now routes through `copyCapped`/`readCappedBytes`/`readCappedText`, with a new 256MB container cap and 4MB EPUB-metadata cap (the 24MB single-image and 16MB chapter caps already existed). `parseEpubStream`/`extractEpubCover` gained `finally { tempFile.delete() }`, because the new early returns would otherwise leak temp files.
  - The contract is worth remembering: `copyCapped` returns **-1** past the limit, the readers return **null**, and `copyCapped` deliberately does **not** delete the partial file — each caller does. Adding a delete inside it would double-delete in the paths that already clean up in a `finally`.
  - All three moved into `BookParser`'s companion object as `internal` purely so the caps are testable: triggering a 256MB limit otherwise needs a 256MB fixture, whereas a test can pass a 1000-byte limit and a 1001-byte stream.
- **`LanTransferClient` enforced only the scheme**, so a pasted `http://evil.example.com/backup/…` was fetched from the public internet and handed to the backup importer. It now applies `OpdsAddress.isPrivateHost()` — the same policy OPDS uses — and requires `/backup/` + 16 hex. The rule generalizes: the `INTERNET` permission is for the LAN, and that has to be checked at each call site, not just in the OPDS client.
- **`LanTransferServer` had no per-connection timeout and matched the token loosely.** `serve()` runs on a single-thread executor, so one peer that connects and stays silent parked the only worker on `readLine()` for as long as the dialog stayed open. `soTimeout = 5s` turns "forever" into "five seconds per freeloader".
  - Separately, `requestLine.contains("/backup/$token")` answered 200 to `GET /anything/backup/<token>` and `GET /backup/<token>extra` — while the class KDoc promised the exact path. Not a leak by itself (the token still has to be right), but the documented boundary should be the enforced one, or a later change to path handling inherits a false premise. `isBackupRequest()` matches the whole request line and accepts HTTP/1.1 and 1.0, since `HttpURLConnection` may downgrade. It is `internal` so the matching is testable without a socket.
- Each fix ships a regression suite: `ImportFileNameTest` 15, `BookParserCapsTest` 10, `LanTransferClientTest` 8, `LanTransferServerTest` +5. All were mutation-checked — reverting a fix fails its tests. The slow-loris test leans on a 20s *client* timeout on purpose, so a regression shows up as a failure instead of hanging the suite.
- **#6 (release was signed with the debug config) is fixed.** `app/build.gradle.kts` gained a `signingConfigs { create("release") }` that reads `keystore.properties` from the repo root (git-ignored; the keystore itself is covered by `*.jks`), with `keystore.properties.example` documenting the fields. `buildTypes.release` picks it only when that file exists and falls back to the debug config otherwise, so CI and fresh clones without the keystore still produce a testable `app-release.apk`. Verified on the artifact, not the config: `apksigner verify --print-certs` → `CN=HuZaiGong, OU=Dev, O=flowreader`, a different cert and fingerprint from debug's `CN=Android Debug`. **Check the APK's mtime against `keystore.properties` before believing a verification** — the first APK I checked predated the config, so the signing block was right and the artifact was still debug-signed. Do not commit `keystore.properties` or the `.jks`.
- **#7 (exported ContentProvider with no permission) is fixed** — this reverses the older "deliberate tradeoff" note above, so don't restore it. `com.flowreader.app.permission.READ_LIBRARY` (`dangerous`) now backs `android:readPermission`: a caller must declare the permission *and* get a runtime grant, instead of reading the whole library the moment it is installed. The export itself stays — that is the feature. `dangerous` rather than `signature` because `signature` limits the provider to apps signed with this repo's key, which deletes the capability instead of gating it; `WRITE_LIBRARY` is `signature` as a second layer over write methods that already throw. Label/description strings exist in all 9 locales because they surface in the system consent dialog. Android enforces this outside the provider class, so **no unit test can cover it — the manifest attributes are the enforcement point**. Known cost: any existing external consumer gets a `SecurityException` after upgrading; there are none in or out of the repo, and no doc ever advertised the authority as an API.
- **#5 (plaintext API key): the review's location is wrong, and so was my first correction of it.** The review said `.claude/providers.yaml`; I then said root `providers.yaml`, tracked since v45.0.2. Neither is right. `providers.yaml`'s *history* has only ever held `{env:DEEPSEEK_API_KEY}`. The working copy on this machine does carry a real key, but it is hidden by `git update-index --skip-worktree` (`git ls-files -v providers.yaml` prints an `S` prefix), so `git status` reports clean while worktree and HEAD differ — which is exactly what fooled the first pass. A full-history scan found the only committed copy of the key literal at `V56.5.0_PLAN.md:128`, inside a paragraph arguing the key was a harmless public placeholder — correct conclusion, but it quoted the value, which is what made it a leak. Now redacted. The key is a shared public free-tier value, so no rotation; rewriting public `main` history is the owner's call and was not done. **When judging "is a secret committed", read `git ls-files -v` and scan history — never the working tree.** Note `skip-worktree` lives in `.git/index`: per-clone, not inherited, and it makes `git pull` fail if an incoming commit touches the file.

**v56.6.1 v56.6.0 follow-up sweep (2026-08-19)**:
- **`String.format("%.1f", x)` without a `Locale` is a bug in this app, not a style nit.** The color studio's contrast readout pre-formatted the number and passed the string into `stringResource`. `String.format`'s no-locale overload uses the **JVM default locale** while string resources resolve against the **in-app language**, so under a comma-decimal locale the two disagree and a numeric placeholder throws. Format inside the resource (`%1$.1f`) and pass the raw `Double` — `Resources.getString(id, args)` uses the resource config locale. This was the only such call site; keep it that way. (The hardcoded "4.5:1" threshold in the same sentence needed the comma too, in de/es/fr/pt/ru.)
- **Writing to a state you read in the same composition costs an extra pass every frame of a drag.** `ColorStudioDialog` stored the hex field's text and reassigned it from each picker callback during composition. The fix is the general one: keep only the *user's* draft in state (`null` = not editing) and **derive** the displayed value — `val hexDisplay = hexDraft ?: ColorSpaces.toHexString(argb)`.
- A subtitle that reads one field must gate on the field that decides whether it applies: the 自调色 row showed a stored `customSeedArgb` as "in use" while `ColorSource` was `BRAND`.
- **`backup_rules.xml`/`data_extraction_rules.xml` back up nothing, deliberately.** `<include>` *restricts* backup to the domains it lists, and the only listed domain (`sharedpref`) is empty — the app has no `getSharedPreferences` call anywhere, and DataStore's `dataStoreFile()` lands in `filesDir/datastore/`, i.e. the `file` domain. The comments used to claim the opposite; they now state the net effect and warn against "fixing" it with `<include domain="file">`, which would push reading habits off-device through Google's backup transport. Cross-device migration is the in-app SAF/LAN backup, user-triggered.
- **`FlowTheme`'s `remember` around the `ColorScheme` is hardening, not a shipped-bug fix.** The compiler report says `restartable skippable fun FlowTheme(...)` with all five params `stable` (`./gradlew :core:compileDebugKotlin -PcomposeReports=true`, now wired in `:core` too), so Compose skips the body outright when inputs are unchanged and the scheme was never actually rebuilt per navigation. It is kept because losing skippability on this one composable recomposes the whole app (`staticCompositionLocalOf`) and `ColorScheme` has no `equals`, so nothing downstream would catch it. `FlowThemeStabilityTest` pins scheme **identity** across recompositions; its KDoc says plainly that it is not a regression gate for a past defect. Don't let a later edit relabel either as a bug fix.

**v56.6.0 app color presets + color studio (2026-08-19)** — 配色来源 went from two options to 12 presets + wallpaper + a custom picker:
- `AppColorPreset` (12 seeds) in `:domain`, `ColorSource.CUSTOM` added, `AppSettings` gained `colorPreset` + `customSeedArgb`. The three sources each read a different field, so none is a duplicate of another.
- **`AppColorPreset.VIOLET` bypasses the generator by design.** It is the default, and `:core` returns `FlowLightColorScheme`/`FlowDarkColorScheme` verbatim for it — generating the default would recolor every existing install and move the Roborazzi goldens as a side effect of adding presets. `FlowColorPresetsTest` pins the individual roles.
- **`SeedColorScheme` (`:core/util`, Compose-free) promises WCAG AA on every text-on-surface pair for any seed.** HSL approximation of M3's HCT, chosen over adding `material-color-utilities` to an offline-first app; being Compose-free is what makes the promise unit-testable (12 presets × 2 modes × 13 pairs, plus the hue circle every 5°, plus black/white/grey seeds). Roles sweep away from their background and fall back to black/white — and `onSurfaceVariant` is held against **both** `surfaceVariant` and plain `surface`, because every subtitle in the app renders it on `surface`.
- `updateColorPreset()`/`updateCustomSeedColor()` write `COLOR_SOURCE` in the **same `edit{}`**; splitting the writes emits a "new source + old color" `AppSettings` and the theme flashes the old scheme.
- `ColorStudioDialog` + `:core` `ColorWheelMath`: wheel gesture ownership is decided once at touch-down; the hex field and the bars' `setProgress` semantics are the accessible path around a Canvas. See `AGENTS.md`.

**v56.4.4 safe-area fix (2026-08-12)** — 「顶栏遮蔽页面内容」 on the entry pages:
- `FlowStateHost` applied its `modifier` in the error / loading / empty branches but not in the success branch (`else -> content()`). `LibraryScreen`, `StatsScreen` and `BookDetailScreen` pass the `Scaffold`'s inset padding in through that `modifier`, so once a page had data it lost all 88dp of safe area and drew from y=0 under the `TopAppBar` — while the very same screen's loading and empty states were placed correctly. Success branch is now `Box(modifier = modifier) { content() }`.
- `SettingsScreen` was never affected (it pads its `Column` directly); screens on `FlowScaffold` were immune because it pads inside its own `Box`; `ReaderScreen` passes no `modifier` and stays full-bleed.
- v56.4.2's `ShellWindowInsetsTest` stayed green because its `FakeScreen` pads its own `Box` and never routes through `FlowStateHost`. Two cases now reproduce the real chain. See `AGENTS.md`.

**v56.4.3 functional bug sweep (2026-08-12)** — two user-visible features were entirely dead:
- **Book-scoped full-text search matched nothing.** `book_content_fts` is an FTS5 *external content* table, so its columns have no declared type and no affinity, and `rawQuery()` can only bind `String`. SQLite never equates INTEGER `5` with TEXT `'5'` without an affinity to convert one side, so `WHERE book_id = ?` was silently always false. The SQL now lives in `FullTextSearch.SEARCH_IN_BOOK_SQL` and uses `CAST(? AS INTEGER)`. `searchAll()` (no book filter) and `deleteBookContent()` (regular table, declared `INTEGER`) were never affected. Robolectric's SQLite has no fts5 module, so `FullTextSearchQueryTest` asserts the SQL shape and the underlying comparison semantics rather than running the query.
- **PAGED-mode and comic reading stats were discarded wholesale.** `updatePosition()`'s `position` is a character offset in `SLIDE`/`NONE` but a rendered page index in `PAGED`/comics; both went into character math, so `readPages` stayed 0 and `saveReadingStats()`'s `pages > 0` guard dropped the session's time too. `ReaderPositionUnit.of(pageMode, format)` is now the single source of that rule; page indices go to `ReaderSessionTracker.recordPageProgress()`.
- **Text selection stored the wrong range, and overlapping highlights garbled the paragraph.** `ParagraphContent.rawRange()` returns an *inclusive* last offset while every consumer of `Annotation.endPosition` treats it as *exclusive*, so a highlight lost its last character (a one-character selection highlighted nothing); `ParagraphContent.selectionSpan()` now does that `+1` and the text slice in one tested place. Separately, `buildParagraphContent()` appended each annotation's full range unconditionally, so overlapping/nested highlights emitted shared characters twice and desynchronized `rawOffsets` — breaking every later selection. Span starts are now clamped to what has been emitted and fully covered spans are skipped.
- **Selection bookmarks jumped to the wrong place.** `Bookmark.position` is consumed as the reader's own position (scroll pixels in SLIDE/NONE, page index in PAGED), never a character offset — a char offset scrolled to a meaningless pixel or got clamped to the chapter's last page. `addBookmark(text)` no longer accepts a position; `Annotation` positions really are character offsets and were fine.
- **The stats screen's 7-day chart showed 2–3 days.** `reading_stats` has one row per book per day, so `getRecentStats(limit)`'s row `LIMIT` is not a day limit. `getRecentDailyStats` now filters by date via the new `ReadingStatsDao.getStatsSince(startDate)`. See `AGENTS.md`.
- Also: the FTS index `Mutex` moved from `SearchRepositoryImpl` into `FullTextSearch.withIndexLock()` (the reader's per-book re-index used to bypass it; the lock is not re-entrant, hence `rebuildIndex()` vs `rebuildIndexLocked()`), `goToChapter()` no longer overwrites `chapterPositions` with the 250ms-throttled `uiState.currentPosition`, and `CacheManager.memoryUsage` is released on eviction/replacement instead of only growing.

**v56.4.2 window-inset fix (2026-08-11)** — issue #6, 「每个界面的顶部UI都很宽，导致内容被截断」:
- The shell `Scaffold` in `ui/Navigation.kt` is now `FlowShellScaffold`, with `contentWindowInsets = WindowInsets(0, 0, 0, 0)` **and** `.consumeWindowInsets(paddingValues)`. `Modifier.padding()` applies an inset without consuming it, so the previous shell let all 9 nested screens re-apply the status-bar inset through their own `Scaffold` + `TopAppBar` (112dp above the title instead of 88dp) and double-count the nav-bar inset at the bottom. **The inset contract belongs in that one composable** — see `AGENTS.md` for the full rule.
- Inset regressions are invisible to both existing gates: Robolectric reports zero insets unless `WindowInsetsCompat` is dispatched onto the ComposeView (not the decor view), and the Roborazzi goldens contain no `Scaffold`. `ShellWindowInsetsTest` is the regression gate; it asserts measured positions.

**v56.4.1 security hardening (2026-08-07)** — see `SECURITY_AUDIT_REPORT.md` / `SECURITY_FIX_SUMMARY.md`:
- `FlowReaderContentProvider` no longer wraps DAO reads in `runBlocking` (that blocked Binder threads); `BookDao` gained `getAllBooksSync()` / `getBookByIdSync()` for it. A ContentProvider must never suspend-bridge on a Binder thread — add a sync DAO method instead.
- **`file_paths.xml` now grants exactly one path, `share_cards/`**, down from three whole-tree `path="."` grants. There is exactly one `getUriForFile()` call site in the app (`ReaderScreen`'s 分享阅读卡片 → `ShareCardGenerator`), and `ShareCardGenerator` was changed to write into `cacheDir/share_cards/` to match — it previously wrote to the cache root. **Those two must stay in sync** (`ShareCardGenerator.SHARE_CARD_DIR`); a mismatch throws `IllegalArgumentException: Failed to find configured root` only at share time, and no unit test can catch it since `getUriForFile()` needs a real Android runtime. `opds/`, `covers/` and the LAN backup JSON deliberately have no FileProvider grant — they never travel through it.
- `LanTransferServer.generateToken()` indexed its charset with the target token length rather than `charset.length`. Both are 16 today so tokens were never actually weakened; the fix removes the dependence on that coincidence (raising the length would have crashed, lowering it would have silently cut entropy).
