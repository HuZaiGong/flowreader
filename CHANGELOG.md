# 更新日志

本项目遵循语义化版本控制规范。

---

## [v56.3.0] - 2026-08
> 全面漏洞与死代码清理（v54-v56 收尾审计）。

### 修复
- **「屏幕常亮」设置终于生效**：此前持久化但从未应用（v52 清理遗留）；现阅读器按设置应用/清除 `FLAG_KEEP_SCREEN_ON`。
- **退出时进度/统计不再丢失**：`ReaderViewModel.onCleared()` 在 `viewModelScope` 已取消后仍会启动协程保存——改为独立 IO 作用域同步落库。
- **备份导入读取上限**：SAF 备份导入与 LAN 导入一致增加 200MB 上限，超限明确报错而非 OOM。
- **LAN 服务生命周期**：关闭局域网传输对话框即停止 HTTP 服务，不再后台常驻。
- 阅读器选中引擎消除 `!!`（合规门禁要求无新增 `!!`）；删除 v54 拆分后遗留的死函数 `saveProgressImmediately`；修复语言枚举单测（校验新增五种语言资源目录存在）。

## [v56.0.0] - 2026-08
> 打磨、无障碍与性能门禁：让应用对所有用户可读、对 CI 可测。

### 无障碍（TalkBack 走查）
- 漫画阅读逐页播报「漫画第 N 页」；阅读器正文新增「切换阅读控制栏」TalkBack 自定义操作；全量核对图标/按钮 contentDescription。

### 开放与生态
- **ContentProvider**：`content://com.flowreader.app.provider` 只读暴露书籍元数据与阅读进度（不含文件路径与正文），写操作一律拒绝；Hilt EntryPoint 获取数据库，路径路由纯函数可 JVM 单测。
- **本地化扩展**：新增法语 / 德语 / 西班牙语 / 葡萄牙语 / 俄语完整字符串集（169 键，覆盖 v55 全部新功能），应用内语言选择器同步新增五项。

### 工程门禁
- **截图测试接入 CI**：Roborazzi 1.40 + Robolectric（JVM，无需模拟器）——两张金样入库（书架浅色封面卡、深色骨架屏），`recordRoborazziDebug` 记录新金样，CI 用 `verifyRoborazziDebug` 做视觉回归门禁；截图测试不进 APK。
- **性能基线**：新增 `performanceBaseline` 任务，输出 debug/release APK 体积并与已入库基线对比（debug 27.5MB / release 10.8MB），CI 汇总到 step summary。
- **仿真翻页评估**：`docs/page_turn_evaluation.md` 完成评估——拟物化翻页与性能目标、内容重分页、无障碍冲突，暂不实现、不恢复 UI 入口。
- 修复一个日期敏感单测（硬编码 2026-07-26 窗口，随日历推进开始失败）。


> 书架门面、自适应与分享：让书架更像书架，让阅读更可分享，并完成离线局域网传输。

### 书架
- **双视图**：书架支持网格/列表切换（顶栏切换按钮，选择持久化）；网格为自适应封面栅格，首卡是「继续阅读」大卡（封面 + 书名 + 进度条）。
- **独立搜索目的地**：顶栏搜索进入全库搜索页——空态显示可清空的搜索历史；结果分「书籍」（标题/作者）与「章节命中」（FTS 摘要）两段；章节结果每页 20 条「加载更多」，不再截断。

### 导航与主题
- **自适应导航**：宽度 ≥600dp 自动切换为导航栏（rail），紧凑布局保持底部栏；三档宽度下导航均可用。
- **自定义主题编辑器**：阅读设置新增「自定义主题」——8 背景色 × 8 文字色可组合覆盖色板；`:core` `ReaderCustomTheme` 对组合做 WCAG AA 正文对比度校验，不达标自动回退到可读配色（含全组合单测）。

### 分享与传输
- **阅读卡片分享**：分享进度对话框新增「生成分享卡片」，按当前色板绘制 1080×1440 PNG（书名/章节/进度/进度条）并走系统分享。
- **书架导出**：设置页可将书架（书名/作者/格式/进度/分类/标签/阅读列表）导出为 CSV 或 JSON。
- **局域网传输**：设置页「局域网传输」——发送端一键生成带随机令牌的 `http://` 备份链接，接收端粘贴链接即可导入；全程不依赖互联网。
- **PDF 标注**：PDF 阅读新增标注模式：拖拽框选区域即生成高亮，已有标注叠加渲染；区域按页内坐标编码，可与正文标注统一管理。

### 工程
- 新增 SearchScreen/SearchViewModel、LanTransferServer/Client、ShareCardGenerator、ShelfExporter（:core）及对应单测（LAN 服务含本地套接字集成测试）；备份仓库支持文件级导出/导入并加 200MB 上限。
- 测试广度 67.7%（42/62 文件）。


> 阅读器重塑第一阶段：原生文本选中、真正的分页翻页、ViewModel 拆分与启动/缓存性能。

