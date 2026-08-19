# FlowReader

<p align="center">
  <a href="README.md">中文</a> | <b>English</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose" alt="UI">
  <img src="https://img.shields.io/badge/minSdk-26-red?style=flat" alt="minSdk">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=flat" alt="License">
</p>

<p align="center">
  <b>An offline-first Android e-book reader</b><br>
  No accounts, no cloud sync, no telemetry — your books and reading data stay on this device, period.
</p>

---

## Overview

FlowReader is built with **Jetpack Compose + Material 3** and layered as multi-module **Clean Architecture + MVVM**. It deliberately ships no account system, no cloud sync, no analytics and no crash collection: the whole app requests a single `INTERNET` permission, used only for LAN OPDS catalogs and LAN backup transfer. There is no WebView anywhere in the app.

Current version: **v56.6.1** (versionCode 5661).

**Supported formats**: EPUB, TXT, PDF, Markdown, FB2, MOBI, and comics — single JPG / PNG / WebP images and image-only ZIP / CBZ archives. FB2 and MOBI are imported read-only, split into chapters at import time just like EPUB. **DRM-protected MOBI / AZW files are rejected outright — no decryption of any kind**; HUFF/CDIC-compressed files are rejected whole rather than half-decoded.

The UI and documentation are primarily Chinese while code identifiers are English. The app itself can switch between 9 languages (zh, en, ja, ko, de, es, fr, pt, ru).

---

## Features

### Library

- **Import**: single or batch import; the system "open with another app" path goes through the same restricted pipeline and is consumed exactly once (a configuration change never re-imports).
- **Bookshelf**: sort by date added / last read / title / author, grid and list views, a large "Continue reading" card; author, description, cover and tags are all editable.
- **Whole-library search**: cross-book full-text search on SQLite **FTS5**, results split into book titles and chapters, paged loading + search history.
- **Reading lists**: organize books into user-created lists.
- **LAN OPDS**: connect to OPDS catalogs on the same local network and download from them. **LAN only** — hosts other than loopback, RFC1918 / RFC4193 private ranges and `.local`-style names are unreachable, and **every redirect is re-validated**.
- **Progress persistence**: reading progress is written with a 3-second debounce to keep database IO low.

### Reader

- **18 reading palettes**: paper white, cream, eye-care green, linen, morning mist, cool gray, e-ink, solarized light, rose quartz, night black, ink blue, dark brown, obsidian, pure black, solarized dark, nord, gruvbox, forest — all asserted automatically against WCAG AA body-text contrast; custom background / text colors are supported too, with the same contrast checks.
- **Adjustable typography**: font size 12–32sp, line spacing 1.0–2.5x, paragraph spacing, first-line indent, external `.ttf` / `.otf` font import. Chinese body text is line-wrapped at a 34-character-per-line cap.
- **Three page-turn modes**: slide (animated scrolling), **true pagination** (per-page measurement + swipe / tap to turn), and no-animation jumps. Only genuinely implemented options are offered — the simulation, curl and cover modes that once had nothing but a UI entry were removed along with their entry points.
- **Native text selection**: long-press to select, word-level selection, drag to extend, dual handles; a floating action bar highlights (5 colors), copies or bookmarks, and the selected range maps exactly to the chapter source.
- **Comics**: horizontal page-by-page flipping, or a vertically virtualized long list.
- **PDF**: zoom, drag to flip pages, box-select region annotation.
- **Fully configurable gestures**: tap zones, double tap, long press, left/right swipes and edge hot-zone width are all adjustable — and actually take effect.
- **Others**: TTS read-aloud, focus mode (fullscreen immersive), keep screen on, scheduled auto night mode (19:00–07:00, re-evaluated every minute), eye-protection reminders (15/20/30/45/60 minutes), a draggable bottom progress bar.

### Appearance & Theming

- **12 built-in color presets** (v56.6): violet, indigo, azure, teal, emerald, moss, amber, tangerine, crimson, rose, plum, graphite. Picking one **regenerates the whole Material scheme**, not just an accent. Violet is the default and maps to the hand-tuned brand scheme verbatim, so upgrading changes nothing for existing installs.
- **Color studio** (v56.6): Settings → Appearance → Color studio offers a **hue ring with an inscribed saturation/value disc**, a **spectrum bar**, brightness and saturation bars, and a hex field — four controls bound to one HSV state, so any of them can finish an adjustment another started. The dialog previews the generated scheme and its measured body-text contrast live; nothing is written until you hit Apply.
- **Contrast is a guarantee, not a coincidence**: for any seed color, **every text-on-surface pair in the generated scheme clears WCAG AA (4.5:1)**. The generator is Compose-free inside `:core`, so that promise is unit-tested across all 12 presets in both modes, every 5° of the hue circle, and the degenerate black / white / mid-grey seeds.
- **Theme mode and color source are independent axes**: light / dark / follow-system is one dimension, built-in / wallpaper / custom is another, and the reader's 18 palettes are a third that never follows the app theme.

