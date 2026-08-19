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
  <b>一个只属于这台设备的 Android 阅读器</b><br>
  没有账号，没有云同步，没有任何数据上报
</p>

---

## 这是什么

一个安卓电子书阅读器。打开就能读，不用注册，不用登录，也不会有人问你要手机号。

它不上传你的书，不统计你的阅读习惯，不收集崩溃日志。整个应用只申请了一个 `INTERNET` 权限，而这个权限只做两件事：连接你自己局域网里的 OPDS 书库，以及在你自己的两台设备之间传备份。想验证的话，`AndroidManifest.xml` 就那么长，翻一遍不费时间。

当前版本 **v56.6.1**。界面支持 9 种语言（中、英、日、韩、德、西、法、葡、俄），应用内随时切换。

## 为什么要这样做

云同步很方便，这一点没什么可争的。但方便的代价通常是：你的书单、你在哪一页停下、你每天读多久，都存在别人的服务器上。

FlowReader 选了另一边 —— 所有东西留在本地，代价是换设备得你自己动手（导出一个文件，或者在同一 WiFi 下直传）。这是个明确的取舍，不是还没做完的功能。

同样的想法贯穿到一些不那么显眼的地方：

- 阅读色板的对比度不是"设计师觉得差不多"，而是**每一套都由单元测试断言不低于 WCAG AA 标准**，改坏了构建就过不去。
- 设置里不会出现假开关。有过三种翻页动画只有 UI 入口而没有实现，后来连入口一起删了 —— 一个点了没反应的选项比没有这个选项更糟。
- 带 DRM 的 MOBI / AZW 直接拒绝导入，不做任何解密尝试。

## 能读什么

EPUB、TXT、PDF、Markdown、FB2、MOBI，以及漫画 —— 单张 JPG / PNG / WebP，或者打包好的 ZIP / CBZ。

FB2 和 MOBI 是只读导入，进来的时候会像 EPUB 一样切成章节。

## 用起来是什么样

### 找书、管书

导入支持单本和批量，从系统「用其他应用打开」进来的文件走的是同一条管线。书架可以按添加时间、最近阅读、书名、作者排序，网格和列表两种视图随你，顶部有张「继续阅读」的大卡片。作者、简介、封面、标签都能手动改 —— 元数据解析得再好也总有几本认不准的。

书多了之后有两个东西比较救命：一个是基于 SQLite FTS5 的**全库全文搜索**，能跨书搜正文，不只是搜书名；另一个是**书单**，自己建分类往里扔书。

如果你家里跑着 Calibre 或 Komga，可以直接连**局域网 OPDS** 下载。这里有个硬限制：只有回环地址、私有网段和 `.local` 这类内网名称可达，公网主机连不上，而且每一次重定向都会重新校验一遍 —— 免得一个看起来是内网的地址把你跳到外面去。

### 读

**18 套阅读色板**：纸白、米黄、护眼绿、亚麻、晨雾、冷灰、电子墨水、曝光浅、石英粉、夜黑、墨蓝、深棕、曜石、纯黑、曝光深、极地、复古暖、深林。也可以自己配背景色和文字色，同样会做对比度校验 —— 配出一对读不清的颜色时，它会自动往回调而不是让你眯着眼看。

排版方面：12–32sp 字号、1.0–2.5 倍行距、段间距、首行缩进，还能导入 `.ttf` / `.otf` 字体。中文正文按每行 34 字的上限控制行宽，这是个照抄排版经验的数字，比让文字铺满整个屏幕舒服。

**翻页有三种**：滑动（带动画滚动）、真实分页（逐页测量，横滑或点按翻页）、无动画直接跳。就这三种，都真的实现了。

**长按即选中**，按词吸附，可以拖拽扩选，两端有手柄。选完浮动栏里能高亮（5 色）、复制或加书签，选中范围和章节原文精确对应 —— 这一段自己写了个映射，因为 Compose 平台选中的那个接口至今还是 `internal`。