### 阅读体验
- **原生文本选中（v54.2）**：长按段落进入选中态——按词选中、拖拽扩选、两端手柄可微调；浮动操作栏支持「高亮 / 复制 / 书签」，替代 v52 的段落级操作面板。显示文本→原始章节偏移的纯函数映射（`ReaderTextMapping`）保证标注、书签与用户所见完全一致，含 6 项 JVM 单测。
- **分页翻页模式（v54.4）**：`PageMode` 新增真实 `PAGED`——`ChapterPaginator` 用真实排版测量把章节切成横向页，`HorizontalPager` 翻页，左右 1/3 点击翻一页；进度、进度条、Widget 均按页折算；章节跳转落在记忆页。分页参数全部来自 `:core` 排版纯函数，字号/行距/字体改动即重新分页。
- 漫画纵向拼接改为 `LazyColumn` 虚拟列表（v54.1），整本漫画不再一次组合全部页，进度按当前可见页折算。

### 工程
- **`ReaderViewModel` 拆分（v54.3）**：785 → 约 640 行。进度计算（`ReaderProgressEngine`）、会话跟踪（`ReaderSessionTracker`，可注入时钟）、TTS 协调（`ReaderTtsCoordinator`）全部可单测，`feature:reader` 新增 22 项单测。
- **启动速度（v54.5）**：接入 `profileinstaller` 与手写 `baseline-prof.txt`（Application/主题/书架/阅读器/Room/DataStore/Coil 冷启动路径）。
- **缓存策略（v54.5）**：章节缓存容量随命中率动态调节（2–12 章/书），内存回收按使用频率驱逐冷门书；命中/未命中统计此前从未被调用，现正式接线。
- **大书内存防护（v54.5）**：EPUB 单章 16MB、单图 24MB、TXT/MD/FB2/MOBI 整档 128MB 上限，超限条目跳过或明确报错。
- **OPDS 加固（v54.1）**：目录读取 2MB 上限；acquisition 链接过滤不支持的 MIME（不再把 APK 暴露为可下载书籍）。
- **Widget 修复（v54.1）**：阅读进度 Widget 改用 `goAsync()`，移除广播回调中的 `runBlocking`。
- 测试广度 67.2%（39/58 文件），全部门禁（assembleDebug / testDebugUnitTest / verifyKotlinStyle / coverageSummary）通过。

## [v54.0.0] - 2026-07

> 阅读器重塑启动：先补齐图片/漫画阅读入口，让图像内容走独立渲染路径，不再伪装成普通文本章节。

### 图片/漫画阅读
- 新增 `BookFormat.COMIC`，支持 JPG / JPEG / PNG / WebP 单图导入。
- ZIP / CBZ 中只要包含图片，即按一整部漫画导入；图片按文件名自然排序（`1, 2, 10` 而非 `1, 10, 2`）。普通书籍 ZIP 批量导入仍保留，图片条目不会被当成散书解包。
- 漫画页复制到应用内部目录，每张图作为一页章节，第一张图作为封面。
- 阅读器新增 `ComicReader`：`SLIDE` 模式为左右滑动切页，`NONE` 模式为上下拼接滚动。
- 图片 ZIP 继续复用 zip-slip 防护、隐藏文件过滤、条目数与单条目大小限制。

### 工程
- 版本号更新至 `54.0.0`。
- 新增图片格式识别、漫画自然排序与漫画 ZIP 条目规则单测。

## [v53.0.0] - 2026-07
> UI 重构第二阶段 + 兑现原 v52 顺延的书架管理能力。主题是「组件成库、文案出码、书架能批量」。

### `:core` 组件库补全
- 新增 `BookCover`：封面渲染只此一处。传入路径而非 painter，文件解析交给 Coil；**没有封面的书不再共用一个灰色书本图标**，改为按书名/作者哈希生成的确定性渐变 + 首字母（`CoverArt` 纯函数，同一本书永远同一张封面）。
- 新增 `SkeletonBox` / `SkeletonLine` / `BookShelfSkeleton`：微光扫过基于 `rememberInfiniteTransition`，在 `@Preview` / inspection 模式下自动静止。
- 新增 `FlowScaffold`（强制接好 snackbar 宿主与内容内边距）、`FlowTopBar`（标题单行省略，返回键有统一 contentDescription）、`FlowSelectionTopBar`（多选态用 primaryContainer，与常态一眼可辨）。
- 新增 `@FlowComponentPreviews` 多重预览注解：**浅色 / 深色 / 1.5 倍字号 / 阿拉伯语 RTL** 四连拍，`:core` 组件必须四种都过。
- `FlowStateHost` 新增 `loadingContent` 插槽，并把内置文案迁到 `:core` 自己的 `strings.xml`（四语言）。
- v52 遗留：标注导出格式化从 `AnnotationRepositoryImpl` 抽到 `:core/AnnotationExporter`，跨书笔记页与单书导出共用一份实现。

### 骨架屏替换转圈
- 书架冷启动首屏从居中 `CircularProgressIndicator` 改为 `BookShelfSkeleton`，占位几何与真实书架行一致，数据到达时不再跳版。

