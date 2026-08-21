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
  <b>An Android reader that belongs to one device</b><br>
  No accounts, no cloud sync, no telemetry
</p>

---

## What this is

An Android e-book reader. Open it and read. No sign-up, no login, nobody asking for your phone number.

It doesn't upload your books, doesn't tally your reading habits, doesn't collect crash logs. The app's only network permission is `INTERNET`, and it does exactly two things: reach an OPDS library on your own local network, and move a backup between two of your own devices. The manifest retains maxSdk-capped storage permissions for older Android releases. If you'd rather check than take my word for it, `AndroidManifest.xml` is short enough to read in a minute.

Current version: **v56.6.2**. The interface speaks 9 languages (Chinese, English, Japanese, Korean, German, Spanish, French, Portuguese, Russian), switchable in-app at any time.

## Why build it this way

Cloud sync is genuinely convenient — there's no arguing that. But the usual price is that your library, the page you stopped on, and how long you read each day all live on somebody else's server.

FlowReader takes the other side: everything stays local, and the cost is that moving to a new device is something you do by hand (export a file, or send it directly over your own WiFi). That's a deliberate trade, not an unfinished feature.

The same instinct shows up in less visible places:

- Reading palette contrast isn't "the designer thought it looked about right" — **every palette is asserted against WCAG AA by a unit test**, and breaking one breaks the build.
- Settings contain no fake switches. Three page-turn animations once existed as UI entries with no implementation behind them; the entries were eventually deleted along with the modes. An option that does nothing when tapped is worse than no option.
- DRM-protected MOBI / AZW files are refused at import. No decryption is attempted.

## What it reads

EPUB, TXT, PDF, Markdown, FB2, MOBI, and comics — single JPG / PNG / WebP images, or packaged ZIP / CBZ archives.

FB2 and MOBI import read-only, split into chapters on the way in, the same as EPUB.

## What using it is like

### Finding and organizing books

Import handles single files and batches, and files arriving through the system's "open with" share sheet go through the same pipeline. The shelf sorts by date added, last read, title or author, offers grid and list views, and puts a large "Continue reading" card up top. Author, description, cover and tags are all editable by hand — metadata parsing is never good enough to get every book right.

Two things start earning their keep once the shelf fills up. One is **whole-library full-text search** on SQLite FTS5, which searches inside books rather than only across titles. The other is **reading lists**: make your own groupings and drop books in.

If you run Calibre or Komga at home, you can pull straight from it over **LAN OPDS**. One hard limit here: only loopback, private ranges and `.local`-style names are reachable — public hosts simply are not, and **every redirect is re-validated** so an address that looks internal can't bounce you somewhere external.

### Reading

**18 reading palettes**: paper white, cream, eye-care green, linen, morning mist, cool gray, e-ink, solarized light, rose quartz, night black, ink blue, dark brown, obsidian, pure black, solarized dark, nord, gruvbox, forest. You can also set your own text and background colors, and those get the same contrast check — pick a pair that's hard to read and it adjusts back toward legibility instead of letting you squint at it.

Typography: 12–32sp font size, 1.0–2.5× line spacing, paragraph spacing, first-line indent, and `.ttf` / `.otf` font import. Chinese body text wraps at 34 characters per line, a number borrowed straight from typesetting practice — considerably easier on the eye than letting text run the full width of a phone.

**Three page-turn modes**: slide (animated scrolling), true pagination (measured page by page, swipe or tap to turn), and instant jumps with no animation. Three, and all three work.

**Long-press selects**, snapping to word boundaries, with drag-to-extend and handles at both ends. The floating bar highlights (5 colors), copies, or bookmarks, and the selected range maps exactly onto the chapter source — that mapping is hand-written here, because Compose's own hoisted-selection API is still `internal`.

Comics flip horizontally page by page or scroll as a vertical list. PDF supports zoom, drag-to-flip, and box-selecting a region to annotate (PDF has no text layer, so annotations are rectangles).

