# 更新日志

本项目遵循语义化版本控制规范。

---

## [v45.0.1] - 2026-07
- **按书籍阅读统计**：BookDetailScreen 添加统计卡片，显示每本书累计阅读时长和页数。
- **阅读趋势图表**：StatsScreen 以 Canvas 柱状图展示最近 7 天阅读时长趋势，替换原有纯文本列表。
- **EPUB 内嵌图片**：BookParser 自动提取 EPUB 中的图片并渲染到阅读器正文中。
- **EPUB 排版保留**：HTML 解析保留标题（`##`）、粗体（`**`）、斜体（`*`）格式标记，ReaderContent 渲染对应样式。
- **自定义字体导入**：设置页导入 .ttf/.otf 字体文件，路径持久化到 DataStore，支持清除恢复默认。
- **动态缓存容量**：CacheManager 注入 MemoryManager，根据设备可用内存动态调整 LRU 缓存上限。
- **领域接口补齐**：README.md 目录树更新为当前实际结构（8 个 domain repository 接口）。

## [v45.0.0] - 2026-07
- **字体选择器**：ReaderSettingsDialog 添加 8 种字体（默认/衬线/无衬线/等宽/宋/黑/楷/仿宋）FilterChips。
- **全文搜索**：集成 FTS5，阅读器控制栏添加搜索入口 + SearchDialog，结果跳转对应章节。
- **标注高亮渲染**：ReaderContent 使用 `buildAnnotatedString` + `SpanStyle` 渲染标注背景色。
- **书籍详情标注 Tab**：标注列表显示颜色、选中文本、备注，支持删除。
- **缓存整合**：移除 `ChapterRepositoryImpl.contentCache`，统一通过 `CacheManager` 存取章节内容。
- **领域接口拆分**：Chapter、Bookmark、Annotation、Category 接口从 `BookRepository.kt` 迁移到独立文件。
- **SettingsRepository 抽象**：创建 `domain/repository/SettingsRepository.kt` 接口，`data/repository/SettingsRepository.kt` 更名为 `SettingsRepositoryImpl` 实现接口，ViewModel 全部注入接口。
- **移除死代码**：`GetBookUseCase`、`SaveProgressUseCase`、`TextPaginator`、`ParagraphMode`、`BackgroundTexture`、`AmbientSound` 枚举、11 个未用的 `ReadingSettings` 字段。
- **主题简化**：仅保留深色/浅色两种主题，全局统一生效，移除 autoTimeTheme/dynamicColor 等逻辑，净减约 260 行。

## [v43.0.0] - 2026-07
*   **决策转盘改进**：`spin()` 改为自动管理协程（`viewModelScope.launch`），无需外部 `LaunchedEffect` 触发；旋转角度基于当前角度叠加，连续旋转更流畅；转盘文字始终正向可读，不再倒置。
*   **Gradle 升级**：Gradle Wrapper 从 `8.7` 升级到 `9.6.1`；重构 `gradle.properties`，添加 `UseParallelGC`、`vfs.watch`、`kotlin.daemon.jvmargs` 等优化项，提升构建性能。
*   **漏洞修复**：
    *   `StatsViewModel`：阅读统计数据不再只快取一次，每次收集时实时刷新
    *   `FullTextSearch`：消除不安全的 `!!` 操作符，使用局部变量确保空安全
    *   `WheelViewModel`：动画协程取消后 `isSpinning` 自动恢复为 false，防止永久卡死
    *   `WheelScreen`：消除转盘结果对话框的 NPE 隐患（`!!` → 局部变量判空）
    *   `PdfViewer`：`printStackTrace()` 替换为 `Log.e()`，符合 Android 规范
    *   `ReaderContent`：`paragraphSpacing.toInt().dp` 改用直接浮点数转换，消除精度丢失
    *   `CacheManager.ChapterMeta.toDomain()`：新增 `bookId` 参数，不再始终返回 0