### 字符串外置与应用内语言切换
- 书架与设置两屏全部文案迁入 `strings.xml`，中/英/日/韩四语言补齐（此前 `stringResource` 使用率为 **0**，四个 `values-*` 目录是死资源）。
- 新增 `AppLanguage` 与设置页语言选择器。切换通过 `FlowLocaleProvider` 覆盖 `LocalContext`/`LocalConfiguration`/`LocalLayoutDirection` **即时生效，不重建 Activity、不丢失导航栈**。
  - 关键约束：`LocalizedContext` 是 `ContextWrapper`（base 仍是 Activity）而非裸的 `createConfigurationContext()` 结果——后者不包裹 Activity，会让 `hiltViewModel()` 的 `findActivity()` 抛异常。
- 底部导航标题从字面量改为 `@StringRes`，否则语言切换后 Tab 文案会冻结在首次组合的语言上。
- 批量操作的结果反馈用 `LibraryMessage` 密封类回传，由屏幕决定措辞，避免 ViewModel 里写死中文。

### 批量操作
- 书架长按进入多选：批量删除、批量移动分类、批量编辑元数据（作者 / 标签，留空表示不修改，空白作者不会清空原作者）、批量加入阅读列表。
- 每种批量动作是一条 SQL 语句而非逐行循环，选中 200 本只触发一次 Room 失效。
- 选中项在书籍消失（删除、切换筛选）后自动收敛，不会留下指向空气的 id。

### 阅读列表
- 新增 `reading_lists` / `reading_list_items` 两张表与 **`MIGRATION_6_7`**（纯新增，不动既有表）；`(listId, bookId)` 唯一索引让「加入书单」在存储层天然幂等。
- 新增阅读列表页（列表 ↔ 详情同屏切换）：创建/重命名/删除、添加/移出书籍、长按拖拽排序。
- 拖拽只改内存顺序，手指抬起才写库——20 项重排是一个事务而不是 20 个。排序算术抽成 `ReadingListOrder` 纯函数并单测。
- 每行同时保留上移/下移按钮：拖拽手势对 TalkBack 不可达，排序不能只有鼠标语义。

### 阅读笔记独立管理
- 新增阅读笔记页：全库高亮与批注集中呈现，支持跨书搜索（同时匹配原文与批注正文）、按书籍筛选、跳回原文章节、删除、按当前筛选结果导出。

### 导入增强
- **ZIP 批量导入**：一个压缩包导入整批书。`ZipImportRules` 拒绝绝对路径与 `..`（zip slip）、跳过 `__MACOSX` 与隐藏项、限制条目数与单条目体积、只放行解析器真正支持的扩展名。
- **OPDS 局域网目录**：新增 OPDS 浏览与下载页。`OpdsAddress` 把可达范围**硬限制在回环 / RFC1918 / RFC4193 / `.local` 类地址**，并对每一跳重定向重新校验，公网地址无法被访问。新增的 `INTERNET` 权限只服务这一条路径，应用仍无账号、无同步、无统计上报。
- `BookParser` 的文件名/大小解析对 `file://` URI 补了回退，否则解压出的书全部会以「未知书籍」「0 字节」入库。

### 书籍格式扩展（只读）
- **MOBI / PRC / AZW**：自实现 PDB + PalmDOC LZ77 解码，处理 `extraDataFlags` 尾部条目（不处理会在每 4KB 边界产生乱码）。**受 DRM 保护的文件一律拒绝导入，不做任何解密**；HUFF/CDIC 压缩同样明确拒绝而非半解成乱码。
- **FB2 / FB2.ZIP**：XML 解析书名、作者、简介、封面（base64 binary）与顶层 section；`<body name="notes">` 脚注不会被当成章节。

### 工程
- Room DB version 6 → **7**，新增手写 `MIGRATION_6_7`，仍无 destructive fallback。
- `:core` 新增 Coil 依赖与自有 `res/values*`（组件库要自带文案才谈得上"库"）。
- 测试 81 → **159 个**，测试广度 55.8% → **58.6%（34/58）**：MOBI 解压与尾部裁剪、DRM 拒绝、FB2 解析、zip slip 防护、OPDS 局域网边界（含 `fcbooks.com` 这类"看起来像 IPv6 前缀"的域名）、批量元数据的 null/空白语义、拖拽排序的越界与不丢项性质。

## [v52.0.0] - 2026-07
> UI 全面重构第一阶段：地基与清账。主题是「先把假的变成真的，再谈美」。

