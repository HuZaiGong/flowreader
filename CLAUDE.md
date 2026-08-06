# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

本文件为 Claude Code (claude.ai/code) 在本仓库工作时提供指引。仓库约定：文档与用户可见字符串用中文，代码与标识符用英文。

FlowReader (心流阅读) is an offline-first Android e-book reader — Jetpack Compose + Material 3, Clean Architecture + MVVM, no network or account layer. Supports EPUB / TXT / PDF / Markdown / FB2 / MOBI / comics (CBZ and image ZIPs).

FlowReader（心流阅读）是离线优先的 Android 电子书阅读器：Jetpack Compose + Material 3、Clean Architecture + MVVM，无网络与账号层。支持 EPUB / TXT / PDF / Markdown / FB2 / MOBI / 漫画（CBZ 与图片 ZIP）。

`AGENTS.md` / `AGENTS_CN.md` carry the long list of reader and Compose behavioral gotchas — read them alongside this file, but check "Doc drift" at the end: several of their counts are stale. When a doc disagrees with the source, the source wins.

`AGENTS.md` / `AGENTS_CN.md` 保存了更长的阅读器与 Compose 行为陷阱清单，请与本文件一并阅读；但请留意文末「文档漂移」：其中若干计数已过期。文档与源码冲突时以源码为准。

## Commands / 命令

Toolchain: JDK 17, Android SDK 35 (compileSdk/targetSdk 35, minSdk 26), AGP 8.6.0, Kotlin 2.1.0, Compose BOM 2024.12.01, Hilt 2.55, Room 2.6.1, Roborazzi 1.40, Robolectric 4.14.1. The wrapper pulls Gradle 9.6.1 from a Tencent Cloud mirror, so wrapper download failures are usually mirror-related rather than project-related.

工具链如上。wrapper 从腾讯云镜像拉取 Gradle 9.6.1，因此 wrapper 下载失败通常是镜像问题而非项目问题。

```bash
./gradlew assembleDebug            # dev APK / 开发包
./gradlew assembleRelease          # R8 full-mode minify + resource shrink; signs with the DEBUG config on purpose / 故意用 debug 签名
./gradlew testDebugUnitTest        # all JVM unit tests / 全部 JVM 单元测试
./gradlew verifyKotlinStyle        # ktlint (non-:app modules) + repo-wide whitespace gate / ktlint（非 :app）+ 全仓空白门禁
./gradlew coverageSummary          # test-breadth file ratio, must stay >= 40% / 测试广度文件比，须 >= 40%
./gradlew verifyRoborazziDebug     # screenshot regression gate / 截图回归门禁
./gradlew recordRoborazziDebug     # re-record goldens after an intentional visual change / 视觉变更后重录基准图
./gradlew performanceBaseline      # debug+release APK size vs baseline (rewrites the baseline) / APK 体积对比（会重写基线）
./gradlew clean                    # when KSP/generated state looks stale / KSP 产物状态异常时
```

Single test class, or one module's tests / 单个测试类或单模块测试:

```bash
./gradlew :app:testDebugUnitTest --tests com.flowreader.app.util.BookParserTest
./gradlew :domain:testDebugUnitTest
```

## CI and release automation / CI 与发布自动化

Two workflows, both triggered on push to `main` and on PRs to `main`. / 两个工作流，均在推送 `main` 与向 `main` 提 PR 时触发。

- `ci.yml` runs, in order: `verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug` → `verifyRoborazziDebug` → `performanceBaseline`.
  `ci.yml` 按上述顺序执行六个任务。
