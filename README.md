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
  <b>面向 Android 平台的本地优先电子书阅读应用程序</b><br>
  不设账号体系，不提供云同步，不开展数据上报
</p>

---

## 一、项目概况

FlowReader 是面向 Android 平台开发的离线电子书阅读应用程序。应用无需注册或登录即可使用，不收集用户个人信息，不向外部采集或上报阅读行为数据，不上传崩溃日志，不接入第三方分析服务。

应用声明的网络权限仅为 `INTERNET`，使用范围限于访问用户局域网内的 OPDS 书库及在用户自有设备之间进行备份传输。为兼容旧版 Android，Manifest 仍保留带 `maxSdkVersion` 限制的旧存储权限，具体权限声明以 `AndroidManifest.xml` 为准。

当前版本为 **v56.6.2**。应用已提供中文、英文、日文、韩文、德文、西班牙文、法文、葡萄牙文、俄文共 9 种界面语言，用户可在应用内切换。

## 二、设计定位与适用范围

本项目实行本地优先的数据管理原则。书籍、阅读进度、阅读时长、书单及设置等数据均保存在设备本地；设备之间的迁移由用户通过备份文件或局域网传输功能主动完成。应用不提供账号体系、云端存储或后台同步服务。

该原则落实为以下约束：

- 阅读色板的文字与背景对比度须符合 WCAG AA 标准；相关色板均通过单元测试验证，未达到标准时构建流程将失败。
- 应用界面不得提供未具备对应实现的功能入口。原有但未实现的三种模拟翻页动画已连同入口一并移除。
- 含 DRM 保护的 MOBI、AZW 等文件直接拒绝导入，不实施任何解密或绕过处理。

## 三、支持的文件格式

应用支持 EPUB、TXT、PDF、Markdown、FB2、MOBI，以及漫画文件：单张 JPG、PNG、WebP 图片，或以 ZIP、CBZ 打包的图片集合。

FB2 与 MOBI 以只读方式导入，导入后按章节切分，处理方式与 EPUB 相同。

## 四、主要功能

### 4.1 书籍管理

- 支持单本导入、批量导入及通过系统“用其他应用打开”传入文件；上述入口统一使用受限导入管线。
- 书架支持按添加时间、最近阅读、书名、作者排序，提供网格与列表两种视图，并提供“继续阅读”快捷入口。
- 书籍作者、简介、封面及标签等元数据支持手动编辑。
- 基于 SQLite FTS5 提供跨书籍正文全文检索，同时支持书单创建、分类及书籍管理。
- 支持通过局域网 OPDS 协议连接 Calibre 或 Komga 服务端下载书籍。连接地址仅允许回环地址、RFC 1918 / RFC 4193 私有网段及 `.local` 域名；每次重定向均重新校验地址，公网地址不予访问。

### 4.2 阅读功能

- **阅读色板**：内置 18 套阅读色板，包括纸白、米黄、护眼绿、亚麻、晨雾、冷灰、电子墨水、曝光浅、石英粉、夜黑、墨蓝、深棕、曜石、纯黑、曝光深、极地、复古暖、深林；支持自定义文字色与背景色，并进行对比度校验及必要的自动调整。
- **排版设置**：字号范围为 12–32sp，行距范围为 1.0–2.5 倍；支持段间距、首行缩进及 `.ttf` / `.otf` 字体导入。中文正文行宽上限为 34 字。
- **翻页模式**：提供滑动模式、分页模式和无动画跳转模式。分页模式按照实际排版测量章节内容并支持横向翻页；三种模式均有对应实现。
- **文字选择**：支持长按按词吸附选择、拖拽扩展选区及双端手柄调整。浮动操作栏提供 5 色高亮、复制及添加书签功能，选区可映射回章节原文位置。
- **漫画浏览**：支持横向逐页浏览和纵向长列表浏览。
- **PDF 浏览**：支持缩放、拖拽翻页和矩形区域标注；由于 PDF 可能缺少文本层，标注以矩形区域保存和呈现。
- **交互配置**：点击分区、双击、长按、左右滑动及边缘热区宽度等参数均可配置，并即时生效。
- **辅助功能**：提供 TTS 朗读、专注模式、屏幕常亮、自动夜间模式（19:00–07:00，每分钟重新评估）、护眼提醒（15/20/30/45/60 分钟可选）及底部拖拽进度条。

### 4.3 应用配色系统

自 v56.6 起，应用配色来源分为 12 套内置配色、跟随壁纸动态配色和自定义配色三类。选择配色方案后，应用重新生成完整 Material 配色体系，而非仅替换强调色。默认配色为经典紫，并继续采用既有的手工调整品牌配色方案。

自定义配色提供色相环、饱和度/明度色盘、光谱条、明度条、饱和度条和十六进制颜色值输入框。上述输入方式共用同一 HSV 状态，面板实时显示配色预览及正文对比度实测值，用户确认“应用”后生效。

