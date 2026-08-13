# FlowReader 心流阅读

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Kotlin-2.1.0-7F52FF?style=flat&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat&logo=jetpackcompose" alt="UI">
  <img src="https://img.shields.io/badge/minSdk-26-red?style=flat" alt="minSdk">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=flat" alt="License">
</p>

<p align="center">
  <b>离线优先的 Android 电子书阅读器</b><br>
  没有账号，没有云同步，没有遥测 —— 书和阅读数据只留在这台设备上
</p>

---

## 概述

FlowReader 用 **Jetpack Compose + Material 3** 构建，**Clean Architecture + MVVM** 多模块分层。它刻意不做账号体系、云同步、数据上报和崩溃收集：整个应用只申请一个 `INTERNET` 权限，仅供局域网 OPDS 书目与局域网备份互传使用；应用内没有任何 WebView。

当前版本 **v56.4.4**（versionCode 5644）。

**支持格式**：EPUB、TXT、PDF、Markdown、FB2、MOBI，以及漫画 —— 单张 JPG / PNG / WebP 与纯图片 ZIP / CBZ 包。FB2 与 MOBI 是只读导入，在导入时像 EPUB 一样切成章节；**带 DRM 的 MOBI / AZW 直接拒绝导入，不做任何解密**，HUFF/CDIC 压缩的文件也是整体拒绝而非半解码。

界面与文档为中文，代码标识符为英文。应用内可切换 9 种语言（中、英、日、韩、德、西、法、葡、俄）。

---

## 功能

### 书库

- **导入**：单本或批量导入；系统「用其他应用打开」也走同一条受限管线，且只消费一次（配置变更不会重复导入）。
- **书架**：按添加时间／最近阅读／书名／作者排序，网格与列表双视图，「继续阅读」大卡；作者、简介、封面、标签均可编辑。
- **全库搜索**：基于 SQLite **FTS5** 的跨书全文检索，书名与章节两段结果，分页加载 + 搜索历史。
- **书单**：自建书单归类书籍。
- **局域网 OPDS**：连接同一局域网内的 OPDS 书目并下载。**仅限内网** —— 回环地址、RFC1918 / RFC4193 私有网段与 `.local` 式名称之外的主机一律不可达，且**每一次重定向都重新校验**。
- **进度持久化**：阅读进度 3 秒防抖写入，减少数据库 IO。

### 阅读器

- **12 套阅读色板**：纸白、米黄、护眼绿、亚麻、晨雾、冷灰、电子墨水、夜黑、墨蓝、深棕、曜石、纯黑，全部通过 WCAG AA 正文对比度自动断言；也可自定义背景／文字色，同样带对比度校验。
- **排版可调**：12–32sp 字号、1.0–2.5 倍行距、段间距、首行缩进，可导入 `.ttf` / `.otf` 外部字体。中文正文按每行 34 字上限控制行宽。
- **三种翻页模式**：滑动（动画滚动）、**真实分页**（逐页测量 + 横滑／点按翻页）、无动画跳转。只提供真正实现了的选项 —— 仿真、卷曲、覆盖三种曾经只有 UI 入口的模式已连同入口一并删除。
- **原生文本选中**：长按即选中，按词选中、拖拽扩选、双端手柄；浮动操作栏可高亮（5 色）、复制或加书签，选中范围与章节原文精确对应。
- **漫画**：横向逐页翻页，或纵向虚拟化长列表。
- **PDF**：缩放、拖拽翻页、框选区域标注。
- **手势全可配**：点击分区、双击、长按、左右滑动与边缘热区宽度都能改，且真实生效。
- **其他**：TTS 朗读、专注模式（全屏沉浸）、屏幕常亮、定时自动夜间模式（19:00–07:00，每分钟重估）、护眼提醒（15/20/30/45/60 分钟）、底部可拖拽进度条。

### 笔记与数据

