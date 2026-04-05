# FlowReader 性能优化与功能增强计划

## 一、项目现状评估

基于对项目代码的初步分析，FlowReader 是一款基于 Jetpack Compose + Room + Hilt 的 Android 电子书阅读应用，当前支持 EPUB 和 TXT 格式。项目整体架构遵循 Clean Architecture + MVVM 模式，代码结构清晰。

### 1.1 现有功能

- 书库管理：导入书籍、书架展示、分类浏览、阅读进度保存
- 阅读体验：5种阅读主题、字体/行间距调节、翻页模式
- 书签功能：添加、列表管理、跳转
- 目录导航：章节展示、快速跳转

---

## 二、性能优化方案

### 2.1 核心性能问题识别

| 优先级 | 问题 | 位置 | 影响 |
|--------|------|------|------|
| **高** | 大文件书籍解析时阻塞 IO 线程 | BookParser.kt | 导入大文件时界面卡顿 |
| **高** | 阅读器内容渲染使用 verticalScroll，无虚拟化 | ReaderScreen.kt | 长章节滚动性能差 |
| **中** | 封面图片每次都从文件系统读取 | LibraryScreen.kt | 列表滚动时重复 IO |
| **中** | 阅读进度每次翻页都写入数据库 | ReaderViewModel | 频繁数据库写操作 |
| **低** | 未实现图片内存缓存策略 | Coil 配置 | 内存使用可优化 |

### 2.2 性能优化实施计划

#### 2.2.1 书籍解析优化（高优先级）

**问题**：当前 BookParser 在 IO 线程中一次性加载整个文件内容到内存，大文件（>10MB）会导致 OOM 或长时间阻塞。

**优化方案**：
1. 流式读取：使用 InputStream 分块读取，避免一次性加载整个文件
2. 渐进式解析：先解析元数据，后台线程逐步解析章节
3. 添加进度回调：解析过程中向 UI 层报告进度

```kotlin
// 优化后的解析接口示例
suspend fun parseBook(uri: Uri, onProgress: (Float) -> Unit): Result<BookParseResult>
```

#### 2.2.2 阅读器渲染优化（高优先级）

**问题**：ReaderContent 使用 `verticalScroll(scrollState)` 加载全部内容，长章节（>10000字）会导致：
- 首次渲染时间长
- 滚动时内存占用高
- 无法利用 Compose 的重组优化

**优化方案**：
1. 分页加载：将章节内容分页，每页固定字数（如 3000 字）
2. 状态记忆：记住用户离开时的阅读位置
3. 预加载：提前加载下 2 页内容

```kotlin
// 分页内容数据结构
data class PageContent(
    val chapterIndex: Int,
    val pageIndex: Int,
    val content: String,
    val isLastPage: Boolean
)
```

#### 2.2.3 图片加载优化（中优先级）

**问题**：Cover 图片使用 Coil 加载，但未配置内存缓存策略，导致重复读取。

**优化方案**：
1. 配置内存缓存：`memoryCacheKey`, `memoryCachePolicy`
2. 添加占位图和错误图
3. 使用 `remember` 缓存已解码的 Bitmap

```kotlin
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(File(book.coverPath))
        .crossfade(true)
        .memoryCacheKey(book.coverPath)  // 添加内存缓存键
        .build(),
    // ...
)
```

#### 2.2.4 阅读进度写入优化（中优先级）

**问题**：每次翻页都调用 `updateReadingProgress`，频繁写入数据库。

**优化方案**：
1. 批量写入：使用 `CoroutineScope` 延迟写入，每 5 秒写入一次
2. 退出保存：用户离开阅读器时强制保存
3. 关键节点保存：在切换章节时立即保存

```kotlin
// 使用 Debounce 延迟保存
private val progressSaveJob = CoroutineScope(Dispatchers.IO).launch {
    delay(5000)  // 5秒延迟
    repository.updateReadingProgress(...)
}
```

#### 2.2.5 数据库查询优化（低优先级）

