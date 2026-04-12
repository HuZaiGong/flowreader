# FlowReader (心流阅读) - Project Context

## 项目概述

FlowReader（心流阅读）是一款基于 Android 平台的电子书阅读应用，采用 Kotlin 语言和 Jetpack Compose 构建。项目采用**离线优先**策略，支持 EPUB、TXT、PDF、Markdown 格式，提供丰富的阅读自定义选项和沉浸式的阅读体验。

**核心信息：**
- **包名：** `com.flowreader.app`
- **当前版本：** v41.0.0 (versionCode 4100)
- **Min SDK：** 26 / **Target SDK：** 35
- **许可证：** GPL-3.0
- **架构模式：** MVVM + Clean Architecture
- **开发语言：** Kotlin 2.0.21
- **构建工具：** Gradle 8.7 + AGP 8.6.0

---

## 技术栈

| 类别 | 技术 |
|------|------|
| **UI 框架** | Jetpack Compose + Material 3 |
| **依赖注入** | Hilt (Dagger) 2.50 |
| **数据库** | Room + SQLite FTS5 (全文搜索) |
| **异步处理** | Kotlin Coroutines + Flow |
| **导航** | Navigation Compose |
| **图片加载** | Coil 2.7.0 |
| **数据持久化** | DataStore Preferences |
| **EPUB 渲染** | Readium Kotlin Toolkit 3.1.2 |
| **HTML 解析** | JSoup 1.18.3 |
| **测试框架** | JUnit 4 + MockK 1.13.16 |

---

## 项目结构

```
./
├── .github/workflows/          # CI/CD: build.yml (自动构建 main 分支)
├── app/src/main/
│   ├── java/com/flowreader/app/
│   │   ├── data/               # 数据层
│   │   │   ├── local/          # Room 数据库 (DAOs + Entities)
│   │   │   └── repository/     # 仓库实现
│   │   ├── di/                 # Hilt 依赖注入模块
│   │   ├── domain/             # 领域层
│   │   │   ├── model/          # 领域模型
│   │   │   ├── repository/     # 仓库接口
│   │   │   └── usecase/        # 用例 (GetBookUseCase, SaveProgressUseCase)
│   │   ├── ui/                 # 表现层
│   │   │   ├── screens/        # 页面 (library, reader, bookdetail, settings, stats)
│   │   │   ├── theme/          # 主题样式
│   │   │   └── Navigation.kt   # 导航配置
│   │   ├── util/               # 工具类
│   │   │   ├── BookParser.kt   # 书籍解析
│   │   │   ├── TtsManager.kt   # TTS 语音朗读
│   │   │   └── FullTextSearch.kt # 全文搜索
│   │   ├── FlowReaderApplication.kt  # Application 类 (@HiltAndroidApp)
│   │   └── MainActivity.kt     # 主 Activity
│   └── assets/books/           # 预置书籍目录 (当前为空)
│
├── build.gradle.kts            # 根构建配置 (插件版本管理)
├── app/build.gradle.kts        # App 模块构建配置
├── settings.gradle.kts         # 项目设置 (仓库配置)
├── gradle.properties           # Gradle 全局配置
├── .editorconfig               # 代码格式规范
├── .gitignore                  # Git 忽略规则
└── AGENTS.md                   # AI 代理知识库
```

---

## 构建与运行

### 环境要求
- **JDK：** 17
- **Android SDK：** 34+
- **Kotlin：** 2.0.21
- **Android Gradle Plugin：** 8.6.0

### 常用命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK (启用 R8 混淆)
./gradlew assembleRelease

# 运行单元测试
./gradlew :app:testDebugUnitTest

# 清理构建产物
./gradlew clean

