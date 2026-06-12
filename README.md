# FlowReader 心流阅读

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat&logo=kotlin" alt="Language">
  <img src="https://img.shields.io/badge/Architecture-Clean%20Architecture-lightgrey?style=flat" alt="Architecture">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose" alt="UI">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=flat" alt="License">
  <img src="https://img.shields.io/badge/MinSDK-26+-red?style=flat" alt="MinSDK">
</p>

<p align="center">
  <b>一款简洁优雅的 Android 离线电子书阅读器</b><br>
  专为沉浸式阅读体验而设计，兼顾性能与美观
</p>

---

## 📖 概述

FlowReader 是一款采用 **Jetpack Compose** 构建的 **Android 电子书阅读应用**。我们遵循 **Clean Architecture + MVVM** 设计，打造了一个纯本地、离线优先的阅读环境，支持 **EPUB、TXT、PDF、Markdown** 等多种格式。应用提供了可高度定制的阅读体验、强大的书籍管理系统以及多维度的阅读数据统计，帮助用户找回阅读心流。

---

## ✨ 核心特性

### 📚 书库与书籍管理
*   **多格式导入**：支持 EPUB、TXT、PDF、Markdown文件的本地导入与批量导入。
*   **智能书架**：支持按添加时间、阅读时间、书名、作者等多种维度排序；书籍元数据（作者、描述、封面）可编辑。
*   **搜索与筛选**：快速定位书架中的目标书籍；支持查看最近阅读记录。
*   **数据持久化**：3秒延迟写入的阅读进度自动保存机制，有效减少数据库 IO。

### 📖 沉浸式阅读体验
*   **个性化页面**：12 种可自定义的阅读主题（浅色、深色、护眼、羊皮纸、AMOLED纯黑等），12sp-32sp 无级字体调节，1.0-2.5 倍行间距调节。
*   **流畅翻页**：支持滑动、仿真、无动画、卷曲、滑动覆盖等多种翻页模式；兼容边缘手势防误触。
*   **智能交互**：左/右 30% 区域点击翻页，中间呼出菜单；底部可拖拽进度条快速跳转；支持屏幕常亮及自动夜间模式。
*   **PDF 专精渲染**：内置流畅的 PDF 渲染引擎，支持缩放与拖拽翻页。

### ☁️ 阅读云服务 (本地)
*   **备份与恢复**：支持将书籍及阅读进度导出为备份文件；支持从备份文件恢复数据。

### 📊 阅读数据统计
*   **目标管理**：自定义每日阅读时长目标，实时查看当日完成进度。
*   **多维统计**：按日统计阅读时长、阅读页数、阅读速度；提供阅读时长趋势分析。

### 🎡 决策转盘 (灵感工具)
*   **决策辅助**：内置可定制的决策转盘工具，支持自定义选项和颜色，帮助用户解决阅读选择困难症。

### ⚙️ 更多贴心功能
*   **全局搜索**：基于 SQLite FTS5 实现单本书籍内部全文检索，关键词高亮，快速定位。
*   **笔记与批注**：支持文字高亮（黄、绿、蓝、粉、橙 5 色标注）、添加批注想法，并可导出。
*   **TTS 朗读**：接入系统 TTS 语音引擎，中英文多语速/音调朗读，解放双眼。
*   **护眼提醒**：开启后每 20 分钟提醒用户适当休息，保护视力。

---

## 📂 项目架构

我们采用 **Clean Architecture** 分层架构，实现关注点分离与高度解耦：

**`UI` → `Domain` → `Data`**

### 1. UI 层
*   **职责**：负责页面渲染与用户交互。
*   **构成**：`ui/screens/` 按屏幕维度组织（`library`, `reader`, `wheel` 等），每个 Screen 包含其 Composable 与 `*ViewModel`。根导航由 `Navigation.kt` 与 `FlowReaderApp.kt` 统一管理。

### 2. Domain 层
*   **职责**：定义业务规则与跨数据源编排，是核心逻辑所在。
*   **构成**：`domain/model/` 存放数据载体（如 `Book`, `ImportResult`, `AppException`）；`domain/repository/` 按接口一文件定义数据仓库契约；`domain/usecase/` 封装业务用例（如 `ImportBookUseCase`, `GetBookUseCase`, `SaveProgressUseCase`）。
*   **导入边界**：书籍导入统一通过 `ImportBookUseCase` 编排，解析器只负责识别格式、提取章节、复制内部文件，避免 UI 直接接触文件系统细节。