任意种子色生成的正文文字与背景配色，对比度均须达到 4.5:1（WCAG AA）。配色生成器位于 `:core` 模块，不依赖 Compose 运行时，可通过 JVM 单元测试独立验证。验证覆盖 12 套预设色板的明暗模式、13 组配对、每 5° 的色相采样，以及纯黑、纯白、中灰等边界输入。

主题模式（浅色、深色、跟随系统）、应用配色来源（内置、跟随壁纸、自定义）和阅读器色板彼此独立；阅读器色板不随应用主题模式变化。

### 4.4 笔记、统计与数据交换

- 高亮标记提供 5 种颜色，每条高亮可附加批注；书籍详情页支持将笔记导出为 Markdown、HTML 或纯文本，导出过程对特殊字符进行转义。
- 阅读统计按日记录阅读时长、阅读页数及阅读速度，提供趋势图、周报、月报及阅读目标。PAGED 模式和漫画按渲染页索引统计；SLIDE / NONE 模式以滚动位置作为字符位置代理，相关数据不表述为所有模式均精确统计真实字符数。
- 支持生成阅读分享卡片、将书架数据导出为 CSV 或 JSON、通过主屏小组件展示最近阅读书籍及进度，以及在同一 Wi-Fi 网络内进行局域网备份互传。
- 局域网传输使用随机令牌保护，传输服务在对话框关闭后停止；备份导入采用单一原子事务，导入失败时不保留部分数据。
- 提供决策转盘用于随机选择书籍。自 v52 起，入口由底部导航栏调整至书架顶部“更多”菜单。

## 五、构建环境与命令

### 5.1 环境要求

- JDK 17
- Android SDK 35（compileSdk 35 / minSdk 26）
- AGP 8.6.0 + Kotlin 2.1.0

Gradle Wrapper 从腾讯云镜像下载 Gradle 9.6.1。下载失败时，通常应优先检查镜像可访问性。

### 5.2 构建命令

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

