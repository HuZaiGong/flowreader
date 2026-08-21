# FlowReader（心流阅读）

> 本文是 [AGENTS.md](AGENTS.md) 的中文对照版。两份内容保持同步；**当文档与源码冲突时，一律以源码为准**。
> （v56.6.2 重写：此前这份镜像停在 v45，声称项目「没有网络功能」、领域接口都在 `BookRepository.kt`、只有一个测试文件、没有 ktlint —— 全部已不成立。）

离线优先的 Android 电子书阅读器：Jetpack Compose + Material 3、Clean Architecture + MVVM、Hilt DI、Room + FTS5。所有数据留在设备上——无账号、无统计、无同步。支持 EPUB / TXT / PDF / Markdown / FB2 / MOBI / JPG-PNG-WebP / ZIP / CBZ（只读；DRM 一律拒绝）。文档与用户可见字符串用中文，代码、标识符与注释用英文。

Gradle 模块：`:app`、`:core`、`:data`、`:domain`、`:feature:library`、`:feature:reader`。包名 `com.flowreader.app`，applicationId `com.flowreader.app`，minSdk 26，compile/target SDK 35，GPL-3.0。当前构建文件版本 `56.6.2`（versionCode 5662），与 `CHANGELOG.md` 最新条目 v56.6.2 一致。

## 命令

- `./gradlew assembleDebug` —— 开发包。
- `./gradlew assembleRelease` —— R8 全模式压缩 + 资源裁剪。**仓库根目录存在 `keystore.properties`（已 git-ignore）时用正式密钥库签名，缺失时回退到 debug 密钥**（见 `app/build.gradle.kts` 的 `signingConfigs`/`buildTypes` 块）。回退是为了让没有密钥库的 CI 仍能产出可测试的 `app-release.apk`；只有在持有密钥库的机器上构建才会得到正式签名。
- `./gradlew testDebugUnitTest` —— 全部 JVM 单元测试（JUnit 4 + MockK + Robolectric，不需要模拟器）。
- `./gradlew :app:testDebugUnitTest --tests <全限定类名>` —— 单个测试类（如 `com.flowreader.app.util.BookParserTest`）。
- `./gradlew verifyKotlinStyle` —— 对**除 `:app` 以外**的每个模块跑 ktlint（根 `build.gradle.kts` 的 `subprojects` 块），外加一道覆盖全仓**所有** `.kt`/`.kts` 的轻量空白字符门禁：任何 tab 字符或行尾空格都会让构建失败。ktlint 强制 140 字符行宽上限，签名能放一行就必须放一行。
- `./gradlew recordRoborazziDebug` —— 录制截图基准到 `app/src/test/snapshots/`；`./gradlew verifyRoborazziDebug` 是 CI 门禁。Roborazzi 1.40 + Robolectric 跑在 JVM 上（`@GraphicsMode(NATIVE)`）；截图 API 是 `captureRoboImage(filePath = "src/test/snapshots/<name>.png") { content }` —— 1.40 里已经没有 `RoborazziRule` 了。当前基准图：`library_shelf_light.png`、`library_skeleton_dark.png`。
- `./gradlew performanceBaseline` —— 构建 debug+release，打印 APK 体积并与 `baseline/apk-size.properties` 对比（当前 debug 27687KB / release 11027KB），体积增长时给出警告，然后**重写基线**。属于 CI 的一环。两个坑：`PERF-WARNING` 只是一句 `println`，不会让构建失败；而且该任务**每次运行都重写基线**，所以一个错误的数字会直接变成新的参照。**永远不要用增量构建出来的 APK 判断体积回归。** AGP 的增量打包（zipflinger）替换 dex 条目时不重写整个归档，会把旧条目留成空洞；一个会话里重复构建十几次之后，debug APK 携带了 **1114KB 的条目间填充，而干净构建只有 73KB**（`DebugProbesKt.bin` 前面一个 933KB 的空洞），读起来像是「增长了 1063KB」，实际差异只有 8KB。先跑 `clean assembleDebug`，或者在一个用完即弃的 `git worktree` 里跟干净构建对比。
- `./gradlew coverageSummary` —— 测试广度的文件数比值（见「测试策略」），下限 40%；当前 **85.3%（58/68 个文件）**。
- `./gradlew clean` —— 生成代码／KSP 状态看起来不对时用。
- CI 是 `.github/workflows/ci.yml`（只监听 `main` 的 push/PR）：`verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug` → `verifyRoborazziDebug` → `performanceBaseline`。**没有真的跑过 Gradle 任务或查过 CI 结果，就不要声称某道门禁通过了。**

## 工具链

- 需 JDK 17 与 Android SDK 35。
- Gradle wrapper `9.6.1`，从 `mirrors.cloud.tencent.com` 下载；wrapper／网络失败通常是镜像问题，不是项目问题。
- AGP `8.6.0`、Kotlin `2.1.0`、Compose BOM `2024.12.01`、Hilt `2.55`、Room `2.6.1`（KSP）、ktlint `12.1.2`、Roborazzi `1.40.0`、Robolectric `4.14.1`、MockK `1.13.16`、coroutines `1.9.0`、Navigation Compose `2.8.5`、Coil `2.7.0`、JSoup `1.18.3`、Readium Kotlin Toolkit `3.1.2`、`desugar_jdk_libs 2.0.4`、`profileinstaller 1.4.1`。
- 启用 `coreLibraryDesugaring`（minSdk 26 上 `java.time` 可用）。Room schema 导出到 `data/schemas/`（`:app` 里那个指向 `app/schemas` 的 ksp 参数是遗留物，数据库现在在 `:data`）。KSP 输出在各模块的 `build/generated/ksp/` 下。
- `.editorconfig`：UTF-8、LF、4 空格缩进、`max_line_length = 140`、ktlint `android_studio` 风格、`ktlint_function_naming_ignore_when_annotated_with = Composable`。

## 模块与依赖规则

允许方向：`feature:* → core/domain`、`data → core/domain`、`app → core/data/domain/feature:*`。feature 模块不得依赖 `:app`；`:data` 不得依赖 feature；`:domain` 必须与 app/Room/Compose/Hilt 完全无关。