### Notes & Data

- **Notes and annotations**: 5-color highlights (yellow / green / blue / pink / orange) with optional thought notes; export from the book detail page as Markdown / HTML / plain text (escaped on export).
- **Reading statistics**: daily time, pages and speed, bar-chart trends, weekly / monthly reports, fastest reading day and most-read book; custom daily / weekly / monthly reading goals.
- **Share & transfer**: one-tap reading share cards (image); bookshelf export to CSV / JSON; **LAN backup transfer** — device-to-device direct transfer on the same WiFi, protected by a random token, bound to the actual LAN interface instead of `0.0.0.0`, and the server stops as soon as the dialog closes.
- **Backup & restore**: export / import books and reading progress; import is a **single atomic transaction**, capped at 200MB.
- **Home-screen widget**: shows the most recently read book and its current progress.
- **Decision wheel**: a small tool with customizable options and colors; moved from the bottom primary navigation into the bookshelf top-bar "More" menu in v52.

---

## Architecture

Allowed dependency direction: `feature:* → core / domain`, `data → core / domain`, `app → core / data / domain / feature:*`. `:domain` carries no app / Room / Compose / Hilt dependencies at all.

| Module | Contents |
|------|------|
| `:app` | Composition root: `MainActivity`, Hilt wiring (`di/AppModule.kt`), navigation, all screens and ViewModels, **all 10 repository implementations**, `util/`, `widget/` |
| `:core` | Design system (since v52): tokens, `FlowTheme`, the 12 reading palettes, shared components, plus a set of Compose-free pure functions testable directly on the JVM |
| `:data` | Room only: `AppDatabase` + 7 DAOs + 7 entities |
| `:domain` | 10 repository interfaces + domain models. `domain/usecase/` has been removed — business logic deliberately lives in the ViewModels |
| `:feature:reader` | `ChapterPaginator`, `ReaderProgressEngine`, `ReaderSessionTracker`, `ReaderPositionUnit` (all unit-tested) |
| `:feature:library` | **No source files yet** — a placeholder module already wired into compilation / lint / tests |

Data flow: `Composable → ViewModel → domain repository interface → data repository impl → Room DAO / DataStore`. Each ViewModel exposes an immutable `StateFlow<XxxUiState>`; errors use Kotlin's built-in `Result`.

Note one naming mismatch: module namespaces are `com.flowreader.domain` / `com.flowreader.data` and so on, but source packages are uniformly `com.flowreader.app.*`. New files should keep using `com.flowreader.app.*`.

### Three independent persistence layers

1. **Room** (`flowreader_db`) — `AppDatabase` is currently version 7 with `exportSchema = true`. There is **no** `fallbackToDestructiveMigration()`; every schema change requires a hand-written migration.
2. **A second bare SQLite database** (`flowreader_fts.db`) — FTS5 virtual tables managed directly by `util/FullTextSearch.kt`, completely outside Room, powering both in-book and whole-library search.
3. **DataStore Preferences** (`settings`) — `SettingsRepositoryImpl` exclusively owns all preference keys.

`util/CacheManager.kt` is the only chapter / metadata / cover cache: sized to the `MemoryManager` recommendation, implements `ComponentCallbacks2` to react to memory pressure, and adapts per-book chapter capacity by hit rate. Do not add a second chapter cache.

---

## Building

Environment: **JDK 17**, **Android SDK 35** (compileSdk 35 / minSdk 26), AGP 8.6.0. The Gradle wrapper fetches Gradle 9.6.1 from a Tencent Cloud mirror; a failing wrapper download is usually a mirror problem, not a project problem.

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

