# FlowReader

**Current version: 45.0.1**

Offline-first Android e-book reader (EPUB/TXT/PDF/Markdown). Single-module, Clean Architecture + MVVM.

All Phase 1-3 roadmap items are complete. Key features include: per-book reading stats, 7-day trend chart, EPUB image/CSS rendering, custom font import, font family selector, full-text search (FTS5), annotation highlighting, annotation/bookmark management, dynamic LRU cache with MemoryManager awareness.

---

## 架构 / Architecture

```
Composable → ViewModel → UseCase/Repository → Room DAO → SQLite
  ↕              ↕               ↕               ↕
ui/screens    domain/usecase  data/repository  data/local/dao
```

### DI (Hilt)

`di/AppModule.kt` has two modules:
- `DatabaseModule` (`@Provides`) — 6 DAOs: Book, Chapter, Bookmark, Annotation, Category, ReadingStats.
- `RepositoryModule` (`@Binds`) — 8 repos: Book, Chapter, Bookmark, Annotation, Category, ReadingStats, Backup, **Settings**.

### Navigation

`sealed class Screen` in `ui/Navigation.kt`. Routes: `library`, `wheel`, `stats`, `settings`, `book_detail/{bookId}`, `reader/{bookId}`. 4 bottom tabs: Library → Wheel → Stats → Settings.

### State

Each ViewModel exposes `StateFlow<XxxUiState>` via private `_uiState`. Progress save uses 3-second debounce (`debouncedSaveProgress()` in `ReaderViewModel.kt`).

### Full-Text Search

`FullTextSearch` (FTS5) is injected into `ReaderViewModel`. On book load, chapters are indexed in the background. Search is accessible via a search icon in the reader controls, opening a `SearchDialog` with results that navigate to the matching chapter. -> 阅读器加载书籍后自动索引全部章节，点击搜索按钮打开 SearchDialog 搜索，点击结果跳转到对应章节。

### Font Family

`FontFamily` enum (8 variants: DEFAULT, SERIF, SANS_SERIF, MONOSPACE, SONG, HEI, KAI, FANGSONG) can be selected via FilterChips in `ReaderSettingsDialog`. The selection is persisted in DataStore and applied to reader text rendering. -> 阅读设置面板支持 8 种字体选择，字体设置持久化到 DataStore。

### Theme / 主题

Only **DARK** and **LIGHT** themes. Applied globally at the `FlowReaderNavHost` level via `FlowReaderTheme` wrapping the outer `Scaffold`. No per-screen theme wrappers. The settings page exposes a simple dark mode toggle instead of a theme picker dialog. ->
主题仅深色和浅色两种，在 `FlowReaderNavHost` 层通过 `FlowReaderTheme` 全局包裹 `Scaffold` 生效，不在各页面单独包裹。设置页以开关形式切换深色模式。

### Error Handling / 错误处理

- ViewModel load methods (`loadBook()`, `loadBookDetails()`, `loadStats()`, `loadChapterContent()`): wrap in `try-catch`, store error in `UiState.error` (`String?`). This prevents uncaught exceptions from crashing the app. -> ViewModel 的加载方法用 try-catch 包裹，错误通过 UiState.error 展示，避免闪退。
- `bookId` from `SavedStateHandle` defaults to `0L` — always validate `bookId > 0` before DB queries. -> SavedStateHandle 取出的 bookId 可能为 null（默认 0L），必须校验 > 0。
- `WheelViewModel.spin()` validates `items` is non-empty before spinning; shows error if empty. -> 转盘旋转前校验是否有选项，无选项则展示错误提示。
- `PdfViewer` has retry button on load failure via `retryTrigger` state. -> PDF 加载失败时展示重试按钮。
- `SettingsScreen` shows Snackbar for export/import success/failure results. -> 设置页导入导出结果通过 Snackbar 展示。
- ReaderContent renders annotation highlights via `buildAnnotatedString` + `SpanStyle(background=color)`. -> ReaderContent 使用 buildAnnotatedString 渲染标注高亮背景色。
- Custom `AppException.kt` (sealed class + custom `Result<T>`) and `DataManager.kt` have been removed — they were dead code.

### Performance / 性能关键点

- **Reading progress save**: 3-second debounce to reduce DB writes. 阅读进度保存 3 秒防抖.
- **Cache**: `CacheManager` uses `LinkedHashMap` (LRU) for chapter content, `ConcurrentHashMap` for book metadata. Tracks real hit/miss rates (previously hardcoded `0.75f`).
- **ReadingStats `date` index**: `ReadingStatsEntity` has `(bookId, date)` unique + `date` index for date-only queries. 统计数据表有 date 独立索引.
- **`BookLoader`**: uses single `CoroutineScope` (not per-call), call `cancelAll()` carefully — it permanently cancels the scope.

---

## Room DB

- 6 entities, DB version 4, `exportSchema=true` (schema JSON in `app/schemas/`).
- All DAO and Entity files are in `data/local/dao/` and `data/local/entity/` respectively, mirroring 1:1.
- `BackupRepositoryImpl.importData()` uses `database.withTransaction` for atomic batch insert.
- `fallbackToDestructiveMigration()` has been removed — migrations use explicit `addMigrations()` for future version bumps.
- `ChapterRepositoryImpl` routes all chapter content through `CacheManager` (single cache layer with LRU eviction + `ComponentCallbacks2`).
- Domain repository interfaces are now in separate files under `domain/repository/` (BookRepository.kt is the only one that previously contained 4 additional interfaces).