- `:app` —— 组合根：Hilt 图、导航外壳、所有页面与 ViewModel、仓库实现、解析器（`util/`）、桌面小组件、ContentProvider。历史上大部分 UI 都在这里；页面正在按 `:feature:*` 边界迁移。
- `:core` —— 设计系统与纯函数：`designsystem/token`（`FlowTokens`、`FlowBrandColors`、`FlowTypography`）、`designsystem/theme`（`FlowTheme`、`FlowLocale`）、`designsystem/component`（`BookCover`、`FlowScaffold`、`FlowTopBar`、`FlowStateHost`、`SkeletonBox`、`FlowComponentPreviews`）、`designsystem/reader`（`ReaderPalette`、`ReaderTypography`、`ReaderMetrics`）、`core/util`（纯函数：`ReaderBehavior`、`ReaderCustomTheme`、`ColorContrast`、`ReadingProgress`、`FlowFormatters`、`CoverArt`、`ShelfExporter`、`AnnotationExporter`）。只依赖 `:domain`；Coil 是 `api` 依赖，好让 `BookCover` 开箱可用。
- `:data` —— Room 本地存储：`AppDatabase`、7 个 DAO、8 个实体（`ReadingListItemEntity` 与 `ReadingListEntity.kt` 共用一个文件）。无 Hilt、无 Compose。
- `:domain` —— 模型（`Book`、`ReadingSettings`、`ReadingList`、`ReadingStats` 等）与仓库契约，另有 `ReadingListOrder` 这类纯逻辑。只依赖 coroutines-core。
- `:feature:library` —— 空的迁移边界（还没有源文件）。
- `:feature:reader` —— 已迁移的阅读器逻辑：`ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`（均有单测）。

## 架构

数据流：`Composable → ViewModel → domain 仓库接口 → data 仓库实现 → Room DAO`。

- 入口：`MainActivity.kt`（同时负责解析 `ACTION_VIEW` 导入 URI，见「安全」）、`FlowReaderApplication.kt`（`@HiltAndroidApp`）、根 UI 在 `ui/FlowReaderApp.kt`（`FlowReaderRoot`）与 `ui/Navigation.kt`（`FlowReaderNavHost`）。
- `ui/screens/<screen>/` 下 `*Screen.kt` 与 `*ViewModel.kt` 配对；页面有：`library`、`stats`、`settings`、`wheel`、`bookdetail`、`reader`、`search`、`notes`、`readinglist`、`opds`。每个 ViewModel 暴露一个不可变的 `StateFlow<XxxUiState>`，背后是私有的 `MutableStateFlow`。外壳是 `ui/AppShellViewModel.kt`，负责把主题模式 + 配色来源（`AppSettings`）交给 `FlowReaderNavHost`。
- 仓库接口在 `domain/repository/` 下**各自独立成文件**（10 个：Book、Chapter、Bookmark、Annotation、Category、ReadingStats、Backup、Settings、Search、ReadingList）；实现在 `:app` 的 `data/repository/`，在 `di/AppModule.kt` 里绑定。
- `di/AppModule.kt`：`DatabaseModule`（`@Provides`：带迁移的 AppDatabase + 7 个 DAO）与 `RepositoryModule`（`@Binds` × 10）。新增仓库时在这里加绑定。
- **完全没有 use-case 层**（`domain/usecase/` 不存在——是被删掉了，不要去找一个空目录）；逻辑放在 ViewModel 与 feature 模块的引擎里。

## 导航与主题