*   **架构优化**：
    *   移除完全未使用的 `domain/model/AppException.kt`（含自定义 `Result<T>`）
    *   移除完全未使用的 `data/repository/DataManager.kt` 和 `DataCleaner`
    *   `BookLoader.kt`：移除与 `domain/usecase/` 重复的 `TextPaginator` 类
    *   `FlowReaderApp.kt`：移除未使用的导航导入和未使用的 `SettingsViewModel` 注入
    *   `ChapterDao`：移除与 `BookDao` 重复的 `getBookCount()`
    *   `LibraryScreen`：移除未使用的 `singleBookPickerLauncher`
    *   移除各 Repository 实现中的无用实体导入（`BookEntity` 后恢复，`MemoryManager` 中的 `Build`）
*   **性能提升**：
    *   `ReadingStatsEntity`：新增 `date` 列独立索引，优化按日期筛选查询
    *   `CacheManager.getCacheStats()`：使用 `synchronized` 保护并发访问
    *   `CacheManager`：真实缓存命中/未命中统计替换原有的硬编码 `0.75f`
*   **构建修复**：修复 `LibraryViewModel` 中缺失的 `Job` 导入，消除 Kotlin 2.0 编译错误。
*   **APK 构建完成**：通过 `./gradlew assembleDebug` 验证，单元测试通过。

## [v44.0.1] - 2026-07
*   **阅读统计修复**：`ReaderViewModel.sessionReadPages` 从未递增导致阅读数据永不保存；现根据滚动字符增量合理累计页数。
*   **代码清理**：移除 `ReaderViewModel` 中未使用的 `MemoryManager` 注入、`AnnotationType` 导入、未使用的 `sessionCharactersRead` 字段；移除 `ReaderScreen` 中 7 个未使用的导入（`Intent`、`Bitmap`、`PdfRenderer` 等）；修复 `BookDetailScreen` 中 `Icons.Default.ArrowBack` 废弃用法。
*   **闪退修复**：所有书籍加载流程添加 `try-catch` 和 `bookId > 0` 校验，数据库异常或文件缺失时显示错误提示而非崩溃。

## [v44.0.0] - 2026-07
*   **CI 修复**：Release 构建类型添加 `signingConfig = signingConfigs.getByName("debug")`，修复 GitHub Actions 中 `build-and-release` Job 因产物路径 `app-release.apk` 不存在而导致上传失败和 Release 创建失败的问题。
*   版本号更新至 44.0.0。

## [v42] - 2025-06
*   **交互体验**：全面优化页面交互动画，列表项添加 `AnimatedVisibility` 淡入效果，使交互更平滑自然。
*   **书架优化**：新增下拉刷新功能（使用 Material 3 PullToRefresh 组件替代旧版），优化书籍列表加载动画。
*   **书籍详情**：改善 Tab 切换动画效果，添加书签删除淡出动画。

## [v41] - 2025-05
*   **决策转盘**：新增可定制的决策转盘功能，帮助解决阅读选择困难。
*   **底部导航**：优化底部导航栏，增加转盘入口。
*   **版本规范**：规范版本号为语义化版本控制。

## [v40.1] - 2025-04
*   **高亮修复**：优化高亮功能交互，长按/点击段落后手动输入文本再添加高亮。
*   **章节跳转修复**：修复跳转下一章时滚动位置重置问题，切换章节自动回到开头。

## [v40] - 2025-03
*   **TTS修复**：修复语音朗读功能，添加朗读/停止按钮到设置界面。
*   **版本规范**：规范版本号为 40.0.0，CI 使用语义化版本。
*   **单元测试**：GitHub Actions 集成单元测试。
*   **代码清理**：移除 DataManager 中 Sync 残留代码。

## [v36] - 2025-01
*   **Bug修复**：修复空安全断言 `!!` 问题 (ReaderViewModel)。
*   **CI修复**：修复 GitHub Actions build.yml job 定义问题。
*   **代码优化**：移除过期 Icons.Filled 使用。

## [v35] - 2024-12
*   **离线优先**：移除账号系统、云端同步等网络功能，纯本地运行。
*   **代码治理**：重命名 Application 类 FlowReaderApp → FlowReaderApplication。
*   **代码治理**：重命名 Composable FlowReaderApp → FlowReaderRoot。
*   **代码规范**：添加 .editorconfig 代码规范配置。
*   **CI优化**：优化 CI permissions，按需授权（最小权限原则）。
*   **异常处理**：新增 AppException.kt 统一异常处理机制。
*   **架构优化**：新增 domain/usecase/ 层 (GetBookUseCase, SaveProgressUseCase)。
*   **性能优化**：新增 TextPaginator 分页加载 (3000字/页, 预加载2页)。
*   **进度防抖**：3秒延迟保存减少数据库写入。