### Indexes / 索引

| Entity | Index | Purpose |
|--------|-------|---------|
| `ReadingStatsEntity` | `(bookId, date)` UNIQUE | Per-book daily stats |
| `ReadingStatsEntity` | `(date)` | Date-only aggregation queries |

---

## ⚠️ Gotchas / 容易踩的坑

### Domain interface files / 领域接口文件位置

Domain interfaces for Chapter, Bookmark, Annotation, and Category are **all inside `domain/repository/BookRepository.kt`**, not in separate files. When adding new methods to these interfaces, edit that file. ->
Chapter、Bookmark、Annotation、Category 的接口都定义在 `domain/repository/BookRepository.kt` 中，不是独立文件。

### `SettingsRepository` now has a domain interface

`domain/repository/SettingsRepository.kt` interface exists with `data/repository/SettingsRepositoryImpl` (renamed from `SettingsRepository`, implements the interface). Injected via Hilt `@Binds` in `AppModule.kt`.

### `sessionReadPages` must be incremented manually

`ReaderViewModel` declares `sessionReadPages` but **does not auto-increment it**. Reading stats will silently fail to save unless `sessionReadPages += pages` is called in `updatePosition()`. ->
阅读统计的 `sessionReadPages` 值需要在 `updatePosition()` 中手动累加，否则统计永远不保存。

### Release build uses debug signing / Release 用 debug 签名

`signingConfig = signingConfigs.getByName("debug")` in `app/build.gradle.kts:31`. This is intentional for CI — it produces `app-release.apk` (not `-unsigned`), matching the GitHub Actions upload path. The APK is signed with the Android SDK debug keystore, fine for testing. ->
CI 构建的 Release APK 使用 debug 签名，产物是 `app-release.apk`，仅用于测试分发。

### `kotlin.Result` usage

The codebase uses the built-in `kotlin.Result` (not a custom `Result<T>`). Some repository methods return `kotlin.Result`. The old custom `AppException.kt` sealed class and its `Result<T>` wrapper have been removed as dead code.

### WheelViewModel spin() is non-suspend

`WheelViewModel.spin()` is a regular function that launches its own coroutine via `viewModelScope.launch`. It is **not** a `suspend` function. No `LaunchedEffect` needed from the Composable.

### Nested LazyColumn crashes / 嵌套 LazyColumn 会闪退

Never put a `LazyColumn` inside another `LazyColumn`'s `item` block. Compose throws `IllegalStateException` because the inner scrollable component has no height constraint. Use a plain `Column` instead. This was fixed in `BookDetailScreen.kt`. ->
LazyColumn 内部 item 里不能再放 LazyColumn，否则 Compose 会因测量高度为 0 而抛出 `IllegalStateException` 崩溃。应使用 `Column` 替代。

### 3-second debounce on progress

`ReaderViewModel.debouncedSaveProgress()` cancels the previous job and delays 3000ms before writing. Rapid scrolling triggers only one write every 3 seconds.

---

## Build & Test / 构建与测试

```bash
./gradlew assembleDebug          # debug APK
./gradlew assembleRelease        # release APK (R8 minified, shrink resources)
./gradlew testDebugUnitTest      # unit tests only (JUnit 4 + MockK)
./gradlew clean
```

- **No lint/detekt/ktlint** — only `.editorconfig` for code style. 没有配置代码检查工具.
- **Single test file**: `app/src/test/java/com/flowreader/app/util/BookParserTest.kt`.
- Room KSP output: `app/build/generated/ksp/`.
- `coreLibraryDesugaring` enabled for `java.time` backport.
- **JDK 17 required**, Android SDK 35.

### CI (GitHub Actions)

| Event | Jobs |
|-------|------|
| PR | `testDebugUnitTest` + `assembleDebug` → upload `app-debug.apk` |
| Push to `main` | Same + `assembleRelease` → create GitHub Release (`v$versionName`) + upload `app-release.apk` |

---

## Layout Conventions / 目录约定

| Directory | Purpose |
|-----------|---------|
| `ui/screens/*/` | Each screen: `*Screen.kt` (Composable) + `*ViewModel.kt` (Hilt) |
| `domain/repository/` | Interface definitions (Chapter etc. live in `BookRepository.kt`) |
| `data/repository/` | Interface implementations |
| `data/local/dao/` | Room DAOs |
| `data/local/entity/` | Room entities |
| `util/` | Tooling: `BookParser`, `BookLoader`, `CacheManager`, `TtsManager`, `FullTextSearch`, `MemoryManager` |
| `app/src/test/` | Unit tests |

All Kotlin source under `app/src/main/java/com/flowreader/app/`.

### Config files / 配置文件

| File | Role |
|------|------|
| `app/build.gradle.kts` | AGP 8.6.0, Kotlin 2.0.21, Compose BOM 2024.12.01 |
| `build.gradle.kts` | Root plugins (Hilt, KSP, Compose) |
| `gradle.properties` | Parallel GC, VFS watch, incremental Kotlin, daemon JVM args |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.6.1, 60s network timeout |