- 路由在密封类 `Screen`（`ui/Navigation.kt`）里，标题用 `@StringRes` 而**绝不用字面量**（v53 的应用内语言切换会在首次组合时冻结持有字符串的静态字段）：`library`、`stats`、`settings`（底部标签／侧边栏），`wheel`、`notes`、`reading_lists`、`opds`（二级页面，从书架顶栏溢出菜单进入），`search?query={query}`、`book_detail/{bookId}`、`reader/{bookId}?chapterIndex={chapterIndex}`。
- `Screen.Reader.createRoute(bookId, chapterIndex = -1)` 在续读时**省略** `chapterIndex`；传非负值则直接跳到该章。`Screen.Search.createRoute(query)` 在 query 为空时省略参数。
- 自 v55 起导航自适应：`screenWidthDp >= 600` 时把底部栏换成手写的 `NavigationRail`（用 `NavigationSuiteScaffold` 需要 m3 1.4 / Compose 1.9+）。
- 应用主题是 `AppThemeMode`（LIGHT/DARK/FOLLOW_SYSTEM）+ `ColorSource`（默认 BRAND，另有 DYNAMIC、CUSTOM），在 `FlowReaderNavHost` 里通过 `:core` 的 `FlowTheme` **只应用一次**；阅读器正文有自己的色板，从不跟随应用主题。不要加逐页面的主题包裹。
- **三种配色来源读的是 `AppSettings` 的三个不同字段**（v56.6）：BRAND → `colorPreset`（12 个 `AppColorPreset` 之一），CUSTOM → `customSeedArgb`，DYNAMIC → 都不读。`FlowTheme` 在「DYNAMIC 但系统低于 Android 12」和「CUSTOM 但没有种子色」两种情况下都回退到 `colorPreset`。
- **`AppColorPreset.VIOLET` 是刻意特判的**：`FlowColorPresets.schemeOf(VIOLET)` 原样返回手工调过的 `FlowLightColorScheme`/`FlowDarkColorScheme`，不走生成器。它是默认值，让它走生成器会把每一个从未动过该设置的安装重新染色——顺带把 Roborazzi 基准图也挪了。`FlowColorPresetsTest` 逐个断言各色值角色；不要「顺手简化」掉这个分支。
- **`SeedColorScheme`（`:core/util`，不含 Compose）承诺任意种子色下每一对文字配色都满足 WCAG AA。** 它是 M3 HCT 色调板的 HSL 近似，不是移植——`material-color-utilities` 不是本项目依赖，而 `dynamicLightColorScheme` 只读壁纸。单靠色调阶梯守不住 AA（一个中等亮度的种子色会把 `onPrimary` 放到与 `primary` 相同的亮度上），所以每个 `onX` 角色都会朝远离其背景的方向扫，兜底落到纯黑或纯白。新增角色时，要对它**可能出现的每一种背景**都做校验：`onSurfaceVariant` 同时对 `surfaceVariant` 和普通 `surface` 校验，因为应用里所有副标题都是把它画在 `surface` 上的。
- 生成色板里的 error 角色是固定值，不由种子色推导：红色表示错误是一种约定。也没有施加饱和度下限，所以 `GRAPHITE` 保持中性灰。
- `updateColorPreset()` / `updateCustomSeedColor()` 必须在**同一个 `edit{}`** 里连 `COLOR_SOURCE` 一起写。分成两次写会发出一个中间态 `AppSettings`（新来源 + 旧颜色），而主题正订阅着这条流，于是应用会明显闪一下旧配色。
- 调色台（`ColorStudioDialog`，在 `:app`）把色相环 + SV 方块、光谱条、明度／饱和度条和十六进制输入框绑到同一份 HSV 状态上。所有几何计算在 `:core` 的 `ColorWheelMath`（不含 Compose、有测试）——逆映射差一个像素的表现是「圆点跟不上手指」，这种问题只有在 JVM 测试里抓才便宜。一次拖拽由哪个控件接管，**在按下的那一刻就决定**并保持整段拖拽；改成每次移动都判断，会让 SV 拖拽一越过方块边缘就被色相环抢走。十六进制输入框是无障碍通路（Canvas 没法被 TalkBack 操作）；那几根条带带 `setProgress` 语义，同理。
- 阅读器颜色是 18 个 `ReaderPaletteId`（`domain/model/ReadingSettings.kt`），实际颜色值在 `:core` 的 `ReaderPalette.kt`。18 个色板的对比度都在 `core/.../ReaderPaletteContrastTest` 里对着 WCAG AA 断言。
- 新 UI 里配色相关枚举的文案走 `stringResource`，**不用**枚举自带的 `displayName`。那些字段是 v53 语言切换之前留下的中文字面量，阅读器设置面板还在用；在新 UI 里复用会让其余八种语言下显示中文。
- 自定义文字／背景色（`ReadingSettings.customTextColorArgb`/`customBackgroundColorArgb`，DataStore 存储）：`:core` 的 `ReaderCustomTheme.resolve()` 会用 WCAG AA 守住这一对颜色，并回退到能恢复对比度的那一侧。不涉及 Room——阅读设置从来没进过 Room。
- 应用内语言切换（`AppLanguage`）：`FlowLocaleProvider` 用一个 **`ContextWrapper`** 包住 `LocalContext`/`LocalConfiguration`/`LocalLayoutDirection`——裸的 `createConfigurationContext()` 返回值不是 Activity，会让 `hiltViewModel()` 里的 `findActivity()` 抛异常。语言：zh（默认）、en、ja、ko、de、es、fr、pt、ru。
- **`FlowTheme` 用 `remember` 包住 `ColorScheme` 属于加固，不是修 bug——不要把它重新贴成 bug 修复。** Compose 编译器报告（`./gradlew :core:compileDebugKotlin -PcomposeReports=true`，v56.6.1 起在 `:core` 里也接好了）显示 `restartable skippable fun FlowTheme(...)` 且五个参数全部 `stable`，也就是说输入不变时 Compose 直接跳过函数体，色板从来没有因为导航而重建。保留 `remember` 只是因为一旦失效，后果严重且无声：`MaterialTheme` 的 `staticCompositionLocalOf` 在拿到新值时会重建整棵子树，而 `ColorScheme` 没有 `equals`（material3 1.3.x 只声明了 `toString`，所以比较按引用），生成虽便宜但不免费（约 4µs 生成 / 14µs 完整色板）。`FlowThemeStabilityTest` 钉住色板跨重组的**引用同一性**；把它当成某个已发生缺陷的回归门禁之前，先读它的 KDoc——它不是。
- `SettingsScreen` 显示 `BuildConfig.VERSION_NAME`。

## Room 与数据

- `AppDatabase` 是**第 7 版**，**8 个实体**（Book、Chapter、Bookmark、Annotation、Category、ReadingStats、ReadingList、ReadingListItem）与 **7 个 DAO**，`exportSchema = true` → `data/schemas/com.flowreader.app.data.local.AppDatabase/{4,6,7}.json`。
- `di/AppModule.kt` 里的显式迁移：`MIGRATION_4_5`（`books.tags`）、`MIGRATION_5_6`（书签 `(bookId, chapterIndex, position)` 索引）、`MIGRATION_6_7`（reading_lists + reading_list_items、`(listId, bookId)` 唯一索引、外键级联）。**绝不**使用 `fallbackToDestructiveMigration()`。
- `BackupRepositoryImpl.importData()` 把整个导入包在 `database.withTransaction` 里——备份导入必须保持原子。备份导入／导出上限 200MB。
- `ChapterRepositoryImpl` 把章节元数据／正文都路由过 `CacheManager`；不要再加第二个章节正文缓存。`CacheManager` 每 50 次访问采样一次命中率，据此把每本书的章节容量调整在 2–12 之间，中度内存回收时淘汰最少使用的书；v54.5 才真正把它接到命中／未命中统计上。
- **FTS 索引锁在 `FullTextSearch.withIndexLock()` 里，不在调用方**（v56.4.3）。v56.4 把 `Mutex` 放在 `SearchRepositoryImpl` 内部，但 `ReaderViewModel.indexBookForSearch()` 直接调 `deleteBookContent` + `indexChapter`，绕过了它——单本书的重新索引可能与全局重建交错，删掉重建已经计为「已索引」的书。现在锁挂在共享对象上，任何路径都躲不掉。`kotlinx` 的 `Mutex` **不可重入**：每个入口恰好取一次锁，这正是 `rebuildIndex()`（取锁）与 `rebuildIndexLocked()`（假定已持锁）分成两个函数的原因。
- `indexChapter()` 用 `INSERT OR REPLACE INTO book_content`，而 SQLite 在 REPLACE 冲突处理中**不会**触发 `DELETE` 触发器，除非打开 `PRAGMA recursive_triggers`（默认关闭）。两个调用点都先删（`deleteBookContent` / `deleteAllContent`），所以今天走不到冲突分支——但如果将来对已存在的 `(book_id, chapter_index)` 不删就重新索引，旧 rowid 的分词会留在 FTS 索引里而其内容行已消失，产生列全为 NULL 的幽灵结果。保持「先删再索引」的形状。
- **`book_content_fts` 的列没有类型亲和性，所以 `book_id` 必须通过 `CAST(? AS INTEGER)` 比较**（v56.4.3，正是让书内搜索恒返回空的那个 bug）。它是 FTS5 的*外部内容*表（`content='book_content'`），FTS5 不声明列类型，而 `SQLiteDatabase.rawQuery()` 只能绑定 `String`。两边都没有亲和性时，SQLite 永远不会认为 INTEGER `5` 等于 TEXT `'5'`，于是裸的 `book_id = ?` 静默地恒为假。SQL 放在 `SEARCH_IN_BOOK_SQL` / `SEARCH_ALL_SQL` 常量里，好让 `FullTextSearchQueryTest` 断言其形状。查底层 `book_content` 表（如 `deleteBookContent()`）没问题——那张表的列声明了 `INTEGER`。Robolectric 的 SQLite **没有 fts5 模块**，所以没有测试能真的执行这些查询；改为测 SQL 形状加上底层比较语义。
- **`reading_stats` 是「每本书每天一行」**（`(bookId, date)` 唯一索引），所以行数 `LIMIT` 永远不等于天数 `LIMIT`（v56.4.3）。`getRecentDailyStats(7)` 曾用 `getRecentStats(7)` = `ORDER BY date DESC LIMIT 7`，结果一个同时读三本书的用户在统计图上只看到 2–3 天。任何想要「最近 N 天」的地方都必须按日期过滤——用 `getStatsSince(startDate)` 传 `今天 - (N - 1)`，或者用 `getReadingReport(days)` 已经在用的 `getAllStats()` + 起始日期过滤。`date` 以 `yyyy-MM-dd` 存储，其字典序与时间序一致，所以 `date >= :startDate` 是正确的范围扫描。
- 需要时用内置的 `kotlin.Result`；旧的自定义 `AppException`/`Result` 包装已删除。

