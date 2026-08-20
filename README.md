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
  <b>本地优先的 Android 电子书阅读器</b><br>
  无账号体系，无云同步，无数据上报
</p>

---

## 概述

FlowReader 是一款面向 Android 平台的离线电子书阅读应用程序。本软件无需注册或登录即可使用，不收集用户个人信息，不采集阅读行为数据，不上传崩溃日志，不接入任何第三方分析服务。

本软件仅申请一项 `INTERNET` 权限，用途限于以下两项：访问用户局域网内的 OPDS 书库，以及在用户自有设备之间进行备份传输。权限声明详见 `AndroidManifest.xml`。

当前版本 **v56.6.2**。界面已本地化为 9 种语言（中文、英文、日文、韩文、德文、西班牙文、法文、葡萄牙文、俄文），用户可在应用内随时切换。

## 设计原则

云同步功能在便利性方面具有明显优势，但其代价通常涉及用户隐私让渡：书单、阅读进度、阅读时长等数据需存储于第三方服务器。

FlowReader 采取相反的设计取向：所有数据本地存储，设备间迁移需由用户手动完成（导出备份文件，或通过局域网直传）。此为有意识的设计取舍，并非功能未完成。

上述原则亦体现于以下具体实现：

- 阅读色板的对色度比须满足 WCAG AA 标准，每套色板均通过单元测试验证，任何修改若导致标准不达标均会阻断构建流程。
- 应用程序不包含无实际功能对应的界面入口。此前存在的三种翻页动画仅有界面入口而缺乏实现，已于后续版本中连同入口一并移除。
- 含有 DRM 保护的 MOBI 及 AZW 文件直接拒绝导入，不进行任何解密尝试。

## 支持格式

本软件支持以下文件格式：EPUB、TXT、PDF、Markdown、FB2、MOBI，以及漫画格式（单张 JPG / PNG / WebP 图片，或打包为 ZIP / CBZ 的图片集合）。

FB2 与 MOBI 格式以只读方式导入，导入后按章节切分，处理方式与 EPUB 一致。

## 功能说明

### 书籍管理

导入功能支持单本导入及批量导入。通过系统"用其他应用打开"传入的文件，均经同一导入管线处理。

书架支持以下排序方式：添加时间、最近阅读、书名、作者。提供网格与列表两种视图模式。书架顶部设有"继续阅读"快捷入口。书籍元数据（作者、简介、封面、标签）支持手动编辑。

本软件提供基于 SQLite FTS5 的全文搜索功能，可跨书籍检索正文内容，不限于书名检索。另提供书单功能，用户可自行创建分类并管理书籍。

支持通过局域网 OPDS 协议连接 Calibre 或 Komga 服务端下载书籍。连接地址须满足以下限制：仅限回环地址、私有网段（RFC 1918 / RFC 4193）及 `.local` 域名，公网地址不可达。每次重定向均重新校验地址合规性。

### 阅读功能

**色板配置**：内置 18 套阅读色板，包括纸白、米黄、护眼绿、亚麻、晨雾、冷灰、电子墨水、曝光浅、石英粉、夜黑、墨蓝、深棕、曜石、纯黑、曝光深、极地、复古暖、深林。用户亦可自定义背景色与文字色，自定义色板同样接受对比度校验，未达标准时将自动调整。

**排版设置**：字号范围 12–32sp，行距范围 1.0–2.5 倍，支持段间距与首行缩进，支持导入 `.ttf` / `.otf` 字体文件。中文正文行宽上限为 34 字，此为根据排版经验确定的值。

**翻页模式**：提供三种翻页模式——滑动模式（带滚动动画）、分页模式（逐页测量，支持横向滑动或点击翻页）、无动画跳转模式。三种模式均已完整实现。

**文字选择**：支持长按按词吸附选择，可拖拽扩展选区，两端设有选择手柄。选择后浮动操作栏提供高亮（5 色可选）、复制、添加书签等功能。选中范围与章节原文精确对应，由自实现映射组件完成，原因在于 Compose 平台内置选中接口仍为 `internal` 访问级别。

**漫画浏览**：支持横向逐页浏览与纵向长列表浏览两种模式。

**PDF 浏览**：支持缩放、拖拽翻页，以及矩形区域标注（PDF 格式缺乏文本层，故标注以矩形形式呈现）。