- `build.yml` builds APKs and, **on push to `main`, publishes a GitHub Release automatically**: it greps `versionName` out of `app/build.gradle.kts`, tags `v<versionName>`, and fills the release body by `awk`-extracting the `## [v<versionName>]` section of `CHANGELOG.md`. `versionName`, the CHANGELOG heading and the intended tag must agree before anything lands on `main`, or the release ships mislabeled with an empty body. `versionCode` is `major*100 + minor*10 + patch` (5640 = 56.4.0). A version bump is therefore two coupled edits — `versionName`/`versionCode` in `app/build.gradle.kts` and a matching `## [vX.Y.Z]` heading in `CHANGELOG.md`.
  `build.yml` 会在推送 `main` 时**自动发布 GitHub Release**：从 `app/build.gradle.kts` 取 `versionName` 打 `v<versionName>` 标签，并用 `awk` 抽取 `CHANGELOG.md` 中 `## [v<versionName>]` 段作为发布说明。合入 `main` 前，`versionName`、CHANGELOG 标题与目标标签必须一致，否则发布标签错配且正文为空。`versionCode` 规则为 `主*100 + 次*10 + 修订`（5640 = 56.4.0）。因此升版本必须成对修改：`app/build.gradle.kts` 里的 `versionName`/`versionCode`，以及 `CHANGELOG.md` 中对应的 `## [vX.Y.Z]` 标题。

Never claim a check passed unless you actually ran the Gradle task. / 未真正运行 Gradle 任务前，不要声称检查已通过。

## Four verification gates that trip people up / 四个容易踩的验证门禁

- **ktlint skips `:app`.** The root `build.gradle.kts` `subprojects` block applies ktlint to every module *except* `:app`. App-module Kotlin is only covered by the whitespace gate inside `verifyKotlinStyle`, which fails the whole build on any tab character or trailing whitespace in any `.kt`/`.kts` file in the repo. `.editorconfig`: 4-space indent, LF, 140-char lines, `android_studio` ktlint style.
  **ktlint 跳过 `:app`。** 根 `build.gradle.kts` 的 `subprojects` 块对除 `:app` 外的所有模块启用 ktlint。`:app` 的 Kotlin 仅受 `verifyKotlinStyle` 内的空白门禁约束——仓库内任何 `.kt`/`.kts` 出现 Tab 或行尾空格都会让整个构建失败。

- **`coverageSummary` is a file-count ratio, not line coverage.** Numerator: test files in the `app`/`core`/`feature:*`/`domain` test source sets. Denominator: app `data/repository/*Repository*.kt` + app `ui/screens/**/*ViewModel.kt` + feature `*ViewModel.kt` + **every** `:core` main source file + `domain/repository/*.kt` + `domain/model/*.kt`. Currently 44/62 = 71% (15 app + 10 core + 3 feature + 16 domain tests). Adding a `:core` file, a domain model or a ViewModel without a matching test can break the build on its own.
  **`coverageSummary` 是文件数量比，不是行覆盖率。** 分子为四个模块测试源集中的测试文件；分母为 app 仓库实现 + app ViewModel + feature ViewModel + **`:core` 全部主源文件** + domain 的 repository 与 model。当前 44/62 = 71%。仅新增一个 `:core` 文件、domain 模型或 ViewModel 而不配测试，就足以让构建失败。

- **Screenshot goldens are committed.** Roborazzi 1.40 + Robolectric run on the JVM (no emulator); the two goldens live in `app/src/test/snapshots/` (`library_shelf_light.png`, `library_skeleton_dark.png`). The capture API is `captureRoboImage(filePath = "src/test/snapshots/<name>.png") { content }` with `@GraphicsMode(NATIVE)` — `RoborazziRule` no longer exists in 1.40. The plugin is applied only in `:app`. Any deliberate visual change needs `recordRoborazziDebug` and the new PNGs committed.
  **基准截图已入库。** Roborazzi 1.40 + Robolectric 跑在 JVM 上（无需模拟器），两张基准图在 `app/src/test/snapshots/`。1.40 已移除 `RoborazziRule`，请用上述 `captureRoboImage` 写法。插件仅在 `:app` 启用。任何有意的视觉改动都需重录并提交新 PNG。

