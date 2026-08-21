# FlowReader 长期规划书

> 版本: v56.6.2 | 更新: 2026-08-20

---

## 一、产品定位

FlowReader 是一款**纯本地、离线优先**的 Android 电子书阅读器，专注于沉浸式阅读体验。不依赖任何云服务，用户数据完全存储在本地。核心差异化在于：多格式支持（EPUB/TXT/PDF/MD）、高度可定制的阅读排版、全文检索、阅读统计分析，以及轻量级的决策辅助工具。

### 核心原则

1. **离线优先** — 不引入账号系统，不上传用户数据
2. **性能敏感** — 启动速度 < 1s，翻页无卡顿，大型书籍（>10MB）流畅加载
3. **隐私至上** — 所有数据存储在本地，备份文件由用户自行管理
4. **克制迭代** — 不堆砌功能，每项新功能必须回答"对沉浸式阅读有何帮助"

---

## 二、现状评估 (v56.6.2)

### 已完成的核心能力

| 领域 | 状态 | 说明 |
|------|------|------|
| 多格式支持 | ✅ 扩展 | EPUB / TXT / PDF / Markdown / FB2 / MOBI / JPG / PNG / WebP / CBZ（只读，拒绝 DRM） |
| 阅读排版 | ✅ 稳定 | 字体/字号/行距/翻页模式/屏幕常亮/专注模式；18 套阅读色板 + 自定义背景（带强制可读性蒙层） |
| 应用配色 | ✅ 扩展 | 12 套内置配色 + 跟随壁纸 + 自调色（色相环／光谱条调色台）；任意种子色由 `SeedColorScheme` 保证 WCAG AA |
| 多语言 | ✅ 扩展 | 9 种语言（zh / en / ja / ko / de / es / fr / pt / ru），应用内随时切换 |
| 全文搜索 (FTS5) | ✅ 稳定 | 单书检索 + 全库跨书检索，结果标明书籍来源 |
| 标注/高亮 | ✅ 稳定 | 5 色高亮，批注笔记，CRUD |
| 阅读统计 | ✅ 可用 | 分页/漫画按页索引累计；滑动/无动画模式仍以滚动像素作为字符位置代理，周/月报告与目标可用但绝对字数不应视为精确值 |
| 决策转盘 | ✅ 稳定 | 自定义选项和颜色，60fps 动画 |
| 备份恢复 | ✅ 基础 | 导出/导入书籍 + 进度 + 标注 |
| ZIP / OPDS 导入 | ✅ 基础 | ZIP 批量导入；OPDS 仅允许局域网 / .local 地址 |
| 自定义字体 | ✅ 基础 | .ttf/.otf 导入 |
| 阅读标签 | ✅ 扩展 | 书籍详情可编辑标签，书架支持筛选与批量编辑 |
| 阅读列表 | ✅ 基础 | 自定义书单，支持添加/移出书籍和排序 |
| 阅读笔记管理 | ✅ 基础 | 全库笔记集中检索、删除和导出 |
| 书签系统 | ✅ 回归 | 阅读器入口恢复，长按段落可添加备注书签 |
| TTS 朗读 | ✅ 回归 | 系统 TextToSpeech API，支持朗读/暂停 |
| 局域网传输 | ✅ 基础 | `LanTransferServer` 随机端口 + 16 位令牌只服务一个备份文件；两端都只接受局域网地址 |
| 分享与导出 | ✅ 基础 | 阅读卡片 PNG（`ShareCardGenerator`）、书架 CSV/JSON（`ShelfExporter`）、标注 MD/HTML/TXT |
| 主屏 Widget | ✅ 基础 | 显示当前书名与进度百分比（读 DataStore 快照） |
| ContentProvider | ✅ 基础 | 只读暴露书籍元数据与进度，无文件路径/正文，写操作拒绝 |
| 自适应布局 | ✅ 基础 | <600dp 底部栏 / ≥600dp 导航栏；书架网格与列表双视图 |

### 已知技术债务