- **笔记与批注**：黄／绿／蓝／粉／橙 5 色高亮，可加想法备注；书籍详情页可导出为 Markdown / HTML / 纯文本（导出时做转义）。
- **阅读统计**：按日统计时长、页数、速度，柱状图趋势，周报／月报，最快阅读日与最常读书籍；支持自定义每日／每周／每月阅读目标。
- **分享与传输**：一键生成阅读分享卡片（图片）；书架导出 CSV / JSON；**局域网备份互传** —— 同一 WiFi 下设备间直传，随机令牌保护，绑定实际局域网网卡而非 `0.0.0.0`，对话框关闭即停止服务。
- **备份恢复**：导出／导入书籍与阅读进度，导入是**单个原子事务**，上限 200MB。
- **主屏 Widget**：显示最近阅读的书籍与当前进度。
- **决策转盘**：可自定义选项与颜色的小工具，自 v52 起从底部一级导航移到书架顶栏「更多」中。

---

## 架构

允许的依赖方向：`feature:* → core / domain`，`data → core / domain`，`app → core / data / domain / feature:*`。`:domain` 不含 app / Room / Compose / Hilt 任何依赖。

| 模块 | 内容 |
|------|------|
| `:app` | 组装根：`MainActivity`、Hilt 装配（`di/AppModule.kt`）、导航、全部界面与 ViewModel、**全部 10 个 Repository 实现**、`util/`、`widget/` |
| `:core` | 设计系统（v52 起）：Token、`FlowTheme`、12 套阅读色板、通用组件，以及一批与 Compose 解耦、可在 JVM 上直接测的纯函数 |
| `:data` | 仅 Room：`AppDatabase` + 7 个 DAO + 7 个 Entity |
| `:domain` | 10 个 Repository 接口 + 领域模型。`domain/usecase/` 已删除 —— 业务逻辑有意内聚在各 ViewModel |
| `:feature:reader` | `ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`、`ReaderPositionUnit`（均有单测覆盖） |
| `:feature:library` | **目前还没有源文件**，是已接入编译／lint／测试的占位模块 |

数据流：`Composable → ViewModel → domain 仓库接口 → data 仓库实现 → Room DAO / DataStore`。每个 ViewModel 暴露一个不可变 `StateFlow<XxxUiState>`，错误用 Kotlin 内置 `Result`。

注意一处命名错位：模块 namespace 是 `com.flowreader.domain` / `com.flowreader.data` 这类，但源码包名统一是 `com.flowreader.app.*`。新文件请沿用 `com.flowreader.app.*`。

### 三套互相独立的持久化

1. **Room**（`flowreader_db`）—— `AppDatabase` 当前 version 7，`exportSchema = true`。**没有** `fallbackToDestructiveMigration()`，每次 schema 变更都要手写迁移。
2. **第二个裸 SQLite 库**（`flowreader_fts.db`）—— 由 `util/FullTextSearch.kt` 直接管理的 FTS5 虚拟表，完全在 Room 之外，同时支撑书内搜索与全库搜索。
3. **DataStore Preferences**（`settings`）—— `SettingsRepositoryImpl` 独占所有偏好键。

`util/CacheManager.kt` 是唯一的章节／元数据／封面缓存，按 `MemoryManager` 的建议值定容，实现 `ComponentCallbacks2` 响应内存压力，并按命中率自适应每本书的章节容量。不要再加第二个章节缓存。

---

## 构建

环境：**JDK 17**、**Android SDK 35**（compileSdk 35 / minSdk 26）、AGP 8.6.0。Gradle wrapper 从腾讯云镜像拉取 Gradle 9.6.1，wrapper 下载失败通常是镜像问题而不是项目问题。

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

