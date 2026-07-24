# FlowReader 未来发展规划

> 基于 v45.0.0 代码库分析，按优先级和阶段组织。已领取阶段标记 ✅。

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
| `WheelViewModel` | `spin()` 加权随机 + 状态切换 | 中 |
| ViewModel 状态机 | 每个 ViewModel 的 loading/error/success 状态流转 | 中 |

### 1.3 数据安全 ✅

- **替换 `fallbackToDestructiveMigration()`**：已移除 `fallbackToDestructiveMigration()`，启用 `exportSchema=true`（`AppModule.kt:33`）
- **`BackupRepository` 导入加事务**：`database.withTransaction` 包裹批量插入，失败时回滚（`BackupRepositoryImpl.kt`）

---

## 第二阶段：功能补齐（中期 2-4 月） ✅

### 2.1 阅读器体验 ✅

| 功能 | 现状 | 方案 | 状态 |
|------|------|------|------|
| 字体选择 | `FontFamily` 定义 8 种字体，UI 无选择入口 | 阅读设置面板添加字体选择器 | ✅ 已实现 |
| 全文搜索 | `FullTextSearch` 已实现但从未被调用 | 阅读器添加搜索入口，连接 FullTextSearch | ✅ 已实现 |
| 标注高亮渲染 | 标注颜色定义但未渲染到文本中 | 使用 `SpanStyle` + `buildAnnotatedString` 渲染高亮 | ✅ 已实现 |

### 2.2 书籍详情页 ✅

- **添加标注 Tab**：已有"标注"Tab，AnnotationListContent 列表显示颜色+文本+备注 ✅
- **标注数据加载**：BookDetailViewModel 加载 annotations 并支持删除 ✅

### 2.3 阅读统计

- **按书籍统计**：`ReadingStatsRepository.getStatsByBookId()` 接口已存在，尚未对接 UI
- **趋势图表**：待实现

---

## 第三阶段：架构优化（中期 3-5 月） ✅

### 3.1 缓存层整合 ✅

- 移除 `ChapterRepositoryImpl.contentCache`，所有章节内容通过 `CacheManager` 存取 ✅
- `CacheManager` 作为唯一缓存层，LRU 淘汰 + `ComponentCallbacks2` 内存响应 ✅

### 3.2 领域接口拆分 ✅

- `ChapterRepository` → `domain/repository/ChapterRepository.kt` ✅
- `BookmarkRepository` → `domain/repository/BookmarkRepository.kt` ✅
- `AnnotationRepository` → `domain/repository/AnnotationRepository.kt` ✅
- `CategoryRepository` → `domain/repository/CategoryRepository.kt` ✅
- `BookRepository.kt` 仅保留 `BookRepository` 接口 ✅

### 3.3 SettingsRepository 抽象 ✅

- 创建 `domain/repository/SettingsRepository.kt` 接口 ✅
- `data/repository/SettingsRepository.kt` 更名为 `SettingsRepositoryImpl` 实现该接口 ✅
- ViewModel 全部注入接口而非实现类 ✅

### 3.4 移除死代码 ✅

| 文件/代码 | 状态 |
|-----------|------|
| `BackgroundTexture`、`AmbientSound` 枚举 | ✅ 已移除 |
| `ReadingSettings` 中 11 个未使用字段 | ✅ 已移除 |
| `GetBookUseCase`、`SaveProgressUseCase` | ✅ 已移除 |
| `TextPaginator`、`TextPage` | ✅ 已移除 |

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
| v45.0.0 | 第二、三阶段 | 字体选择 + 全文搜索 + 标注高亮 + 标注Tab + 缓存整合 + 接口拆分 + Settings抽象 + 移除死代码 | ✅ 已完成 |
| v46.0 | 第四阶段 | 词典 + 注释导出 + 本地化 |