- `:feature:library` 与 `:feature:reader` 仍是迁移边界，业务 UI 尚主要留在 `:app`（v54 起阅读器纯逻辑已迁入 `:feature:reader`）
- `SavedStateHandle` 取出的 bookId 默认 0L，各 ViewModel 需各自校验
- Room DB version 7，已有 4→5、5→6、6→7 显式迁移，无 destructive migration 兜底
- 分页模式进度以"页号"为位置语义，与滚动模式像素语义并存；切换模式后进度按章内比例近似恢复。v56.4.3 起这两种单位由 `ReaderPositionUnit` 显式区分，阅读统计各走各的计数路径
- **滑动/无动画模式的阅读统计仍以滚动像素近似字符位置**：`ReaderContent` 把 `ScrollState.value` 传入 `ReaderSessionTracker.recordProgress()`；分页和漫画则走 `ReaderPositionUnit.PAGE_INDEX`。因此 SLIDE/NONE 下的已读字数、速度和页数适合看趋势，不应当作排版无关的精确值。后续若改为像素到字符的映射，需要同步调整统计口径和测试。

### 静态审查 7 条的处置（v56.6.2 收口）

`SECURITY_REVIEW_2026-08-20.md`（第三方静态审查）共报 7 条，v56.6.2 全部处理完毕。#1–#4 是代码缺陷，各配回归测试；剩下三条的处置记在这里，因为它们的"改法"比"改动"更值得留档：

- **#5 明文 API key —— 报告的位置是错的，我第一次核查的结论也是错的。** 报告说在 `.claude/providers.yaml`（`.claude/` 从 v56.6.1 起整体 git-ignore）；我第一次说在根目录 `providers.yaml`、自 v45.0.2 进了历史。都不对。真相是：`providers.yaml` 的提交历史里**从来没有过**明文 key，一直是 `{env:DEEPSEEK_API_KEY}`；本机工作区那份确实有真 key，但被 `git update-index --skip-worktree` 藏住了（`git ls-files -v` 前缀 `S`），所以 `git status` 永远干净 —— 这正是我误判的来源。全历史扫描后，唯一真被提交过的 key 字面量在 `V56.5.0_PLAN.md:128`，那段话正在论证"这个 key 是公共占位值、不算泄露"，然后把值抄进了正文。已改为不复述。key 本身是公共免费中转站的共享值，没轮换；改写已公开的 `main` 历史是破坏性操作，留给仓库主人定。**方法论教训：这类判断要看 `git ls-files -v` 和全历史扫描，不能看工作区文件。**
- **#6 release 签名 —— 已换成正式密钥。** `app/build.gradle.kts` 新增 `signingConfigs { create("release") }`，从 git-ignore 的 `keystore.properties` 读密钥库；文件缺失时回退 debug，好让 CI 和新克隆照样产出可测的 `app-release.apk`。新增 `keystore.properties.example`。**验证过，不是"配置看着对"**：`apksigner verify --print-certs` 给出 `CN=HuZaiGong, OU=Dev, O=flowreader`，与 debug 的 `CN=Android Debug` 指纹不同。踩过一个坑：第一次验证时磁盘上的 APK 比 `keystore.properties` 还老，配置是对的但产物还是旧的 —— 先看时间戳。
- **#7 导出的 ContentProvider —— 已加权限门禁。** 新增 `com.flowreader.app.permission.READ_LIBRARY`（`dangerous`）挂在 `android:readPermission` 上：调用方得声明权限、还得用户运行时同意，导出能力保留。取 `dangerous` 不取 `signature`，是因为后者只允许同签名应用读，等于把功能删了而不是给它加门。写权限用 `signature` 级兜底（`insert`/`update`/`delete` 本来就抛异常）。代价说清楚：已有外部集成方升级后会拿到 `SecurityException` —— 核查过仓库内外都没有已知集成方，README/ROADMAP 也从没把它当对外接口宣传，所以收紧。这道门在 Android 框架侧执行，单测覆盖不到，manifest 属性就是执行点。

### 流程债