- **`performanceBaseline` rewrites its own baseline.** It builds debug + release APKs, warns on growth against `baseline/apk-size.properties` (debug 27540KB / release 10839KB), then overwrites that file. Running it locally shows up as a diff — don't commit the rewrite unless the size change is intended.
  **`performanceBaseline` 会重写自己的基线。** 它构建 debug + release 包，与 `baseline/apk-size.properties` 比较并对增长告警，随后覆盖该文件。本地运行会产生 diff——除非体积变化是预期的，否则不要提交这次重写。

## Module layout and its current reality / 模块划分与当前实况

Modules: `:app`, `:core`, `:data`, `:domain`, `:feature:library`, `:feature:reader`.

Allowed dependency direction: `feature:* → core/domain`, `data → core/domain`, `app → core/data/domain/feature:*`. Features must not depend on `:app`; `:data` must not depend on features; `:domain` stays free of app/Room/Compose/Hilt.

允许的依赖方向如上。feature 不得依赖 `:app`；`:data` 不得依赖 feature；`:domain` 不引入 app/Room/Compose/Hilt。

The `:feature:*` extraction is still in progress and it is uneven / `:feature:*` 抽取仍在进行且进度不均：

- `:domain` — 10 models in `domain/model/` and 10 repository interfaces in `domain/repository/` (one interface per file). Several model files hold more than one type: `Chapter`, `PageMode`, `GestureSettings`, `AppThemeMode`, `ColorSource`, `ReaderPaletteId` and `ReaderFontFamily` all live inside other files. There is no `usecase/` package — business logic lives in ViewModels by deliberate choice.
  `:domain` — 10 个模型与 10 个仓库接口（接口一文件一个）。多个模型文件内含多个类型（如 `Chapter`、`PageMode`、`GestureSettings`、`AppThemeMode`、`ColorSource`、`ReaderPaletteId`、`ReaderFontFamily` 都寄居在其他文件中）。**没有** `usecase/` 包，业务逻辑刻意放在 ViewModel。

- `:core` — 22 Kotlin files: the design system plus every Compose-free helper worth unit-testing. `designsystem/token/`, `designsystem/theme/` (`FlowTheme`, `FlowLocale`), `designsystem/component/` (`BookCover`, `FlowScaffold`, `FlowTopBar`, `FlowStateHost`, `SkeletonBox`, `FlowComponentPreviews`), `designsystem/reader/` (12 `ReaderPalette`s, `ReaderMetrics`, `ReaderTypography`), and `core/util/` (`ColorContrast`, `CoverArt`, `FlowFormatters`, `ReaderBehavior`, `ReaderCustomTheme`, `ReadingProgress`, `AnnotationExporter`, `ShelfExporter`). `core/util/` and `ReaderMetrics` are deliberately Compose-free. `:core` owns its own `strings.xml` (plus `values-en/ja/ko`) and **does** expose Coil via `api(...)` so `BookCover` ships complete — it has no Hilt, Room or navigation.
  `:core` — 22 个 Kotlin 文件：设计系统 + 所有值得单测的无 Compose 依赖工具。`core/util/` 与 `ReaderMetrics` 刻意与 Compose 解耦。`:core` 自带 `strings.xml`（含 en/ja/ko），并通过 `api(...)` 暴露 Coil 以保证 `BookCover` 自洽；不含 Hilt、Room 与导航。

- `:feature:reader` — the reader's pure logic, extracted in v54: `ChapterPaginator`, `ReaderProgressEngine`, `ReaderSessionTracker`, each with JVM tests. Note this module uses the `com.flowreader.feature.reader` package, unlike everything else.
  `:feature:reader` — v54 抽出的阅读器纯逻辑三件套，各配 JVM 测试。注意此模块使用 `com.flowreader.feature.reader` 包名，与其余模块不同。

- `:feature:library` — still an empty compiled/linted placeholder, no source files.
  `:feature:library` — 仍是参与编译与 lint 的空占位模块，无源文件。

