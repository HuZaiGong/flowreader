# CLAUDE.md（中文版）

本文件是 [CLAUDE.md](CLAUDE.md) 的中文对照版，为 Claude Code (claude.ai/code) 在本仓库工作时提供指引。两份内容保持同步；当文档与源码冲突时，一律以源码为准。

仓库语言约定：文档与用户可见字符串用中文，代码与标识符用英文。

FlowReader（心流阅读）是离线优先的 Android 电子书阅读器：Jetpack Compose + Material 3、Clean Architecture + MVVM，没有网络层也没有账号层。支持 EPUB / TXT / PDF / Markdown / FB2 / MOBI / 漫画（CBZ 与图片 ZIP）。

`AGENTS.md` 与 `AGENTS_CN.md` 保存了更长的阅读器与 Compose 行为陷阱清单，请与本文件一并阅读；但请先看文末「需注意的文档漂移」——其中若干计数已经过期。

## 命令

工具链：JDK 17、Android SDK 35（compileSdk/targetSdk 35，minSdk 26）、AGP 8.6.0、Kotlin 2.1.0、Compose BOM 2024.12.01、Hilt 2.55、Room 2.6.1、Roborazzi 1.40、Robolectric 4.14.1。wrapper 从腾讯云镜像拉取 Gradle 9.6.1，因此 wrapper 下载失败通常是镜像问题，而不是项目问题。

```bash
./gradlew assembleDebug            # 开发包
./gradlew assembleRelease          # R8 全模式压缩 + 资源裁剪；故意使用 debug 签名配置
./gradlew testDebugUnitTest        # 全部 JVM 单元测试
./gradlew verifyKotlinStyle        # ktlint（不含 :app）+ 全仓空白字符门禁
./gradlew coverageSummary          # 测试广度文件比，须保持 >= 40%
./gradlew verifyRoborazziDebug     # 截图回归门禁
./gradlew recordRoborazziDebug     # 有意的视觉改动后重录基准图
./gradlew performanceBaseline      # debug+release APK 体积与基线对比（会重写基线）
./gradlew clean                    # KSP/生成产物状态看起来异常时
```

单个测试类，或单个模块的测试：

```bash
./gradlew :app:testDebugUnitTest --tests com.flowreader.app.util.BookParserTest
./gradlew :domain:testDebugUnitTest
```

## CI 与发布自动化

两个工作流，都在推送 `main` 与向 `main` 提 PR 时触发。

- `ci.yml` 按此顺序执行：`verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug` → `verifyRoborazziDebug` → `performanceBaseline`。
- `build.yml` 构建 APK，并且**在推送 `main` 时自动发布 GitHub Release**：用 grep 从 `app/build.gradle.kts` 取出 `versionName`，打 `v<versionName>` 标签，再用 `awk` 抽取 `CHANGELOG.md` 中 `## [v<versionName>]` 段落作为发布正文。所以在任何内容合入 `main` 之前，`versionName`、CHANGELOG 标题与目标标签三者必须一致，否则发布出去的版本标签错配、正文为空。`versionCode` 规则是 `主*100 + 次*10 + 修订`（5640 = 56.4.0）。因此升版本必然是一对耦合的改动：`app/build.gradle.kts` 里的 `versionName`/`versionCode`，以及 `CHANGELOG.md` 中对应的 `## [vX.Y.Z]` 标题。

未真正运行过 Gradle 任务前，不要声称检查已通过。

## 四个容易踩的验证门禁

- **ktlint 跳过 `:app`。** 根 `build.gradle.kts` 的 `subprojects` 块对除 `:app` 以外的每个模块启用 ktlint。`:app` 模块的 Kotlin 只受 `verifyKotlinStyle` 内那道空白字符门禁约束——仓库里任何 `.kt`/`.kts` 文件出现 Tab 或行尾空格，都会让整个构建失败。`.editorconfig` 规定：4 空格缩进、LF 换行、140 字符行宽、`android_studio` ktlint 风格。

- **`coverageSummary` 是文件数量比，不是行覆盖率。** 分子：`app`/`core`/`feature:*`/`domain` 四个测试源集里的测试文件。分母：app 的 `data/repository/*Repository*.kt` + app 的 `ui/screens/**/*ViewModel.kt` + feature 的 `*ViewModel.kt` + **`:core` 的全部主源文件** + `domain/repository/*.kt` + `domain/model/*.kt`。当前 44/62 = 71%（15 个 app + 10 个 core + 3 个 feature + 16 个 domain 测试）。仅仅新增一个 `:core` 文件、一个 domain 模型或一个 ViewModel 而不配上对应测试，就足以让构建失败。

- **基准截图已入库。** Roborazzi 1.40 + Robolectric 跑在 JVM 上（不需要模拟器）；两张基准图在 `app/src/test/snapshots/`（`library_shelf_light.png`、`library_skeleton_dark.png`）。截图 API 是 `captureRoboImage(filePath = "src/test/snapshots/<name>.png") { content }` 搭配 `@GraphicsMode(NATIVE)`——1.40 已经没有 `RoborazziRule` 了。插件只在 `:app` 启用。任何有意的视觉改动都需要跑 `recordRoborazziDebug` 并提交新的 PNG。