## 阅读器注意事项

- `ReaderViewModel` 从 `SavedStateHandle` 读 `bookId` 和可选的 `chapterIndex`；碰数据库之前先校验 `bookId > 0`。进度计算在 `ReaderProgressEngine`（feature:reader），会话计时与 EMA 速度在 `ReaderSessionTracker`（时钟可注入），TTS 的「从某位置朗读」在 `ReaderTtsCoordinator`（`app/ui/screens/reader/`）。
- `goToChapter()` 必须先加载章节正文，再设置 `currentChapter`；设置一个只有元数据的章节会让阅读器一片空白。
- 进度保存在 `debouncedSaveProgress()` 里防抖 3 秒。统计每 30 秒保存一次，切章时保存，`onCleared()` 里也保存——自 v56.3 起 `onCleared()` 的保存跑在一个独立的 IO scope 上（不是 `viewModelScope`，那个已经被取消了）。页数来自真实的章节字符增量；未读完一页的字符会跨滚动更新累计；暂停超过 5 分钟就切成新会话。
- **`currentPosition` 是两种不同的单位，阅读统计路径必须按单位分支**（v56.4.3）。`SLIDE`/`NONE` 传给 `updatePosition()` 的是字符偏移；`PAGED` 和漫画传的是**渲染后的页序号**。用 `ReaderPositionUnit.of(pageMode, format)` 去问，**绝不**在调用处重新推导这条规则。字符偏移进 `ReaderSessionTracker.recordProgress()`，页序号进 `recordPageProgress()`。搞错的后果不只是页数少算：页序号每翻一页只加 1，字符路径看到的是「每页约 1 个字符」，于是 `readPages` 恒为 0，`saveReadingStats()` 的 `if (seconds > 0 && pages > 0)` 判断会把**整个会话连累计时长一起丢掉**。同一条区分也适用于移动阈值（200 字符 vs 1 页）和 TTS——页序号不能当作字符起始偏移，所以分页模式下从章首朗读。
- `ReadingStatsRepository.getRecentDailyStats()` 必须先按日期聚合再画图；`getReadingReport(days)` 支撑周报／月报与目标。
- 护眼提醒间隔存在 `ReadingSettings.eyeProtectionIntervalMinutes`（`ReaderSettingsSheet` 里的 15/20/30/45/60 选项）。
- 每一项阅读器偏好都通过唯一的 `ReaderViewModel.updateReadingSettings(ReadingSettings)` 写入；逐字段的 setter 在 v52 已删除。
- 阅读器文本样式必须来自 `:core`（`readerBodyStyle`、`paragraphSpacing`、`ReaderMetrics`）。硬编码 `bodyLarge` 正是 v52 之前字体／自定义字体／段间距三项设置形同摆设的原因。
- `PageMode` 就是 `SLIDE` / `PAGED` / `NONE` 三个值，三个都真的渲染：`SLIDE` 做章节滚动动画，`PAGED` 在 `HorizontalPager` 里渲染实测分页（`PagedReader`；`ChapterPaginator` 按原始偏移切分超长段落，好让高亮／书签范围在重新分页后依然有效），`NONE` 直接跳。**绝不要在实现之前先加模式**——v52 那两个假的 `SIMULATION`/`CURL` 已被删除，而 `docs/page_turn_evaluation.md`（v56）的结论是仿真翻页维持不实现。漫画走 `ComicReader`（SLIDE = 逐页滑动，NONE = 纵向 `LazyColumn` 虚拟化列表）。
- 点击区域、滑动、双击与长按由 `:core` 的 `ReaderBehavior` 根据 `GestureSettings` 解析；自动夜间模式是一分钟一跳的 ticker，不是在组合时读一次 `Calendar`。
- 滚动位置按章记在 `ReaderViewModel.chapterPositions` 里；保持 `ReaderScreen` 的滚动恢复与 `uiState.currentPosition` 一致。
- `FullTextSearch` 注入到 `ReaderViewModel`；书籍加载后索引章节（在 `withIndexLock` 内），`SearchDialog` 负责跳到命中的章节。
- `TtsManager`（`util/TtsManager.kt`）包装 Android 的 `TextToSpeech`，暴露 `StateFlow<TtsState>`；`ReaderViewModel` 订阅它来决定按钮状态，并在 `onCleared()` 里调 `shutdown()`。朗读／暂停走 `ReaderTtsCoordinator`，字符模式下从 `currentPosition` 起，分页模式下从章首起。**懒初始化**——进入阅读器不得启动 TTS 引擎。
- 进度小组件用 DataStore 的 `widget_book_title`（string）/ `widget_progress_percent`（int），键定义在 `data/repository/SettingsRepository.kt`，由 `ReaderViewModel.updateWidgetSnapshot()` 更新；`ReadingProgressWidgetProvider` 通过 `goAsync()` 刷新（**绝不** `runBlocking`）。
- `ReadingSettings.autoNightMode` 在 `ReaderScreen` 里按时间生效（19:00–07:00 暗色）；它不改变全局应用主题。
- 字体选择用 `ReaderFontFamily`（4 个可解析字族），在 `domain/model/ReadingSettings.kt`；导入的 `.ttf/.otf` 优先于内置字族，读不出来时静默回退。
- 原生划词选择（v54）：长按打开 `ReaderContent` 里自研的引擎（`ReaderParagraph` + `ReaderSelectionBar`），支持拖拽扩展与可拖动手柄；操作栏对准确范围做高亮／复制／加书签。`ReaderTextMapping`（`components/ReaderTextMapping.kt`）把显示偏移映射回原始章节偏移（纯函数、有测试）。
- **`rawRange()` 的末端是闭区间，`Annotation.endPosition` 是开区间。** 用 `ParagraphContent.selectionSpan()` 转换，**绝不**把这段算术内联展开（v56.4.3）：`buildParagraphContent` 渲染的是 `substring(relStart, relEnd)`，而 `ReaderContent`/`PagedReader` 里的段落过滤比较的是开区间段末，所以把闭区间值原样传下去会静默丢掉选中的最后一个字符——选一个字符时等于什么都没高亮。
- **`buildParagraphContent` 不得重复输出前一个标注已经追加过的文本。** 高亮可以重叠或嵌套，把每个标注的完整范围都追加一遍，会在渲染出的段落里重复共享字符，*并且*让 `rawOffsets` 与显示文本失去同步，于是之后每一次选择都映射到错误的章节偏移。嵌套标注还会把 `lastEnd` 拽回去、重复输出间隙文本。做法是把每段的起点钳到「已输出到哪里」，并跳过被完全覆盖的段。
- **`Bookmark.position` 是阅读器自己的位置，绝不是字符偏移。** `goToBookmark()` 把它经 `goToChapter()` 送进 `currentPosition`，而后者在 SLIDE/NONE 下是滚动像素、在 PAGED 下是页序号——划词得到的字符偏移两者都不是，没有布局信息也换算不回去。所以 `addBookmark(text)` 干脆不接受位置参数（v56.4.3）；在那之前，用选中文本建的书签会滚到一个毫无意义的像素处，或者被钳到该章最后一页。`Annotation` 的位置**确实**是字符偏移——两者不可互换。**不要**用平台 `SelectionContainer` 那个可提升选择状态的重载——它在 Compose 1.9.x 之前一直是 `internal`；本引擎刻意只用公开的 `TextLayoutResult` API。
- 书签仓库会规范化文本、校验 ID 为正、通过 `addBookmark()` 存储，并按章节／位置排序以保证导航稳定。