./gradlew assembleDebug          # development APK
./gradlew assembleRelease        # R8 full-mode minify + resource shrinking (deliberately signed with the debug config)
./gradlew testDebugUnitTest      # all JVM unit tests
```

### Verification gates

CI (`.github/workflows/ci.yml`) runs these six steps in strict order:

```bash
./gradlew verifyKotlinStyle      # ktlint (every module except :app) + repo-wide whitespace check
./gradlew testDebugUnitTest      # :app / :core / :domain / :feature:reader
./gradlew coverageSummary        # file-count test breadth ≥ 40% (currently 77.8%)
./gradlew assembleDebug
./gradlew verifyRoborazziDebug   # screenshot regression
./gradlew performanceBaseline    # APK size comparison against baseline/apk-size.properties
```

Two common traps:

- **ktlint does not apply to `:app`**. Kotlin in `:app` is only checked by the whitespace gate inside `verifyKotlinStyle` — a single tab or trailing-space character in any `.kt` / `.kts` file in the repo fails the entire build. `.editorconfig` mandates 4-space indents, LF and a 140-char line cap.
- **`coverageSummary` is a file-count ratio, not line coverage**. The denominator includes every main-source file in `:core`, domain models, repository interfaces, ViewModels, etc. **Adding a domain model or a `:core` file without tests fails the build even if nothing else changed.**

---

## Security constraints

The project is offline-first and privacy-minded; these are hard constraints:

- **A single `INTERNET` permission**, used only for LAN OPDS. No accounts, no analytics, no crash reporting, no sync.
- **Caps everywhere on import paths**: EPUB chapters 16MB, embedded images 24MB, whole TXT/MD/FB2/MOBI files 128MB; ZIP / CBZ reject absolute paths and `..` (zip-slip), skip `__MACOSX` and hidden entries, cap entry count and per-entry size, and only allow extensions the parsers support.
- **No DRM circumvention**.
- **Backups contain no executable content**, and import is a single atomic transaction; cloud backup includes shared prefs only — the database never leaves the device via Android Backup.
- The **ContentProvider** (`com.flowreader.app.provider`) exposes read-only book metadata and progress — no file paths, no book text; all writes are refused.
- **FileProvider authorizes exactly one subdirectory: `share_cards/`** (there is exactly one `getUriForFile()` call site in the whole repo).
- **No WebView anywhere**; FTS queries and HTML / Markdown export are escaped; the audit gate forbids new `!!`.

---

## Changelog

Recent releases:

- **v56.6.1** — follow-up sweep on v56.6.0. The color studio pre-formatted its contrast number with a locale-less `String.format` while string resources resolve against the in-app language, so comma-decimal locales disagreed and threw; the hex field wrote to a state it read in the same composition, costing an extra pass per drag frame; the Color studio subtitle still claimed a custom color was in use after switching back to a preset. Also corrected `backup_rules.xml`'s comments, which described the opposite of the real behavior — the backup scope has always been empty (nothing leaves the device) and that is deliberate, so only the comments changed.
- **v56.6.0** — the color source expanded from two options to **12 built-in presets + wallpaper + a custom picker**, with a hue-ring / disc / spectrum-bar studio. `:core`'s `SeedColorScheme` generates all 24 Material roles from one seed and guarantees WCAG AA (4.5:1) on every text-on-surface pair for any seed.
- **v56.5.2** — Compose stability configuration eliminates unstable inference for domain models. The `:domain` module has no Compose compiler, causing `Book`/`Chapter`/`Annotation`/`ReadingSettings` parameters to be inferred unstable, re-executing every visible book card on any state change. Declared `com.flowreader.app.domain.model.*` stable via `compose_compiler_config.conf` and wired into all four Compose modules; unstable classes dropped from 52 to 37, all UiState now stable.
- **v56.5.1** — Localized book detail and stats pages. `:core`'s `FlowFormatters` formats numbers, dates and units by `Locale`; domain models now carry raw values instead of concatenated strings, surviving the v53 language switch instead of frozen Chinese.
- **v56.4.4** — fixed "content hidden behind the top bar after data loads" on the bookshelf / stats / book detail pages: the success branch of `FlowStateHost` dropped the `modifier`, so the 88dp safe area vanished in the success state while the loading / empty / error states of the same pages were fine.
- **v56.4.3** — an 11-fix functional bug sweep. In-book full-text search always returned empty because FTS5 columns lacked affinity; reading stats were lost entirely in PAGED mode and for comics; text-selection highlights saved one character short and overlapping highlights rendered twice; the "last 7 days" trend chart actually showed only 2–3 days.
- **v56.4.2** — fixed issue #6: the outer shell only "applied" window insets without "consuming" them, and the 9 screens below each applied them again, adding a full status-bar height on top.

See [CHANGELOG.md](CHANGELOG.md) for the full history, `ROADMAP.md` for longer-term plans and known tech debt, and `AGENTS.md` for the behavioral pitfalls one by one.

---

## Community & Contributing

- Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or PR: environment setup, CI gates, code conventions and security hard constraints are all there (bilingual).
- All participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Security vulnerabilities: report privately per [SECURITY.md](SECURITY.md) — never as a public issue.

---

## License

Released under the [GNU General Public License v3.0](LICENSE).

<p align="center">Made with ❤️ by HuZaiGong</p>