**问题**：当前查询已使用 Flow，但可进一步优化索引。

**优化方案**：
1. 为 `lastReadTime`、`addedTime` 添加复合索引
2. 对于搜索功能，考虑使用 Room FTS（Full-Text Search）

```kotlin
@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC")
    fun getAllBooks(): Flow<List<BookEntity>>
    
    // 添加索引
    @Query("SELECT * FROM books WHERE lastReadTime IS NOT NULL ORDER BY lastReadTime DESC LIMIT :limit")
    fun getRecentlyReadBooks(limit: Int = 10): Flow<List<BookEntity>>
}
```

---

## 三、新增核心功能方案

### 3.1 功能优先级评估

| 功能 | 实现复杂度 | 用户价值 | 建议优先级 |
|------|-----------|---------|------------|
| PDF 阅读支持 | 高 | 高 | P1（后期） |
| 全局搜索功能 | 中 | 高 | P1 |
| 阅读统计 | 低 | 中 | P2 |
| 笔记功能 | 中 | 中 | P2 |
| 自动夜间模式 | 低 | 中 | P3 |

### 3.2 功能详细设计

#### 3.2.1 全局搜索功能（P1）

**功能描述**：在书库中搜索书籍名称、作者；在阅读中搜索书籍内容。

**技术方案**：
1. 书库搜索：扩展现有 `searchBooks` 方法，增加作者搜索
2. 内容搜索：在 BookParser 中建立简单的词索引（或者使用 Room FTS）
3. UI：在 LibraryScreen 添加搜索入口，在 ReaderScreen 添加搜索入口

**实现步骤**：
```
1. 阶段一：书库搜索增强
   - 添加搜索入口 UI（SearchBar）
   - 扩展 BookDao.searchBooks 支持作者模糊匹配
   - 实现搜索历史记录（使用 DataStore）

2. 阶段二：阅读内容搜索
   - 在 ReaderViewModel 添加搜索状态管理
   - 实现关键词高亮显示
   - 添加搜索结果导航
```

**预计改动范围**：
- 新增：SearchRepository, SearchViewModel
- 修改：LibraryScreen, LibraryViewModel, ReaderScreen, ReaderViewModel
- 依赖：无新依赖

#### 3.2.2 阅读统计功能（P2）

**功能描述**：记录用户阅读时长、阅读页数、阅读书籍数量等统计数据。

**数据模型**：
```kotlin
@Entity(tableName = "reading_statistics")
data class ReadingStatisticsEntity(
    @PrimaryKey val bookId: Long,
    val totalReadTime: Long = 0,        // 总阅读时间（毫秒）
    val totalPagesRead: Int = 0,        // 已读页数
    val lastReadDate: Long = 0,         // 最后阅读日期
    val readCount: Int = 0              // 阅读次数
)
```

**UI 展示**：
- 入口：设置页面添加"阅读统计"入口
- 展示：本周阅读时长、连续阅读天数、已完成书籍数

#### 3.2.3 笔记功能（P2）

**功能描述**：在阅读过程中添加笔记，与书签类似但可编辑。

**数据模型**：
```kotlin
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val chapterIndex: Int,
    val position: Int,
    val content: String,
    val createdTime: Long,
    val updatedTime: Long
)
```

**实现步骤**：
```
1. 创建 NoteEntity 和 NoteDao
2. 在 ReaderScreen 添加笔记按钮
3. 创建笔记列表页面（可编辑、删除）
4. 与书籍详情页集成
```

#### 3.2.4 自动夜间模式（P3）

**功能描述**：根据时间或环境光线自动切换阅读主题。

**技术方案**：
1. 时间触发：使用 WorkManager 定时检查
2. 光线触发：使用 SensorManager 监听光线传感器
3. 配置选项：在设置中允许用户配置自动切换规则

**实现步骤**：
```
1. 在 SettingsRepository 添加自动夜间模式配置
2. 创建 AutoThemeManager 处理自动切换逻辑
3. 在 ReaderScreen 监听配置变化并自动应用主题
```