- **v56.5.0 曾漏记 CHANGELOG**：两个 `wip` 提交完成了阅读器入口本地化、6 套新色板和阅读背景图。现已补入 `CHANGELOG.md`，发布规则仍要求每个版本保留一条条目。
- **CI 只在 `main` 上跑**（`.github/workflows/ci.yml` 的 `push` / `pull_request` 都只监听 `main`），所以推到 `dev` 的提交没有任何远端验证，六道门禁必须在本地实跑。
- **中英文 agent 文档需要持续同步**：`AGENTS.md` / `AGENTS_CN.md` 与 `CLAUDE.md` / `CLAUDE_CN.md` 都描述实现约束。英文版是事实基准，中文镜像应在同一提交更新；若无法维护镜像，应删除镜像而不是保留过期版本。

---

## 三、短期目标 (v48 — v50)

### v48 — 稳定性和体验打磨

**目标**: 修复已知问题，提升日常使用的流畅度

- [x] **阅读统计基础能力**：按阅读位置累计页数并区分连续阅读会话（暂停超过 5 分钟另计新会话）；滑动模式的滚动像素代理口径仍列在当前技术债务中
- [x] **护眼提醒可配置**：提醒间隔从硬编码 20 分钟改为用户可设置（15/20/30/45/60 分钟）
- [x] **翻页记忆**：每个章节独立记忆滚动位置，切换回来后恢复
- [x] **书架搜索**：书架搜索已支持作者匹配，并新增分类筛选入口
- [x] **错误状态优化**：加载失败时提供重试按钮而非仅文字提示
- [x] **APK 体积优化**：启用 R8 完整模式并保留资源压缩

### v49 — 阅读体验增强

**目标**: 让"读"本身更舒适、更专注

- [x] **阅读统计回归书签系统**：解冻书签，重新设计交互——长按段落添加书签，支持书签备注
- [x] **朗读模式 (TTS 回归)**：重新实现 TTS，使用系统 TextToSpeech API，支持朗读/暂停/停止，不引入第三方 SDK
- [x] **阅读进度 Widget**：Android 主屏幕 Widget 显示当前阅读书籍和进度
- [x] **阅读专注模式**：隐藏状态栏/导航栏，全屏沉浸阅读
- [x] **夜间模式自动切换**：按本地时间自动切换深色/浅色阅读配色（19:00–07:00，每分钟重新评估；不使用光线传感器）

### v50 — 数据与搜索

**目标**: 跨书籍的信息检索和数据分析

- [x] **全局搜索 v2**：从单书搜索扩展到全库搜索，在所有书籍中检索关键词，展示结果来自哪本书
- [x] **阅读报告**：生成周报/月报——阅读时长趋势、最快阅读日、最常读书籍
- [x] **标注导出**：将高亮和笔记导出为 Markdown / HTML / 纯文本
- [x] **阅读目标进阶**：支持周目标、月目标，目标未达成时温和提醒
- [x] **阅读标签**：为书籍打标签（如"技术"、"小说"、"在读"），标签筛选

---

## 四、中期目标 (v51 — v55)

### v51 — 多模块化与架构升级

**目标**: 解决技术债务，为后续功能奠定基础

- [x] **多模块迁移**：拆分为 `:core`, `:data`, `:domain`, `:feature:reader`, `:feature:library` 等模块，缩短编译时间
- [x] **Kotlin 2.1 + Compose 1.7**：升级到 Kotlin 2.1.0，继续使用 Compose BOM 2024.12.01（Compose 1.7 系列）
- [x] **单元测试覆盖率 ≥ 40%**：Repository + ViewModel/domain 核心门禁达到 41.9% 测试文件覆盖口径
- [x] **引入 detekt 或 ktlint**：引入 ktlint，`verifyKotlinStyle` 与 CI 自动检查新模块/domain 代码风格
- [x] **Room DB version 5+**：当前为 version 7，包含 4→5 标签、5→6 书签索引和 6→7 阅读列表迁移

### v52 — UI 地基与死设置清账

**目标**: 建立设计系统，让 UI 上能点的每一项都真实生效

> 本版本采纳 `UI_REFACTOR_PLAN.md` 第一阶段范围。原计划的「书籍管理进阶」整体顺延到 v53：
> 在死设置未清账、设计系统未建立之前叠加新功能，只会放大问题面。