Tap zones, double tap, long press, swipes and edge hot-zone width are all configurable — and configuring them actually changes behavior.

There's also TTS read-aloud, focus mode, keep-screen-on, scheduled auto night mode (19:00–07:00, re-checked every minute), eye-strain reminders (15/20/30/45/60 minutes), and a draggable progress bar along the bottom.

### A word about colors

v56.6 grew the app's color options from two into **12 built-in presets + wallpaper + a custom picker**. Choosing a preset **regenerates the entire Material scheme**, not just where the accent color lands. Violet is the default and maps verbatim to the hand-tuned brand scheme — existing installs look exactly as they did before, which is the point.

The custom option is a proper color studio: a **hue ring with an inscribed saturation/value disc**, a **spectrum bar**, brightness and saturation bars, and a hex field. All five inputs are bound to one HSV state, so any of them can pick up where another left off. The dialog previews the generated scheme and its measured body-text contrast live, and nothing is written until you press Apply.

It also makes an unusual promise: **for any seed color, every text-on-surface pair in the generated scheme clears 4.5:1**. Tone ladders alone can't deliver that — a mid-luminance seed will land foreground and background at nearly the same luminance — so each foreground role sweeps away from its background and falls back to black or white. The generator lives in `:core` and doesn't depend on Compose, which is what makes the promise testable: 12 presets × 2 modes × 13 pairs, every 5° around the hue circle, plus degenerate inputs like pure black, pure white and mid-grey.

Worth stating plainly, since they're easy to conflate: **theme mode** (light / dark / follow-system), **color source** (built-in / wallpaper / custom), and **reader palette** (those 18) are three independent axes. Reader palettes never follow the app theme.

### Notes and numbers

Highlights come in 5 colors and each can carry your own note. The book detail page exports notes as Markdown / HTML / plain text, escaped on the way out so a stray `<` in the book doesn't corrupt the file.

Statistics track time, pages and speed per day, with bar-chart trends and weekly / monthly reports. It'll tell you your fastest reading day and your most-opened book, and you can set daily / weekly / monthly goals.

A few smaller things that turn out useful: one-tap reading share cards, bookshelf export to CSV / JSON, a home-screen widget showing what you're reading and how far in, and **LAN backup transfer** — two devices on the same WiFi, direct, protected by a random token, with the server shutting down the moment you close the dialog. Backup import is a **single atomic transaction**, so a failure partway through doesn't leave half your data behind.

Oh, and a decision wheel — spin it when you can't pick what to read next. It moved out of the bottom navigation into the shelf's "More" menu in v52, because it didn't really earn a primary slot.

---

## Building it yourself

You'll need **JDK 17** and **Android SDK 35** (compileSdk 35 / minSdk 26), with AGP 8.6.0 and Kotlin 2.1.0. The Gradle wrapper pulls Gradle 9.6.1 from a Tencent Cloud mirror, so a failing wrapper download is usually the mirror's fault rather than the project's.

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

