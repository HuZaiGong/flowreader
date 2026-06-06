# 项目指南（Repository Guidelines）

## 项目概述

**FlowReader** 是一款离线优先的 Android 电子书阅读器，支持 EPUB、TXT、PDF 和 Markdown 格式。采用 Jetpack Compose 构建，遵循 Clean Architecture + MVVM。所有数据均为本地存储，没有网络功能。

- **包名**：`com.flowreader.app`
- **最低 SDK**：26，**目标/编译 SDK**：35
- **许可证**：GPL-3.0

---

## 架构与数据流

项目采用 **Clean Architecture**，分层如下：

```
UI 层（Compose 页面 + ViewModel）
  ↕
领域层（模型、仓库接口、用例）
  ↕
数据层（仓库实现、Room 数据库、DAO、实体）
```

- **UI 层**：`ui/screens/` 中每个子包对应一个页面（包含 Composable 和 `*ViewModel`）。根导航置于 `Navigation.kt`。
- **领域层**：`domain/model/` 存放数据类与密封类；`domain/repository/` 存放接口；`domain/usecase/` 存放业务逻辑（如 `GetBookUseCase`）。
- **数据层**：`data/local/`（Room 数据库、DAO、实体）和 `data/repository/`（实现类）。`BackupRepository.kt` 也在此处。

数据流：**Composable → ViewModel → UseCase/Repository → Room DAO → SQLite**。

关键 DI 配置：`di/AppModule.kt` 同时包含 `DatabaseModule`（`@Provides`）和 `RepositoryModule`（`@Binds`）。

---

## 关键目录

| 目录 | 用途 |
|-----------|---------|
| `app/src/main/java/com/flowreader/app/` | 全部 Kotlin 源代码 |
| `data/local/entity/` | Room 实体（6 张表） |
| `data/local/dao/` | Room DAO |
| `data/repository/` | 仓库实现 |
| `domain/model/` | 领域模型与 `AppException` |
| `domain/usecase/` | 业务逻辑 / 用例 |
| `ui/screens/` | 页面目录（`library/`、`reader/`、`bookdetail/`、`settings/`、`stats/`、`wheel/`） |
| `ui/theme/` | Compose 主题（`Color.kt`、`Theme.kt`、`Typography.kt`） |
| `util/` | 工具类：`BookParser`、`BookLoader`、`TtsManager`、`FullTextSearch`、`MemoryManager`、`CacheManager` |
| `app/src/test/java/...` | 单元测试（JUnit 4） |
| `.github/workflows/` | CI/CD（GitHub Actions） |

---

## 开发命令

```bash
# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK（R8 压缩）
./gradlew assembleRelease

# 运行单元测试
./gradlew testDebugUnitTest

# 清理构建
./gradlew clean
```

**环境要求**：JDK 17，Android SDK 35。

---

## 代码规范与常见模式

### 命名规范
- **包名**：全部小写，与目录结构一致。
- **类名**：`PascalCase`，例如：`BookRepositoryImpl`、`ReaderViewModel`。
- **函数 / 变量**：`camelCase`。
- **常量**：`SCREAMING_SNAKE_CASE` 或 Object 中的顶层 `val`。

### 依赖注入
- 使用 **Hilt** 进行依赖注入。
- `FlowReaderApplication.kt` 标注 `@HiltAndroidApp`。
- `di/AppModule.kt` 包含 `@Provides`（DatabaseModule）和 `@Binds`（RepositoryModule）。
- 在 Composable 中使用 `hiltViewModel()` 注入 ViewModel。

### 状态管理
- ViewModel 暴露 Compose `StateFlow`，在 Composable 中收集。
- 使用 `derivedStateOf` 缓存昂贵的 UI 计算。
- 阅读进度保存采用 **3 秒防抖** 以减少数据库写入。

### 错误处理
- `AppException` 是用于领域错误的密封类（`DatabaseError`、`FileError`、`ParseError` 等）。
- 使用 `Result<T>` 包装可能失败的操作。

### 异步模式
- 使用 Kotlin **Coroutines + Flow** 处理异步任务。
- Room 查询返回 `Flow<T>`。
- 在 ViewModel 中使用 `viewModelScope.launch`。

### Compose 规范
- 根 Composable：`FlowReaderRoot()`（位于 `ui/FlowReaderApp.kt`）。
- 导航使用 `sealed class Screen` 定义路由。
- 底部导航有 4 个标签：书架、转盘、统计、设置。
- 采用 Material 3 主题，支持动态颜色（Material You）。

---

## 重要文件

| 文件 | 作用 |
|------|------|
| `app/build.gradle.kts` | App 级构建配置（AGP 8.6.0、Kotlin 2.0.21、Compose BOM 2024.12.01） |
| `build.gradle.kts` | 根项目插件（Hilt、KSP、Compose） |
| `settings.gradle.kts` | 项目名称与包含模块 |
| `gradle.properties` | Gradle JVM 参数、AndroidX、缓存标志 |
| `app/src/main/AndroidManifest.xml` | 应用清单、权限、MainActivity |
| `FlowReaderApplication.kt` | `@HiltAndroidApp` 入口 |
| `MainActivity.kt` | 设置 Compose 内容根布局，支持 edge-to-edge |
| `Navigation.kt` | `NavHost`、底部导航、`Screen` 密封类 |
| `di/AppModule.kt` | Hilt 模块：数据库与仓库 |
| `data/local/AppDatabase.kt` | Room 数据库（v4），`flowreader_db` |
| `proguard-rules.pro` | Release 构建的 R8 ProGuard 规则 |

---

## 运行时/工具偏好

- **构建系统**：Gradle（wrapper 8.7）
- **AGP**：8.6.0
- **Kotlin**：2.0.21（KSP 2.0.21-1.0.27）
- **JDK**：17
- **无额外运行时**（无 Bun、Node、Python 等）
- 启用 `coreLibraryDesugaring` 以向后兼容 `java.time`

---

## 测试与质量保障

- **框架**：JUnit 4 + MockK
- **协程测试**：`kotlinx-coroutines-test`
- **测试位置**：`app/src/test/java/com/flowreader/app/util/BookParserTest.kt`
- **CI**：GitHub Actions（`build.yml`）
  - PR：运行单元测试 + 构建 Debug APK + 上传构建产物
  - Push 到 `main`：运行单元测试 + 构建 Debug + 构建 Release + 创建 GitHub Release
  - 使用 JDK 17 Temurin，缓存 Gradle 包

### 运行测试
```bash
./gradlew testDebugUnitTest    # 仅单元测试
./gradlew test                 # 所有测试（如有连接设备则包含仪器化测试）
```

未配置 lint/detekt/ktlint；仅使用 `.editorconfig` 进行代码风格管理。