### 3. Data 层
*   **职责**：负责数据的获取与持久化。
*   **构成**：`data/local/` 基于 **Room** 实现本地数据库存储（含 DAO、Entity）；`data/repository/` 提供 Repository 接口的具体实现。
*   **恢复边界**：备份恢复会重新生成本地数据库主键，并重建书籍、章节、书签、分类之间的映射，外部备份中的内部文件路径不会被直接信任。

### 4. Util 层
*   **职责**：承载解析、全文检索、缓存、TTS 与安全文件命名等无 UI 状态的工具能力。
*   **构成**：`BookParser` 负责 EPUB/TXT/PDF/Markdown 解析与内部文件复制；`SafeFileNames` 负责清洗导入文件名，避免路径穿越和覆盖现有内部文件。

---

### 5. Domain Layer (领域层)
*   **职责**：封装业务用例，实现跨 Repository 的业务逻辑编排。
*   **构成**：`domain/usecase/` 目录包含各种业务用例类：
    *   `ImportBookUseCase` - 书籍导入用例
    *   `GetBookUseCase` - 获取书籍信息用例
    *   `SaveProgressUseCase` - 保存阅读进度用例
    *   `DeleteBookUseCase` - 删除书籍用例（包含关联数据清理）
    *   `UpdateBookUseCase` - 更新书籍元数据和封面用例
    *   `SearchBooksUseCase` - 搜索书籍和批注用例
    *   `ManageAnnotationUseCase` - 管理批注用例（增删改查和导出）
    *   `TextPaginator` - 文本分页器

## 🌳 项目目录结构

