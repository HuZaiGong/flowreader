# FlowReader 架构说明

本文档描述当前 `dev` 分支（v56.6.2）的实际代码结构，不是未来迁移计划。新增代码应遵守这里的依赖方向；行为层面的细节约束见 [AGENTS.md](AGENTS.md)，用户功能说明见 [README.md](README.md)。

## 总览

FlowReader 是一个离线优先的 Android 应用，采用 Jetpack Compose + Material 3、Clean Architecture 与 MVVM。应用的组合根在 `:app`，领域契约在 `:domain`，Room 持久化在 `:data`，共享设计系统与纯逻辑在 `:core`，部分阅读器逻辑已抽到 `:feature:reader`。

```text
Composable
    ↓
ViewModel (StateFlow)
    ↓
domain repository interface
    ↓
app repository implementation
    ↓
Room DAO / DataStore / FullTextSearch
```

允许的 Gradle 依赖方向：

```text
:feature:*  →  :core, :domain
:data      →  :core, :domain
:app       →  :core, :data, :domain, :feature:*
```

禁止反向依赖：feature 不得依赖 `:app`，`:data` 不得依赖 feature，`:domain` 不得依赖 Android UI、Compose、Room 或 Hilt。

## 模块地图

| 模块 | 当前职责 | 当前状态 |
| --- | --- | --- |
| `:app` | Android 入口、Hilt 图、导航外壳、全部屏幕/ViewModel、解析器与导入、网络客户端、Widget、ContentProvider、10 个仓库实现 | 组合根；大部分 UI 仍在这里 |
| `:core` | `FlowTheme`、颜色/token、阅读色板、共享 Compose 组件，以及可 JVM 测试的纯工具 | 设计系统与跨模块工具 |
| `:data` | `AppDatabase`、8 个 Room entity、7 个 DAO、schema 导出 | 仅 Room 持久化层 |
| `:domain` | 领域模型与 10 个 repository interface | 纯 Kotlin 契约层；没有 `usecase/` |
| `:feature:reader` | `ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`、`ReaderPositionUnit` | 已抽出的阅读器纯逻辑，均有 JVM 测试 |
| `:feature:library` | 迁移边界 | 空占位模块，已接入编译与 lint |

### `:app` 的主要边界

- 入口：`MainActivity.kt`、`FlowReaderApplication.kt`。
- 外壳：`ui/FlowReaderApp.kt`、`ui/Navigation.kt`、`ui/AppShellViewModel.kt`。
- 页面：`ui/screens/<screen>/`，当前包含 library、stats、settings、wheel、bookdetail、reader、search、notes、readinglist、opds。
- 仓库实现：`data/repository/`。`BackupRepository.kt` 和 `SettingsRepository.kt` 的文件名按接口命名，但类分别是 `BackupRepositoryImpl` 与 `SettingsRepositoryImpl`。
- 导入与阅读基础设施：`util/BookParser.kt`、`BookLoader.kt`、`ZipImporter.kt`、`CacheManager.kt`、`FullTextSearch.kt`、`TtsManager.kt`、`OpdsClient.kt`、`LanTransferClient/Server.kt` 等。
- IPC/系统集成：`provider/FlowReaderContentProvider.kt`、`widget/ReadingProgressWidgetProvider.kt`。

源码包名大多仍为 `com.flowreader.app.*`，即使模块 namespace 是 `com.flowreader.core`、`com.flowreader.data` 或 `com.flowreader.domain`。新增文件沿用现有源码包名；`:feature:reader` 使用 `com.flowreader.feature.reader`。

## 导航与 UI 外壳

路由定义在 `app/.../ui/Navigation.kt` 的 sealed class `Screen` 中：

```text
library, stats, settings, wheel, notes, reading_lists, opds
search?query={query}
book_detail/{bookId}
reader/{bookId}?chapterIndex={chapterIndex}
```

底部导航只承载书库、统计、设置；宽度 `screenWidthDp >= 600` 时切换为导航栏，其余页面从书库顶栏进入。路由标题保存为 `@StringRes`，避免应用内语言切换后静态字符串冻结。`Screen.Reader.createRoute(bookId, chapterIndex = -1)` 中的 `-1` 表示续读，不传章节参数。

全局主题只在 NavHost 层应用一次：`AppThemeMode`（浅色/深色/跟随系统）与 `ColorSource`（内置/动态/自定义）是独立维度；阅读器正文使用自己的 `ReaderPalette`，不随应用主题改变。窗口 inset 的唯一契约在 `FlowShellScaffold`：外层 `Scaffold` 使用零 `contentWindowInsets` 并消费 shell padding，页面自身的 `Scaffold` 只应用一次状态栏 inset。修改时应同时查看 `ShellWindowInsetsTest`。