./gradlew assembleDebug          # 开发包
./gradlew assembleRelease        # R8 full-mode 混淆 + 资源压缩（有意使用 debug 签名配置）
./gradlew testDebugUnitTest      # 全部 JVM 单测
```

### 验证门禁

CI（`.github/workflows/ci.yml`）严格按此顺序跑这六项：

```bash
./gradlew verifyKotlinStyle      # ktlint（:app 以外的模块）+ 全仓空白字符检查
./gradlew testDebugUnitTest      # :app / :core / :domain / :feature:reader
./gradlew coverageSummary        # 测试广度文件比 ≥ 40%（当前 75.8%）
./gradlew assembleDebug
./gradlew verifyRoborazziDebug   # 截图回归
./gradlew performanceBaseline    # APK 体积对比 baseline/apk-size.properties
```

两个容易踩的点：

- **ktlint 不作用于 `:app`**。`:app` 的 Kotlin 只由 `verifyKotlinStyle` 里的空白字符门禁检查 —— 全仓任意 `.kt` / `.kts` 里出现一个 tab 或行尾空格，整个构建就失败。`.editorconfig` 规定 4 空格缩进、LF、行宽上限 140。
- **`coverageSummary` 是文件数量比，不是行覆盖率**。分母包含 `:core` 的每个主源文件、领域模型、仓库接口、ViewModel 等。**新增一个领域模型或 `:core` 文件却不加测试，就算别处一行没动也会挂**。

---

## 安全约束

项目是离线优先且注重隐私的，这些是硬约束：

- **只有一个 `INTERNET` 权限**，仅供局域网 OPDS 使用。无账号、无统计、无崩溃上报、无同步。
- **导入处处设上限**：EPUB 单章 16MB、内嵌图片 24MB、TXT/MD/FB2/MOBI 整文件 128MB；ZIP / CBZ 拒绝绝对路径与 `..`（zip-slip），跳过 `__MACOSX` 与隐藏项，限制条目数与单条大小，只放行解析器支持的扩展名。
- **不做 DRM 破解**。
- **备份不含可执行内容**，导入是单个原子事务；云备份只含 shared prefs，数据库不经 Android Backup 离开设备。
- **ContentProvider**（`com.flowreader.app.provider`）只暴露只读的书籍元数据与进度 —— 不含文件路径，不含正文，写入一律拒绝。
- **FileProvider 只授权 `share_cards/` 一个子目录**（全仓仅一处 `getUriForFile()` 调用点）。
- **全应用无 WebView**；FTS 查询、HTML / Markdown 导出均做转义；审计门禁要求不新增 `!!`。

---

## 更新日志

最近几个版本：

- **v56.4.4** —— 修复书架／统计／书籍详情三页「加载出数据后内容被顶栏遮挡」：`FlowStateHost` 的成功分支丢弃了 `modifier`，导致 88dp 安全区在成功状态下消失，而同一页面的加载／空／错误状态反而正常。
- **v56.4.3** —— 功能性 bug 排查 11 处。书内全文搜索因 FTS5 列无亲和性而恒返回空结果；PAGED 与漫画的阅读统计整段丢失；划词高亮少存一个字符、重叠高亮重复渲染；「最近 7 天」趋势图实际只显示 2–3 天。
- **v56.4.2** —— 修复 issue #6：外层 shell 只「应用」而未「消费」窗口 inset，下层 9 个界面又各应用一遍，顶部多出整条状态栏高度。
- **v56.4.1** —— 安全加固：移除 ContentProvider 中的 `runBlocking`；FileProvider 授权从三棵完整目录树收紧到一个子目录；修复令牌生成中的潜伏缺陷。
- **v56.3.0 / v56.4.0** —— 两轮专项安全审计。

完整历史见 [CHANGELOG.md](CHANGELOG.md)，更长期的规划与已知技术债见 `ROADMAP.md`，逐条行为陷阱见 `AGENTS.md`。

---

## 许可证

基于 [GNU General Public License v3.0](LICENSE) 开源。

<p align="center">Made with ❤️ by HuZaiGong</p>