### 死设置清账（UI 上能点的，渲染层必须生效）
- **字体族真实生效**：`:core` 新增 `ReaderTypography`，`ReaderContent` 改为消费 `readerBodyStyle`，字体选择不再被 `bodyLarge` 吞掉。字体枚举同时从 8 项收敛为 4 项（默认/衬线/无衬线/等宽）——楷体、仿宋在系统上无法解析，属于同一批假选项；旧值按语义迁移到最接近的真实字体。
- **自定义字体真实加载**：导入的 `.ttf/.otf` 通过 `Typeface.createFromFile` 校验后加载，失败静默回落内置字体，不再是"导入成功但毫无变化"。
- **段间距语义修正**：`paragraphSpacing` 从被当作 dp 使用（默认值 1.0f → 1dp 间距，等于没有间距）改为字号倍数，读取侧对旧值做值域迁移。设置面板新增段间距滑块与首行缩进开关。
- **手势设置真实接线**：左/中/右点击、双击、长按、左右滑动全部映射到 `GestureAction`；边缘热区宽度 `leftEdgeWidth/rightEdgeWidth` 现在生效并可在设置中调节，此前仅 `tapZoneRatio` 被读取且左右行为硬编码。
- **翻页模式收敛**：删除从未实现的 `SIMULATION`（仿真）、`CURL`（卷曲）、`SLIDE_OVER`（滑动覆盖）三个假开关，只保留真实存在差异的 `SLIDE`（动画翻页）与 `NONE`（瞬时切换）；旧持久化值回落到 `SLIDE`。仿真翻页降级为 v55+ 独立课题。
- **备份/恢复接线**：`onExportReady`/`onImportReady` 此前无任何调用点，备份与恢复按钮点了没有反应；现已接上系统文件选择器。
- **书签死代码处置**：书籍详情页补齐第三个「书签」Tab，渲染此前已实现但从未被调用的书签列表。

### 设计系统（`:core` 从空壳变成真实模块）
- 新增 `designsystem/token`：`FlowSpacing`（6 档）、`FlowRadius`（4 档）、`FlowElevation`、`FlowMotion`（4 时长 + 3 曲线）、`FlowShapes`、`FlowTypography`（正文去掉为拉丁文设计的 0.5sp 字距）、`FlowBrandColors`。
- 新增 `FlowTheme`：**动态取色不再是 Android 12+ 的强制行为**。新增 `ColorSource`（品牌配色 / 跟随壁纸），默认品牌配色，设置页可切换；`AppThemeMode` 新增「跟随系统」。
- 新增 12 套 `ReaderPalette`（纸白/米黄/护眼绿/亚麻/晨雾/冷灰/电子墨水/夜黑/墨蓝/深棕/曜石/纯黑），阅读设置面板改为色板网格，所见即所得。此前阅读器只有 2 组硬编码配色。
- 新增 `FlowStateHost`：书架/详情/阅读器/统计四套各写一遍的 loading/empty/error 收敛为一套。

### 阅读器
- **自动夜间模式真实触发**：改为每分钟轮询时间源，19:00 到点即切换到所选夜间色板；此前 `Calendar` 在组合期只读一次，必须退出重进阅读器才生效。
- **进度条是真实阅读进度**：进度 = 章节序号 + 章内滚动比例，此前是 `当前章/总章数`（5 章的书读完第 1 章直接显示 20%，章内滚动毫无变化）。拖动时浮层显示目标章节名，松手才跳转。
- **控制层避让系统栏**：顶栏/底栏改用 `WindowInsets`，边到边模式下不再被状态栏压住；顶栏 8 个同权重图标收敛为 4 个高频动作 + overflow。
- **高亮范围与文本一致**：删除让用户"自己输入要高亮的文本"的对话框（输入内容与实际存储的字符区间可能不一致），改为长按段落弹出底部操作面板，高亮精确覆盖该段落，并支持复制与带备注书签。
- **阅读设置面板**：`AlertDialog` → `ModalBottomSheet`，改动实时预览；`ReaderViewModel` 六个近乎重复的设置写入方法收敛为一个。
- **CJK 排版**：正文行宽上限 34 字（平板不再拉出 100+ 字的长行），首行缩进两字可开关，标点避头尾。

### 信息架构
- **转盘降级**：从底部一级 Tab 移出（底部导航 4 项 → 3 项：书架/统计/设置），改由书架顶栏 overflow 进入。转盘的 60fps 实现原样保留。
- **书架继续阅读直达**：「继续阅读」卡片点击直接进入阅读器，不再绕一次详情页。
- 导入改为 `ExtendedFloatingActionButton`，排序/转盘/设置收进 overflow。
- 设置页删除与阅读器面板重复的字号/行间距/翻页模式入口（此前两处互相覆盖），只保留应用级与设备级设置。

### 修复与性能
- **书架导入失败不再静音**：`LibraryScreen` 此前用 `LaunchedEffect { clearError() }` 直接丢弃错误，用户永远看不到失败原因；改为 Snackbar 呈现。
- **千章详情页不再卡死**：章节从"塞进 `LazyColumn` 单个 item 的 `Column`"改为 `LazyColumn` 自己的 items，2000 章书籍不再一次性组合全部行。
- 书架封面改用 Coil 异步解析，移除组合期的 `File(...).exists()` 主线程磁盘 IO。
- 统计图表重写：删除被完全覆盖的重复矩形（每帧白画一次）、补坐标轴与今日高亮、逐柱语义标签供 TalkBack 朗读；删除与 `formatDateShort` 完全重复且从未被调用的 `formatDate`。
- 标注导出从截断 12 行的 `AlertDialog` 改为系统分享。