漫画是横向逐页或纵向长列表；PDF 支持缩放、拖拽翻页，以及框选一块区域做标注（PDF 没有文本层，所以标注是矩形的）。

点击分区、双击、长按、左右滑动、边缘热区宽度全都能配，而且配了真的生效。

另外还有 TTS 朗读、专注模式、屏幕常亮、定时自动夜间模式（19:00–07:00，每分钟重新判断）、护眼提醒（15/20/30/45/60 分钟可选）、底部可拖拽的进度条。

### 关于配色，多说两句

v56.6 把应用配色从两个选项扩成了 **12 套内置配色 + 跟随壁纸 + 自调色**。选一套配色会**重新生成整套 Material 配色**，不是只换个强调色的位置。经典紫是默认值，它直接沿用手工调过的那套品牌配色 —— 老用户升级后颜色一点不变，这是故意的。

「自调色」是个专门的调色台：**色相环 + 内嵌的饱和度／明度色盘**、**光谱条**、明度条、饱和度条，加一个十六进制输入框。五种输入方式绑在同一份 HSV 状态上，任何一种都能接着另一种没调完的颜色继续。弹窗里实时预览生成后的效果和实测正文对比度，点「应用」才真正写入。

这里有一条不太常见的承诺：**任意种子色生成出来的配色，每一组正文文字／背景配对都不低于 4.5:1**。做到这件事光靠色阶不够 —— 中等亮度的种子会让文字和背景撞到几乎一样的亮度上 —— 所以每个前景角色都会朝远离背景的方向扫描，最后兜底到纯黑或纯白。生成器写在 `:core` 里且不依赖 Compose，所以这条承诺是被单元测试逐一验证的：12 套预设 × 明暗两档 × 13 组配对，整个色相圈每 5° 一档，外加纯黑、纯白、中灰这些退化输入。

顺便说清楚三个互不干扰的维度：**主题模式**（浅色／深色／跟随系统）、**配色来源**（内置／跟随壁纸／自定义）、**阅读器色板**（那 18 套）。阅读器色板永远不跟随应用主题走。

### 记笔记、看数据

高亮 5 色，每条可以加自己的想法。书籍详情页能把笔记导出成 Markdown / HTML / 纯文本（导出时做转义，不会因为书里有个 `<` 就把文件搞坏）。

统计按天记时长、页数、速度，有柱状图趋势和周报月报，会告诉你读得最快的一天和最常翻的那本书，也可以设每日／每周／每月的阅读目标。

还有几个零碎但好用的：一键生成阅读分享卡片、书架导出 CSV / JSON、主屏小组件显示最近在读的书和进度、**局域网备份互传**（同一 WiFi 下两台设备直传，随机令牌保护，关掉对话框服务就停）。备份导入是**单个原子事务**，中途失败不会留下一半数据。

哦，还有个决策转盘 —— 不知道读哪本时转一下。v52 之后它从底部导航挪到了书架顶栏的「更多」里，因为它确实不配占一个一级入口。

---

## 自己编译

需要 **JDK 17** 和 **Android SDK 35**（compileSdk 35 / minSdk 26），AGP 8.6.0 + Kotlin 2.1.0。Gradle wrapper 从腾讯云镜像拉 Gradle 9.6.1，所以 wrapper 下载失败一般是镜像的事，不是项目的事。

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader

./gradlew assembleDebug          # 开发包
./gradlew testDebugUnitTest      # 全部 JVM 单测
```

CI 按固定顺序跑六道门，本地也可以照着跑：

```bash
./gradlew verifyKotlinStyle      # ktlint（:app 以外）+ 全仓空白字符检查
./gradlew testDebugUnitTest      # :app / :core / :domain / :feature:reader
./gradlew coverageSummary        # 测试广度文件比 ≥ 40%（当前 80.9%）
./gradlew assembleDebug
./gradlew verifyRoborazziDebug   # 截图回归
./gradlew performanceBaseline    # APK 体积对比 baseline/apk-size.properties
```

两个第一次改代码容易踩的坑：

- **ktlint 不管 `:app`**。`:app` 的 Kotlin 只被空白字符门禁扫 —— 全仓任意 `.kt` / `.kts` 里出现**一个** tab 或行尾空格，整个构建就红。`.editorconfig` 是 4 空格、LF、行宽 140。
- **`coverageSummary` 是文件数量比，不是行覆盖率**。分母算的是 `:core` 的每个主源文件、领域模型、仓库接口、ViewModel。所以新加一个领域模型却不加测试，哪怕别处一行没动也会挂。

---

## 代码长什么样

**Jetpack Compose + Material 3**，**Clean Architecture + MVVM** 多模块。依赖方向：`feature:* → core / domain`，`data → core / domain`，`app → core / data / domain / feature:*`。`:domain` 里没有 app / Room / Compose / Hilt 任何一个依赖，纯 Kotlin。

| 模块 | 装了什么 |
|------|------|
| `:app` | 组装根：`MainActivity`、Hilt 装配、导航、全部界面与 ViewModel、**全部 10 个 Repository 实现** |
| `:core` | 设计系统：Token、`FlowTheme`、18 套阅读色板、配色生成器、通用组件，以及一批不依赖 Compose、能在 JVM 上直接测的纯函数 |
| `:data` | 只有 Room：`AppDatabase` + 8 个 Entity + 7 个 DAO |
| `:domain` | 10 个 Repository 接口 + 领域模型。没有 `usecase/` —— 业务逻辑有意留在各自的 ViewModel 里 |
| `:feature:reader` | `ChapterPaginator`、`ReaderProgressEngine`、`ReaderSessionTracker`、`ReaderPositionUnit`，都有单测 |
| `:feature:library` | **还没有源文件**，是个已经接进编译／lint／测试的占位模块 |

数据流是 `Composable → ViewModel → domain 仓库接口 → data 仓库实现 → Room DAO / DataStore`。每个 ViewModel 暴露一个不可变的 `StateFlow<XxxUiState>`，错误用 Kotlin 内置的 `Result`。

有个命名错位值得提前知道：模块 namespace 是 `com.flowreader.domain` 这种，但源码包名统一是 `com.flowreader.app.*`。新文件请跟后者。

持久化有**三套，互相独立**：Room（`flowreader_db`，version 7，**没有** `fallbackToDestructiveMigration()`，每次改 schema 都得手写迁移）；一个 Room 之外的裸 SQLite 库（`flowreader_fts.db`，FTS5 虚拟表，撑起书内和全库搜索）；DataStore Preferences（所有偏好设置）。

章节／元数据／封面缓存只有 `util/CacheManager.kt` 一个，按可用内存定容，响应系统内存压力，还会按命中率自适应每本书的章节容量。别再加第二个。

更细的模块图看 `ARCHITECTURE.md`，逐条行为陷阱看 `AGENTS.md` —— 后者是这个项目里最值得先读的文件。

---

## 隐私与安全边界

这些是硬约束，不是"目前的实现"：

- **只有一个 `INTERNET` 权限**，只给局域网 OPDS 和局域网传输用。无账号、无统计、无崩溃上报、无同步。
- **Android 自动备份什么都不传**。备份规则里唯一列出的域在本应用中是空的，所以书、进度、设置都不会经 Google 的备份通道离开设备。这是刻意的 —— 跨设备迁移走应用内的导出／局域网传输，由你自己触发。
- **导入处处有上限**：EPUB 单章 16MB、内嵌图片 24MB、TXT/MD/FB2/MOBI 整文件 128MB。ZIP / CBZ 拒绝绝对路径和 `..`（zip-slip），跳过 `__MACOSX` 和隐藏项，限制条目数与单条大小，只放行解析器认识的扩展名。
- **不做 DRM 破解**，带 DRM 的文件整体拒绝而不是半解码。
- **ContentProvider** 只暴露只读的书籍元数据和进度 —— 不含文件路径，不含正文，写入一律拒绝。
- **FileProvider 只授权了 `share_cards/` 一个子目录**（全仓就一处 `getUriForFile()` 调用）。
- **全应用没有 WebView**。FTS 查询和 HTML / Markdown 导出都做转义；审计门禁要求不新增 `!!`。

---

## 最近的变化

- **v56.6.1** —— v56.6.0 的排查性修复。调色台的对比度数值原先用不带 locale 的 `String.format` 预格式化，而字符串资源是按应用内语言解析的，在小数点为逗号的语言下两边不一致并会抛异常；十六进制输入框在组合期回写了自己读的状态，每拖一次色相环就白付一趟组合；「自调色」副标题在已经切回内置配色之后还声称自定义色在用。另外把 `backup_rules.xml` 那份写反了的注释改正 —— 备份范围一直是空的（什么都不出设备），这是刻意的，所以只改注释不改行为。
- **v56.6.0** —— 配色来源从 2 个选项扩成 **12 套内置配色 + 跟随壁纸 + 自调色**，新增色相环／色盘／光谱条调色台。`:core` 的 `SeedColorScheme` 从单一种子色生成 24 个 Material 角色，并保证任意种子下正文对比度不低于 WCAG AA 的 4.5:1。
- **v56.5.2** —— Compose 稳定性配置消掉了 domain 模型的不稳定推断。`:domain` 没有 Compose 编译器，于是 `Book`／`Chapter` 这些参数全被推断为不稳定，导致任意状态变更都会重新执行每一张可见书卡。声明稳定后不稳定类从 52 降到 37，所有 UiState 现在都稳定。
- **v56.5.1** —— 书籍详情与统计页本地化。domain 模型现在带纯数值而不是拼好的字符串，不会再被 v53 的语言切换冻结成中文。
- **v56.4.4** —— 修了书架／统计／详情三页「加载出数据后内容被顶栏遮挡」：`FlowStateHost` 的成功分支把 `modifier` 丢了，于是 88dp 安全区在有数据时消失，而同一页的加载／空／错误状态反倒都正常。
- **v56.4.3** —— 一次 11 处的功能性 bug 排查。书内全文搜索因 FTS5 列没有类型亲和性而恒返回空；PAGED 模式和漫画的阅读统计整段丢失；划词高亮少存一个字符、重叠高亮重复渲染；「最近 7 天」趋势图实际只显示 2–3 天。
- **v56.4.2** —— 修了 issue #6：外层 shell 只「应用」而没有「消费」窗口 inset，底下 9 个界面又各应用一遍，顶部白多出一整条状态栏的高度。

完整历史在 [CHANGELOG.md](CHANGELOG.md)，更长期的规划和已知技术债在 `ROADMAP.md`。

---

## 参与进来 / Community & Contributing

- 提 Issue 或 PR 之前请先读 [CONTRIBUTING.md](CONTRIBUTING.md)：环境搭建、CI 门禁、代码约定和安全红线都在里面（中英双语）。
- 参与者请遵守[行为准则](CODE_OF_CONDUCT.md)。
- **安全漏洞请不要开公开 Issue**，按 [SECURITY.md](SECURITY.md) 走私密报告渠道。

- Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening an issue or PR (setup, CI gates, conventions, security constraints; bilingual).
- All participants are expected to follow the [Code of Conduct](CODE_OF_CONDUCT.md).
- Security vulnerabilities: report privately per [SECURITY.md](SECURITY.md) — never as a public issue.

---

## 许可证

[GNU General Public License v3.0](LICENSE)

<p align="center">Made with ❤️ by HuZaiGong</p>