## [v30] - 2024-10
*   **Markdown 支持**：新增 .md 格式解析支持。
*   **测试基础设施**：引入 JUnit 4 + MockK 测试框架。
*   **构建优化**：Release 启用 R8 混淆压缩。
*   **现代 Android 适配**：Edge-to-edge, Splash Screen 支持。

## [v29] - 2024-09
*   **UI组件拆分重构**：ReaderScreen 拆分为独立组件模块。
*   **新增组件**：ReaderContent, PdfViewer, ReaderControls, 各 Dialog 组件。
*   **智能阅读**：基于阅读速度预测剩余阅读时间，实时计算阅读速度(字/分钟)。
*   **护眼提醒**：Regular 提醒，每20分钟提醒用户休息。
*   **阅读目标**：显示每日阅读目标完成进度，建议休息时间。
*   **书籍分类增强**：支持分类筛选、添加、删除书籍分类功能。

## [v28] - 2024-08
*   **内存管理优化**：新增 MemoryManager，实时监控内存状态和压力级别。
*   **智能缓存管理**：新增 CacheManager，实现 LRU 缓存和自动内存回收。
*   **章节内容缓存**：缓存已加载章节内容，减少重复数据库查询。
*   **分页加载支持**：新增 getBooksPaged() 支持分页加载书籍列表。
*   **Lazy Loading**：章节内容按需加载，首屏加载更快。

## [v27] - 2024-07
*   **性能优化**：章节内容 Lazy Loading，减少首次加载时间。
*   **新增内容缓存机制**，避免重复解析。

## [v26] - 2024-06
*   **性能优化**：数据库版本升级到 v2，添加复合索引。
*   **ViewModel 优化**：使用 first() 替代 collect 加载设置。
*   **UI 渲染优化**：使用 derivedStateOf 缓存计算值。
*   **手势设置持久化到 DataStore**。

## [v25] - 2024-05
*   **新增文本高亮/笔记功能**。
*   **新增阅读进度条**。
*   **新增阅读统计页面**。
*   **新增书籍封面自动提取**。
*   **新增手势自定义 UI**。
*   **新增阅读进度分享**。

## [v24] - 2024-04
*   **新增多语言支持**（中文、英语、日语、韩语）。
*   **性能优化**：章节内存缓存。
*   **性能优化**：数据库查询优化。

## [v19] - 2024-03
*   修复 Kotlin/Compose 版本兼容性问题。
*   更新 Android Gradle Plugin 至 8.6.0。
*   更新 Kotlin 至 2.0.21。
*   更新 Gradle 至 8.7。
*   更新 compileSdk/targetSdk 至 35。

## [v17] - 2024-02
*   **新增 Readium Kotlin Toolkit EPUB 渲染引擎**，支持复杂 CSS/排版。
*   **新增边缘手势识别设置**，解决滑动翻页与系统返回手势冲突。
*   **新增笔记/批注功能**（划线、想法、导出）。
*   **新增全文搜索**（单本书籍内 FTS 检索）。
*   **新增 TTS 文本朗读功能**。
*   **性能优化**：提升大型书籍解析速度。

## [v15] - 2024-01
*   Latest release version.

## [v12.0.0] - 2023-12
*   新增阅读目标设置（每日阅读时长目标）。
*   新增搜索历史记录功能。
*   **性能优化**：数据库索引优化。
*   **性能优化**：图片缓存优化。
*   **性能优化**：书籍解析流式处理。
*   **UI 优化**：阅读进度百分比显示增强。

## [v11.0.0] - 2023-11
*   **新增 PDF 格式支持**。
*   **新增阅读统计功能**（阅读时长、页数）。
*   **新增底部可拖拽进度条**。
*   **新增时间自动夜间模式**。
*   **新增批量导入书籍**。
*   **新增书籍排序功能**。
*   **新增阅读记录导出**。
*   **新增每日阅读提醒**。
*   **新增备份/恢复功能**。
*   **新增关于页面**。
*   **新增 Material You 动态颜色支持**。
*   **性能优化**：启动速度、内存占用优化。
*   深色主题对比度优化。
*   AMOLED 纯黑模式增强。
