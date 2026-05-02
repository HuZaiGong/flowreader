# README 文档偏差清单

> 生成日期：2026-05-02
> 对比对象：README.md、AGENTS.md、PROJECT_SUMMARY.md、index.html vs 实际代码库

---

## README.md 偏差

### 1. 版本号过时
- **文档声明**: `Version-34.0.0`（badge，第 7 行）
- **实际**: `app/build.gradle.kts:18` → `versionName = "41.0.0"`, `versionCode = 4100`
- **涉及**: README 第 7 行、index.html 第 1086 行

### 2. 阅读主题数量错误
- **文档声明**: "5 reading themes: light, dark, sepia, eye-care, AMOLED black"（第 69 行）
- **实际**: `ReadingSettings.kt:3-14` 的 `ReaderTheme` 枚举有 **10 种主题**: LIGHT, DARK, SEPIA, PAPER, AMOLED, SYSTEM, MORNING, NOON, EVENING, NIGHT
- **涉及**: README 第 69 行、AGENTS.md 第 69 行、index.html 主题描述

### 3. 环境要求版本过低
- **文档声明**: "Kotlin 1.9+", "Android Gradle Plugin 8.1+", "Android SDK 34+"（第 193-196 行）
- **实际**: Kotlin **2.0.21**, AGP **8.6.0**, compileSdk/targetSdk = **35**（`build.gradle.kts:4,11,16`）
- **涉及**: README 第 193-196 行、index.html 第 1086 行

### 4. compileSdk/targetSdk 错误
- **文档声明**: MinSDK 26+（badge 正确），但未明确标注 compileSdk/targetSdk
- **PROJECT_SUMMARY.md 声明**: "目标/编译版本: Android 14 (API 34/35)"（第 44 行）
- **实际**: `compileSdk = 35`, `targetSdk = 35`（均为 35）

### 5. 测试框架描述矛盾
- **文档声明**: changelog v30 "引入 JUnit 5 + MockK 测试框架"（第 242 行）
- **实际**: `app/build.gradle.kts:113` 使用 `junit:junit:4.13.2`（JUnit **4**），测试文件 `BookParserTest.kt:4-5` 确认使用 `org.junit.Test`（JUnit 4）
- **涉及**: README 第 242 行、PROJECT_SUMMARY.md 第 794 行

### 6. 预置书籍目录描述错误
- **文档声明**: `assets/books/` 注释为"预置书籍"（第 155 行）
- **实际**: `app/src/main/assets/books/` 目录为空（glob 返回 No files found）
- **PROJECT_SUMMARY.md**: 第 561 行写"预置书籍（空）"是正确的，但 README 未标注

### 7. Application 类名过时
- **文档声明**: changelog v35 声称已重命名 `FlowReaderApp` → `FlowReaderApplication`（第 231 行）
- **但**: README 项目结构（第 146 行）仍写 `FlowReaderApp.kt`
- **实际文件名**: `FlowReaderApplication.kt`（`AndroidManifest.xml:12` 确认）

### 8. screens 列表不完整
- **文档声明**: 项目结构（第 148-153 行）只列出 library, reader, bookdetail, settings
- **实际**: 还有 `stats/` 和 `wheel/` 两个 screen 目录

### 9. util 目录列表不完整
- **文档声明**: 项目结构（第 156-158 行）列出 BookParser, TtsManager, FullTextSearch
- **实际**: 还有 `BookLoader.kt`, `MemoryManager.kt`, `CacheManager.kt`

### 10. CI release tag 硬编码
- **CI `build.yml:108`**: `tag_name: v40`, `name: FlowReader v40.0.0` 硬编码为旧版本
- **当前版本**: v41.0.0，每次发版需手动更新此文件

---

## AGENTS.md 偏差（更新前）

### 11. Application 类名错误
- **声明**: `FlowReaderApp | Application @HiltAndroidApp`（第 42 行）
- **实际**: 类名已重命名为 `FlowReaderApplication`

### 12. Composable 类名错误
- **声明**: `FlowReaderApp (UI) | Composable`（第 43 行）
- **实际**: 已重命名为 `FlowReaderRoot`（在 `ui/FlowReaderApp.kt` 中）

### 13. 声称无 .editorconfig
- **声明**: "No .editorconfig, lint.xml, or detekt configs"（第 58 行）
- **实际**: `.editorconfig` 存在于项目根目录（v35 已添加）

### 14. 预置书籍描述错误
- **声明**: `assets/books/ # Preloaded books`（第 23 行）
- **实际**: 目录为空

### 15. compileSdk/targetSdk 过时
- **声明**: "Target/Compile SDK 34"（第 71 行）
- **实际**: 均为 35

### 16. 阅读主题数量过时
- **声明**: "5 reading themes"（第 69 行）
- **实际**: 10 种

---

## PROJECT_SUMMARY.md 偏差（已删除）

### 17. 根 Composable 文件名错误
- **声明**: `FlowReaderRoot.kt`（第 519 行）
- **实际**: 文件名是 `FlowReaderApp.kt`，其中 composable 函数名为 `FlowReaderRoot`

### 18. Hilt 模块名错误
- **声明**: 提到独立的 `DatabaseModule` 和 `RepositoryModule`（第 141-142 行）
- **实际**: 两者都在 `di/AppModule.kt` 中（DatabaseModule 是 object，RepositoryModule 是 abstract class）

### 19. CI 权限描述与实际不符
- **声明**: "不再使用 `permissions: contents: write` 全局权限"（第 645 行）
- **实际**: `build.yml:59` 的 `build-and-release` job 仍使用 `permissions: contents: write`

---

## 更新优先级

| 优先级 | 项目 | 原因 |
|--------|------|------|
| **P0** | #1 版本号、#10 CI tag 硬编码 | 影响发版流程 |
| **P1** | #2 主题数量、#3-4 版本要求、#5 测试框架 | 误导开发者环境搭建 |
| **P1** | #7-8-9 项目结构 | 误导文件查找 |
| **P2** | #11-12 AGENTS.md 类名 | 影响 AI agent 准确性 |
| **P2** | #13 .editorconfig、#14 预置书籍 | 与事实矛盾 |
| **P3** | #17-19 PROJECT_SUMMARY.md | 已删除，无需修复 |

---

## 修复状态

- [x] AGENTS.md — 已重写修正（`AGENTS.md`）
- [x] AGENTS 中文版 — 已生成（`AGENTS_CN.md`）
- [x] PROJECT_SUMMARY.md — 已删除
- [x] README.md — 已修复（版本号 41.0.0、10 种主题、SDK 35、项目结构、JUnit 4）
- [x] index.html — 已修复（环境要求：SDK 35、Kotlin 2.0.21）
- [x] CI build.yml — 已修复（从 build.gradle.kts 动态读取版本号，移除硬编码）