```
flowreader/
│
├── .github/workflows           # CI/CD 工作流 (GitHub Actions)
├── app/
│   ├── build.gradle.kts        # App 级别构建配置
│   ├── proguard-rules.pro      # R8/ProGuard 混淆规则
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/flowreader/app/
│       │   │   ├── MainActivity.kt                    # 应用主入口，设置 Compose Content
│       │   │   ├── FlowReaderApplication.kt           # @HiltAndroidApp 注入点
│       │   │   │
│       │   │   ├── data/                            # 数据层
│       │   │   │   ├── local/
│       │   │   │   │   ├── AppDatabase.kt           # Room 数据库 (v4)
│       │   │   │   │   ├── dao/                     # Data Access Objects
│       │   │   │   │   │   ├── AnnotationDao.kt
│       │   │   │   │   │   ├── BookDao.kt
│       │   │   │   │   │   ├── BookmarkDao.kt
│       │   │   │   │   │   ├── CategoryDao.kt
│       │   │   │   │   │   ├── ChapterDao.kt
│       │   │   │   │   │   └── ReadingStatsDao.kt
│       │   │   │   │   └── entity/                  # 数据库实体
│       │   │   │   │       ├── AnnotationEntity.kt
│       │   │   │   │       ├── BookEntity.kt
│       │   │   │   │       ├── BookmarkEntity.kt
│       │   │   │   │       ├── CategoryEntity.kt
│       │   │   │   │       ├── ChapterEntity.kt
│       │   │   │   │       └── ReadingStatsEntity.kt
│       │   │   │   └── repository/                  # Repository 实现
│       │   │   │       ├── AnnotationRepositoryImpl.kt
│       │   │   │       ├── BackupRepository.kt
│       │   │   │       ├── BookRepositoryImpl.kt
│       │   │   │       ├── BookmarkRepositoryImpl.kt
│       │   │   │       ├── CategoryRepositoryImpl.kt
│       │   │   │       ├── ChapterRepositoryImpl.kt
│       │   │   │       ├── ReadingStatsRepositoryImpl.kt
│       │   │   │       └── SettingsRepository.kt
│       │   │   │
│       │   │   ├── di/
│       │   │   │   └── AppModule.kt                 # Hilt 注入模块 (Database + Repository)
│       │   │   │
│       │   │   ├── domain/                          # 领域层
│       │   │   │   ├── model/                       # 业务模型
│       │   │   │   │   ├── Annotation.kt
│       │   │   │   │   ├── AppException.kt          # 统一异常处理
│       │   │   │   │   ├── Book.kt
│       │   │   │   │   ├── ReadingSettings.kt
│       │   │   │   │   ├── ReadingStats.kt
│       │   │   │   │   ├── WheelItem.kt
│       │   │   │   │   └── ImportResult.kt          # 备份导入结果模型
│       │   │   │   ├── repository/                  # 数据仓库接口（每个接口独立文件）
│       │   │   │   │   ├── AnnotationRepository.kt
│       │   │   │   │   ├── BackupRepository.kt
│       │   │   │   │   ├── BookRepository.kt
│       │   │   │   │   ├── BookmarkRepository.kt
│       │   │   │   │   ├── CategoryRepository.kt
│       │   │   │   │   ├── ChapterRepository.kt
│       │   │   │   │   └── ReadingStatsRepository.kt
│       │   │   │   └── usecase/                     # 业务用例
│       │   │   │       ├── ImportBookUseCase.kt
│       │   │   │       ├── GetBookUseCase.kt
│       │   │   │       ├── SaveProgressUseCase.kt
│       │   │   │       └── TextPaginator.kt
│       │   │   │
│       │   │   ├── ui/                              # 表现层 (UI Layer)
│       │   │   │   ├── FlowReaderApp.kt             # 根 Composable
│       │   │   │   ├── Navigation.kt                # 导航配置 (4个Tab)
│       │   │   │   ├── theme/                       # Compose 主题 (Color, Theme, Typography)
│       │   │   │   └── screens/
│       │   │   │       ├── bookdetail/              # 书籍详情页
│       │   │   │       │   ├── BookDetailScreen.kt
│       │   │   │       │   └── BookDetailViewModel.kt
│       │   │   │       ├── library/                 # 书库主页
│       │   │   │       │   ├── LibraryScreen.kt
│       │   │   │       │   └── LibraryViewModel.kt
│       │   │   │       ├── reader/                  # 阅读器页面
│       │   │   │       │   ├── ReaderScreen.kt
│       │   │   │       │   ├── ReaderViewModel.kt
│       │   │   │       │   └── components/          # 阅读器子组件
│       │   │   │       │       ├── BookmarksDialog.kt
│       │   │   │       │       ├── ChapterListDialog.kt
│       │   │   │       │       ├── Dialogs.kt
│       │   │   │       │       ├── PdfViewer.kt
│       │   │   │       │       ├── ReaderContent.kt
│       │   │   │       │       ├── ReaderControls.kt
│       │   │   │       │       └── ReaderSettingsDialog.kt
│       │   │   │       ├── settings/                # 设置页面
│       │   │   │       │   ├── SettingsScreen.kt
│       │   │   │       │   └── SettingsViewModel.kt
│       │   │   │       ├── stats/                 # 阅读统计页面
│       │   │   │       │   ├── StatsScreen.kt
│       │   │   │       │   └── StatsViewModel.kt
│       │   │   │       └── wheel/                   # 决策转盘页面
│       │   │   │           ├── WheelScreen.kt
│       │   │   │           ├── WheelViewModel.kt
│       │   │   │           └── components/
│       │   │   │               └── WheelSpinner.kt
│       │   │   │
│       │   │   └── util/                            # 工具类模块
│       │   │       ├── BookParser.kt                # 书籍解析 (EPUB/TXT/PDF/MD) 与内部复制
│       │   │       ├── CacheManager.kt              # LRU 缓存管理
│       │   │       ├── FullTextSearch.kt            # 全文搜索 (FTS5)
│       │   │       ├── MemoryManager.kt             # 内存压力监控与管理
│       │   │       ├── SafeFileNames.kt             # 导入/封面文件安全命名
│       │   │       └── TtsManager.kt                # TTS 朗读管理
│       │   │
│       │   └── res/                               # Android 资源文件
│       │       ├── drawable/
│       │       ├── mipmap-*/
│       │       ├── values/ (含多种语言 strings: en, ja, ko, zh)
│       │       └── xml/
│       │
│       └── test/java/com/flowreader/app/util/     # 单元测试
│           └── BookParserTest.kt
│
├── build.gradle.kts              # 项目根目录构建脚本
├── gradle.properties             # Gradle 属性配置
├── gradlew / gradlew.bat       # Gradle Wrapper
├── settings.gradle.kts           # 项目配置
├── .editorconfig                 # 代码格式统一配置
└── README.md                     # 本文件
```