**交互配置**：点击分区、双击、长按、左右滑动、边缘热区宽度等交互参数均可自定义配置，配置后即时生效。

**附加功能**：TTS 朗读、专注模式、屏幕常亮、定时自动夜间模式（19:00–07:00，每分钟重新评估）、护眼提醒（可选 15/20/30/45/60 分钟）、底部可拖拽进度条。

### 配色系统

v56.6 版本将应用配色来源从两项扩展为以下三类：12 套内置配色、跟随壁纸动态配色、自调色。选择配色方案后，系统将重新生成整套 Material 配色体系，而非仅替换强调色。默认配色为经典紫，沿用预先手工调整的品牌配色方案，以保持升级后外观一致。

自调色功能提供以下调色组件：色相环及内嵌饱和度/明度色盘、光谱条、明度条、饱和度条、十六进制颜色值输入框。五种输入方式绑定至同一 HSV 状态，任一种方式均可接续其他方式的未完成颜色继续调整。调色面板实时预览生成效果与正文对比度实测值，点击"应用"后方生效。

系统承诺：任意种子色生成的配色方案，其正文文字与背景之间的对比度均不低于 4.5:1（WCAG AA 标准）。实现方式为：各前景色角色向远离背景方向扫描，兜底至纯黑或纯白。配色生成器位于 `:core` 模块，不依赖 Compose 运行时，可被单元测试独立验证。验证覆盖范围包括：12 套预设色板 × 明暗两档 × 13 组配对，色相圈每 5° 采样，外加纯黑、纯白、中灰等边界输入。

以下三个维度相互独立，互不干扰：主题模式（浅色/深色/跟随系统）、配色来源（内置/跟随壁纸/自定义）、阅读器色板（前述 18 套）。阅读器色板不受应用主题模式影响。

### 笔记与统计

高亮标记支持 5 色区分，每条高亮可附加批注。书籍详情页支持将笔记导出为 Markdown、HTML 或纯文本格式，导出过程对特殊字符进行转义处理。

阅读统计按日记录阅读时长、阅读页数及阅读速度，提供柱状图趋势、周报及月报，展示阅读速度最快日期及最常阅读书籍。支持设置每日、每周或每月的阅读目标。

其他功能：一键生成阅读分享卡片、书架数据导出为 CSV 或 JSON 格式、主屏小组件显示最近阅读书籍及进度、局域网备份互传（同一 WiFi 网络下设备直传，随机令牌保护，传输服务在对话框关闭后即停止）。备份导入采用单个原子事务，中途失败不会残留部分数据。

另提供决策转盘功能，用于随机选择阅读书籍。v52 版本起，该功能入口从底部导航栏移入书架顶部"更多"菜单。

## 构建指南

### 环境要求

- JDK 17
- Android SDK 35（compileSdk 35 / minSdk 26）
- AGP 8.6.0 + Kotlin 2.1.0

Gradle wrapper 从腾讯云镜像下载 Gradle 9.6.1，下载失败通常与镜像可访问性相关，而非项目配置问题。

### 构建命令

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