## Compose/UI 注意事项

- 绝不要把 `LazyColumn` 放进另一个 `LazyColumn` 的 `item` 里；嵌套列表用普通 `Column`（高度无界会崩）。
- `WheelViewModel.spin()` 是普通函数，内部自己启动协程；不要把它当挂起函数从 `LaunchedEffect` 里调。转盘动画用 `System.nanoTime()` 加 `delay(16L)`——保持基于实际流逝时间的动画，不要改成固定步长循环。`WheelScreen` 用 `derivedStateOf` 把 `error`/`result` 隔离开，好让 60 FPS 的 `rotationAngle` 更新不会重组无关 UI。
- 书架筛选（分类标签 + 查询 + 排序）在 `LibraryViewModel` 里；保持 `selectedCategoryId` 反映在 `LibraryUiState` 中。批量操作每个动作发一条 SQL，通过密封类 `LibraryMessage` 回报结果。
- 全局搜索用 `SearchRepository.rebuildIndex()` + `FullTextSearch.searchAll()`，入口是独立的 `SearchScreen`；结果必须带上来源书名。书架本身不再内嵌全局搜索。
- `BookParser` 的读取上限：EPUB 单章 16MB、内嵌图片 24MB、TXT/MD/FB2/MOBI 整文件 128MB；超限条目会被跳过，或者以明确信息失败。
- `ZipImporter` / `ZipImportRules`（都定义在 `ZipImporter.kt`）守着 ZIP/CBZ 导入：拒绝绝对路径与 `..`（zip-slip），跳过 `__MACOSX` 与隐藏条目，限制条目数与单条大小，只放行解析器支持的扩展名。漫画 ZIP 作为一本漫画导入（按文件名自然序，第一张图作封面）；普通书籍 ZIP 走批量导入。
- 启动：`androidx.profileinstaller` 已接好，`app/src/main/baseline-prof.txt` 携带手写的冷启动 profile（类级规则对重命名鲁棒，方法规则钉住热路径）；启动路径上的类改名时，记得同步 profile 规则。
- **窗口 inset 的契约只在一个地方：`ui/Navigation.kt` 的 `FlowShellScaffold`**（v56.4.2，issue #6）。应用在外壳 `Scaffold` 内部又嵌了逐页面的 `Scaffold` + `TopAppBar`，两层读的是同一份 `WindowInsets`。`Modifier.padding(paddingValues)` 只*应用*inset 而不*消费*它，所以最初的外壳等于放任每个页面把状态栏 inset 又加了一遍——标题上方多出一条状态栏高度的空白（24 + 24 + 64 = 112dp，本该 88dp），底部同样翻倍。因此外壳既设 `contentWindowInsets = WindowInsets(0, 0, 0, 0)` **又**加 `.consumeWindowInsets(paddingValues)`：顶部归零，让每个页面的 `TopAppBar` 只应用一次状态栏并画到它下面去（这本来就是 `enableEdgeToEdge()` + `themes.xml` 里透明状态栏想要的效果）；底部则要消费，因为外壳的底部内边距是实测的 `NavigationBar` 高度，那里面已经含了导航栏 inset。不要在此之上再给单个页面加 inset 内边距，也不要把这里的 `contentWindowInsets` 恢复成默认值。由 `ShellWindowInsetsTest` 守着。
- **Robolectric 默认看不见 inset**（`WindowInsets.statusBars.top` 是 0），这正是 issue #6 能同时躲过单测套件和 Roborazzi 门禁的原因。要测 inset 行为，必须把 `WindowInsetsCompat` 派发到 **ComposeView** 上（`activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)`）；派发到 `window.decorView` 传不进 Compose。`ShellWindowInsetsTest.injectSystemBars()` 是可用的写法。
- `screenWidthDp >= 600` 那条分支用的是放在普通 `Row` 里的 `NavigationRail`，没有 `Scaffold`，所以那里的 inset 已经由页面自己应用过一次了；它刻意不走 `FlowShellScaffold`。
- **一个接受 `modifier` 的包装组件，必须在它可能渲染的*每一个*分支上都把 `modifier` 应用上。** `FlowStateHost` 在 error / loading / empty 三个分支应用了 `modifier`，但成功分支是裸的 `content()`（v56.4.4）。`LibraryScreen`、`StatsScreen` 与 `BookDetailScreen` 正是*通过*这个 `modifier` 把 `Scaffold` 的 inset 内边距传进去的，于是加载完成后的内容丢掉了整整 88dp 安全区、从 y=0 画到了 `TopAppBar` 底下，而同一个页面的加载态和空态位置却是对的——典型症状就是「页面没数据时一切正常，一有数据就顶穿」。走 `FlowScaffold` 的页面免疫，因为它在自己的 `Box` 内部加内边距；`SettingsScreen` 免疫，因为它直接给 `Column` 加内边距。状态包装组件渲染调用方内容时，要包起来（`Box(modifier = modifier) { content() }`），而不是裸调。
- **给用户看的数字绝不要用 `String.format("%.1f", x)`；在字符串资源里格式化，把原始数值传进去。** 不带 `Locale` 的重载使用 **JVM 默认 locale**，而字符串资源按**应用内** `AppLanguage` 解析——于是在小数点为逗号的语言（de/es/fr/pt/ru）下两者不一致，把得到的 `"4,7"` 喂给数值占位符会抛异常。做法是把 `%1$.1f` 写进资源，把 `Double` 交给 `stringResource`：`Resources.getString(id, args)` 会用资源配置的 locale 去格式化。v56.6.1 修掉了唯一一处（调色台的对比度读数）；同一句话里硬编码的阈值（`4.5:1`）在那五种语言下也需要写成逗号。新增带格式数字时，两半都要检查。
- **不要写入一个你在同一次组合里也读取的状态**——那是反向写入，Compose 必须多跑一遍才能收敛，而且是在拖拽的*每一帧*上。`ColorStudioDialog` 曾把十六进制输入框的文本放在 `mutableStateOf` 里，并在组合期间从每个选色回调里重新赋值（v56.6.1）。通用解法：状态里只留**用户的**编辑草稿（`null` = 未在编辑），显示值靠**派生**——`val hexDisplay = hexDraft ?: ColorSpaces.toHexString(argb)`——回调把草稿重置为 `null`，而不是往里写新值。
- 一行的副标题要以「决定该值是否生效」的那个字段为条件，而不是只看值存不存在：自调色那一行读 `customSeedArgb`，在 `ColorSource` 还是 `BRAND` 时就宣称它「正在使用」（v56.6.1）。写成 `customSeedArgb?.takeIf { colorSource == ColorSource.CUSTOM }`。
- 外壳层的 inset 测试只能证明外壳。`ShellWindowInsetsTest.tabScreenAppliesEachInsetExactlyOnce` 在整个 v56.4.4 期间都是绿的，因为它的 `FakeScreen` 给自己的 `Box` 加内边距，从不经过 `FlowStateHost`——而后者才是真正丢掉内边距的组件。inset 回归测试必须复现**真实的**组合链条：`Scaffold` → `FlowStateHost` → 列表，而不是一个理想化的替身。