---

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| **UI** | Jetpack Compose + Material 3 (Material You) |
| **架构** | MVVM + Clean Architecture |
| **依赖注入** | Hilt |
| **异步** | Kotlin Coroutines + Flow |
| **数据库** | Room + SQLite (FTS5 全文检索) |
| **PDF 渲染** | 内置 PDF 渲染引擎 |
| **构建系统** | Gradle (AGP 8.6.0) |

---

## 🚀 快速开始

### 环境要求
- 操作系统: Windows / macOS / Linux
- JDK: 17 (Temurin 推荐)
- Android SDK: 35
- Gradle: 8.7 (已包含 Wrapper)

### 构建与运行

```bash
# 1. 克隆仓库
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

# 2. 构建 Debug 版本 (用于开发调试)
./gradlew assembleDebug

# 3. 构建 Release 版本 (启用 R8 混淆压缩)
./gradlew assembleRelease

# 4. 运行单元测试
./gradlew testDebugUnitTest

# 5. 运行代码质量检查
./gradlew lintDebug
```

### 开发分支说明

*   **main** - 主分支，包含稳定的发布版本
*   **dev** - 开发分支，包含最新的开发特性和重构代码
*   **feature/*** - 功能分支，用于开发新功能
*   **fix/*** - 修复分支，用于修复问题

### 安全与本地数据边界
*   **离线优先**：应用不提供网络同步能力，所有书籍、进度、书签与设置保存在本地。
*   **安全导入**：外部 URI 的显示名只作为标题来源，内部复制前会移除路径分隔符、控制不安全字符，并在重名时生成唯一文件名。
*   **安全恢复**：备份 JSON 恢复时不复用外部主键、封面路径或书籍文件路径，避免覆盖现有记录或引入伪造内部路径。
---

## 📝 近期更新日志

### v43 (最新开发版)
*   **架构重构**：新增多个 UseCase 类，进一步完善 Clean Architecture 分层
    *   `DeleteBookUseCase` - 处理书籍删除及关联数据清理
    *   `UpdateBookUseCase` - 处理书籍元数据和封面更新
    *   `SearchBooksUseCase` - 统一处理搜索功能
    *   `ManageAnnotationUseCase` - 完整的批注管理功能
*   **日志优化**：为所有业务逻辑层添加详细的日志记录，便于调试和问题排查
*   **错误处理增强**：统一使用 Result 类型处理错误，提供更好的错误信息
*   **CI/CD 改进**：
    *   支持 dev 分支的自动化构建和测试
    *   新增代码质量检查（Lint）步骤
    *   优化 Gradle 缓存策略，提升构建速度
    *   测试和构建结果自动上传为 Artifacts

### v42
*   **架构整理**：Repository 契约拆分为独立接口文件，书籍导入流程下沉到 `ImportBookUseCase`，ViewModel 只保留 UI 状态管理。
*   **导入安全**：新增 `SafeFileNames`，统一清洗导入书籍与封面文件名，避免路径穿越与内部文件覆盖。
*   **备份安全**：备份恢复重新生成本地主键并重建关联映射，不再信任外部备份中的内部文件路径。
*   **备份可靠性**：备份导出现在包含章节内容，导入兼容旧备份缺失章节字段的情况。
*   **交互体验**：全面优化页面交互动画，列表项添加 `AnimatedVisibility` 淡入效果，交互更平滑自然。
*   **书架优化**：新增下拉刷新功能（PullToRefresh），列表加载动画优化。
*   **书籍详情**：改善 Tab 切换动画与书签删除淡出效果。
---

## 📄 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 开源。

---

## 🤝 致谢

感谢所有反馈问题与提交建议的用户。

<p align="center"> Made with ❤️ by HuZaiGong </p>