./gradlew assembleDebug          # 开发版本
./gradlew testDebugUnitTest      # 全部 JVM 单元测试
```

### 5.3 CI 门禁

持续集成按以下顺序执行检查，开发环境可逐项执行：

```bash
./gradlew verifyKotlinStyle      # ktlint（:app 模块除外）+ 全仓库空白字符检查
./gradlew testDebugUnitTest      # :app / :core / :domain / :feature:reader
./gradlew coverageSummary        # 测试广度文件比 ≥ 40%（当前值 85.3%）
./gradlew assembleDebug
./gradlew verifyRoborazziDebug   # 截图回归测试
./gradlew performanceBaseline    # APK 体积对比 baseline/apk-size.properties
```

补充要求：`ktlint` 不应用于 `:app` 模块，但全仓库 `.kt` / `.kts` 文件不得包含 Tab 字符或行尾空格；`.editorconfig` 规定 4 空格缩进、LF 换行及 140 字符行宽。`coverageSummary` 检查的是测试文件数量比而非行覆盖率，新增领域模型、ViewModel 或 `:core` 主源文件时，应同步补充测试文件。

## 六、工程架构

本项目采用 Jetpack Compose + Material 3 技术栈，遵循 Clean Architecture + MVVM 多模块架构。

依赖方向约束为：`feature:* → core / domain`，`data → core / domain`，`app → core / data / domain / feature:*`。`:domain` 不得依赖 app、Room、Compose 或 Hilt。

| 模块 | 主要职责 |
|------|----------|
| `:app` | 组合根模块，包含 `MainActivity`、Hilt 依赖注入、导航、界面与 ViewModel，以及 10 个 Repository 实现 |
| `:core` | 设计系统、Token、`FlowTheme`、18 套阅读色板、配色生成器、通用组件及可在 JVM 独立测试的纯函数 |
| `:data` | Room 本地持久化层，包含 `AppDatabase`、8 个 Entity 和 7 个 DAO |
| `:domain` | 10 个 Repository 接口及领域模型；不设 `usecase/` 目录，业务逻辑由 ViewModel 及功能模块承载 |
| `:feature:reader` | `ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`、`ReaderPositionUnit` 及其单元测试 |
| `:feature:library` | 迁移边界占位模块，当前无源文件，已纳入编译、lint 和测试流程 |

数据流为：`Composable → ViewModel → domain 仓库接口 → data 仓库实现 → Room DAO / DataStore`。ViewModel 对外暴露不可变 `StateFlow<XxxUiState>`，错误处理使用 Kotlin 内置 `Result` 类型。

模块 namespace 与源码包名存在差异：模块 namespace 形如 `com.flowreader.domain`，源码包名统一为 `com.flowreader.app.*`。新增源码文件应遵循后者。

### 6.1 持久化存储

项目包含三套独立的持久化机制：

1. **Room 数据库**（`flowreader_db`）：版本 7，启用 `exportSchema = true`，包含 8 个 Entity 和 7 个 DAO。禁止使用 `fallbackToDestructiveMigration()`；Schema 变更必须提供显式迁移脚本。
2. **独立 SQLite 数据库**（`flowreader_fts.db`）：使用 FTS5 虚拟表，由独立组件管理，用于书内及全库搜索。
3. **DataStore Preferences**：用于保存应用及阅读偏好设置。

章节内容、元数据和封面缓存统一由 `util/CacheManager.kt` 管理。该组件根据可用内存和命中率调整每本书的缓存章节容量，并响应系统内存压力；不得新增重复的章节缓存组件。

详细模块图见 [ARCHITECTURE.md](ARCHITECTURE.md)，行为约束见 [AGENTS.md](AGENTS.md)。

## 七、安全边界与隐私要求

以下内容属于实现约束：

- 网络权限仅为 `INTERNET`，仅用于局域网 OPDS 和局域网传输；Manifest 另保留针对旧版 Android 的 `maxSdk` 存储权限。应用无账号、无云同步、无阅读行为统计上报、无崩溃上报。
- Android 自动备份规则的包含范围为空，书籍、阅读进度及设置不会经 Google 备份通道离开设备。跨设备迁移须通过应用内导出或局域网传输，并由用户主动触发。
- 导入容量上限为：EPUB 单章 16MB、内嵌图片 24MB、TXT/MD/FB2/MOBI 整文件 128MB、整包 256MB、EPUB 结构性元数据 4MB。ZIP/CBZ 拒绝绝对路径及 `..` 路径遍历，跳过 `__MACOSX` 目录和隐藏条目，限制条目数量及单条大小，仅允许解析器支持的扩展名。
- 外部文件名按不可信输入处理：仅取显示名称最后一段，过滤分隔符和控制字符，并在写入前通过规范路径确认目标位于应用私有目录内。
- 局域网备份接收端校验对方地址，仅接受回环地址、RFC 1918 / RFC 4193 私有地址或 `.local` 域名；路径须严格匹配，公网地址拒绝访问。
- 不进行 DRM 破解，含 DRM 保护的文件整体拒绝，不执行部分解码。
- ContentProvider 仅只读暴露书籍元数据及阅读进度，不暴露文件路径和正文内容；写入请求一律拒绝。自 v56.6.2 起，调用方须声明自定义权限并经用户运行时授权后方可读取。
- FileProvider 仅授权 `share_cards/` 子目录。应用不含 WebView；FTS 查询及 HTML/Markdown 导出均进行转义处理。
- Release 构建优先使用独立正式密钥签名，密钥库及密码不得进入版本控制；未配置密钥库时回退至 debug 签名，以确保 CI 和新克隆可构建。

## 八、当前版本与历史入口

当前版本为 **v56.6.2**。本版本完成第三方静态审查所列 7 项事项的处理，包括外部文件名路径校验、读取容量上限、局域网地址校验、备份服务器连接超时、Release 正式签名及 ContentProvider 权限门禁；同时补齐 `:core` 模块缺失的 5 种语言资源。

近期版本摘要如下：

- **v56.6.1**：修复非中文语言环境下的格式化异常、调色台组合期状态回写及自定义配色状态显示问题；更正备份规则注释。
- **v56.6.0**：新增 12 套内置配色、跟随壁纸动态配色和自定义配色，并新增调色台。
- **v56.5.2**：完善 Compose 稳定性配置，降低无关状态变化引起的界面重组范围。
- **v56.5.1**：完成书籍详情页和统计页的本地化改造。
- **v56.4.4**：修复数据加载完成后书架、统计和详情页面内容被顶栏遮挡的问题。
- **v56.4.3**：修复 FTS 书内检索、分页及漫画阅读统计、选区高亮边界、重叠高亮及最近日期趋势统计问题。
- **v56.4.2**：修复窗口 inset 在外层 shell 和各页面重复应用导致的布局异常。

完整版本历史见 [CHANGELOG.md](CHANGELOG.md)，长期规划及技术债务见 [ROADMAP.md](ROADMAP.md)。

---

## 九、社区参与与贡献

- 提交 Issue 或 PR 前，请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)（中英双语），其中载明环境搭建、CI 门禁、代码约定及安全边界。
- 所有参与者须遵守[行为准则](CODE_OF_CONDUCT.md)。
- 安全漏洞须按照 [SECURITY.md](SECURITY.md) 规定的私密渠道报告，不得通过公开 Issue 披露。

- Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or PR (setup, CI gates, conventions, security constraints; bilingual).
- All participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Security vulnerabilities: report privately per [SECURITY.md](SECURITY.md) — never as a public issue.

---

## 十、许可证

[GNU General Public License v3.0](LICENSE)

作者：HuZaiGong