## 测试策略

- CI 里只有 JVM 单元测试（门禁中没有 instrumented 测试）。`:app` 的测试用 Robolectric（`unitTests.isIncludeAndroidResources = true`）；`:core` 设 `unitTests.isReturnDefaultValues = true`。
- 测试位置：`app/src/test`（21 个文件：解析器、仓库、provider、`ReaderTextMapping`、`LanTransferServer`、`CacheManager`、`FullTextSearchQueryTest`、`ShellWindowInsetsTest`、`FlowThemeStabilityTest`、Roborazzi 截图），`core/src/test`（15 个：色板对比度、`ReaderBehavior`、`ReaderCustomTheme`、`ColorSpaces`、`SeedColorScheme`、`ColorWheelMath`、`FlowColorPresets`、导出器／格式化器、metrics），`domain/src/test`（18 个：模型行为——`ReadingSettings` 规范化、`ReadingListOrder`、`ReaderPaletteId` 迁移映射、`AppColorPreset`、`AppLanguage` 等），`feature/reader/src/test`（4 个：`ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`、`ReaderPositionUnit`）。
- **没有任何 ViewModel 有单元测试**——这是既定约定，不是疏漏，而且 `ui/screens/**` 下的 ViewModel 本来就在覆盖率分母里。当 bug 出在 ViewModel 逻辑里时，把规则抽成 `:core`/`:feature:*` 里的纯类再测它（`ReaderPositionUnit`、`ReaderBehavior`、`ReaderProgressEngine` 都是这么来的）；不要作为修 bug 的副产物，给项目引入第一个 ViewModel 测试脚手架。
- `coverageSummary` 是**文件数比值，不是行覆盖率**：`(app/core/feature/domain 各 test 源集下的测试文件数) / (app 仓库实现 + `ui/screens/**` 下的 ViewModel + :core 全部 main 源文件 + domain 仓库接口 + domain 模型)` 必须 ≥ 40%。注意 `AppShellViewModel`（在 `ui/` 而非 `ui/screens/`）不在分母里。新增一个 domain 模型、ViewModel 或 `:core` 文件而不配测试，即使别的什么都没动也能让构建失败。当前 85.3%（58/68）。另注意 `:feature:*` 的 main 源文件**不在**分母里，只有它们的测试计入分子。
- 截图基准：用 `recordRoborazziDebug` 录制，用 `verifyRoborazziDebug` 把关；值得钉住的新 UI 应该在 `app/src/test/snapshots/` 下加一张基准图。已提交的两张只截了 `BookCover` 和 `BookShelfSkeleton`——没有 `Scaffold`、没有应用栏——所以截图门禁对布局／inset 回归**什么都证明不了**。那类问题要像 `ShellWindowInsetsTest` 那样用实测布局断言（`assertTopPositionInRootIsEqualTo` / `getBoundsInRoot`）。

## 安全考量

本项目离线优先、重视隐私——请保持这一点。v56.3/v56.4 是两轮专门的安全审计，v56.4.1 交付了后续加固（`SECURITY_AUDIT_REPORT.md` / `SECURITY_FIX_SUMMARY.md`）；v56.6.2 关掉了 `SECURITY_REVIEW_2026-08-20.md` 里第三方静态审查的全部 7 条。以下是硬性约束：

- **只有一项网络权限** —— `INTERNET` 是唯一的网络权限（`AndroidManifest.xml`），仅用于局域网 OPDS 客户端与局域网备份传输。无账号、无统计、无崩溃上报、无同步。允许明文流量只是因为家庭局域网里的 OPDS 服务端（Calibre、COPS、Komga）很少启用 HTTPS；真正的边界在代码里，不在配置里。（旧的存储权限仍有声明，但按版本收窄，只为在老 Android 上访问文件：`READ_EXTERNAL_STORAGE` maxSdkVersion 32，`WRITE_EXTERNAL_STORAGE` maxSdkVersion 29。）
- **OPDS 仅限局域网**：`OpdsAddress` 把可达范围限制在回环 / RFC1918 / RFC4193 / `.local` 类名称，并对**每一跳重定向**重新校验；公网主机不可达。目录读取上限 2MB，下载 200MB，获取链接按 MIME 过滤。
- **导入处处有上限**：`BookParser` 的读取上限（见上），`ZipImportRules` 的 zip-slip 与条目上限，SAF 与局域网两条路径的备份导入均 200MB；`ACTION_VIEW`（「用 FlowReader 打开」）导入走同一条带上限的管线，并且只消费一次（清掉 `intent.data`/`EXTRA_STREAM`），使配置变化不会重复导入。
- **`ContentResolver` 给的 `DISPLAY_NAME` 是攻击者可控的输入，不是文件名。** 每个导入名都要过 `ImportFileName.sanitize()`（只取最后一段路径，`[字母 数字 . _ - 空格]` 之外一律替换，去掉开头的点，截到 120 字符），每个文件都要用 `ImportFileName.resolveWithin(dir, name)` 构造——它通过 `canonicalFile` 复核包含关系，无法证明时返回 null。v56.6.2 之前，原始名字会直达 `File(booksDir, name)`，于是一个恶意应用的 `ACTION_VIEW` 加上 `../../databases/flowreader_db` 就能覆盖数据库。
- **`ImportFileName.sanitize()` 刻意保留 Unicode——不要把它「收紧」成纯 ASCII。** 纯 ASCII 的那个是 `BookParser.sanitizeFileName()`，它是给漫画*目录名*用的；拿去处理书名会把 `三体.epub` 变成 `_.epub`。两个函数，两种职责。
- **导入名只解析一次，由调用方解析。** `copyFileToInternal` 把它作为参数接收，而不是自己再查一次 resolver——因为对同一个 Uri 查两次可能得到两个不同的名字：一个通过了你的校验，另一个才是你真正写下去的（TOCTOU）。
- **每一次读取攻击者提供的字节都要过上限。** `BookParser.copyCapped`（超限返回 **-1**）与 `readCappedBytes`/`readCappedText`（超限返回 **null**）是唯一被认可的读取方式；在 EPUB 条目或用户选中的 Uri 上裸调 `copyTo()`/`readText()`/`readBytes()` 就是 bug。上限：整包 256MB、整文档 128MB、单张图片 24MB、EPUB 单章 16MB、EPUB 结构性元数据 4MB（`container.xml`/OPF——v56.6.2 之前是无上限的 `readText()`）。
- **`copyCapped` 按契约不删除残留文件。** 每个调用方自己删，在 `finally` 里或者在拿到 -1 时显式删。在它内部加删除会让本来已经清理的路径重复删除。三个上限辅助函数放在 `BookParser` companion object 里并标为 `internal`，只为一件事：让 `BookParserCapsTest` 能传一个 1000 字节的上限，而不是造一个 256MB 的夹具。
- **局域网边界适用于应用*拉取*的一切，不只是 OPDS。** `LanTransferClient.download()` 在 v56.6.2 之前只校验 `http://` 协议，于是粘贴一个公网 URL 就能从互联网拉 200MB 下来交给备份导入器——而备份导入会整体覆盖书库。现在它跑同一套 `OpdsAddress.isPrivateHost()` 策略，并要求 `/backup/` + 16 位十六进制。任何新增的对外拉取都要给同样的校验：全应用只有一项 `INTERNET` 权限，它是为局域网存在的，而只有调用点能落实这一点。
- **不做 DRM 破解**：带 DRM 的 MOBI/AZW 直接拒绝；HUFF/CDIC 压缩的文件也是拒绝，而不是解一半。
- **Android 自动备份什么都不传，这是有意的**（注释在 v56.6.1 已修正；此前它们写的正好相反，而这份文档里更早的一条还声称 OPDS 密码在被备份——OPDS 根本没有认证，所以整个应用里不存在任何凭证）。`<include>` 列表是*限定*备份范围到它列出的域，而 `backup_rules.xml` / `data_extraction_rules.xml` 只列了 `sharedpref`，而那个域是**空的**：本应用没有任何 `getSharedPreferences`/`PreferenceManager` 调用，而 DataStore 的 `dataStoreFile()` 落在 `filesDir/datastore/settings.preferences_pb`，属于 `file` 域。所以 Room 库、FTS 库、封面、阅读背景*以及*偏好设置全部留在设备上。**不要**用 `<include domain="file" .../>` 去「修」它：那会把用户的阅读习惯通过 Google 的备份通道送出设备，与离线优先的设计相悖。跨设备迁移靠应用内的 SAF／局域网备份，由用户显式触发，并在单个原子事务里导入。
- **ContentProvider**（`com.flowreader.app.provider`）只暴露只读的书籍元数据与进度——不含文件路径、不含正文；写操作一律拒绝。它绝不能阻塞 Binder 线程：自 v56.4.1 起改用同步 DAO 方法 `getAllBooksSync()`/`getBookByIdSync()`，不再 `runBlocking`。自 v56.6.2 起 `android:readPermission` 是自定义的 `dangerous` 权限 `com.flowreader.app.permission.READ_LIBRARY`（`writePermission` 是一个 `signature` 级的），所以调用方需要声明权限**并且**拿到用户运行时授权，而不是一装上就能静默读取。保持该 authority 导出是有意的；取 `dangerous` 而非 `signature`，正是为了让第三方工具还能用得上它。这道检查由 Android 在本类之外执行，所以没有任何单元测试覆盖它——**manifest 属性本身就是执行点**；那 4 条标签／说明文案必须在 9 种语言里都存在，因为它们会出现在系统授权对话框上。
- **FileProvider** 的路径是白名单（`res/xml/file_paths.xml`），自 v56.4.1 从三处全树授权收窄到恰好一个目录（`share_cards/`——全仓唯一一处 `getUriForFile()` 的消费方）。新增路径时取能用的最窄子目录；不要放宽回 `.`。
- **「某个密钥是否已被提交」要靠 `git ls-files -v` 与全历史扫描来回答，绝不能靠读工作区。** 根目录的 `providers.yaml` 看起来存着一个明文 key，而 `git status` 看起来干净——两件事同时为真，因为该文件被标了 `S`（`skip-worktree`），git 完全忽略工作区那一份。这个标记存在 `.git/index` 里：每个 clone 各自持有、克隆者不会继承，而且一旦拉取到的提交动了这个文件，`git pull` 会直接失败。被提交的历史里只出现过 `{env:VAR_NAME}`。两轮核查（包括那份第三方报告，它给出的路径自 v56.6.1 起已被整体 git-ignore）都判断错了，直到全历史扫描才找到唯一一处真实出现——在一份*计划文档*里，那段话正在论证这个 key 无害，同时把它抄了出来。值得留下的推论：**论证「某个值不是密钥」，并不需要引用那个值**——引用它才使之成为泄露。
- **release 签名要在产物上验证，不是在构建文件上。** `apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` 必须打印本项目的证书（`CN=HuZaiGong, OU=Dev, O=flowreader`），而不是 `CN=Android Debug`。先比一下 APK 与 `keystore.properties` 的 mtime：一个在密钥库出现之前构建的旧 APK，验证结果就是 debug 签名，而配置完全正确——现象跟「修复没生效」一模一样。
- **局域网传输**（`LanTransferServer`/`LanTransferClient`）：随机令牌保护，绑定发现到的局域网网卡（自 v56.4 起绝不绑 `0.0.0.0`），对话框关闭时 HTTP 服务停止。v56.4.1 修了一个潜伏的令牌生成偏差（`random.nextInt(charset.length)`；两个回归测试覆盖）。服务端只用**一个**线程，所以每个连接都必须有读超时：v56.6.2 设了 `soTimeout = 5s`，没有它的话，一个连上来却不说话的对端会把唯一的工作线程占住，直到对话框关闭。令牌匹配走 `isBackupRequest()`——对整个请求行精确匹配，并且是 `internal`，好让它脱离 socket 被测试。它替换掉的 `requestLine.contains("/backup/$token")` 还会应答 `GET /anything/backup/<token>` 和 `GET /backup/<token>extra`，这正是后来的路径处理改动会悄悄建立在其上的那种缺口。
- **转义**：FTS 查询转义（`FullTextSearch.escapeFtsQuery()`），HTML/Markdown 标注导出转义，`OpdsClient` 过滤 MIME 类型。
- 全项目没有 WebView。v56.3 的审计门禁要求**不新增 `!!`**（唯一剩下的是 ContentProvider 在 `onCreate` 里那个标准的 `context!!`）。

## 项目文档与开发环境产物

- `README.md` / `README_EN.md` —— 中英文产品概览，架构与门禁信息保留但移到功能介绍之后。两份都在当时对着代码核对过（色板数、实体数、覆盖率、备份那条）；结构方面仍以 `AGENTS.md` / `ARCHITECTURE.md` 为准，且两份 README 必须互相同步。
- `ARCHITECTURE.md` —— 当前 v56.6.2 的模块地图、依赖方向、持久化边界、导航外壳与质量门禁。
- `CHANGELOG.md` —— 完整的语义化版本更新日志，中文；最新条目 v56.6.2。
- `ROADMAP.md` —— 产品计划与「否决清单」；在添加触及原则（离线优先、性能、克制）的功能之前先查这里。
- `SECURITY_AUDIT_REPORT.md` —— v56.3/v56.4 安全审计，中文。结论：0 高危、2 中危（ContentProvider 阻塞 Binder、FileProvider 授权过宽）、3 低危（含那个潜伏的令牌 bug）、2 信息级；未发现可远程或可由第三方利用的活跃漏洞。
- `SECURITY_FIX_SUMMARY.md` —— v56.4.1 所修问题的中文小结（CVE-LOCAL-001 令牌生成器、移除 ContentProvider 的 `runBlocking`、FileProvider 收窄）。
- `SECURITY_REVIEW_2026-08-20.md` —— 第三方静态审查，7 条，v56.6.2 全部处置完毕（含两条「报告定位有误」的更正记录）。
- `docs/page_turn_evaluation.md` —— v56 的评估，结论是仿真翻页不实现，也不得给它加 UI 入口。
- `CLAUDE.md` / `CLAUDE_CN.md` —— 当前的 agent 指南（保持同步，CN 版是译本）。本文件是 `AGENTS.md` 的译本，v56.6.2 重写。`UI_REFACTOR_PLAN.md` 是 v52 时期的迁移计划。
- `.opencode/` + `providers.yaml`（及 `providers.yaml.example`）、`index.html`、`.claude/settings.local.json` 属于开发／AI 工具的环境产物，不是应用代码。