### 工程
- `coverageSummary` 口径扩展到 `:core` 与 `:feature`，避免"把代码搬出 :app 就自动过门禁"的失真；测试广度 41.9% → **55.8%**（24/43）。
- 新增 40 个 JVM 单测：阅读排版数学、进度计算、手势判定、时长/日期格式化，以及**12 套阅读主题与品牌配色的 WCAG AA 对比度断言**（正文 ≥ 4.5:1）。
- `.editorconfig` 增加 `ktlint_function_naming_ignore_when_annotated_with = Composable`，使 `:core` 的 Compose 代码可通过完整 ktlint。
- 删除 9 个从未被调用的 `SettingsRepository` 方法与 `app/ui/theme` 整包（迁入 `:core`）。

## [v51.0.0] - 2026-07
- **多模块架构升级**：新增 `:core`、`:data`、`:domain`、`:feature:library`、`:feature:reader` 模块，应用层保留导航/Hilt 装配，Room 本地层迁入 `:data`。
- **工具链升级**：Kotlin 与 Compose compiler 升级到 `2.1.0`，KSP 升级到 `2.1.0-1.0.29`，Hilt 升级到 `2.55` 以兼容 Kotlin 2.1 metadata。
- **测试覆盖门禁**：新增 8 个 domain 模型测试，`coverageSummary` 改为覆盖 Repository、ViewModel 与 domain 核心文件，当前达到 41.9%。
- **代码规范与 CI**：引入 ktlint、`.editorconfig` 和 GitHub Actions，CI 自动执行 `verifyKotlinStyle`、`testDebugUnitTest`、`coverageSummary`、`assembleDebug`。
- **Room 计划落地**：Room DB 保持 version 6，schema 迁移到 `data/schemas/`，继续保留 v4→v5 标签字段和 v5→v6 书签索引迁移。

## [v50.0.0] - 2026-07
- **开始阅读闪退修复**：TTS 引擎改为点击朗读时懒初始化，避免进入阅读器时因系统 TTS 初始化异常导致崩溃。
- **架构补强**：抽出真实 `:domain` 模块，领域模型/仓库接口不再依赖 Compose UI 或 data 实现。
- **测试与规范门禁**：新增核心模块 JVM 测试、`verifyKotlinStyle` 和 `coverageSummary` 验证任务。
- **阅读统计重构**：日报按日期合并，新增周报/月报、最快阅读日、最常读书籍和周/月目标进度。
- **TTS 重构**：`TtsManager` 新增初始化、播放、完成和错误状态流，阅读器按钮状态由系统回调驱动。
- **全局搜索 v2**：新增 `SearchRepository`，可重建全库章节索引并在书架搜索框展示跨书全文命中。
- **标注导出**：书籍详情页支持将标注导出为 Markdown、HTML 或纯文本预览。
- **阅读目标进阶**：统计页支持周目标、月目标设置和未达成提醒。
- **阅读标签**：Book 模型新增标签字段，Room v5 迁移新增 `books.tags`，书籍详情页可编辑标签。
- **书签模块重构**：Room v6 为书签新增 `(bookId, chapterIndex, position)` 索引，Repository 改为校验 ID、规范备注文本并按章节/位置稳定排序。

## [v49.0.0] - 2026-07
- **书签系统回归**：阅读器控制栏恢复书签入口，长按段落可添加带备注的书签，支持章节内书签列表跳转/删除。
- **TTS 朗读回归**：新增系统 TextToSpeech 管理器，阅读器支持从当前阅读位置朗读、暂停和释放资源，不引入第三方 SDK。
- **阅读进度 Widget**：新增 Android 主屏幕 Widget，展示最近阅读书籍标题和进度百分比。
- **阅读专注模式**：阅读器新增全屏专注按钮，可隐藏状态栏和导航栏，滑动临时唤出系统栏。
- **夜间模式自动切换**：阅读设置新增自动夜间模式，根据本地时间在夜间切换为深色阅读配色。

## [v48.0.0] - 2026-07
- **阅读统计精确化**：阅读页数改为按章节真实字符位置累计，并按字号/行距估算每页字符数；未满一页的字符会跨滚动累计，暂停超过 5 分钟后切分为新阅读会话。
- **护眼提醒可配置**：阅读设置新增 15/20/30/45/60 分钟护眼提醒间隔，持久化到 DataStore。
- **翻页记忆**：阅读器按章节记忆滚动位置，切换章节再返回时恢复到上次位置。
- **书架筛选增强**：书架页面新增分类 FilterChip 筛选；搜索继续同时匹配书名和作者。
- **错误状态优化**：阅读器加载失败状态新增重试按钮，避免只能返回。
- **APK 体积优化**：Release 构建启用 R8 full mode，并保留资源压缩/混淆。