- `:data` — Room only: `AppDatabase` + 7 DAOs + 8 entities under `data/local/`. The 8 entities live in 7 files (`ReadingListEntity.kt` holds both `ReadingListEntity` and `ReadingListItemEntity`, plus the `ReadingListWithCount` / `ReadingListBookRow` projections).
  `:data` — 仅 Room：`AppDatabase` + 7 个 DAO + 8 个实体。8 个实体分布在 7 个文件中（`ReadingListEntity.kt` 同时含 `ReadingListEntity` 与 `ReadingListItemEntity` 及两个投影类）。

- `:app` — composition root: `MainActivity`, `FlowReaderApplication`, `ui/Navigation.kt`, `ui/FlowReaderApp.kt`, `ui/AppShellViewModel.kt`, `di/AppModule.kt`, 10 screen packages under `ui/screens/`, `util/` (parsers, `CacheManager`, `FullTextSearch`, `MemoryManager`, `TtsManager`, `OpdsClient`, `LanTransfer*`, `ShareCardGenerator`, `ZipImporter`, `BookLoader`), `widget/`, `provider/`, and **all 10 repository implementations** in `data/repository/`.
  `:app` — 组装根：入口、导航、DI、10 个屏幕包、`util/` 工具集、Widget、ContentProvider，以及 **全部 10 个仓库实现**。

Note the package/namespace mismatch: module namespaces are `com.flowreader.core` / `.domain` / `.data`, but source packages are `com.flowreader.app.*`. Keep new files in `com.flowreader.app.*` (except in `:feature:reader`).

注意命名空间与包名不一致：模块 namespace 为 `com.flowreader.core` / `.domain` / `.data`，但源码包名是 `com.flowreader.app.*`。新文件请沿用 `com.flowreader.app.*`（`:feature:reader` 除外）。

When moving code into `:feature:*`, remember those modules declare only Compose UI + Material3 (+ `:core`/`:domain`). Hilt, Room, navigation, lifecycle and Coil are declared in `:app` and must be added explicitly.

向 `:feature:*` 迁移代码时注意：这些模块只声明了 Compose UI + Material3（外加 `:core`/`:domain`）。Hilt、Room、导航、lifecycle 与 Coil 都在 `:app` 声明，需显式补充。

## Data flow / 数据流

`Composable → ViewModel → domain repository interface → data repository impl → Room DAO / DataStore`.

Each `ui/screens/<screen>/` directory pairs `*Screen.kt` with `*ViewModel.kt`. Every ViewModel exposes an immutable `StateFlow<XxxUiState>` backed by a private `MutableStateFlow`. Errors use built-in `kotlin.Result`; the old custom `AppException`/`Result` wrapper is gone.

每个 `ui/screens/<screen>/` 目录内 `*Screen.kt` 与 `*ViewModel.kt` 成对出现。每个 ViewModel 以私有 `MutableStateFlow` 支撑，对外暴露不可变 `StateFlow<XxxUiState>`。错误用 Kotlin 内置 `Result`；旧的自定义 `AppException`/`Result` 封装已删除。

All Hilt wiring lives in one file, `app/src/main/java/com/flowreader/app/di/AppModule.kt`: `DatabaseModule` builds `AppDatabase`, registers migrations and `@Provides` each of the 7 DAOs; `RepositoryModule` `@Binds` all 10 impls to their interfaces. A new repository means interface in `domain/repository/`, impl in `app/data/repository/`, a `@Binds` entry, and a test for the coverage gate. Two impl files are named after the interface rather than the impl — `BackupRepository.kt` holds `BackupRepositoryImpl`, `SettingsRepository.kt` holds `SettingsRepositoryImpl`.

所有 Hilt 装配集中在 `di/AppModule.kt`：`DatabaseModule` 构建 `AppDatabase`、注册迁移并 `@Provides` 全部 7 个 DAO；`RepositoryModule` 用 `@Binds` 绑定全部 10 个实现。新增仓库需要四件事：`domain/repository/` 接口、`app/data/repository/` 实现、`@Binds` 条目，以及一个测试（否则覆盖率门禁会挂）。注意两个实现文件以接口命名。

