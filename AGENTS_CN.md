# FlowReader — Agent 指南（中文版）

Android 电子书阅读器（EPUB/TXT/PDF/Markdown）。Jetpack Compose + MVVM + Hilt。离线优先，无网络功能。

## 构建与测试

```bash
./gradlew assembleDebug           # Debug APK → app/build/outputs/apk/debug/
./gradlew assembleRelease         # Release APK（R8 混淆压缩，需配置签名）
./gradlew :app:testDebugUnitTest  # 单元测试（JUnit 4）
./gradlew clean                   # 清理构建
```

- 需要 JDK 17。compileSdk/targetSdk = 35，minSdk = 26。
- Release 构建启用了 `isMinifyEnabled` + `isShrinkResources`（见 `app/build.gradle.kts:30-36`）。
- 未配置 lint/detekt/ktlint，仅有 `.editorconfig` 做代码风格约束。

## 版本信息（以 `build.gradle.kts` / `app/build.gradle.kts` 为准）

| 组件 | 版本 |
|------|------|
| AGP | 8.6.0 |
| Kotlin | 2.0.21 |
| KSP | 2.0.21-1.0.27 |
| Hilt | 2.50 |
| Gradle wrapper | 8.7 |
| Compose BOM | 2024.12.01 |
| Room | 2.6.1 |
| app versionName | 41.0.0 |

## 项目结构

单模块：`:app`。源码根目录：`app/src/main/java/com/flowreader/app/`

```
├── MainActivity.kt              # 入口，设置 Compose 内容
├── FlowReaderApplication.kt     # @HiltAndroidApp（注意：不是 FlowReaderApp）
├── data/
│   ├── local/
│   │   ├── AppDatabase.kt       # Room DB v4，数据库名="flowreader_db"
│   │   ├── dao/                 # BookDao, ChapterDao, BookmarkDao, AnnotationDao, CategoryDao, ReadingStatsDao
│   │   └── entity/              # 与各 DAO 对应的实体类
│   └── repository/              # 实现类 + SettingsRepository, DataManager, BackupRepository
├── di/
│   └── AppModule.kt             # 包含 DatabaseModule（提供）和 RepositoryModule（绑定）
├── domain/
│   ├── model/                   # Book, ReadingSettings, Annotation, AppException, WheelItem 等
│   ├── repository/              # 接口：BookRepository, BackupRepository, ReadingStatsRepository
│   └── usecase/                 # GetBookUseCase, SaveProgressUseCase, TextPaginator
├── ui/
│   ├── FlowReaderApp.kt         # 根 Composable 是 FlowReaderRoot()（不是 FlowReaderApp）
│   ├── Navigation.kt            # Sealed Screen 类、NavHost、底部导航（4 个 tab）
│   ├── theme/                   # Color.kt, Theme.kt, Typography.kt
│   └── screens/
│       ├── library/             # LibraryScreen + ViewModel
│       ├── bookdetail/          # BookDetailScreen + ViewModel
│       ├── reader/              # ReaderScreen + ViewModel + components/
│       ├── settings/            # SettingsScreen + ViewModel
│       ├── stats/               # StatsScreen + ViewModel
│       └── wheel/               # WheelScreen + ViewModel + components/WheelSpinner
└── util/                        # BookParser, BookLoader, TtsManager, FullTextSearch, MemoryManager, CacheManager
```

## 架构要点

- **命名冲突已解决（v35）**：Application = `FlowReaderApplication`，根 Composable = `FlowReaderRoot`（在 `ui/FlowReaderApp.kt` 中）。旧名称 `FlowReaderApp` 已废弃。
- **Hilt 模块**：`DatabaseModule`（提供 DB + DAO）和 `RepositoryModule`（绑定接口）都在同一个文件 `di/AppModule.kt` 中。
- **导航**：4 个底部 tab — 书架（Library）、转盘（Wheel）、统计（Stats）、设置（Settings）。阅读器（Reader）和书籍详情（BookDetail）是非 tab 路由。见 `Navigation.kt:29-40`。
- **ReadingSettings**（`domain/model/ReadingSettings.kt`）定义了 10 种 `ReaderTheme`（LIGHT, DARK, SEPIA, PAPER, AMOLED, SYSTEM, MORNING, NOON, EVENING, NIGHT）、5 种 `PageMode`、8 种 `FontFamily`，以及手势/纹理/音效等枚举。
- **3 秒防抖**：ReaderViewModel 中阅读进度保存使用 3 秒 debounce。
- **测试框架**：JUnit 4（不是 JUnit 5）。测试文件在 `app/src/test/java/com/flowreader/app/util/BookParserTest.kt`。
- **无预置书籍**：`app/src/main/assets/books/` 目录存在但为空，用户自行导入。
- **PDF 渲染**：Android 原生 `PdfRenderer`，无外部 PDF 库。
- **EPUB 渲染**：Readium Kotlin Toolkit 3.1.2（readium-shared, readium-streamer, readium-navigator, readium-opds）。

## CI（`.github/workflows/build.yml`）

- PR → `build` 任务：单元测试 + debug APK 上传。
- Push 到 main → `build-and-release` 任务：测试 + debug + release APK + GitHub release。
- `build-and-release` 使用 `permissions: contents: write`（较宽权限）。
- Release 的 tag/name 从 `app/build.gradle.kts` 的 `versionName` **动态读取**（通过 grep 步骤）。
- 使用 `actions/cache@v3`（不是 v4）。

## 易踩坑点

- `gradle.properties` 中 `org.gradle.jvmargs` 声明了**两次**（第 2 行和第 23 行），第二次覆盖第一次。
- README.md 和 index.html 包含过时信息（版本 badge 写 34.0.0、声称 5 种主题、Kotlin 1.9+）。以 `build.gradle.kts` 为准。
- 启用了 `coreLibraryDesugaring`（`app/build.gradle.kts:45,62`）用于 java.time API 向下兼容。