## [v47.0.0] - 2026-07
- **修复搜索"未找到匹配结果"提前显示**：SearchDialog 新增 `hasSearched` 标记，用户点击搜索后才显示"未找到匹配结果"，消除输入即触发的误报
- **修复阅读统计不保存**：`saveReadingStats()` 原仅 `onCleared()` 调用，导航保存状态下 ViewModel 不销毁导致统计不落库；新增每 30 秒定期保存 + 章节切换时保存，保存后重置会话计数器防止重复统计
- **修复阅读统计异常崩溃**：`saveReadingStats()` 添加 try-catch，防止 DB 写入异常导致应用闪退
- **移除设置页"关于"入口的版本号副标题**：`SettingsItem.subtitle` 改为可选参数，关于行不再显示硬编码的"版本 44.0.2"
- **书籍详情目录可点击跳转**：ChapterItem 添加点击事件，点击章节直接打开阅读器并跳转到对应章节；Reader 路由新增可选 `chapterIndex` 查询参数
- **书签系统入口隐藏**：阅读器控制栏移除书签/添加书签按钮，书籍详情页移除书签 Tab

## [v46.0.0] - 2026-07
- **移除 TTS 朗读模块**：删除 TtsManager 及所有 UI 入口、DI 注入、文档引用
- **修复批注位置错误**：ReaderContent 长按选中位置改为 chapter-absolute（原为 paragraph-relative，仅第一段可渲染高亮）
- **修复高亮颜色失效**：HighlightMenu 选择的颜色完整传递到 addAnnotation()，不再始终写入 YELLOW
- **备份新增标注**：BackupRepository 导出/导入新增 annotations 数组
- **新增 (bookId, chapterIndex) 复合索引**：优化按章节查询标注性能
- **修复 FTS5 特殊字符崩溃**：新增 escapeFtsQuery() 处理 ^ * - + ~ ( ) 等运算符
- **搜索历史持久化**：searchInBook() 调用 settingsRepository.addSearchHistory()
- **Release body 自动填充**：CI 从 CHANGELOG.md 提取当前版本条目作为 GitHub Release 正文

## [v45.0.3] - 2026-07
- **转盘性能优化**：WheelSpinner 改用 drawArc + Canvas rotate 变换替代手动 Path+20 步三角循环，Paint 移至 remember，GC 减少 90%+
- **动画帧率提升**：WheelViewModel 改用 System.nanoTime() + delay(16ms) 实现 ~60 FPS 帧同步（原 66ms 阶梯循环仅 ~15 FPS）
- **重组范围优化**：WheelScreen 使用 derivedStateOf 隔离 error/result 与 60 FPS 的 rotationAngle

## [v45.0.2] - 2026-07
- **章节切换白屏修复**：goToChapter() 异步加载 content 后未更新 currentChapter，改为协程内同步加载完 content 再更新状态

## [v45.0.1] - 2026-07
- **按书籍阅读统计**：BookDetailScreen 添加统计卡片，显示每本书累计阅读时长和页数。
- **阅读趋势图表**：StatsScreen 以 Canvas 柱状图展示最近 7 天阅读时长趋势，替换原有纯文本列表。
- **EPUB 内嵌图片**：BookParser 自动提取 EPUB 中的图片并渲染到阅读器正文中。
- **EPUB 排版保留**：HTML 解析保留标题（`##`）、粗体（`**`）、斜体（`*`）格式标记，ReaderContent 渲染对应样式。
- **自定义字体导入**：设置页导入 .ttf/.otf 字体文件，路径持久化到 DataStore，支持清除恢复默认。
- **动态缓存容量**：CacheManager 注入 MemoryManager，根据设备可用内存动态调整 LRU 缓存上限。
- **领域接口补齐**：README.md 目录树更新为当前实际结构（8 个 domain repository 接口）。

## [v45.0.0] - 2026-07
- **字体选择器**：ReaderSettingsDialog 添加 8 种字体（默认/衬线/无衬线/等宽/宋/黑/楷/仿宋）FilterChips。
- **全文搜索**：集成 FTS5，阅读器控制栏添加搜索入口 + SearchDialog，结果跳转对应章节。
- **标注高亮渲染**：ReaderContent 使用 `buildAnnotatedString` + `SpanStyle` 渲染标注背景色。
- **书籍详情标注 Tab**：标注列表显示颜色、选中文本、备注，支持删除。
- **缓存整合**：移除 `ChapterRepositoryImpl.contentCache`，统一通过 `CacheManager` 存取章节内容。
- **领域接口拆分**：Chapter、Bookmark、Annotation、Category 接口从 `BookRepository.kt` 迁移到独立文件。
- **SettingsRepository 抽象**：创建 `domain/repository/SettingsRepository.kt` 接口，`data/repository/SettingsRepository.kt` 更名为 `SettingsRepositoryImpl` 实现接口，ViewModel 全部注入接口。
- **移除死代码**：`GetBookUseCase`、`SaveProgressUseCase`、`TextPaginator`、`ParagraphMode`、`BackgroundTexture`、`AmbientSound` 枚举、11 个未用的 `ReadingSettings` 字段。
- **主题简化**：仅保留深色/浅色两种主题，全局统一生效，移除 autoTimeTheme/dynamicColor 等逻辑，净减约 260 行。

