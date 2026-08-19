# FlowReader 心流阅读

<p align="center">
  <b>中文</b> | <a href="README_EN.md">English</a>
</p>

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

当前版本 **v56.6.1**（versionCode 5661）。

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

- **18 套阅读色板**：纸白、米黄、护眼绿、亚麻、晨雾、冷灰、电子墨水、曝光浅、石英粉、夜黑、墨蓝、深棕、曜石、纯黑、曝光深、极地、复古暖、深林，全部通过 WCAG AA 正文对比度自动断言；也可自定义背景／文字色，同样带对比度校验。
- **排版可调**：12–32sp 字号、1.0–2.5 倍行距、段间距、首行缩进，可导入 `.ttf` / `.otf` 外部字体。中文正文按每行 34 字上限控制行宽。
- **三种翻页模式**：滑动（动画滚动）、**真实分页**（逐页测量 + 横滑／点按翻页）、无动画跳转。只提供真正实现了的选项 —— 仿真、卷曲、覆盖三种曾经只有 UI 入口的模式已连同入口一并删除。
- **原生文本选中**：长按即选中，按词选中、拖拽扩选、双端手柄；浮动操作栏可高亮（5 色）、复制或加书签，选中范围与章节原文精确对应。
- **漫画**：横向逐页翻页，或纵向虚拟化长列表。
- **PDF**：缩放、拖拽翻页、框选区域标注。
- **手势全可配**：点击分区、双击、长按、左右滑动与边缘热区宽度都能改，且真实生效。
- **其他**：TTS 朗读、专注模式（全屏沉浸）、屏幕常亮、定时自动夜间模式（19:00–07:00，每分钟重估）、护眼提醒（15/20/30/45/60 分钟）、底部可拖拽进度条。

### 外观与主题

- **12 套内置配色**（v56.6）：经典紫、靛蓝、晴空蓝、青碧、翠绿、苔绿、琥珀、橘橙、绯红、玫瑰、梅紫、石墨。选一套会**重新生成整套 Material 配色**，不是只换一个强调色。经典紫为默认，直接沿用手工调过的品牌配色，老用户升级后颜色不变。
- **自调色**（v56.6）：设置 → 外观 → 自调色，提供**色相环 + 内嵌饱和度／明度色盘**、**光谱条**、明度与饱和度条，以及十六进制输入框，四种方式绑定同一份 HSV 状态，可互相接续微调。弹窗内实时预览生成后的配色与实测正文对比度，点「应用」才写入。
- **对比度是保证，不是巧合**：任意种子色生成的配色，**每一组正文文字／背景配对都不低于 WCAG AA 的 4.5:1**。生成器在 `:core` 内不依赖 Compose，因此这条承诺由单元测试覆盖 12 套预设 × 明暗两档、整个色相圈每 5° 一档，以及纯黑／纯白／中灰等退化种子。
- **主题模式与配色来源相互独立**：浅色／深色／跟随系统与「内置配色／跟随壁纸／自定义」是两个维度；阅读器的 18 套色板又是第三套，永远不跟随应用主题。

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
./gradlew coverageSummary        # 测试广度文件比 ≥ 40%（当前 77.8%）
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

- **v56.6.1** —— v56.6.0 的排查性修复。调色台的对比度数值此前用不带 locale 的 `String.format` 预格式化，而字符串资源按应用内语言解析，在小数点为逗号的语言下两者不一致并会抛异常；十六进制输入框在组合期回写自己读取的状态，每拖一次色相环多付一趟组合；「自调色」副标题在已切回内置配色时仍声称自定义色在用。另外把 `backup_rules.xml` 写反了的注释改正 —— 备份范围一直是空的（什么都不出设备），这是刻意的，只改注释不改行为。
- **v56.6.0** —— 配色来源从 2 个选项扩展为 **12 套内置配色 + 跟随壁纸 + 自调色**，并新增色相环／色盘／光谱条调色台。`:core` 的 `SeedColorScheme` 由单一种子色生成 24 个 Material 角色，并保证任意种子下正文文字／背景都不低于 WCAG AA 的 4.5:1。
- **v56.5.2** —— Compose 稳定性配置消除 domain 模型不稳定推断。`:domain` 模块无 Compose 编译器，导致 `Book`/`Chapter`/`Annotation`/`ReadingSettings` 等参数被推断为不稳定，每个可见书卡在任意状态变更时都重新执行。通过 `compose_compiler_config.conf` 声明 `com.flowreader.app.domain.model.*` 稳定并接入四个 Compose 模块；不稳定类从 52 个降至 37 个，所有 UiState 现在稳定。
- **v56.5.1** —— 书籍详情与统计页本地化。`:core` 的 `FlowFormatters` 根据 `Locale` 格式化数字、日期与单位；domain 模型现在携带纯数值而非拼接字符串，避免 v53 语言切换冻结中文。
- **v56.4.4** —— 修复书架／统计／书籍详情三页「加载出数据后内容被顶栏遮挡」：`FlowStateHost` 的成功分支丢弃了 `modifier`，导致 88dp 安全区在成功状态下消失，而同一页面的加载／空／错误状态反而正常。
- **v56.4.3** —— 功能性 bug 排查 11 处。书内全文搜索因 FTS5 列无亲和性而恒返回空结果；PAGED 与漫画的阅读统计整段丢失；划词高亮少存一个字符、重叠高亮重复渲染；「最近 7 天」趋势图实际只显示 2–3 天。
- **v56.4.2** —— 修复 issue #6：外层 shell 只「应用」而未「消费」窗口 inset，下层 9 个界面又各应用一遍，顶部多出整条状态栏高度。

完整历史见 [CHANGELOG.md](CHANGELOG.md)，更长期的规划与已知技术债见 `ROADMAP.md`，逐条行为陷阱见 `AGENTS.md`。

---

## 社区与贡献 / Community & Contributing

- 提 Issue 或 PR 之前请先阅读 [CONTRIBUTING.md](CONTRIBUTING.md)：环境搭建、CI 门禁、代码约定与安全红线都在里面（中英双语）。
- 所有参与者请遵守 [行为准则 / Code of Conduct](CODE_OF_CONDUCT.md)。
- 安全漏洞请勿开设公开 Issue，请按 [SECURITY.md](SECURITY.md) 通过私密漏洞报告渠道提交。

- Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or PR (setup, CI gates, conventions, security constraints; bilingual).
- All participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Security vulnerabilities: report privately per [SECURITY.md](SECURITY.md) — never as a public issue.

---

## 许可证

基于 [GNU General Public License v3.0](LICENSE) 开源。

<p align="center">Made with ❤️ by HuZaiGong</p>