### Three separate persistence stores / 三套彼此独立的持久化

1. **Room** (`flowreader_db`) — `AppDatabase` is **version 7** with 8 entities and `exportSchema = true`; exported schemas are in `data/schemas/com.flowreader.app.data.local.AppDatabase/` (4, 6, 7 — there is no 5.json). `MIGRATION_4_5` (adds `books.tags`), `MIGRATION_5_6` (bookmark `(bookId, chapterIndex, position)` index) and `MIGRATION_6_7` (v53 reading lists + items) are all registered in `DatabaseModule`. There is intentionally **no** `fallbackToDestructiveMigration()` — every schema change needs a hand-written migration. `app/build.gradle.kts` still sets `room.schemaLocation` to `$projectDir/schemas`, but that directory does not exist and nothing generates into it now that `AppDatabase` lives in `:data`. `BackupRepositoryImpl.importData()` uses `database.withTransaction` — keep backup import atomic.
   **Room**（`flowreader_db`）— `AppDatabase` 为**第 7 版**，8 个实体，`exportSchema = true`，导出的 schema 在 `data/schemas/` 下（有 4、6、7，没有 5.json）。三个迁移全部在 `DatabaseModule` 注册。刻意**不使用** `fallbackToDestructiveMigration()`，任何表结构变更都必须手写迁移。`app/build.gradle.kts` 仍配置了 `room.schemaLocation`，但该目录已不存在也不再生成——`AppDatabase` 已迁至 `:data`。备份导入须保持事务原子性。

2. **A second raw SQLite database** (`flowreader_fts.db`) managed by `util/FullTextSearch.kt` via `openOrCreateDatabase` — an FTS5 virtual table (`book_content_fts`) shadowing a `book_content` table, entirely outside Room. It powers in-book search (injected into `ReaderViewModel`) and global search (`SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()`). `SearchRepositoryImpl` serializes rebuilds behind a `Mutex` — concurrent searches used to interleave delete/index and corrupt the index. Don't try to fold this into `AppDatabase`.
   **第二个裸 SQLite 库**（`flowreader_fts.db`）由 `util/FullTextSearch.kt` 用 `openOrCreateDatabase` 管理：FTS5 虚拟表 `book_content_fts` 影随 `book_content` 表，完全在 Room 之外。它同时支撑书内搜索与全局搜索。`SearchRepositoryImpl` 用 `Mutex` 串行化重建——并发搜索曾交错执行删除/索引并损坏索引。不要试图把它并入 `AppDatabase`。

3. **DataStore Preferences** (`settings`) — `SettingsRepositoryImpl` owns the `Context.dataStore` extension and every preference key. The `reader_theme` key was repurposed to hold a `ReaderPaletteId`, with old `LIGHT`/`DARK` values migrated in `ReaderPaletteId.fromStoredName`. Reading settings never lived in Room, so reader-preference changes never need a migration.
   **DataStore Preferences**（`settings`）— `SettingsRepositoryImpl` 独占 `Context.dataStore` 扩展与所有偏好键。`reader_theme` 键被复用为存放 `ReaderPaletteId`，旧的 `LIGHT`/`DARK` 值在 `ReaderPaletteId.fromStoredName` 中迁移。阅读设置从未进过 Room，因此阅读偏好的改动永远不需要数据库迁移。

`util/CacheManager.kt` is the single chapter-content/metadata/cover cache. It sizes itself from `MemoryManager.getRecommendedCacheSize()`, adapts per-book chapter capacity (2–12) to a hit rate sampled every 50 accesses, evicts least-used books on moderate trims, and implements `ComponentCallbacks2`. `ChapterRepositoryImpl` routes chapter reads through it — do not add a second chapter cache.