## [v43.0.0] - 2026-07
*   **决策转盘改进**：`spin()` 改为自动管理协程（`viewModelScope.launch`），无需外部 `LaunchedEffect` 触发；旋转角度基于当前角度叠加，连续旋转更流畅；转盘文字始终正向可读，不再倒置。
*   **Gradle 升级**：Gradle Wrapper 从 `8.7` 升级到 `9.6.1`；重构 `gradle.properties`，添加 `UseParallelGC`、`vfs.watch`、`kotlin.daemon.jvmargs` 等优化项，提升构建性能。
*   **漏洞修复**：
    *   `StatsViewModel`：阅读统计数据不再只快取一次，每次收集时实时刷新
    *   `FullTextSearch`：消除不安全的 `!!` 操作符，使用局部变量确保空安全
    *   `WheelViewModel`：动画协程取消后 `isSpinning` 自动恢复为 false，防止永久卡死
    *   `WheelScreen`：消除转盘结果对话框的 NPE 隐患（`!!` → 局部变量判空）
    *   `PdfViewer`：`printStackTrace()` 替换为 `Log.e()`，符合 Android 规范
    *   `ReaderContent`：`paragraphSpacing.toInt().dp` 改用直接浮点数转换，消除精度丢失
    *   `CacheManager.ChapterMeta.toDomain()`：新增 `bookId` 参数，不再始终返回 0
*   **架构优化**：
    *   移除完全未使用的 `domain/model/AppException.kt`（含自定义 `Result<T>`）
    *   移除完全未使用的 `data/repository/DataManager.kt` 和 `DataCleaner`
    *   `BookLoader.kt`：移除与 `domain/usecase/` 重复的 `TextPaginator` 类
    *   `FlowReaderApp.kt`：移除未使用的导航导入和未使用的 `SettingsViewModel` 注入
    *   `ChapterDao`：移除与 `BookDao` 重复的 `getBookCount()`
    *   `LibraryScreen`：移除未使用的 `singleBookPickerLauncher`
    *   移除各 Repository 实现中的无用实体导入（`BookEntity` 后恢复，`MemoryManager` 中的 `Build`）
*   **性能提升**：
    *   `ReadingStatsEntity`：新增 `date` 列独立索引，优化按日期筛选查询
    *   `CacheManager.getCacheStats()`：使用 `synchronized` 保护并发访问
    *   `CacheManager`：真实缓存命中/未命中统计替换原有的硬编码 `0.75f`
*   **构建修复**：修复 `LibraryViewModel` 中缺失的 `Job` 导入，消除 Kotlin 2.0 编译错误。
*   **APK 构建完成**：通过 `./gradlew assembleDebug` 验证，单元测试通过。

## [v44.0.1] - 2026-07
*   **阅读统计修复**：`ReaderViewModel.sessionReadPages` 从未递增导致阅读数据永不保存；现根据滚动字符增量合理累计页数。
*   **代码清理**：移除 `ReaderViewModel` 中未使用的 `MemoryManager` 注入、`AnnotationType` 导入、未使用的 `sessionCharactersRead` 字段；移除 `ReaderScreen` 中 7 个未使用的导入（`Intent`、`Bitmap`、`PdfRenderer` 等）；修复 `BookDetailScreen` 中 `Icons.Default.ArrowBack` 废弃用法。
*   **闪退修复**：所有书籍加载流程添加 `try-catch` 和 `bookId > 0` 校验，数据库异常或文件缺失时显示错误提示而非崩溃。

## [v44.0.0] - 2026-07
*   **CI 修复**：Release 构建类型添加 `signingConfig = signingConfigs.getByName("debug")`，修复 GitHub Actions 中 `build-and-release` Job 因产物路径 `app-release.apk` 不存在而导致上传失败和 Release 创建失败的问题。
*   版本号更新至 44.0.0。

## [v42] - 2025-06
*   **交互体验**：全面优化页面交互动画，列表项添加 `AnimatedVisibility` 淡入效果，使交互更平滑自然。
*   **书架优化**：新增下拉刷新功能（使用 Material 3 PullToRefresh 组件替代旧版），优化书籍列表加载动画。
*   **书籍详情**：改善 Tab 切换动画效果，添加书签删除淡出动画。

## [v41] - 2025-05
*   **决策转盘**：新增可定制的决策转盘功能，帮助解决阅读选择困难。
*   **底部导航**：优化底部导航栏，增加转盘入口。
*   **版本规范**：规范版本号为语义化版本控制。

## [v40.1] - 2025-04
*   **高亮修复**：优化高亮功能交互，长按/点击段落后手动输入文本再添加高亮。
*   **章节跳转修复**：修复跳转下一章时滚动位置重置问题，切换章节自动回到开头。

## [v40] - 2025-03
*   **TTS修复**：修复语音朗读功能，添加朗读/停止按钮到设置界面。
*   **版本规范**：规范版本号为 40.0.0，CI 使用语义化版本。
*   **单元测试**：GitHub Actions 集成单元测试。
*   **代码清理**：移除 DataManager 中 Sync 残留代码。

## [v36] - 2025-01
*   **Bug修复**：修复空安全断言 `!!` 问题 (ReaderViewModel)。
*   **CI修复**：修复 GitHub Actions build.yml job 定义问题。
*   **代码优化**：移除过期 Icons.Filled 使用。