./gradlew assembleDebug          # 开发版本
./gradlew testDebugUnitTest      # 全部 JVM 单元测试
```

### CI 门禁

持续集成按以下顺序执行六项检查，支持本地执行：

```bash
./gradlew verifyKotlinStyle      # ktlint（:app 模块除外）+ 全仓库空白字符检查
./gradlew testDebugUnitTest      # :app / :core / :domain / :feature:reader
./gradlew coverageSummary        # 测试广度文件比 ≥ 40%（当前值 85.3%）
./gradlew assembleDebug
./gradlew verifyRoborazziDebug   # 截图回归测试
./gradlew performanceBaseline    # APK 体积对比 baseline/apk-size.properties
```

### 常见注意事项

1. ktlint 不应用于 `:app` 模块。该模块的 Kotlin 代码仅受空白字符门禁约束：全仓库任意 `.kt` 或 `.kts` 文件若包含 Tab 字符或行尾空格，将导致构建失败。`.editorconfig` 配置为 4 空格缩进、LF 换行、行宽 140 字符。

2. `coverageSummary` 检查指标为文件数量比，而非行覆盖率。分母包含 `:core` 模块全部主源文件、领域模型、仓库接口及 ViewModel。新增领域模型而未添加对应测试文件，将导致此项检查失败。

## 架构说明

本软件采用 Jetpack Compose + Material 3 技术栈，遵循 Clean Architecture + MVVM 多模块架构。

依赖方向约束如下：`feature:* → core / domain`，`data → core / domain`，`app → core / data / domain / feature:*`。`:domain` 模块不依赖 app、Room、Compose 或 Hilt 组件。

| 模块 | 内容 |
|------|------|
| `:app` | 组合根模块：`MainActivity`、Hilt 依赖注入配置、导航组件、全部界面与 ViewModel、全部 10 个 Repository 实现 |
| `:core` | 设计系统：Token、`FlowTheme`、18 套阅读色板、配色生成器、通用组件，以及不依赖 Compose 的纯函数（可在 JVM 上直接测试） |
| `:data` | 数据持久化层仅含 Room 组件：`AppDatabase`、8 个 Entity、7 个 DAO |
| `:domain` | 10 个 Repository 接口及领域模型。不包含 `usecase/` 目录，业务逻辑有意识地位于各 ViewModel 中 |
| `:feature:reader` | 包含 `ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`、`ReaderPositionUnit`，均配备单元测试 |
| `:feature:library` | 占位模块，尚无源文件，已接入编译/lint/测试流程 |

数据流方向为：`Composable → ViewModel → domain 仓库接口 → data 仓库实现 → Room DAO / DataStore`。各 ViewModel 对外暴露不可变 `StateFlow<XxxUiState>`，错误处理使用 Kotlin 内置 `Result` 类型。

模块命名空间与源码包名存在差异：模块 namespace 形如 `com.flowreader.domain`，而源码包名统一为 `com.flowreader.app.*`。新增文件应与后者保持一致。

### 持久化存储

本软件包含三套独立的持久化存储机制：

1. **Room 数据库**（`flowreader_db`）：版本号 7，启用 `exportSchema = true`。**不启用** `fallbackToDestructiveMigration()`，每次 Schema 变更均须手工编写迁移脚本。
2. **独立 SQLite 数据库**（`flowreader_fts.db`）：使用 FTS5 虚拟表，独立于 Room 管理，用于书内搜索和全库搜索。
3. **DataStore Preferences**：存储所有偏好设置。

章节内容、元数据及封面缓存统一由 `util/CacheManager.kt` 管理。该组件根据可用内存确定容量，响应系统内存压力回调，并根据命中率自适应调整每本书的缓存章节数量。不应新增其他缓存组件。

详细模块图见 `ARCHITECTURE.md`，行为约束列表见 `AGENTS.md`。

## 隐私与安全规范

以下为硬性约束，非当前实现状态：

- 仅申请一项 `INTERNET` 权限，专用于局域网 OPDS 和局域网传输。无账号、无统计、无崩溃上报、无数据同步。
- Android 自动备份不传输任何数据。备份规则文件所列域在应用中为空，故书籍、阅读进度及设置不会通过 Google 备份通道离开设备。跨设备迁移须通过应用内导出或局域网传输功能，由用户主动触发。
- 导入操作设有容量上限：EPUB 单章 16MB、内嵌图片 24MB、TXT/MD/FB2/MOBI 整文件 128MB、整包 256MB、EPUB 结构性元数据 4MB。ZIP/CBZ 文件拒绝绝对路径及 `..` 路径遍历（zip-slip 防护），跳过 `__MACOSX` 目录及隐藏条目，限制条目总数及单条大小，仅放行解析器支持的扩展名。
- 外部传入的文件名视为不可信输入。通过"用 FlowReader 打开"传入的显示名称仅取最后一段，过滤分隔符及控制字符，写入前通过规范路径确认目标文件位于应用私有目录内。
- 局域网备份接收端校验对方地址：备份链接须为回环地址、RFC 1918/RFC 4193 私有地址或 `.local` 域名，路径须严格匹配，公网链接拒绝。
- 不进行 DRM 破解，含 DRM 保护的文件整体拒绝，不进行部分解码。
- ContentProvider 仅暴露只读的书籍元数据及阅读进度，不含文件路径及正文内容，写入请求一律拒绝。该接口继续对外开放（供自动化工具等读取），但自 v56.6.2 起须由调用方声明自定义权限并经用户运行时授权，不再对同机应用静默开放。
- FileProvider 仅授权 `share_cards/` 一个子目录（全仓库仅一处 `getUriForFile()` 调用）。
- 本软件不含 WebView 组件。FTS 查询及 HTML/Markdown 导出均进行转义处理。代码审计要求不新增 `!!` 操作符。
- Release 构建使用独立的正式密钥签名，密钥库与密码不进版本控制；无密钥库的环境（CI、新克隆）回退至 debug 签名以保证可构建。

## 版本历史

- **v56.6.2**：第三方静态审查发现 7 项问题，全部处理完毕。最严重问题为：通过"用 FlowReader 打开"传入的文件名可包含 `../` 路径遍历序列，导致文件写入应用私有目录之外。另有三项代码缺陷：EPUB 结构性 XML 无读取上限、局域网接收端仅校验协议未校验地址（公网链接可被误拉取）、备份服务器可被单方面连接占用；四项均配备回归测试，撤销修复即导致测试失败。剩余三项：release 构建改用正式密钥签名（并经 `apksigner` 实测确认）；导出的 ContentProvider 加上自定义权限门禁，须用户授权方可读取；报告所指的明文 API key 经全历史核查确认位置有误——被提交的其实只有一份计划文档中引用的字面量，已改为不复述。
- **v56.6.1**：v56.6.0 的排查性修复。调色台对比度数值使用不带 locale 的 `String.format` 预格式化，而字符串资源按应用内语言解析，在小数点为逗号的语言环境下导致类型不匹配异常；十六进制输入框在组合期间回写自身状态，每次拖拽色相环均触发额外一次组合；"自调色"副标题在已切换至内置配色后仍显示自定义色在使用中。另修正 `backup_rules.xml` 注释表述，备份范围始终为空，此为有意设计，仅改注释不改行为。
- **v56.6.0**：配色来源扩展至 12 套内置配色、跟随壁纸、自调色三类，新增色相环/色盘/光谱条调色台。`:core` 模块 `SeedColorScheme` 从单一种子色生成 24 个 Material 色值角色，并保证任意种子色下正文对比度不低于 WCAG AA 标准的 4.5:1。
- **v56.5.2**：Compose 稳定性配置消除 domain 模型的不稳定推断。`:domain` 模块缺乏 Compose 编译器，导致 `Book`/`Chapter` 等参数被推断为不稳定，触发任意状态变化时重新执行所有可见书籍卡片。声明稳定后不稳定类从 52 降至 37，所有 UiState 现为稳定状态。
- **v56.5.1**：书籍详情页与统计页本地化。domain 模型现使用纯数值而非预拼接字符串，不再受 v53 语言切换冻结影响。
- **v56.4.4**：修复书架/统计/详情三页加载数据后内容被顶栏遮挡问题。`FlowStateHost` 成功分支未应用 `modifier`，导致 88dp 安全区在有数据时消失，而同一页的加载/空/错误状态正常。
- **v56.4.3**：11 处功能性 Bug 修复。书内全文搜索因 FTS5 列缺乏类型亲和性而恒返回空；PAGED 模式及漫画的阅读统计丢失；划词高亮少存一个字符、重叠高亮重复渲染；"最近 7 天"趋势图实际仅显示 2–3 天。
- **v56.4.2**：修复 Issue #6。外层 shell 仅应用而未消费窗口 inset，下层 9 个界面各自重复应用，导致顶部多出一整条状态栏高度。

完整版本历史见 [CHANGELOG.md](CHANGELOG.md)，长期规划及技术债务见 `ROADMAP.md`。

---

## 社区参与 / Community & Contributing

- 提交 Issue 或 PR 前，请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)（中英双语），其中包含环境搭建、CI 门禁、代码约定及安全红线。
- 参与者须遵守[行为准则](CODE_OF_CONDUCT.md)。
- 安全漏洞报告请通过 [SECURITY.md](SECURITY.md) 所述私密渠道提交，不得以公开 Issue 形式报告。

- Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or PR (setup, CI gates, conventions, security constraints; bilingual).
- All participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Security vulnerabilities: report privately per [SECURITY.md](SECURITY.md) — never as a public issue.

---

## 许可证

[GNU General Public License v3.0](LICENSE)

<p align="center">Made with ❤️ by HuZaiGong</p>