# 安装到连接的设备
./gradlew installDebug
```

### 构建配置要点
- **Release 构建：** 启用 `isMinifyEnabled = true` 和 `isShrinkResources = true`
- **Java 兼容性：** source/target = Java 17
- **Core Library Desugaring：** 已启用
- **ProGuard 规则：** `app/proguard-rules.pro`

---

## 架构约定

### 分层架构
1. **data 层：** Room 数据库、DataStore、仓库实现
2. **domain 层：** 领域模型、仓库接口、用例
3. **ui 层：** Compose 页面、ViewModel、主题

### 编码规范
- **MVVM 模式：** 每个页面采用 Screen + ViewModel 模式
- **依赖注入：** 使用 `@HiltAndroidApp`，ViewModel 通过 `hiltViewModel()` 获取
- **状态管理：** 使用 `StateFlow` 管理 UI 状态
- **持久化：** Room (结构化数据) + DataStore (偏好设置)
- **进度保存：** 3 秒 debounce 减少数据库写入频率

### 命名注意
- `FlowReaderApplication` - Application 类
- `FlowReaderRoot` - 根 Composable 函数

---

## 核心功能模块

| 模块 | 说明 |
|------|------|
| **书库管理** | 书籍导入、排序、分类、搜索、批量导入 |
| **阅读器** | 多格式支持、主题切换、字体/行距调节、翻页动画 |
| **书签/笔记** | 书签管理、文字高亮划线、批注、导出 |
| **全文搜索** | 基于 SQLite FTS5 的单书检索 |
| **TTS 朗读** | 系统 TTS 引擎接入、语速/音调调节 |
| **阅读统计** | 时长/页数统计、目标设置、进度追踪、连续阅读天数 |
| **数据备份** | 书籍和阅读进度的备份与恢复 |

---

## 性能优化

| 优化项 | 实现方式 |
|--------|----------|
| **内存管理** | MemoryManager 实时监控，自动调节缓存策略 |
| **LRU 缓存** | CacheManager 实现，自动淘汰低优先级缓存 |
| **章节缓存** | 已读章节内存缓存，减少数据库查询 |
| **流式解析** | 书籍解析支持进度回调，大文件不卡顿 |
| **Lazy Loading** | 章节内容按需加载 |
| **分页加载** | 书籍列表分页 (3000字/页，预加载2页) |
| **图片缓存** | 内存+磁盘双缓存 |
| **R8 混淆** | Release 构建启用代码和资源压缩 |

---

## 测试

- **测试框架：** JUnit 4 + MockK
- **测试位置：** `app/src/test/java/com/flowreader/app/`
- **示例测试：** `BookParserTest.kt` (书籍解析测试)
- **UI 测试：** Compose UI 测试已配置

---

## CI/CD

- **平台：** GitHub Actions
- **工作流：** `.github/workflows/build.yml`
- **触发条件：** 
  - `main` 分支推送自动构建并发布 Release APK
  - PR 到 `main` 分支运行测试和 Debug 构建
- **权限：** `contents: read` (PR) / `contents: write` (main push)

---

## 开发注意事项

### 添加新页面
1. 在 `ui/screens/` 下创建新目录
2. 创建 Composable 页面函数
3. 创建对应的 ViewModel (使用 `@HiltViewModel`)
4. 在 `ui/Navigation.kt` 中注册路由

### 数据库变更
1. 修改 `data/local/entity/` 中的实体类
2. 更新 `AppDatabase.kt` 的版本号
3. 添加 Migration 或 fallbackToDestructiveMigration

### 添加依赖
1. 在 `app/build.gradle.kts` 的 `dependencies` 块中添加
2. 同步 Gradle
3. 注意：仓库配置在 `settings.gradle.kts` 中统一管理

### 代码风格
- 遵循 `.editorconfig` 配置
- Kotlin 官方编码规范
- Compose 最佳实践

---

## 已知问题与技术债

- **UseCase 层：** 部分用例已实现，但尚未在 ViewModel 中充分使用
- **SettingsRepository：** 职责较重 (220+ 行)，可拆分为更小的专用 Repository
- **弃用 API 警告：** 部分 Compose API 使用了旧版本 (如 `Icons.Filled.*`、`LinearProgressIndicator` 等)

---

## 版本历史 (近期)

| 版本 | 主要变更 |
|------|----------|
| **v41** | 决策转盘功能、底部导航优化、版本规范 |
| **v40.1** | 高亮交互修复、章节跳转修复 |
| **v40** | TTS 功能、语义化版本、单元测试、代码清理 |
| **v36** | 空安全修复、CI 修复、过期 API 清理 |
| **v35** | 离线优先策略、代码治理、统一异常处理、架构优化 |
| **v30** | Markdown 支持、JUnit5 测试框架、R8 混淆、Edge-to-edge |