- [x] **`:core` 设计系统落地**：Token（间距/圆角/高度/动效/排版）+ `FlowTheme` + 18 套 `ReaderPalette` + 排版纯函数
- [x] **死设置清账**：字体族、自定义字体、段间距、手势设置全部接线；仿真/卷曲/滑动覆盖三个假开关删除；备份/恢复接上文件选择器
- [x] **动态取色可控**：新增 `ColorSource`，默认品牌配色，不再在 Android 12+ 强制跟随壁纸
- [x] **自动夜间模式修复**：改为定时驱动，19:00 到点即切
- [x] **真实阅读进度**：进度纳入章内位置，控制层避让 `WindowInsets`
- [x] **转盘降级**：从底部一级 Tab 移出，改挂书架 overflow，底部导航收敛为 3 项
- [x] **错误必达用户**：书架导入失败不再被静默丢弃
- [x] **千章详情页性能**：章节扁平化为 `LazyColumn` items
- [x] **门禁口径修正 + 测试广度 ≥ 40%**：`coverageSummary` 覆盖 `:core`/`:feature`，当前 85.3%（58/68）
- [x] **对比度自动化断言**：阅读色板与品牌配色的 WCAG AA 正文对比度纳入单测

### v53 — 组件库、状态统一与书籍管理进阶

**目标**: 补齐组件库，同时兑现原 v52 的书架管理能力

- [x] **`:core` 组件库补全**：`FlowScaffold`/`FlowTopBar`/`BookCover`/`SkeletonBox` 等，配 `@Preview`（浅/深/大字号/RTL）
- [x] **骨架屏替换转圈**：书架冷启动首屏使用 `BookShelfSkeleton`
- [x] **字符串外置第一批**：书架 + 设置迁入 `strings.xml`，四语言补齐并实现应用内语言切换
- [x] **批量操作**：批量删除、批量移动分类、批量编辑元数据，并可批量加入阅读列表
- [x] **阅读列表**：自定义阅读列表（如"2026 必读书单"），支持拖拽排序与可访问的上移/下移
- [x] **阅读笔记独立管理**：将批注从单本书中抽离，支持跨书籍检索所有笔记
- [x] **导入增强**：支持 ZIP 批量导入、OPDS 目录导入（仅局域网）
- [x] **书籍格式扩展**：支持 FB2、MOBI 格式（只读，不涉及版权破解）

### v54 — 阅读器重塑与性能

- [x] **图片/漫画阅读**：支持 JPG / PNG / WebP 单图导入；ZIP / CBZ 图片包作为一整部漫画，`SLIDE` 左右切页，`NONE` 上下拼接滚动
- [x] **原生文本选中**：自研选中引擎（平台 `SelectionContainer` 的 hoisted 选中 API 在 Compose 1.7.x 为 internal，故基于公开 `TextLayoutResult` 实现）：长按选词、拖拽扩选、双端手柄，浮动栏支持高亮/复制/书签；显示文本→原始章节偏移的纯函数映射保证标注范围精确
- [x] **`ReaderViewModel` 拆分**：`ReaderProgressEngine`、`ReaderSessionTracker`（可注入时钟）和 `ReaderPositionUnit` 抽入 `:feature:reader`；`ReaderTtsCoordinator` 仍属于 `:app` 的阅读器 UI 边界，所有纯逻辑引擎均有 JVM 单测
- [x] **分页翻页模式**：真正的 `PAGED` 实现——`ChapterPaginator` 用真实 `TextMeasurer` 测量分页，`HorizontalPager` 横向翻页，点击左右 1/3 翻一页，进度按页折算；超大段落按原始偏移切分，标注不漂移
- [x] **大型书籍性能优化**：漫画纵拼改 `LazyColumn` 虚拟化；EPUB 单章读取 16MB、单图 24MB、TXT/MD/FB2/MOBI 整档 128MB 上限；超大章节继续分块入库
- [x] **启动速度优化**：`profileinstaller` + 手写 `baseline-prof.txt`（冷启动路径：Application/主题/书架/阅读器/Room/DataStore/Coil）
- [x] **缓存策略优化**：命中率每 50 次采样，高命中扩容量（最高 12 章/书）、低命中收缩（最低 2）；`TRIM_MEMORY_MODERATE` 按使用频率驱逐冷门书