#### 3.2.5 PDF 阅读支持（P1 - 后期）

**技术挑战**：
- 需要 PDF 渲染库（如 AndroidPdfViewer 或 MuPdf）
- PDF 渲染内存占用高
- 需要处理 PDF 特定的分页逻辑

**依赖建议**：
```kotlin
// build.gradle.kts
implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.1")
```

**实现计划**：
```
1. 添加 PDF 渲染依赖
2. 创建 PdfRendererViewModel
3. 实现 PDF 阅读页面（复用部分 ReaderScreen 逻辑）
4. 处理 PDF 特定功能：缩放、跳转页码
```

---

## 四、代码质量改进

### 4.1 依赖版本管理

当前依赖版本（分析自 build.gradle.kts）：
- Compose BOM: 2023.10.01（较旧，建议升级到 2024.02.00+）
- Room: 2.6.1（当前最新）
- Hilt: 2.48.1（当前最新）
- Coil: 2.5.0（可升级到 2.6.0）
- Kotlin: 1.9.x 配套的 Compose Compiler

**建议升级**：
```kotlin
// 升级 Compose BOM
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
// 升级 Kotlin
kotlin("1.9.22")
// 升级 Compose Compiler
kotlinCompilerExtensionVersion = "1.5.10"
```

### 4.2 架构优化建议

1. **添加 UseCase 层**：将业务逻辑从 ViewModel 抽离到 UseCase
2. **Error Handling 统一**：创建统一的 UiState.Error 处理机制
3. **UI State 不可变**：确保所有 UI State 使用不可变数据类

### 4.3 包体积优化

1. 启用 R8 压缩：
   ```kotlin
   buildTypes {
       release {
           isMinifyEnabled = true
           isShrinkResources = true
           proguardFiles(...)
       }
   }
   ```
2. 移除未使用的 resources
3. 对 Coil 图片库进行针对性裁剪

---

## 五、实施路线图

### 阶段一：性能优化（2-3周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 第1周 | 书籍解析流式改造、分页加载 | BookParser 优化版、PageContent 组件 |
| 第2周 | 图片缓存优化、进度写入优化 | Coil 优化配置、延迟保存机制 |
| 第3周 | 测试与调优 | 性能测试报告、Benchmark |

### 阶段二：核心功能（3-4周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 第4周 | 全局搜索 - 书库搜索 | 搜索 UI、搜索历史 |
| 第5周 | 全局搜索 - 内容搜索 | 阅读内搜索功能 |
| 第6周 | 阅读统计 | 统计数据表、统计页面 |
| 第7周 | 笔记功能 | 笔记 CRUD、笔记列表 |

### 阶段三：增强功能（2-3周）

| 周次 | 任务 | 产出 |
|------|------|------|
| 第8周 | 自动夜间模式 | 自动切换逻辑 |
| 第9周 | PDF 支持（可选） | PDF 阅读器 |

### 阶段四：质量提升（持续）

- 依赖升级
- 代码重构
- 自动化测试补充
- R8 压缩配置

---

## 六、风险与注意事项

1. **向后兼容性**：任何数据模型变更需要迁移策略（Room Migrations）
2. **性能回归**：新增功能后需重新测试性能指标
3. **PDF 库选择**：开源 PDF 库可能有兼容性问题和性能瓶颈
4. **隐私合规**：阅读统计数据需考虑用户隐私

---

## 七、总结

本计划从性能优化、新增功能、代码质量三个维度对 FlowReader 进行全面增强。优先处理影响用户体验的性能问题（书籍解析、阅读器渲染），然后逐步实现用户期待的核心功能（搜索、统计、笔记）。整个计划预计需要 8-12 周的开发时间，可根据实际资源情况灵活调整优先级。

---

*计划版本：v1.0*  
*创建时间：2026-04-05*  
*项目：FlowReader 心流阅读*