## [v35] - 2024-12
*   **离线优先**：移除账号系统、云端同步等网络功能，纯本地运行。
*   **代码治理**：重命名 Application 类 FlowReaderApp → FlowReaderApplication。
*   **代码治理**：重命名 Composable FlowReaderApp → FlowReaderRoot。
*   **代码规范**：添加 .editorconfig 代码规范配置。
*   **CI优化**：优化 CI permissions，按需授权（最小权限原则）。
*   **异常处理**：新增 AppException.kt 统一异常处理机制。
*   **架构优化**：新增 domain/usecase/ 层 (GetBookUseCase, SaveProgressUseCase)。
*   **性能优化**：新增 TextPaginator 分页加载 (3000字/页, 预加载2页)。
*   **进度防抖**：3秒延迟保存减少数据库写入。

## [v30] - 2024-10
*   **Markdown 支持**：新增 .md 格式解析支持。
*   **测试基础设施**：引入 JUnit 4 + MockK 测试框架。
*   **构建优化**：Release 启用 R8 混淆压缩。
*   **现代 Android 适配**：Edge-to-edge, Splash Screen 支持。

## [v29] - 2024-09
*   **UI组件拆分重构**：ReaderScreen 拆分为独立组件模块。
*   **新增组件**：ReaderContent, PdfViewer, ReaderControls, 各 Dialog 组件。
*   **智能阅读**：基于阅读速度预测剩余阅读时间，实时计算阅读速度(字/分钟)。
*   **护眼提醒**：Regular 提醒，每20分钟提醒用户休息。
*   **阅读目标**：显示每日阅读目标完成进度，建议休息时间。
*   **书籍分类增强**：支持分类筛选、添加、删除书籍分类功能。

## [v28] - 2024-08
*   **内存管理优化**：新增 MemoryManager，实时监控内存状态和压力级别。
*   **智能缓存管理**：新增 CacheManager，实现 LRU 缓存和自动内存回收。
*   **章节内容缓存**：缓存已加载章节内容，减少重复数据库查询。
*   **分页加载支持**：新增 getBooksPaged() 支持分页加载书籍列表。
*   **Lazy Loading**：章节内容按需加载，首屏加载更快。

## [v27] - 2024-07
*   **性能优化**：章节内容 Lazy Loading，减少首次加载时间。
*   **新增内容缓存机制**，避免重复解析。

## [v26] - 2024-06
*   **性能优化**：数据库版本升级到 v2，添加复合索引。
*   **ViewModel 优化**：使用 first() 替代 collect 加载设置。
*   **UI 渲染优化**：使用 derivedStateOf 缓存计算值。
*   **手势设置持久化到 DataStore**。

## [v25] - 2024-05
*   **新增文本高亮/笔记功能**。
*   **新增阅读进度条**。
*   **新增阅读统计页面**。
*   **新增书籍封面自动提取**。
*   **新增手势自定义 UI**。
*   **新增阅读进度分享**。

## [v24] - 2024-04
*   **新增多语言支持**（中文、英语、日语、韩语）。
*   **性能优化**：章节内存缓存。
*   **性能优化**：数据库查询优化。

## [v19] - 2024-03
*   修复 Kotlin/Compose 版本兼容性问题。
*   更新 Android Gradle Plugin 至 8.6.0。
*   更新 Kotlin 至 2.0.21。
*   更新 Gradle 至 8.7。
*   更新 compileSdk/targetSdk 至 35。

## [v17] - 2024-02
*   **新增 Readium Kotlin Toolkit EPUB 渲染引擎**，支持复杂 CSS/排版。
*   **新增边缘手势识别设置**，解决滑动翻页与系统返回手势冲突。
*   **新增笔记/批注功能**（划线、想法、导出）。
*   **新增全文搜索**（单本书籍内 FTS 检索）。
*   **新增 TTS 文本朗读功能**。
*   **性能优化**：提升大型书籍解析速度。

## [v15] - 2024-01
*   Latest release version.

## [v12.0.0] - 2023-12
*   新增阅读目标设置（每日阅读时长目标）。
*   新增搜索历史记录功能。
*   **性能优化**：数据库索引优化。
*   **性能优化**：图片缓存优化。
*   **性能优化**：书籍解析流式处理。
*   **UI 优化**：阅读进度百分比显示增强。

## [v11.0.0] - 2023-11
*   **新增 PDF 格式支持**。
*   **新增阅读统计功能**（阅读时长、页数）。
*   **新增底部可拖拽进度条**。
*   **新增时间自动夜间模式**。
*   **新增批量导入书籍**。
*   **新增书籍排序功能**。
*   **新增阅读记录导出**。
*   **新增每日阅读提醒**。
*   **新增备份/恢复功能**。
*   **新增关于页面**。
*   **新增 Material You 动态颜色支持**。
*   **性能优化**：启动速度、内存占用优化。
*   深色主题对比度优化。
*   AMOLED 纯黑模式增强。