### v55 — 书架门面、自适应与分享

- [x] **书架双视图**：网格/列表切换（持久化），程序化封面沿用 `:core` `BookCover`，网格首卡为「继续阅读」大卡
- [x] **独立搜索目的地**：新 `SearchScreen` 两段结果（书籍 + 章节命中）、搜索历史（可清空）、章节结果分页加载（20 条/页），书架搜索栏改为跳转入口
- [x] **自适应导航**：Compact(<600dp) 底部栏 / Medium+Expanded 导航栏三档自适应（`NavigationSuiteScaffold` 需 m3 1.4，故用稳定 API 手写等效布局）
- [x] **自定义主题编辑器**：阅读设置内自定义背景/文字色，覆盖所选色板；WCAG AA 对比度守卫自动回退；设置存 DataStore（阅读设置本就不在 Room，无需迁移点）
- [x] **阅读卡片分享**：`ShareCardGenerator` 按当前色板绘制 1080×1440 分享卡片（书名/章节/大字进度/进度条），FileProvider 分享 PNG
- [x] **本地书单导出**：`ShelfExporter`（:core 纯函数）导出书架 CSV/JSON（含分类、标签、阅读列表归属），设置页 SAF 落盘
- [x] **LAN 传输**：`LanTransferServer` 随机端口 + 16 位随机令牌只服务一个备份文件；`LanTransferClient` 限流下载；设置页生成/接收链接，含 JVM 本地套接字单测
- [x] **PDF 标注**：标注模式下拖拽框选区域，区域按页内归一化坐标编码进标注起止位置；已有标注以半透明矩形叠加渲染

### v56 — 打磨、无障碍与性能门禁

- [x] **TalkBack 全链路走查**：漫画页逐页朗读、阅读器正文提供「切换控制栏」自定义操作；全量检查图标/按钮 contentDescription
- [x] **仿真翻页评估**：评估文档 `docs/page_turn_evaluation.md`——结论：拟物动画与离线性能、内容重分页、无障碍冲突，不恢复 UI 入口；保留 PAGED 横滑
- [x] **ContentProvider 支持**：`content://com.flowreader.app.provider` 只读暴露书籍与进度（元数据，无文件路径/正文），写操作拒绝；路径路由纯函数单测
- [x] **截图测试接入 CI**：Roborazzi 1.40 + Robolectric JVM 视觉回归（两张金样：书架浅色、骨架深色），`recordRoborazziDebug` 记录 / `verifyRoborazziDebug` 门禁，不进 APK
- [x] **性能基线**：`performanceBaseline` 任务输出 debug/release APK 体积并与基线对比（debug 27.5MB / release 10.8MB），CI 汇总到 step summary
- [x] **本地化扩展**：新增法语/德语/西班牙语/葡萄牙语/俄语完整翻译（169 键，含 v55 新功能），应用内语言选择器同步扩展

### v56.1 — v56.6 补丁线（无新功能规划，按需发布）

v56.0 之后没有再排新功能，实际发生的都是审计、修复和一次配色扩展。列在这里是因为「规划里没有、但库里有」的版本最容易在文档里失联（v56.5.0 就是这么漏掉 CHANGELOG 的）：

