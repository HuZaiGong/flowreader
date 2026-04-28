# FlowReader (心流阅读) - 项目总体总结书

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=flat&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue?style=flat&logo=kotlin" alt="Language">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=flat" alt="License">
  <img src="https://img.shields.io/badge/Version-41.0.0-green?style=flat" alt="Version">
  <img src="https://img.shields.io/badge/MinSDK-26+-red?style=flat" alt="MinSDK">
</p>

---

## 📋 目录

1. [项目概述](#1-项目概述)
2. [技术栈](#2-技术栈)
3. [架构设计](#3-架构设计)
4. [功能特性](#4-功能特性)
5. [数据库设计](#5-数据库设计)
6. [导航结构](#6-导航结构)
7. [项目结构](#7-项目结构)
8. [构建与部署](#8-构建与部署)
9. [性能优化](#9-性能优化)
10. [测试策略](#10-测试策略)
11. [版本历史](#11-版本历史)
12. [代码质量](#12-代码质量)

---

## 1. 项目概述

### 1.1 项目简介

**FlowReader（心流阅读）** 是一款基于 Jetpack Compose 构建的 Android 电子书阅读应用，致力于为用户提供简洁、优雅、沉浸式的阅读体验。

| 项目信息 | 详情 |
|----------|------|
| **项目名称** | FlowReader（心流阅读） |
| **平台** | Android |
| **当前版本** | v41.0.0 (versionCode: 4100) |
| **许可证** | GNU General Public License v3.0 |
| **仓库地址** | https://github.com/HuZaiGong/flowreader |
| **最低支持** | Android 8.0 (API 26) |
| **目标/编译版本** | Android 14 (API 34/35) |

### 1.2 项目定位

FlowReader 采用 **离线优先（Offline-First）** 设计理念，用户导入本地书籍后无需网络连接即可阅读。应用支持主流电子书格式（EPUB、TXT、PDF、Markdown），并提供丰富的个性化设置和阅读统计功能。

### 1.3 核心特性概览

- 📚 **多格式支持**：EPUB、TXT、PDF、Markdown
- 🎨 **丰富主题**：9种阅读主题 + 护眼模式
- 📊 **阅读统计**：每日阅读时长、页数、进度追踪
- 🔖 **书签笔记**：高亮标注、想法批注、书签管理
- 🔍 **全文搜索**：单本书籍内快速检索
- 🔊 **语音朗读**：TTS 文本转语音功能
- 🎡 **决策转盘**：解决阅读选择困难
- ⚡ **性能优化**：智能缓存、内存管理、流式解析

---

## 2. 技术栈

### 2.1 构建工具

| 工具 | 版本 | 说明 |
|------|------|------|
| **Android Gradle Plugin** | 8.6.0 | Android 构建系统 |
| **Kotlin** | 2.0.21 | 编程语言 |
| **Gradle** | 8.7 | 构建工具 |
| **KSP** | 2.0.21-1.0.27 | 注解处理器 |

### 2.2 核心依赖

| 类别 | 技术栈 | 版本 | 用途 |
|------|--------|------|------|
| **UI 框架** | Jetpack Compose (BOM) | 2024.12.01 | 声明式 UI |
| | Material 3 | - | Material Design 3 组件 |
| | Activity Compose | 1.9.3 | Compose 活动支持 |
| **架构** | Hilt (DI) | 2.50 | 依赖注入 |
| | Navigation Compose | 2.8.5 | 导航组件 |
| | ViewModel Compose | 2.8.7 | MVVM 架构 |
| **数据持久化** | Room | 2.6.1 | 本地数据库 (SQLite) |
| | DataStore Preferences | 1.1.1 | 键值对存储 |
| **异步处理** | Kotlin Coroutines | 1.9.0 | 协程支持 |
| | Coroutines Android | 1.9.0 | Android 协程扩展 |
| **图片加载** | Coil Compose | 2.7.0 | 图片加载缓存 |
| **HTML 解析** | JSoup | 1.18.3 | HTML 文档解析 |
| **EPUB 渲染** | Readium Kotlin Toolkit | 3.1.2 | 专业 EPUB 渲染引擎 |
| **核心工具** | AndroidX Core KTX | 1.13.1 | Kotlin 扩展 |
| | Lifecycle KTX | 2.8.7 | 生命周期管理 |

### 2.3 测试框架

| 框架 | 版本 | 用途 |
|------|------|------|
| **JUnit 4** | 4.13.2 | 单元测试框架 |
| **MockK** | 1.13.16 | Kotlin Mock 框架 |
| **Coroutines Test** | 1.9.0 | 协程测试 |
| **Espresso** | 3.6.1 | UI 测试 |
| **Compose UI Test** | BOM 2024.12.01 | Compose UI 测试 |

---

## 3. 架构设计

### 3.1 整体架构

FlowReader 采用 **MVVM + Clean Architecture（整洁架构）** 模式，将代码分为三层：

```
┌─────────────────────────────────────────────────────┐
│                   UI Layer (表现层)                  │
│  ┌─────────────────────────────────────────────┐   │
│  │  Screens (Composable) + ViewModels         │   │
│  │  - LibraryScreen, ReaderScreen, Settings... │   │
│  └─────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────┤
│                 Domain Layer (领域层)                │
│  ┌─────────────────────────────────────────────┐   │
│  │  Models + Repository Interfaces + Use Cases  │   │
│  │  - Book, Chapter, ReadingSettings          │   │
│  └─────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────┤
│                  Data Layer (数据层)                 │
│  ┌─────────────────────────────────────────────┐   │
│  │  Room Database + Repository Implementations  │   │
│  │  - DAOs, Entities, Repositories            │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### 3.2 依赖注入 (Hilt)

使用 Hilt 进行依赖注入，关键配置：

- **Application 类**：`@HiltAndroidApp` 注解
- **ViewModel**：通过 `hiltViewModel()` 获取
- **模块配置**：
  - `DatabaseModule`：提供 Room 数据库和 DAO
  - `RepositoryModule`：绑定 Repository 接口与实现

### 3.3 数据流

```
User Action → ViewModel → Use Case → Repository → Data Source (Room/DataStore)
                ↓
           UI State (StateFlow) → Composable (UI 更新)
```

### 3.4 状态管理

- 使用 **StateFlow** 作为 UI 状态容器
- Compose 通过 `collectAsState()` 监听状态变化
- ViewModel 处理业务逻辑和状态更新
- **防抖处理**：阅读进度保存使用 3 秒 debounce

---

## 4. 功能特性

### 4.1 📚 书库管理 (LibraryScreen)

| 功能 | 说明 |
|------|------|
| **导入书籍** | 支持单本/批量导入本地书籍 |
| **格式支持** | EPUB、TXT、PDF、Markdown |
| **书架展示** | 网格/列表视图，分类浏览 |
| **最近阅读** | 自动记录并显示最近阅读书籍 |
| **进度保存** | 阅读进度自动保存（3秒延迟写入） |
| **搜索功能** | 按书名、作者搜索书籍 |
| **排序功能** | 按添加时间/阅读时间/书名/作者排序 |
| **元数据编辑** | 编辑书籍作者、描述、封面 |
| **删除管理** | 支持单本/批量删除书籍 |
| **下拉刷新** | Material 3 PullToRefresh 组件 |

### 4.2 📖 阅读体验 (ReaderScreen)

#### 阅读主题

| 主题 | 说明 |
|------|------|
| **浅色 (LIGHT)** | 白色背景，黑色文字 |
| **深色 (DARK)** | 深色背景，浅色文字 |
| **羊皮纸 (SEPIA)** | 护眼暖色调 |
| **护眼 (PAPER)** | 纸张质感背景 |
| **纯黑 (AMOLED)** | 纯黑背景，省电 |
| **系统 (SYSTEM)** | 跟随系统主题 |
| **清晨 (MORNING)** | 适合早晨阅读 |
| **正午 (NOON)** | 明亮清晰 |
| **傍晚 (EVENING)** | 柔和暖光 |
| **夜晚 (NIGHT)** | 夜间模式 |

#### 字体设置

| 设置项 | 选项 |
|--------|------|
| **字体大小** | 12sp - 32sp 可调 |
| **字体类型** | 默认、衬线体、无衬线、等宽、宋体、黑体、楷体、仿宋 |
| **行间距** | 1.0 - 2.5 可调 |
| **段间距** | 紧凑、标准、宽松、沉浸模式 |
| **首行缩进** | 支持开关 |
| **文字对齐** | 支持两端对齐 |

#### 翻页模式

| 模式 | 说明 |
|------|------|
| **滑动 (SLIDE)** | 左右滑动翻页 |
| **仿真 (SIMULATION)** | 仿真翻页效果 |
| **无动画 (NONE)** | 瞬间切换 |
| **卷曲 (CURL)** | 卷曲翻页效果 |
| **滑动覆盖 (SLIDE_OVER)** | 滑动覆盖效果 |

#### 交互设置

- **点击区域**：左30%上一页，中间呼出菜单，右30%下一页
- **章节记忆**：切换章节自动恢复滚动位置
- **进度条**：底部可拖拽，快速跳转章节
- **屏幕常亮**：阅读时保持屏幕常亮
- **自动夜间模式**：根据时间自动切换深色/浅色主题
- **边缘手势**：自定义边缘滑动区域，避免与系统返回手势冲突
- **背景纹理**：无、纸张、画布、木纹、大理石、渐变
- **环境音效**：无、雨声、风声、柴火、咖啡馆、海浪

### 4.3 📊 阅读统计 (StatsScreen)

- 每日阅读时长统计（图表展示）
- 每日阅读页数统计
- 阅读进度自动记录
- 阅读目标设置（每日阅读时长目标）
- 阅读速度计算（字/分钟）
- 预计剩余阅读时间
- 护眼提醒（每20分钟）

### 4.4 🔖 书签功能

- 随时添加书签（支持多种颜色）
- 书签列表管理
- 快速跳转到书签位置
- 书签删除（带淡出动画）

### 4.5 📑 目录导航

- 章节目录树形展示
- 快速跳转章节
- 当前章节指示器
- 章节进度显示

### 4.6 ✏️ 笔记与批注

- 文字高亮划线（黄、绿、蓝、粉、橙）
- 添加想法/批注
- 笔记列表管理
- 笔记导出功能
- 支持长按/点击选段后添加

### 4.7 🔍 全文搜索

- 单本书籍内全文检索
- 搜索结果高亮显示
- 快速跳转到搜索位置
- 基于 SQLite FTS5 实现

### 4.8 🔊 文本朗读 (TTS)

- 接入系统 TTS 语音引擎
- 支持调节语速和音调
- 中英文语音识别
- 播放/暂停/停止控制
- 朗读状态管理（IDLE/PLAYING/PAUSED/ERROR）

### 4.9 🎡 决策转盘 (WheelScreen)

- 可定制的决策转盘工具
- 支持自定义选项和颜色
- 加权随机算法
- 编辑模式管理选项
- 帮助解决阅读选择困难

### 4.10 ⏰ 提醒功能

- 每日阅读提醒
- 自定义提醒时间
- 本地通知推送

### 4.11 💾 数据管理

- 书籍和阅读进度备份
- 从备份文件恢复数据
- 阅读记录导出（JSON格式）
- 批量导入/导出

### 4.12 ⚙️ 个性化设置 (SettingsScreen)

- 应用主题跟随系统（Material You 动态颜色）
- 默认阅读设置（字体、主题、间距等）
- 简体中文界面
- 手势设置持久化到 DataStore
- 手势动作自定义：
  - 左/中/右点击动作
  - 左/右滑动动作
  - 双击动作
  - 长按动作
- 边缘手势宽度和阈值配置
- 系统手势排除支持

---

## 5. 数据库设计

### 5.1 数据库信息

| 属性 | 值 |
|------|-----|
| **数据库名称** | `flowreader_db` |
| **版本** | 4 |
| **类型** | Room (SQLite) |
| **导出模式** | exportSchema = false |
| **特殊功能** | FTS5 全文搜索支持 |

### 5.2 实体 (Entities)

#### BookEntity（书籍表）

| 字段 | 类型 | 说明 | 索引 |
|------|------|------|------|
| **id** | Long (PK) | 书籍ID，自增 | - |
| **title** | String | 书名 | ✅ |
| **author** | String | 作者 | ✅ |
| **filePath** | String | 文件路径 | - |
| **coverPath** | String? | 封面路径 | - |
| **description** | String | 书籍描述 | - |
| **fileSize** | Long | 文件大小 | - |
| **format** | String | 格式（EPUB/TXT/PDF/MD） | - |
| **totalChapters** | Int | 总章节数 | - |
| **currentChapter** | Int | 当前章节 | - |
| **currentPosition** | Int | 当前位置 | - |
| **readingProgress** | Float | 阅读进度 (0.0-1.0) | - |
| **lastReadTime** | Long? | 最后阅读时间 | ✅ |
| **addedTime** | Long | 添加时间 | ✅ |
| **categoryId** | Long? | 分类ID | ✅ |

#### ChapterEntity（章节表）

存储每本书的章节信息，支持章节导航和进度记录。

#### BookmarkEntity（书签表）

| 字段 | 类型 | 说明 |
|------|------|------|
| **id** | Long (PK) | 书签ID |
| **bookId** | Long | 所属书籍ID |
| **chapterIndex** | Int | 章节索引 |
| **position** | Int | 位置偏移 |
| **note** | String? | 书签备注 |
| **createdTime** | Long | 创建时间 |

#### AnnotationEntity（批注表）

| 字段 | 类型 | 说明 |
|------|------|------|
| **id** | Long (PK) | 批注ID |
| **bookId** | Long | 所属书籍ID |
| **chapterIndex** | Int | 章节索引 |
| **startPosition** | Int | 起始位置 |
| **endPosition** | Int | 结束位置 |
| **selectedText** | String | 选中文本 |
| **note** | String? | 批注内容 |
| **color** | String | 高亮颜色 |
| **createdTime** | Long | 创建时间 |

#### CategoryEntity（分类表）

| 字段 | 类型 | 说明 |
|------|------|------|
| **id** | Long (PK) | 分类ID |
| **name** | String | 分类名称 |
| **color** | Long | 分类颜色 |
| **createdTime** | Long | 创建时间 |

#### ReadingStatsEntity（阅读统计表）

| 字段 | 类型 | 说明 |
|------|------|------|
| **id** | Long (PK) | 统计ID |
| **bookId** | Long | 书籍ID |
| **date** | Long | 统计日期 |
| **durationSeconds** | Long | 阅读时长（秒） |
| **pagesRead** | Int | 阅读页数 |

### 5.3 DAO (Data Access Objects)

| DAO | 功能 |
|-----|------|
| **BookDao** | 书籍增删改查、分页查询、搜索、排序 |
| **ChapterDao** | 章节管理、章节内容获取 |
| **BookmarkDao** | 书签管理、按书籍查询 |
| **AnnotationDao** | 批注管理、按书籍/章节查询 |
| **CategoryDao** | 分类管理、分类列表 |
| **ReadingStatsDao** | 统计数据、每日统计、按日期范围查询 |

### 5.4 数据库迁移

当前版本：v4，支持从旧版本迁移。使用 Room 的自动迁移或自定义 Migration。

---

## 6. 导航结构

### 6.1 底部导航栏

应用使用底部导航栏，包含四个主要入口：

| 屏幕 | 路由 | 图标 | 说明 |
|------|------|------|------|
| **书架** | `library` | LibraryBooks | 书库管理 |
| **转盘** | `wheel` | Casino | 决策转盘 |
| **统计** | `stats` | BarChart | 阅读统计 |
| **设置** | `settings` | Settings | 应用设置 |

### 6.2 导航图 (Navigation Graph)

```
开始 → LibraryScreen (书架)
         ↓
    BookDetailScreen (书籍详情) ← 点击书籍
         ↓
    ReaderScreen (阅读器) ← 点击阅读
         ↓
    SettingsScreen (设置) ← 点击设置
    
独立页面：
    StatsScreen (统计)
    WheelScreen (转盘)
```

### 6.3 路由定义

```kotlin
sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Library : Screen("library", "书架", Icons.AutoMirrored.Filled.LibraryBooks)
    object Wheel : Screen("wheel", "转盘", Icons.Default.Casino)
    object Stats : Screen("stats", "统计", Icons.Default.BarChart)
    object BookDetail : Screen("book_detail/{bookId}", "书籍详情")
    object Reader : Screen("reader/{bookId}", "阅读")
    object Settings : Screen("settings", "设置", Icons.Default.Settings)
}
```

---

## 7. 项目结构

### 7.1 完整目录树

```
FlowReader/
├── .github/
│   └── workflows/
│       └── build.yml              # CI/CD 配置（自动构建发布）
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/flowreader/app/
│   │   │   │   ├── MainActivity.kt                    # 主活动入口
│   │   │   │   ├── FlowReaderApplication.kt           # @HiltAndroidApp 应用类
│   │   │   │   ├── data/
│   │   │   │   │   ├── local/
│   │   │   │   │   │   ├── AppDatabase.kt            # Room 数据库 (v4)
│   │   │   │   │   │   ├── dao/
│   │   │   │   │   │   │   ├── BookDao.kt            # 书籍 DAO
│   │   │   │   │   │   │   ├── ChapterDao.kt         # 章节 DAO
│   │   │   │   │   │   │   ├── BookmarkDao.kt        # 书签 DAO
│   │   │   │   │   │   │   ├── AnnotationDao.kt      # 批注 DAO
│   │   │   │   │   │   │   ├── CategoryDao.kt         # 分类 DAO
│   │   │   │   │   │   │   └── ReadingStatsDao.kt    # 统计 DAO
│   │   │   │   │   │   └── entity/
│   │   │   │   │   │       ├── BookEntity.kt          # 书籍实体
│   │   │   │   │   │       ├── ChapterEntity.kt      # 章节实体
│   │   │   │   │   │       ├── BookmarkEntity.kt     # 书签实体
│   │   │   │   │   │       ├── AnnotationEntity.kt   # 批注实体
│   │   │   │   │   │       ├── CategoryEntity.kt     # 分类实体
│   │   │   │   │   │       └── ReadingStatsEntity.kt # 统计实体
│   │   │   │   │   └── repository/
│   │   │   │   │       ├── BookRepositoryImpl.kt      # 书籍仓库实现
│   │   │   │   │       ├── ChapterRepositoryImpl.kt   # 章节仓库实现
│   │   │   │   │       ├── BookmarkRepositoryImpl.kt  # 书签仓库实现
│   │   │   │   │       ├── AnnotationRepositoryImpl.kt # 批注仓库实现
│   │   │   │   │       ├── CategoryRepositoryImpl.kt  # 分类仓库实现
│   │   │   │   │       ├── ReadingStatsRepositoryImpl.kt # 统计仓库实现
│   │   │   │   │       ├── BackupRepository.kt       # 备份仓库
│   │   │   │   │       └── SettingsRepository.kt     # 设置仓库
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt                  # Hilt 依赖注入模块
│   │   │   │   ├── domain/
│   │   │   │   │   ├── model/
│   │   │   │   │   │   ├── Book.kt                   # 书籍领域模型
│   │   │   │   │   │   ├── Chapter.kt                # 章节领域模型
│   │   │   │   │   │   ├── Bookmark.kt               # 书签领域模型
│   │   │   │   │   │   ├── Annotation.kt             # 批注领域模型
│   │   │   │   │   │   ├── Category.kt               # 分类领域模型
│   │   │   │   │   │   ├── ReadingSettings.kt        # 阅读设置（核心配置）
│   │   │   │   │   │   └── AppException.kt           # 统一异常类
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   ├── BookRepository.kt         # 书籍仓库接口
│   │   │   │   │   │   ├── ChapterRepository.kt      # 章节仓库接口
│   │   │   │   │   │   ├── BookmarkRepository.kt     # 书签仓库接口
│   │   │   │   │   │   ├── AnnotationRepository.kt   # 批注仓库接口
│   │   │   │   │   │   ├── CategoryRepository.kt     # 分类仓库接口
│   │   │   │   │   │   ├── ReadingStatsRepository.kt # 统计仓库接口
│   │   │   │   │   │   └── BackupRepository.kt       # 备份仓库接口
│   │   │   │   │   └── usecase/
│   │   │   │   │       ├── GetBookUseCase.kt          # 获取书籍用例
│   │   │   │   │       └── SaveProgressUseCase.kt     # 保存进度用例
│   │   │   │   ├── ui/
│   │   │   │   │   ├── Navigation.kt                 # Compose 导航配置
│   │   │   │   │   ├── FlowReaderRoot.kt             # 根 Composable
│   │   │   │   │   ├── theme/
│   │   │   │   │   │   ├── Color.kt                  # 颜色定义
│   │   │   │   │   │   ├── Theme.kt                  # 主题配置
│   │   │   │   │   │   └── Typography.kt             # 排版设置
│   │   │   │   │   └── screens/
│   │   │   │   │       ├── library/
│   │   │   │   │       │   ├── LibraryScreen.kt       # 书架页面
│   │   │   │   │       │   └── LibraryViewModel.kt    # 书架 ViewModel
│   │   │   │   │       ├── reader/
│   │   │   │   │       │   ├── ReaderScreen.kt        # 阅读器页面
│   │   │   │   │       │   ├── ReaderViewModel.kt     # 阅读器 ViewModel
│   │   │   │   │       │   └── components/
│   │   │   │   │       │       ├── ReaderContent.kt   # 阅读内容组件
│   │   │   │   │       │       ├── PdfViewer.kt       # PDF 阅读组件
│   │   │   │   │       │       ├── ReaderControls.kt  # 阅读控制栏
│   │   │   │   │       │       ├── ChapterListDialog.kt # 章节列表对话框
│   │   │   │   │       │       ├── BookmarksDialog.kt # 书签对话框
│   │   │   │   │       │       └── Dialogs.kt         # 其他对话框
│   │   │   │   │       ├── bookdetail/
│   │   │   │   │       │   ├── BookDetailScreen.kt    # 书籍详情页
│   │   │   │   │       │   └── BookDetailViewModel.kt # 详情 ViewModel
│   │   │   │   │       ├── settings/
│   │   │   │   │       │   ├── SettingsScreen.kt      # 设置页面
│   │   │   │   │       │   └── SettingsViewModel.kt   # 设置 ViewModel
│   │   │   │   │       ├── stats/
│   │   │   │   │       │   ├── StatsScreen.kt         # 统计页面
│   │   │   │   │       │   └── StatsViewModel.kt      # 统计 ViewModel
│   │   │   │   │       └── wheel/
│   │   │   │   │           ├── WheelScreen.kt          # 决策转盘页
│   │   │   │   │           ├── WheelViewModel.kt       # 转盘 ViewModel
│   │   │   │   │           └── components/
│   │   │   │   │               └── WheelSpinner.kt     # 转盘组件
│   │   │   │   └── util/
│   │   │   │       ├── BookParser.kt                   # 书籍解析（支持 EPUB/TXT/PDF/MD）
│   │   │   │       ├── BookLoader.kt                   # 书籍加载工具
│   │   │   │       ├── TtsManager.kt                   # TTS 语音管理器
│   │   │   │       ├── FullTextSearch.kt               # 全文搜索（FTS5）
│   │   │   │       ├── MemoryManager.kt                 # 内存管理器
│   │   │   │       ├── CacheManager.kt                 # 缓存管理器（LRU）
│   │   │   │       └── TextPaginator.kt                # 文本分页器
│   │   │   ├── assets/
│   │   │   │   └── books/                             # 预置书籍（空）
│   │   │   ├── res/                                   # Android 资源文件
│   │   │   └── AndroidManifest.xml                    # 应用清单
│   │   └── test/                                      # 单元测试
│   │       └── java/com/flowreader/app/util/
│   │           └── BookParserTest.kt                   # BookParser 测试
│   ├── build.gradle.kts                               # App 构建配置
│   └── proguard-rules.pro                             # 混淆规则
├── build.gradle.kts                                    # 根构建配置
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties                  # Gradle 8.7
├── .editorconfig                                       # 代码规范配置
├── .gitignore
├── AGENTS.md                                           # 项目知识库（AI Agent 用）
├── README.md                                           # 项目说明文档
└── LICENSE                                             # GPL v3.0 许可证
```

### 7.2 关键文件说明

| 文件 | 职责 |
|------|------|
| **FlowReaderApplication.kt** | Application 类，初始化 Hilt |
| **MainActivity.kt** | 应用主入口，设置 Compose 内容 |
| **Navigation.kt** | 定义所有导航路由和底部导航栏 |
| **AppDatabase.kt** | Room 数据库配置，版本管理 |
| **AppModule.kt** | Hilt DI 模块，提供数据库和仓库 |
| **ReadingSettings.kt** | 定义所有阅读相关的配置模型 |
| **BookParser.kt** | 解析 EPUB/TXT/PDF/MD 文件 |
| **TtsManager.kt** | 管理 Android TTS 引擎 |

---

## 8. 构建与部署

### 8.1 环境要求

| 工具 | 版本要求 |
|------|----------|
| **JDK** | 17+ |
| **Android SDK** | 34+ |
| **Kotlin** | 2.0.21+ |
| **Android Gradle Plugin** | 8.6.0+ |
| **Gradle** | 8.7 |

### 8.2 构建命令

```bash
# 克隆项目
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

# 构建 Debug APK（调试版）
./gradlew assembleDebug
# 输出：app/build/outputs/apk/debug/app-debug.apk

# 构建 Release APK（发布版，需配置签名）
./gradlew assembleRelease
# 输出：app/build/outputs/apk/release/app-release.apk

# 运行单元测试
./gradlew :app:testDebugUnitTest

# 清理构建
./gradlew clean

# 查看依赖树
./gradlew :app:dependencies
```

### 8.3 CI/CD 配置

使用 GitHub Actions 自动构建，配置文件：`.github/workflows/build.yml`

**特性**：
- 推送 main 分支自动构建
- 使用语义化版本（v41.0.0）
- 单元测试集成
- 自动发布 Release（如配置）

**权限优化**（v35 改进）：
- 使用最小权限原则
- 不再使用 `permissions: contents: write` 全局权限

### 8.4 打包配置

**Release 构建**：
- 启用代码混淆（R8）
- 启用资源压缩
- 使用 ProGuard 规则（`proguard-rules.pro`）

**Debug 构建**：
- 不启用混淆
- 包含调试信息
- 支持 Compose UI 测试

---

## 9. 性能优化

### 9.1 内存管理

| 优化项 | 实现 | 效果 |
|--------|------|------|
| **MemoryManager** | 实时监控内存压力（低/中/高/临界） | 根据压力自动调节缓存策略 |
| **CacheManager** | LRU 缓存实现，自动淘汰低优先级缓存 | 减少内存占用，防止 OOM |
| **章节内存缓存** | 缓存已读取章节内容 | 减少数据库查询，加快切换速度 |
| **图片内存缓存** | Coil 双缓存（内存+磁盘） | 滚动更流畅，减少重复加载 |

### 9.2 数据库优化

| 优化项 | 实现 |
|--------|------|
| **复合索引** | 为 `lastReadTime`, `addedTime`, `categoryId`, `title`, `author` 创建索引 |
| **分页查询** | `getBooksPaged()` 支持分页加载书籍列表 |
| **FTS5 全文搜索** | SQLite FTS5 虚拟表，快速全文检索 |
| **延迟写入** | 阅读进度 3 秒 debounce，减少数据库写入次数 |
| **流式查询** | Flow 响应式查询，数据变化自动更新 UI |

### 9.3 UI 渲染优化

| 优化项 | 实现 | 效果 |
|--------|------|------|
| **Lazy Loading** | 章节内容按需加载 | 首屏加载更快 |
| **TextPaginator** | 3000 字/页，预加载 2 页 | 大文本流畅滚动 |
| **derivedStateOf** | 缓存计算值 | 减少重组次数 |
| **AnimatedVisibility** | 列表项淡入动画 | 提升视觉体验 |
| **悬浮功能栏** | 不遮挡内容区 | 更好的阅读体验 |

### 9.4 文件解析优化

| 优化项 | 实现 |
|--------|------|
| **流式解析** | BookParser 支持进度回调，大文件不再卡顿 |
| **PDF 流式渲染** | 使用 Android PdfRenderer，大型 PDF 流畅加载 |
| **Readium 引擎** | 专业 EPUB 渲染，支持复杂 CSS/排版 |
| **封面提取** | 异步提取 EPUB/PDF 封面，不阻塞主线程 |

### 9.5 其他优化

| 优化项 | 说明 |
|--------|------|
| **Edge-to-edge** | 支持边缘到边缘显示（v30） |
| **Splash Screen** | 支持启动屏（v30） |
| **Material You** | 动态颜色支持（v11） |
| **R8 混淆** | Release 构建代码压缩和优化（v30） |

---

## 10. 测试策略

### 10.1 单元测试

**测试框架**：
- JUnit 4（测试运行器）
- MockK（Mock 对象）
- Coroutines Test（协程测试）

**测试位置**：
```
app/src/test/java/com/flowreader/app/
└── util/
    └── BookParserTest.kt    # 书籍解析测试
```

**运行测试**：
```bash
./gradlew :app:testDebugUnitTest
```

### 10.2 UI 测试

**测试框架**：
- Espresso（传统 View 测试）
- Compose UI Test（Compose 测试）

**测试配置**：
```kotlin
androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
androidTestImplementation("androidx.compose.ui:ui-test-junit4")
debugImplementation("androidx.compose.ui:ui-test-manifest")
```

### 10.3 CI 集成

GitHub Actions 自动运行单元测试：
```yaml
- name: Run Unit Tests
  run: ./gradlew :app:testDebugUnitTest
```

---

## 11. 版本历史

### v42（开发中）
- **交互优化**：全面优化页面交互逻辑和动画效果
- **书架优化**：新增下拉刷新功能，优化书籍列表加载动画
- **书籍详情**：改善 Tab 切换动画效果，添加书签删除淡出动画
- **动画增强**：为列表项添加 AnimatedVisibility 淡入效果
- **组件升级**：使用 Material 3 PullToRefresh 组件替代旧版
- **用户体验**：优化过渡动画，使交互更平滑自然

### v41
- **决策转盘**：新增可定制的决策转盘功能，帮助解决阅读选择困难
- **底部导航**：优化底部导航栏，增加转盘入口
- **版本规范**：规范版本号为语义化版本控制

### v40.1
- **高亮修复**：优化高亮功能交互，长按/点击段落后手动输入文本再添加高亮
- **章节跳转修复**：修复跳转下一章时滚动位置重置问题

### v40
- **TTS 功能**：修复语音朗读功能，添加朗读/停止按钮
- **版本规范**：规范版本号为 40.0.0，CI 使用语义化版本
- **单元测试**：GitHub Actions 集成单元测试
- **代码清理**：移除 DataManager 中 Sync 残留代码

### v35（重要架构改进）
- **离线优先**：移除账号系统、云端同步等网络功能，纯本地运行
- **代码治理**：重命名 `FlowReaderApp` → `FlowReaderApplication`（Application 类）
- **代码治理**：重命名 Composable `FlowReaderApp` → `FlowReaderRoot`
- **代码规范**：添加 `.editorconfig` 代码规范配置
- **CI 优化**：优化 CI permissions，按需授权（最小权限原则）
- **异常处理**：新增 `AppException.kt` 统一异常处理机制
- **架构优化**：新增 `domain/usecase/` 层（GetBookUseCase, SaveProgressUseCase）
- **性能优化**：新增 TextPaginator 分页加载 (3000字/页, 预加载2页)
- **进度防抖**：3秒延迟保存减少数据库写入

### v30
- **Markdown 支持**：新增 .md 格式解析支持
- **测试基础设施**：引入 JUnit 5 + MockK 测试框架
- **构建优化**：Release 启用 R8 混淆压缩
- **现代 Android 适配**：Edge-to-edge, Splash Screen 支持

### v29
- **UI 组件拆分重构**：ReaderScreen 拆分为独立组件模块
- **性能增强**：智能阅读预测、阅读速度跟踪、护眼提醒
- **书籍分类**：支持分类筛选、添加、删除
- **代码模块化**：提升可维护性和可测试性

### v28
- **内存管理优化**：新增 MemoryManager，实时监控内存状态
- **智能缓存管理**：新增 CacheManager，实现 LRU 缓存
- **分页加载支持**：新增 getBooksPaged() 支持分页加载

### v25
- **文本高亮/笔记**：新增高亮、想法、批注功能
- **阅读统计页面**：新增阅读时长、页数统计
- **书籍封面提取**：自动提取 EPUB/PDF 封面
- **阅读进度分享**：支持分享阅读进度

### v24
- **多语言支持**：中文、英语、日语、韩语
- **章节内存缓存**：提升切换速度
- **数据库优化**：索引优化、图片缓存优化

### v19
- **版本兼容性修复**：修复 Kotlin/Compose 版本兼容性问题
- **AGP 升级**：更新至 8.6.0
- **Kotlin 升级**：更新至 2.0.21
- **Gradle 升级**：更新至 8.7
- **SDK 升级**：compileSdk/targetSdk 至 35

### v17（重要功能更新）
- **Readium 集成**：新增 Readium Kotlin Toolkit EPUB 渲染引擎
- **边缘手势**：新增边缘手势识别设置
- **笔记/批注**：新增高亮、想法、导出功能
- **全文搜索**：单本书籍内 FTS 检索
- **TTS 功能**：文本朗读功能上线
- **性能优化**：提升大型书籍解析速度

### v11
- **PDF 支持**：新增 PDF 格式支持
- **阅读统计**：新增阅读时长、页数统计
- **Material You**：动态颜色支持
- **备份/恢复**：数据备份和恢复功能

---

## 12. 代码质量

### 12.1 编码规范

| 规范 | 说明 |
|------|------|
| **Kotlin 风格** | 遵循 Kotlin 官方编码规范 |
| **.editorconfig** | 统一编辑器配置（v35 新增） |
| **命名规范** | 使用驼峰命名法，见名知意 |
| **注释规范** | 关键逻辑添加注释，使用 KDoc |
| **架构规范** | 严格遵循 MVVM + Clean Architecture |

### 12.2 设计模式

| 模式 | 应用 |
|------|------|
| **MVVM** | ViewModel + StateFlow + Compose |
| **Repository** | 数据仓库模式，隔离数据源 |
| **Singleton** | TtsManager, MemoryManager, CacheManager |
| **Factory** | Hilt 依赖注入工厂 |
| **Observer** | StateFlow 响应式编程 |

### 12.3 已知问题与改进

#### 已修复问题

| 问题 | 版本 | 修复方案 |
|------|------|----------|
| **命名冲突** | v35 | `FlowReaderApp` 重命名为 `FlowReaderApplication` 和 `FlowReaderRoot` |
| **CI 权限过宽** | v35 | 使用最小权限原则配置 GitHub Actions |
| **空安全断言** | v36 | 修复 ReaderViewModel 中的 `!!` 问题 |
| **过期 Icons** | v36 | 移除过期 `Icons.Filled` 使用 |

#### 待改进项

| 问题 | 说明 | 建议 |
|------|------|------|
| **无 Lint 配置** | 缺少 lint.xml 或 detekt 配置 | 添加静态代码分析工具 |
| **测试覆盖不足** | 仅 BookParser 有单元测试 | 增加 ViewModel 和 Repository 测试 |
| **UseCase 层待完善** | domain/usecase/ 只有两个用例 | 完善所有业务逻辑用例 |
| **无自动化 UI 测试** | 未配置完整的 UI 测试 | 添加关键流程的 UI 测试 |

### 12.4 代码质量工具

| 工具 | 状态 | 说明 |
|------|------|------|
| **.editorconfig** | ✅ | 代码风格统一（v35 新增） |
| **ktlint** | ❌ | 未集成，建议添加 |
| **detekt** | ❌ | 未集成，建议添加 |
| **lint.xml** | ❌ | 未配置，建议添加 |
| **R8 混淆** | ✅ | Release 构建启用 |

### 12.5 安全考虑

| 项目 | 状态 | 说明 |
|------|------|------|
| **CI 权限** | ✅ 已优化 | v35 使用最小权限原则 |
| **数据备份** | ✅ 本地 | 离线优先，无云端数据传输 |
| **隐私权限** | ✅ 最小化 | 仅读取存储权限，无网络权限 |
| **ProGuard** | ✅ | Release 构建代码混淆 |

---

## 附录 A：核心配置参考

### A.1 ReadingSettings 完整配置

```kotlin
data class ReadingSettings(
    val fontSize: Int = 18,                    // 字体大小
    val lineSpacing: Float = 1.5f,             // 行间距
    val paragraphSpacing: Float = 1.0f,        // 段间距
    val paragraphMode: ParagraphMode = STANDARD, // 段落模式
    val fontFamily: FontFamily = DEFAULT,       // 字体类型
    val customFontPath: String? = null,         // 自定义字体路径
    val theme: ReaderTheme = LIGHT,            // 阅读主题
    val pageMode: PageMode = SLIDE,            // 翻页模式
    val keepScreenOn: Boolean = true,          // 屏幕常亮
    val screenTimeoutMinutes: Int = 0,         // 屏幕超时（0=无限）
    val tapZoneRatio: Float = 0.3f,           // 点击区域比例
    val gestureSettings: GestureSettings,       // 手势设置
    val backgroundTexture: BackgroundTexture,   // 背景纹理
    val backgroundColor: Long = 0xFFF5F5DC,  // 背景颜色
    val textColor: Long = 0xFF000000,         // 文字颜色
    val autoHideControls: Boolean = true,      // 自动隐藏控制栏
    val controlsHideDelay: Long = 3000L,      // 控制栏隐藏延迟
    val fullScreenMode: Boolean = true,        // 全屏模式
    val ambientSound: AmbientSound = NONE,     // 环境音效
    val ambientSoundVolume: Float = 0.5f,      // 音效音量
    val firstLineIndent: Boolean = true,       // 首行缩进
    val justifyText: Boolean = true,           // 两端对齐
    val simplifiedChinese: Boolean = true      // 简体中文转换
)
```

### A.2 GestureSettings 手势配置

```kotlin
data class GestureSettings(
    val leftTapAction: GestureAction = PREVIOUS_PAGE,     // 左点击
    val middleTapAction: GestureAction = TOGGLE_CONTROLS, // 中点击
    val rightTapAction: GestureAction = NEXT_PAGE,        // 右点击
    val swipeLeftAction: GestureAction = NEXT_PAGE,       // 左滑
    val swipeRightAction: GestureAction = PREVIOUS_PAGE,  // 右滑
    val doubleTapAction: GestureAction = SHOW_SETTINGS,    // 双击
    val longPressAction: GestureAction = ADD_BOOKMARK,     // 长按
    val edgeGestureEnabled: Boolean = true,                // 边缘手势
    val leftEdgeWidth: Int = 20,                           // 左边缘宽度
    val rightEdgeWidth: Int = 20,                          // 右边缘宽度
    val edgeSwipeThreshold: Int = 100,                     // 边缘滑动阈值
    val enableSystemGestureExclusion: Boolean = true,      // 系统手势排除
    val preventSystemBackGesture: Boolean = true           // 阻止系统返回
)
```

---

## 附录 B：第三方库许可

| 库 | 许可证 |
|----|--------|
| **Jetpack Compose** | Apache 2.0 |
| **Hilt (Dagger)** | Apache 2.0 |
| **Room** | Apache 2.0 |
| **Kotlin Coroutines** | Apache 2.0 |
| **Coil** | Apache 2.0 |
| **JSoup** | MIT |
| **Readium Kotlin Toolkit** | BSD-3-Clause |
| **MockK** | Apache 2.0 |

---

## 总结

FlowReader 是一款功能完善、架构清晰的 Android 电子书阅读应用。通过采用现代化的技术栈（Jetpack Compose + MVVM + Hilt）和多项性能优化措施，应用能够为读者提供流畅、沉浸的阅读体验。

**核心优势**：
- ✅ 支持主流电子书格式
- ✅ 丰富的个性化设置
- ✅ 完善的阅读统计
- ✅ 强大的笔记批注功能
- ✅ 优秀的性能表现
- ✅ 清晰的架构设计
- ✅ 活跃的版本迭代

**未来展望**：
- 增加更多书籍来源（WebDav、网盘同步）
- 完善测试用例覆盖
- 添加更多主题和字体
- 支持更多电子书格式
- 优化 PDF 渲染性能

---

<p align="center"> Made with ❤️ by HuZaiGong </p>