- **`performanceBaseline` 会重写自己的基线。** 它构建 debug + release 两个包，与 `baseline/apk-size.properties`（debug 27540KB / release 10839KB）对比并对体积增长告警，然后覆盖该文件。本地运行会产生一处 diff——除非体积变化本身是预期的，否则不要提交这次重写。

## 模块划分与当前实况

模块：`:app`、`:core`、`:data`、`:domain`、`:feature:library`、`:feature:reader`。

允许的依赖方向：`feature:* → core/domain`、`data → core/domain`、`app → core/data/domain/feature:*`。feature 不得依赖 `:app`；`:data` 不得依赖 feature；`:domain` 保持不引入 app/Room/Compose/Hilt。

`:feature:*` 的抽取仍在进行中，而且进度并不均匀：

- `:domain` — `domain/model/` 下 10 个模型，`domain/repository/` 下 10 个仓库接口（接口一文件一个）。有几个模型文件里装了不止一个类型：`Chapter`、`PageMode`、`GestureSettings`、`AppThemeMode`、`ColorSource`、`ReaderPaletteId`、`ReaderFontFamily` 都寄居在其他文件中。**没有** `usecase/` 包——业务逻辑刻意放在 ViewModel 里。

- `:core` — 22 个 Kotlin 文件：设计系统，加上所有值得写单测的、不依赖 Compose 的工具。包含 `designsystem/token/`、`designsystem/theme/`（`FlowTheme`、`FlowLocale`）、`designsystem/component/`（`BookCover`、`FlowScaffold`、`FlowTopBar`、`FlowStateHost`、`SkeletonBox`、`FlowComponentPreviews`）、`designsystem/reader/`（12 套 `ReaderPalette`、`ReaderMetrics`、`ReaderTypography`），以及 `core/util/`（`ColorContrast`、`CoverArt`、`FlowFormatters`、`ReaderBehavior`、`ReaderCustomTheme`、`ReadingProgress`、`AnnotationExporter`、`ShelfExporter`）。`core/util/` 与 `ReaderMetrics` 刻意与 Compose 解耦。`:core` 自带 `strings.xml`（另有 `values-en/ja/ko`），并且**确实**通过 `api(...)` 暴露了 Coil，好让 `BookCover` 自洽可用；它不含 Hilt、Room 与导航。

- `:feature:reader` — v54 抽出的阅读器纯逻辑：`ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`，各自配有 JVM 测试。注意这个模块用的是 `com.flowreader.feature.reader` 包名，与其余所有模块都不同。

- `:feature:library` — 仍然是一个参与编译与 lint 的空占位模块，没有源文件。

- `:data` — 只有 Room：`data/local/` 下的 `AppDatabase` + 7 个 DAO + 8 个实体。8 个实体分布在 7 个文件里（`ReadingListEntity.kt` 同时装了 `ReadingListEntity` 与 `ReadingListItemEntity`，外加 `ReadingListWithCount` / `ReadingListBookRow` 两个投影类）。

- `:app` — 组装根：`MainActivity`、`FlowReaderApplication`、`ui/Navigation.kt`、`ui/FlowReaderApp.kt`、`ui/AppShellViewModel.kt`、`di/AppModule.kt`、`ui/screens/` 下 10 个屏幕包、`util/`（各类解析器、`CacheManager`、`FullTextSearch`、`MemoryManager`、`TtsManager`、`OpdsClient`、`LanTransfer*`、`ShareCardGenerator`、`ZipImporter`、`BookLoader`）、`widget/`、`provider/`，以及 `data/repository/` 里的 **全部 10 个仓库实现**。

注意命名空间与包名不一致：模块 namespace 是 `com.flowreader.core` / `.domain` / `.data`，但源码包名是 `com.flowreader.app.*`。新文件请沿用 `com.flowreader.app.*`（`:feature:reader` 除外）。

向 `:feature:*` 迁移代码时记住：那些模块只声明了 Compose UI + Material3（外加 `:core`/`:domain`）。Hilt、Room、导航、lifecycle 与 Coil 都声明在 `:app`，需要显式补上。

## 数据流

`Composable → ViewModel → domain 仓库接口 → data 仓库实现 → Room DAO / DataStore`。

每个 `ui/screens/<screen>/` 目录内，`*Screen.kt` 与 `*ViewModel.kt` 成对出现。每个 ViewModel 都以一个私有 `MutableStateFlow` 作为支撑，对外暴露不可变的 `StateFlow<XxxUiState>`。错误处理使用 Kotlin 内置的 `Result`；旧的自定义 `AppException`/`Result` 封装已经删除。

所有 Hilt 装配集中在一个文件里，即 `app/src/main/java/com/flowreader/app/di/AppModule.kt`：`DatabaseModule` 构建 `AppDatabase`、注册迁移，并逐个 `@Provides` 全部 7 个 DAO；`RepositoryModule` 用 `@Binds` 把全部 10 个实现绑定到各自接口。新增一个仓库意味着四件事：`domain/repository/` 里的接口、`app/data/repository/` 里的实现、一条 `@Binds` 条目，以及一个测试（否则覆盖率门禁会挂）。有两个实现文件是以接口而非实现命名的——`BackupRepository.kt` 里装的是 `BackupRepositoryImpl`，`SettingsRepository.kt` 里装的是 `SettingsRepositoryImpl`。