./gradlew assembleDebug          # development APK
./gradlew testDebugUnitTest      # all JVM unit tests
```

CI runs six gates in a fixed order, and you can run the same ones locally:

```bash
./gradlew verifyKotlinStyle      # ktlint (every module except :app) + repo-wide whitespace check
./gradlew testDebugUnitTest      # :app / :core / :domain / :feature:reader
./gradlew coverageSummary        # file-count test breadth ≥ 40% (currently 85.3%)
./gradlew assembleDebug
./gradlew verifyRoborazziDebug   # screenshot regression
./gradlew performanceBaseline    # APK size against baseline/apk-size.properties
```

Two traps worth knowing about before your first change:

- **ktlint doesn't cover `:app`.** Kotlin there is only scanned by the whitespace gate — **one** tab or trailing space in any `.kt` / `.kts` file anywhere in the repo turns the whole build red. `.editorconfig` sets 4 spaces, LF, 140-column lines.
- **`coverageSummary` is a file-count ratio, not line coverage.** The denominator counts every main-source file in `:core`, plus domain models, repository interfaces and ViewModels. So adding a domain model without a test fails the build even if you touched nothing else.

---

## What the code looks like

**Jetpack Compose + Material 3**, multi-module **Clean Architecture + MVVM**. Dependency direction: `feature:* → core / domain`, `data → core / domain`, `app → core / data / domain / feature:*`. `:domain` has no app / Room / Compose / Hilt dependency at all — it's plain Kotlin.

| Module | What's in it |
|------|------|
| `:app` | Composition root: `MainActivity`, Hilt wiring, navigation, all screens and ViewModels, **all 10 repository implementations** |
| `:core` | Design system: tokens, `FlowTheme`, the 18 reading palettes, the scheme generator, shared components, and a set of Compose-free pure functions that run directly on the JVM |
| `:data` | Room only: `AppDatabase` + 8 entities + 7 DAOs |
| `:domain` | 10 repository interfaces + domain models. No `usecase/` — business logic deliberately stays in the ViewModels |
| `:feature:reader` | `ChapterPaginator`, `ReaderProgressEngine`, `ReaderSessionTracker`, `ReaderPositionUnit`, all unit-tested |
| `:feature:library` | **No source files yet** — a placeholder already wired into compilation / lint / tests |

Data flows `Composable → ViewModel → domain repository interface → data repository impl → Room DAO / DataStore`. Each ViewModel exposes an immutable `StateFlow<XxxUiState>`, and errors use Kotlin's built-in `Result`.

One naming mismatch to know up front: module namespaces read `com.flowreader.domain` and the like, but source packages are uniformly `com.flowreader.app.*`. New files follow the latter.

Persistence is **three independent stores**: Room (`flowreader_db`, version 7, with **no** `fallbackToDestructiveMigration()` — every schema change needs a hand-written migration); a bare SQLite database outside Room (`flowreader_fts.db`, FTS5 virtual tables, backing both in-book and whole-library search); and DataStore Preferences for every setting.

There is exactly one chapter / metadata / cover cache, `util/CacheManager.kt`. It sizes itself to available memory, reacts to system memory pressure, and adapts per-book chapter capacity by hit rate. Don't add a second one.

For a finer-grained module map see `ARCHITECTURE.md`; for the behavioral pitfalls one by one see `AGENTS.md` — that last one is the single most useful file to read first.

---

## Privacy and security boundaries

These are hard constraints, not descriptions of the current implementation:

- **Only one network permission, `INTERNET`**, for LAN OPDS and LAN transfer. The manifest also keeps legacy storage permissions capped with `maxSdkVersion` for older Android releases. There are no accounts, analytics, crash reporting, or sync.
- **Android's automatic backup transfers nothing.** The only domain listed in the backup rules is empty in this app, so books, progress and settings never leave the device through Google's backup transport. That's deliberate — cross-device migration goes through the in-app export or LAN transfer, which you trigger yourself.
- **Every import path is capped**: EPUB chapters at 16MB, embedded images at 24MB, whole TXT/MD/FB2/MOBI files at 128MB, whole containers at 256MB, EPUB structural metadata at 4MB. ZIP / CBZ reject absolute paths and `..` (zip-slip), skip `__MACOSX` and hidden entries, cap entry count and per-entry size, and only admit extensions the parsers recognize.
- **A file name handed over by another app is untrusted input.** When something opens a book "with FlowReader", the display name it supplies is reduced to its last path segment, stripped of separators and control characters, and the destination is re-checked against the canonical app directory before anything is written — otherwise a single `../../databases/…` would overwrite the database.
- **LAN receiving checks the peer's address too**: a pasted backup link has to be a loopback / RFC1918 / RFC4193 / `.local`-style LAN address with an exactly matching path. Public links are refused outright.
- **No DRM circumvention.** Protected files are refused whole rather than half-decoded.
- The **ContentProvider** exposes read-only book metadata and progress — no file paths, no book text, and all writes refused. It stays open to other apps (automation tools and the like), but since v56.6.2 a caller has to declare a custom permission and get your runtime consent, instead of reading your library silently from the moment it installs.
- **FileProvider authorizes exactly one subdirectory, `share_cards/`** (the repo has exactly one `getUriForFile()` call site).
- **No WebView anywhere.** FTS queries and HTML / Markdown exports are escaped; the audit gate forbids new `!!`.
- **Release builds are signed with a real key**, whose keystore and passwords stay out of version control; environments without the keystore (CI, a fresh clone) fall back to debug signing so they still build.

---

## Recent changes

- **v56.6.2** — a third-party static review raised seven findings; all seven are now closed. The one that mattered: when another app opens a book "with FlowReader", the file name it hands over could contain `../` and escape the app's private directory — far enough to overwrite the database. Three more code defects: an EPUB's structural XML was read with no size limit, the LAN receiver checked the scheme but not who it was talking to (a pasted public link really would be fetched off the internet), and the backup server could be held open by a single connection that connected and said nothing. Each of those four ships a regression suite, verified by reverting the fix and watching the tests fail. The remaining three: release builds now use a real signing key (confirmed on the artifact with `apksigner`, not just in the build file); the exported ContentProvider is now behind a permission you have to grant; and the "plaintext API key" turned out not to be where the review said — a full-history scan found the only committed copy was a value quoted inside a planning document, now redacted.
- **v56.6.1** — follow-up sweep on v56.6.0. The color studio pre-formatted its contrast number with a locale-less `String.format` while string resources resolve against the in-app language, so comma-decimal locales disagreed and threw. The hex field wrote to a state it read in the same composition, paying an extra composition pass on every frame of a drag. The Color studio subtitle still claimed a custom color was in use after switching back to a preset. Also corrected `backup_rules.xml`'s comments, which described the opposite of the real behavior — the backup scope has always been empty, nothing leaves the device, and that's deliberate, so only the comments changed.
- **v56.6.0** — the color source expanded from two options into **12 built-in presets + wallpaper + a custom picker**, with a hue-ring / disc / spectrum-bar studio. `:core`'s `SeedColorScheme` generates all 24 Material roles from a single seed and guarantees WCAG AA (4.5:1) on every text-on-surface pair for any seed.
- **v56.5.2** — Compose stability configuration removed unstable inference for domain models. `:domain` has no Compose compiler, so `Book` / `Chapter` and friends were all inferred unstable, re-executing every visible book card on any state change. Declaring them stable dropped unstable classes from 52 to 37; every UiState is stable now.
- **v56.5.1** — localized the book detail and stats pages. Domain models now carry raw values instead of pre-concatenated strings, so they survive the v53 language switch rather than freezing in Chinese.
- **v56.4.4** — fixed "content hidden behind the top bar once data loads" on the shelf / stats / detail pages: `FlowStateHost`'s success branch dropped the `modifier`, so the 88dp safe area vanished exactly when there was data, while the same page's loading / empty / error states were fine.
- **v56.4.3** — an 11-fix functional bug sweep. In-book full-text search always returned empty because FTS5 columns have no type affinity; reading stats were discarded wholesale in PAGED mode and for comics; text-selection highlights saved one character short and overlapping highlights rendered twice; the "last 7 days" trend chart actually showed 2–3.
- **v56.4.2** — fixed issue #6: the outer shell applied window insets without consuming them, and the 9 screens beneath each applied them again, adding a full status-bar height at the top.

Full history in [CHANGELOG.md](CHANGELOG.md); longer-term plans and known tech debt in `ROADMAP.md`.

---

## Getting involved

- Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or PR — setup, CI gates, conventions and security constraints are all in there (bilingual).
- Participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- **Security vulnerabilities don't belong in public issues.** Report them privately per [SECURITY.md](SECURITY.md).

---

## License

[GNU General Public License v3.0](LICENSE)

<p align="center">Made with ❤️ by HuZaiGong</p>