- **v56.3.0 / v56.4.0 / v56.4.1** —— 三轮安全审计与收尾加固（`SECURITY_AUDIT_REPORT.md` / `SECURITY_FIX_SUMMARY.md`）：ContentProvider 不再在 Binder 线程上 `runBlocking`，FileProvider 从三处全树授权收到一个 `share_cards/` 子目录，令牌生成的取模偏差修正。
- **v56.4.2 / v56.4.4** —— 两次窗口 inset 修复。前者是外层 shell「应用但不消费」inset，导致下面 9 个界面各自再应用一遍；后者是 `FlowStateHost` 成功分支丢了 `modifier`，页面**有数据时**才失去安全区。两者都说明 inset 契约必须集中在一处。
- **v56.4.3** —— 11 处功能性 bug 排查：书内全文搜索因 FTS5 列无类型亲和性而恒空、PAGED 与漫画的阅读统计整段丢失、划词高亮少存一个字符、「最近 7 天」实际只显示 2–3 天。
- **v56.5.0** —— 阅读器自定义背景（带强制可读性蒙层）、6 套新阅读色板（12 → 18）、阅读器/统计/转盘入口 i18n。历史上曾漏记，现已补入 `CHANGELOG.md`。
- **v56.5.1 / v56.5.2** —— 9 语言字符串补齐（domain 模型改为携带纯数值，不再被语言切换冻结）；Compose 稳定性配置消除 domain 模型的不稳定推断，不稳定类 52 → 37。
- **v56.6.0 / v56.6.1** —— 配色来源扩为 12 套内置配色 + 跟随壁纸 + 自调色，新增调色台；随后修掉非中文语言下会抛异常的 `String.format`、一处组合期回写，并把两份写反了的备份规则注释改正。
- **v56.6.2** —— 第三方静态审查的 7 项问题全部收口：导入文件名、读取上限、局域网地址、备份服务器超时与路径匹配、release 签名、ContentProvider 权限，以及计划文档中的 key 字面量均已处理。

---

## 五、长期愿景 (v57+)

### 5.1 成为最好的 Android 离线阅读器

- **格式全覆盖**：支持 epub3 全部规范（SMIL 音频同步、MathML、SVG）
- **AI 辅助阅读**：本地端侧模型（ML Kit / ONNX）实现摘要生成、生词提取、阅读理解问答，全部离线运行
- **个人阅读大脑**：长期阅读数据沉淀——每年阅读报告、阅读能力分析、推荐阅读节奏

### 5.2 技术架构演进

- **Compose Multiplatform**：逐步将 UI 层迁移到 Compose Multiplatform，为桌面端（macOS/Windows）打下基础
- **Paging 3 + Room**：书架和搜索结果使用 Paging 3 实现无限滚动
- **基线性能指标**：建立 CI 性能门禁，每次提交对比 APK 体积、启动时间、帧率

### 5.3 社区与生态

- **插件系统**：允许社区开发插件（如格式解析器、翻译工具、数据导出目标）
- **主题市场**：用户分享自定义主题
- **开源治理**：完善 CONTRIBUTING.md，发布里程碑 Issue，吸引社区贡献

---

## 六、版本发布节奏

| 阶段 | 频率 | 说明 |
|------|------|------|
| v48-v50 | 每 2-3 周 | 短期迭代，快速修复和功能增量 |
| v51-v55 | 每 4-6 周 | 中期迭代，含架构升级和较大功能 |
| v56 补丁线 | 按需 | 审计、修复与小幅扩展，不排新功能；一个补丁一条 CHANGELOG |
| v57+ | 每 2-3 月 | 长期愿景，谨慎评估每项功能的必要性 |

### 发布标准

1. 六道门禁按 CI 的顺序**实跑**通过：`verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug` → `verifyRoborazziDebug` → `performanceBaseline`。UP-TO-DATE 不算跑过，必要时用 `--rerun-tasks`
2. 核心功能冒烟测试通过（打开书籍 → 翻页 → 搜索 → 统计 → 转盘）
3. 无新增 `!!` 操作符和未处理的 `try-catch`
4. CHANGELOG.md 已更新（每个版本一条）
5. `versionCode` / `versionName` 与 CHANGELOG 最新条目一致
6. CI 只在 `main` 上触发，因此 `dev` 上的提交没有远端验证 —— 第 1 条必须在本地完成

---

## 七、否决清单 (不会做的事)

以下功能已明确排除在产品范围内，不接受相关 PR：

| 功能 | 原因 |
|------|------|
| 云端账号/同步 | 违反离线优先原则 |
| 广告/付费墙 | 非商业项目 |
| 在线书店/版权内容 | 法律风险，与离线定位冲突 |
| 第三方统计/崩溃上报 | 隐私优先，用户数据不出设备 |
| 有声书/AI 语音合成（非 TTS） | 需要网络或大模型，与离线原则冲突 |
| DRM 解密 | 法律风险 |
| WebView 渲染 | 性能差，破坏原生体验 |