## 数据与持久化

### Room

`data/local/AppDatabase.kt` 当前是 version 7，包含 8 个 entity 与 7 个 DAO，`exportSchema = true`。schema 位于 `data/schemas/com.flowreader.app.data.local.AppDatabase/`。迁移由 `app/di/AppModule.kt` 注册：

- `MIGRATION_4_5`：增加 `books.tags`。
- `MIGRATION_5_6`：增加书签 `(bookId, chapterIndex, position)` 索引。
- `MIGRATION_6_7`：增加 `reading_lists` 与 `reading_list_items` 及其约束。

项目禁止 `fallbackToDestructiveMigration()`。备份导入通过 `database.withTransaction` 保持原子性。

### FTS5

`util/FullTextSearch.kt` 在 Room 之外维护 `flowreader_fts.db`，使用 `book_content` + `book_content_fts` 支持书内搜索和全库搜索。索引写操作必须经过共享对象的 `withIndexLock()`；锁不可重入，因此全局重建与“已持锁”实现分为两个函数。书内查询中的 `book_id` 必须使用 `CAST(? AS INTEGER)`，因为 FTS5 外部内容列没有类型亲和性。删除旧内容后再插入新章节，避免外部内容索引残留 token。

### DataStore 与缓存

`SettingsRepositoryImpl` 通过 `Context.dataStore` 保存应用主题、语言、阅读设置、目标、搜索历史和 Widget 快照；阅读设置从未进入 Room。章节元数据、正文和封面只有一个缓存入口 `CacheManager`，其容量按可用内存与命中率自适应，并响应系统内存压力；不要新增第二个章节内容缓存。

## 依赖注入与仓库

所有 Hilt 装配集中在 `app/di/AppModule.kt`：

- `DatabaseModule` 提供 `AppDatabase`、迁移和 7 个 DAO。
- `RepositoryModule` 用 `@Binds` 将 10 个 `domain/repository` 接口绑定到 `app/data/repository` 实现。

新增仓库需要同步修改接口、实现、`@Binds`，并补测试。ViewModel 对外暴露不可变 `StateFlow<XxxUiState>`；错误使用 Kotlin 内置 `Result`，旧的自定义 `AppException`/`Result` 已删除。

## 测试与质量门禁

- `./gradlew testDebugUnitTest`：`:app`、`:core`、`:domain`、`:feature:reader` 的 JVM 测试。
- `./gradlew verifyKotlinStyle`：除 `:app` 外的 ktlint，加上全仓 `.kt`/`.kts` Tab 与行尾空白门禁。
- `./gradlew coverageSummary`：文件数量比，不是行覆盖率；当前门槛为 40%。分母包含 `:core` 主源文件、domain 模型/接口、app 仓库实现和 ViewModel。
- `./gradlew recordRoborazziDebug` / `verifyRoborazziDebug`：Robolectric JVM 截图基准，基准位于 `app/src/test/snapshots/`。
- `./gradlew performanceBaseline`：构建 debug/release 并比较 APK 体积，然后重写 `baseline/apk-size.properties`；判断体积变化前先做 clean build。

CI 工作流 `.github/workflows/ci.yml` 的顺序是：`verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug` → `verifyRoborazziDebug` → `performanceBaseline`。`.github/workflows/build.yml` 另外负责 PR/`main` 构建与 `main` 推送后的 Release 发布。

## 安全边界

所有外部文件名与字节均视为不可信输入：使用 `ImportFileName.sanitize/resolveWithin`，使用解析器的大小上限，ZIP/CBZ 拒绝路径穿越。网络权限只有 `INTERNET`，但 Manifest 仍保留针对旧 Android 的 maxSdk 存储权限；OPDS 与 LAN 备份在每个请求点校验局域网地址。ContentProvider 只读暴露元数据与进度，v56.6.2 起需要 `READ_LIBRARY` 运行时权限；FileProvider 只开放 `share_cards/`。发布签名在存在 `keystore.properties` 时使用正式密钥，否则为保证 CI 可构建而回退 debug 签名。详见 [SECURITY.md](SECURITY.md) 与 [AGENTS.md](AGENTS.md)。

## 维护规则

本文件记录“现在是什么”。版本历史放在 [CHANGELOG.md](CHANGELOG.md)，路线图放在 [ROADMAP.md](ROADMAP.md)，不要把未来计划写成已存在的模块或 API。改动模块、依赖方向、数据库 schema、路由或 CI 时，应在同一提交更新本文件及对应的中英文用户/贡献者文档。