`util/CacheManager.kt` 是唯一的章节内容/元数据/封面缓存：容量取自 `MemoryManager.getRecommendedCacheSize()`，每 50 次访问采样命中率以在 2–12 之间自适应每本书的章节容量，中度内存回收时淘汰最少使用的书，并实现 `ComponentCallbacks2`。`ChapterRepositoryImpl` 的章节读取全部走它——不要再加第二个章节缓存。

## Navigation shell / 导航外壳

Routes are the sealed class `Screen` in `ui/Navigation.kt`, 10 in total: `library`, `stats`, `settings`, `wheel`, `notes`, `reading_lists`, `opds`, `search?query={query}`, `book_detail/{bookId}`, `reader/{bookId}?chapterIndex={chapterIndex}`. Each `Screen` carries a `@StringRes` int, not a `String` — a baked-in `String` would freeze at first composition and survive a language switch. Use the `createRoute(...)` helpers; `Screen.Reader.createRoute(bookId, chapterIndex = -1)` omits `chapterIndex` to resume, while a non-negative value jumps to that chapter.

路由是 `ui/Navigation.kt` 中的 sealed class `Screen`，共 10 条（见上）。每个 `Screen` 持有 `@StringRes` 整型资源 ID 而非 `String`——写死的 `String` 会在首次组合时固化，语言切换后不更新。跳转请用 `createRoute(...)`；`Screen.Reader.createRoute(bookId, chapterIndex = -1)` 省略章节表示「续读」，非负值则直达该章。

Theme is applied once in the nav host via `:core`'s `FlowTheme` (`AppThemeMode` LIGHT/DARK/FOLLOW_SYSTEM × `ColorSource` BRAND/DYNAMIC) — do not add per-screen theme wrappers. Bottom tabs are only Library / Stats / Settings; the wheel, notes, reading lists and OPDS are secondary destinations reached from the library top bar. Since v55 the shell is adaptive: width ≥ 600dp swaps the bottom bar for a hand-rolled navigation rail, because `NavigationSuiteScaffold` needs Material3 1.4 which needs Compose 1.9+.

主题在 NavHost 层由 `:core` 的 `FlowTheme` 统一应用一次，不要给单个屏幕再包主题。底部标签只有书库/统计/设置；转盘、笔记、书单、OPDS 均为从书库顶栏进入的次级页面。v55 起外壳自适应：宽度 ≥ 600dp 时底栏换成手写的导航栏（`NavigationSuiteScaffold` 需 M3 1.4，而后者需 Compose 1.9+）。

## Doc drift to be aware of / 需注意的文档漂移

These are stale in the companion docs; trust the source and this file. / 以下为配套文档中的过期信息，请以源码与本文件为准。

- `AGENTS.md` says coverage is 55.8% (v52) and calls `:feature:*` "still empty migration boundaries"; it is 71% and `:feature:reader` has had three tested sources since v54. It also lists only `MIGRATION_4_5` / `MIGRATION_5_6` (missing `6_7`) and an incomplete route list (missing `notes`, `reading_lists`, `opds`).
  `AGENTS.md` 的覆盖率、feature 模块状态、迁移清单与路由清单均已过期。
- `README.md` says 8 repository interfaces and "6 DAO + 6 Entity"; the real counts are 10 interfaces, 7 DAOs and 8 entities.
  `README.md` 的接口与 DAO/实体数量已过期（实为 10 / 7 / 8）。
- `ARCHITECTURE.md` still describes the v51 plan and predates the `:core` design-system and `:feature:reader` extractions.
  `ARCHITECTURE.md` 仍在描述 v51 计划，早于 `:core` 设计系统与 `:feature:reader` 的抽取。
- `ROADMAP.md` is stamped `v56.0.0`, and `README.md` still cites v50.0 / v52.0 feature milestones; neither tracks the current version.
  `ROADMAP.md` 标注为 `v56.0.0`，`README.md` 仍引用 v50.0 / v52.0 的特性节点；两者都不跟随当前版本号。

