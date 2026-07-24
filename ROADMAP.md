# FlowReader 未来发展规划

> 基于 v44.0.3 代码库分析，按优先级和阶段组织。已领取阶段标记 ✅。

---

## 第一阶段：稳定与质量（短期 1-2 月）

### 1.1 Crash 防护与错误处理 ✅

| 问题 | 方案 | 涉及文件 | 状态 |
|------|------|---------|------|
| `StatsViewModel.loadStats()` 无 try-catch，DB 异常直接崩溃 | 添加 try-catch，错误通过 UiState.error 展示 | `StatsViewModel.kt` | ✅ 已实现 |
| `ReaderViewModel.loadChapterContent()` content 为 null 时静默失败 | 设置 error 状态，UI 展示错误提示 | `ReaderViewModel.kt` | ✅ 已实现 |
| `WheelViewModel` 删除所有选项后 spin() 无反馈 | 校验 item 数量，不足时显示提示 | `WheelViewModel.kt` | ✅ 已实现 |
| `PdfViewer` 加载失败无重试机制 | 添加重试按钮 | `PdfViewer.kt` | ✅ 已实现 |
| `SettingsViewModel` export/import 结果未展示 | 添加 Snackbar/Toast 反馈 | `SettingsViewModel.kt`, `SettingsScreen.kt` | ✅ 已实现 |

### 1.2 测试覆盖

| 模块 | 测试内容 | 优先级 |
|------|---------|-------|
| `ReadingStatsRepositoryImpl` | `calculateCurrentStreak()`、`calculateLongestStreak()` 日期计算 | 高 |
| `BookParser` | `parseEpubStream()`、`parseTxtStream()`、`parseMarkdownStream()`、`parsePdfStream()` | 高 |
| `CacheManager` | LRU 驱逐策略、命中率统计 | 中 |
| `TextPaginator` | `generatePages()` 分页逻辑 | 中 |
| `WheelViewModel` | `spin()` 加权随机 + 状态切换 | 中 |
| ViewModel 状态机 | 每个 ViewModel 的 loading/error/success 状态流转 | 中 |

### 1.3 数据安全 ✅

- **替换 `fallbackToDestructiveMigration()`**：已移除 `fallbackToDestructiveMigration()`，启用 `exportSchema=true`（`AppModule.kt:33`）
- **`BackupRepository` 导入加事务**：`database.withTransaction` 包裹批量插入，失败时回滚（`BackupRepositoryImpl.kt`）

---

## 第二阶段：功能补齐（中期 2-4 月）

### 2.1 阅读器体验

| 功能 | 现状 | 方案 |
|------|------|------|
| 字体选择 | `FontFamily` 定义 8 种字体，UI 无选择入口 | 阅读设置面板添加字体选择器 |
| 自定义字体 | `ReadingSettings.customFontPath` 已定义未实现 | 支持导入 .ttf/.otf 字体文件 |
| 全文搜索 | `FullTextSearch` 已实现但从未被调用 | 阅读器添加搜索入口，连接 FullTextSearch |
| 翻页模式 | `TextPaginator` 已实现但未使用 | 支持真正的分页模式（当前为滚动） |
| 标注高亮渲染 | 标注颜色定义但未渲染到文本中 | 使用 `SpanStyle` + `buildAnnotatedString` 渲染高亮 |
| EPUB 图片 | `Jsoup` 过滤掉所有 img 标签 | 保留并渲染 EPUB 内嵌图片 |
| EPUB CSS 样式 | HTML 全部转为纯文本 | 保留基础格式化（标题、粗体、斜体） |

### 2.2 书籍详情页

- **添加标注 Tab**：当前只有目录和书签两个 Tab，标注数据已加载但未展示
- **每个 Tab 独立加载状态**：当前整个页面用一个 `isLoading`

### 2.3 阅读统计

- **按书籍统计**：当前只有聚合统计，无单书阅读历史
- **趋势图表**：用 Compose Canvas 绘制阅读时长趋势柱状图/折线图（数据已有，缺 UI）

---

