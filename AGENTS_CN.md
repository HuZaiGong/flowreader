# 项目指南（Repository Guidelines）

**当前版本：44.0.2**

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
- **领域层**：`domain/model/` 存放数据类；`domain/repository/` 存放接口（Chapter、Bookmark、Annotation、Category 接口在 `BookRepository.kt` 中）；`domain/usecase/` 存放业务用例。
- **数据层**：`data/local/`（Room 数据库、DAO、实体）和 `data/repository/`（实现类）。
- **DI**：`di/AppModule.kt` 包含 `DatabaseModule`（`@Provides` 提供 6 个 DAO）和 `RepositoryModule`（`@Binds` 绑定 7 个 Repository）。

数据流：**Composable → ViewModel → UseCase/Repository → Room DAO → SQLite**

---

## 重要提醒 / Gotchas

### 领域接口文件位置

Chapter、Bookmark、Annotation、Category 的接口**都定义在 `domain/repository/BookRepository.kt` 中**，不是独立文件。新增方法时编辑该文件即可。

### SettingsRepository 没有领域接口

`data/repository/SettingsRepository.kt` 直接注入到 ViewModel，没有对应的 `domain/repository/` 接口层。

### sessionReadPages 必须手动递增

ViewModel 声明的 `sessionReadPages` **不会自动递增**。必须在 `updatePosition()` 中调用 `sessionReadPages += pages`，否则阅读统计永远不保存。

### Release 构建使用 Debug 签名

`app/build.gradle.kts:31`: `signingConfig = signingConfigs.getByName("debug")`。这是为了 CI 能产出 `app-release.apk`（而非 `-unsigned`），APK 使用 Android SDK debug 密钥库签名，仅用于测试分发。

### bookId 默认值为 0L

从 `SavedStateHandle` 取出的 `bookId` 默认值为 `0L`。在 DAO 查询前必须校验 `bookId > 0`，否则会导致静默失败或闪退。

### WheelViewModel.spin() 非挂起函数

`spin()` 是普通函数，内部通过 `viewModelScope.launch` 启动协程。Composable 中不需要 `LaunchedEffect`。

### 嵌套 LazyColumn 会闪退

`LazyColumn` 内部 `item` 中不可再放 `LazyColumn`，否则因内层可滚动组件高度测量为 0，Compose 会抛出 `IllegalStateException` 崩溃。应使用 `Column` 替代。

### 3 秒防抖

`ReaderViewModel.debouncedSaveProgress()` 取消前一个 Job 后延迟 3000ms 再写入。快速滚动时每 3 秒只触发一次写入。

---

## 构建与测试

```bash
./gradlew assembleDebug          # 构建 Debug APK
./gradlew assembleRelease        # 构建 Release APK（R8 混淆 + 资源压缩）
./gradlew testDebugUnitTest      # 运行单元测试（JUnit 4 + MockK）
./gradlew clean
```

- **无 lint/detekt/ktlint**，仅 `.editorconfig` 控制代码风格。
- **单测试文件**：`app/src/test/java/com/flowreader/app/util/BookParserTest.kt`。
- Room KSP 输出目录：`app/build/generated/ksp/`。
- 启用 `coreLibraryDesugaring` 以兼容 `java.time`。
- **需 JDK 17**，Android SDK 35。

### CI（GitHub Actions）

| 触发 | 任务 |
|------|------|
| PR | 运行单元测试 + 构建 Debug APK → 上传 `app-debug.apk` |
| Push 到 `main` | 同上 + 构建 Release APK → 创建 GitHub Release（`v$versionName`）+ 上传 `app-release.apk` |

---

## 目录约定

| 目录 | 说明 |
|------|------|
| `ui/screens/*/` | 每个页面含 `*Screen.kt`（Composable）和 `*ViewModel.kt`（Hilt） |
| `domain/repository/` | 接口定义（Chapter 等在 `BookRepository.kt` 中） |
| `data/repository/` | 接口实现 |
| `data/local/dao/` | Room DAO |
| `data/local/entity/` | Room 实体 |
| `util/` | 工具类：BookParser、BookLoader、CacheManager、TtsManager、FullTextSearch、MemoryManager |
| `app/src/test/` | 单元测试 |

全部 Kotlin 源码位于 `app/src/main/java/com/flowreader/app/`。

### 配置文件

| 文件 | 作用 |
|------|------|
| `app/build.gradle.kts` | AGP 8.6.0, Kotlin 2.0.21, Compose BOM 2024.12.01 |
| `build.gradle.kts` | 根项目插件（Hilt、KSP、Compose） |
| `gradle.properties` | 并行 GC、VFS 监控、Kotlin 增量编译、Daemon JVM 参数 |
| `gradle/wrapper/gradle-wrapper.properties` | Gradle 9.6.1、网络超时 60s |