### 三套彼此独立的持久化

1. **Room**（`flowreader_db`）— `AppDatabase` 是**第 7 版**，8 个实体，`exportSchema = true`；导出的 schema 在 `data/schemas/com.flowreader.app.data.local.AppDatabase/`（有 4、6、7，没有 5.json）。`MIGRATION_4_5`（新增 `books.tags`）、`MIGRATION_5_6`（书签 `(bookId, chapterIndex, position)` 索引）与 `MIGRATION_6_7`（v53 的书单及其条目）全部在 `DatabaseModule` 中注册。刻意**不使用** `fallbackToDestructiveMigration()`——任何表结构变更都必须手写迁移。`app/build.gradle.kts` 里仍然把 `room.schemaLocation` 设成 `$projectDir/schemas`，但该目录并不存在，而且自 `AppDatabase` 迁到 `:data` 之后也不再有任何东西生成到那里。`BackupRepositoryImpl.importData()` 使用 `database.withTransaction`——请保持备份导入的原子性。

2. **第二个裸 SQLite 数据库**（`flowreader_fts.db`）由 `util/FullTextSearch.kt` 通过 `openOrCreateDatabase` 管理：一张 FTS5 虚拟表（`book_content_fts`）影随一张 `book_content` 表，完全在 Room 之外。它同时支撑书内搜索（注入到 `ReaderViewModel`）与全局搜索（`SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()`）。`SearchRepositoryImpl` 用 `Mutex` 把重建串行化——并发搜索曾经交错执行删除/索引并损坏索引。不要试图把它并入 `AppDatabase`。

3. **DataStore Preferences**（`settings`）— `SettingsRepositoryImpl` 独占 `Context.dataStore` 扩展与所有偏好键。`reader_theme` 这个键被复用为存放 `ReaderPaletteId`，旧的 `LIGHT`/`DARK` 值在 `ReaderPaletteId.fromStoredName` 中完成迁移。阅读设置从来没有进过 Room，所以阅读偏好的改动永远不需要数据库迁移。

`util/CacheManager.kt` 是唯一的章节内容/元数据/封面缓存。它的容量取自 `MemoryManager.getRecommendedCacheSize()`，每 50 次访问采样一次命中率，据此在 2–12 之间自适应每本书的章节容量，在中度内存回收时淘汰最少使用的书，并实现了 `ComponentCallbacks2`。`ChapterRepositoryImpl` 的章节读取全部走它——不要再加第二个章节缓存。

## 导航外壳

路由是 `ui/Navigation.kt` 中的 sealed class `Screen`，共 10 条：`library`、`stats`、`settings`、`wheel`、`notes`、`reading_lists`、`opds`、`search?query={query}`、`book_detail/{bookId}`、`reader/{bookId}?chapterIndex={chapterIndex}`。每个 `Screen` 持有的是 `@StringRes` 整型资源 ID，而不是 `String`——写死的 `String` 会在首次组合时固化，语言切换后不会更新。跳转请用 `createRoute(...)` 辅助函数；`Screen.Reader.createRoute(bookId, chapterIndex = -1)` 省略 `chapterIndex` 表示「续读」，传非负值则直达该章。

主题在 NavHost 层由 `:core` 的 `FlowTheme` 统一应用一次（`AppThemeMode` LIGHT/DARK/FOLLOW_SYSTEM × `ColorSource` BRAND/DYNAMIC）——不要给单个屏幕再包一层主题。底部标签只有书库 / 统计 / 设置；转盘、笔记、书单与 OPDS 都是从书库顶栏进入的次级页面。v55 起外壳是自适应的：宽度 ≥ 600dp 时把底栏换成手写的导航栏（navigation rail），因为 `NavigationSuiteScaffold` 需要 Material3 1.4，而后者又需要 Compose 1.9+。

## 需注意的文档漂移

以下是配套文档中已经过期的信息；请以源码与本文件为准。

- `AGENTS.md` 写着覆盖率 55.8%（v52），并把 `:feature:*` 称为「仍然空着的迁移边界」；实际覆盖率是 71%，而且 `:feature:reader` 自 v54 起就有三个带测试的源文件了。它还只列了 `MIGRATION_4_5` / `MIGRATION_5_6`（漏了 `6_7`），路由清单也不完整（漏了 `notes`、`reading_lists`、`opds`）。
- `README.md` 写着 8 个仓库接口以及「6 DAO + 6 Entity」；真实数量是 10 个接口、7 个 DAO、8 个实体。
- `ARCHITECTURE.md` 仍在描述 v51 的计划，早于 `:core` 设计系统与 `:feature:reader` 的抽取。
- `ROADMAP.md` 的版本戳是 `v56.0.0`，`README.md` 仍在引用 v50.0 / v52.0 的特性节点；两者都不跟随当前版本号。