## 第三阶段：架构优化（中期 3-5 月）

### 3.1 缓存层整合

**问题**：`ChapterRepositoryImpl` 有自己的 `ConcurrentHashMap` 缓存，同时 `CacheManager` 也有独立缓存。两层缓存浪费内存且可能不一致。

**方案**：
- 移除 `ChapterRepositoryImpl.contentCache`
- `CacheManager` 作为唯一缓存层，所有章节内容通过它存取
- `CacheManager` 根据 `MemoryManager` 建议动态调整缓存大小

### 3.2 领域接口拆分

**问题**：`ChapterRepository`、`BookmarkRepository`、`AnnotationRepository`、`CategoryRepository` 四个领域接口挤在 `BookRepository.kt` 中。

**方案**：拆分为独立文件，保持代码风格一致。

### 3.3 SettingsRepository 抽象

**问题**：`SettingsRepository` 无领域接口，直接注入 ViewModel，违反 Clean Architecture。

**方案**：
- 创建 `domain/repository/SettingsRepository.kt` 接口
- `data/repository/SettingsRepositoryImpl` 实现之
- ViewModel 注入接口而非实现

### 3.4 移除死代码

| 文件 | 说明 |
|------|------|
| `ReadingSettings` 中未使用的字段 | `customFontPath`、`backgroundTexture`、`backgroundColor`、`textColor`、`ambientSound`、`ambientSoundVolume`、`firstLineIndent`、`justifyText`、`simplifiedChinese` |
| `GetBookUseCase`、`SaveProgressUseCase` | 可能未被 ViewModel 使用（需验证） |
| `TextPaginator` | 若不分页模式则可移除 |

---

## 第四阶段：新功能（远期 4-6 月）

### 4.1 词典与翻译

- 长按单词弹出释义（集成系统词典或 Collins/牛津 API）
- 支持翻译选中文本（接入 Google Translate / 本地词典）

### 4.2 阅读目标通知

- `readingReminderEnabled` 已定义但未实现
- 使用 `AlarmManager` 或 `WorkManager` 定时通知
- 每日目标完成后推送鼓励通知

### 4.3 注释导出

- 导出标注/笔记/书签为 HTML、Markdown、JSON 格式
- 与备份机制结合

### 4.4 本地化

- 所有硬编码中文 → `strings.xml`
- 支持英文初始版本（已有多语言 `values/` 目录结构）
- 跟进系统语言切换

---

## 第五阶段：可访问性与国际化（持续）

### 5.1 Accessibility

| 问题 | 方案 |
|------|------|
| `detectTapGestures` 阻塞 TalkBack | 改用 `Modifier.clickable` + `semantics {}` |
| Canvas 组件无声屏阅读支持 | `WheelSpinner` 添加 `semantics {}` |
| 图标缺 `contentDescription` | 所有装饰图标补充描述 |
| 颜色仅作为唯一标识 | 标注颜色增加文字标签或图案 |
| 进度条无声屏描述 | `LinearProgressIndicator` 添加 `semantics {}` |

### 5.2 性能

| 问题 | 方案 |
|------|------|
| `ReaderContent` 逐段落渲染 Text | 改用 `buildAnnotatedString` 单 Text 组件 |
| `BookLoader.cancelAll()` 永久取消 scope | 改用 `Job` 管理 |
| `ReadingStatsRepositoryImpl.calculateCurrentStreak()` 全表加载 | 改用 Room 聚合查询 |
| `CacheManager` 内存压力响应 | 注册 `ComponentCallbacks2` 按级别清理 |

---

## 版本规划建议

| 版本 | 阶段 | 主要内容 |
|------|------|---------|
| v44.1.0 | 第一阶段 | Crash 防护 + Room Migration + 数据安全 | ✅ 已完成 |
| v45.0 | 第二阶段 | 阅读器体验升级（字体、搜索、翻页模式） |
| v45.1.x | 第三阶段 | 架构重构（缓存整合、接口拆分） |
| v46.0 | 第四阶段 | 词典 + 注释导出 + 本地化 |
