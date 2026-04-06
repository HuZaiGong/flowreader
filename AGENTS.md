# 项目知识库

**生成时间：** 2026-04-06
**提交版本：** 65d4620
**分支：** main

## 概述
FlowReader（心流阅读）- 基于 Jetpack Compose 构建的 Android 电子书阅读应用，支持 EPUB、TXT、PDF 格式，提供丰富的自定义阅读功能。

## 目录结构
```
./
├── .github/workflows/       # CI：build.yml（main 分支自动发布）
├── app/
│   └── src/main/
│       ├── java/com/flowreader/app/
│       │   ├── data/           # 数据层（Room，仓库实现）
│       │   ├── di/             # Hilt 依赖注入模块
│       │   ├── domain/         # 领域层（模型与接口）
│       │   ├── ui/             # Compose 界面与主题
│       │   ├── util/           # 工具类（BookParser，使用 JSoup 解析 EPUB/TXT）
│       │   ├── MainActivity.kt
│       │   └── FlowReaderApp.kt
│       └── res/                # 资源（主题、字符串、Drawable）
├── build.gradle.kts          # 根构建文件（插件：AGP 8.2.0, Kotlin 1.9.22, Hilt 2.50, KSP）
├── gradle/                   # Gradle 包装器
└── README.md
```

## 关键路径
| 任务 | 路径 | 说明 |
|------|------|------|
| 添加新页面 | `ui/screens/` | 遵循现有的 Screen+ViewModel 模式 |
| 数据库修改 | `data/local/dao/` + `data/local/entity/` | Room 实体与 DAO |
| 依赖注入配置 | `di/AppModule.kt` | Hilt 模块配置仓库 |
| 阅读设置 | `domain/model/ReadingSettings.kt` | 字体、主题、行间距配置 |
| 书籍解析 | `util/BookParser.kt` | 基于 JSoup 的 EPUB/TXT 解析器 |
| 导航配置 | `ui/Navigation.kt` | Compose Navigation 导航设置 |

## 代码规范
- MVVM + 整洁架构（data/domain/ui 分层）
- Hilt 依赖注入；ViewModel 使用 `hiltViewModel()`
- Room 持久化；DataStore 存储偏好设置
- Compose UI + Material 3；通过 StateFlow 管理状态
- 阅读进度保存采用 3 秒防抖
- 未找到单元测试配置（无 tests/ 目录）

## 反模式（项目特有）
- 源码中无 DO NOT/NEVER/ALWAYS/DEPRECATED 等注释
- 仓库中无单元测试（仅通过 CI 构建测试）
- CI 工作流使用宽泛的 `permissions: contents: write`（建议收紧权限）
- 无 .eslintrc、pyproject.toml 或 .editorconfig（纯 Android/Kotlin 项目）

## 构建命令
```bash
./gradlew assembleDebug      # 构建 Debug 版 APK
./gradlew assembleRelease   # 构建 Release 版 APK
```

## 注意事项
- 预置图书：诡秘之主.txt、神秘复苏.txt 位于根目录
- 支持 5 种阅读主题：浅色、深色、护眼、羊皮纸、AMOLED 纯黑
- PDF 支持采用 Android 原生 PDF 渲染器（无外部库）
- 最低 SDK 26，目标 SDK 34，编译 SDK 34