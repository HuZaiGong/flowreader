# FlowReader UI 全面深层次重构计划书

> 目标版本区间: v52.0.0 → v56.0.0 | 基线: v51.0.0 | 撰写日期: 2026-07-26

---

## 执行状态

**v52.0.0 已交付**（2026-07-26）。第 7 节 v52 阶段的全部 7 项范围均已落地，另外提前完成了原属 v53/v54 的 4 项：
详情页章节扁平化、统计图表重写、`FlowStateHost` 三态统一、真实阅读进度与控制层 insets。

CI 四步实测结果：`verifyKotlinStyle` ✅ / `testDebugUnitTest` ✅（81 测试全绿）/ `coverageSummary` ✅ **55.8%（24/43）** / `assembleDebug` ✅。
比率已直接达到第 6.2 节设定的「重构结束时 ≥ 55%」目标，而不只是 v52 验收线的 45%。

### 与计划书的三处有意偏差

| 计划书原文 | 实际做法 | 理由 |
|---|---|---|
| `ColorSource` 三态（`BRAND`/`DYNAMIC`/`SYSTEM_FOLLOW`） | 只实现 `BRAND`/`DYNAMIC` 两态 | `SYSTEM_FOLLOW` 在运行时与 `DYNAMIC`（低于 Android 12 自动回落品牌色）行为完全一致，属于第 2.2 节「无假开关」原则要禁止的东西。「跟随系统」改为落在 `AppThemeMode` 的深浅色维度上，那里它有真实语义。 |
| 翻页模式收敛为 `SLIDE` + `PAGED` | 收敛为 `SLIDE` + `NONE` | `PAGED` 的分页测量属于 v54 课题，v52 若先加枚举就等于换个名字继续挂假开关。`SLIDE`（动画翻页）与 `NONE`（瞬时切换）两者渲染行为确有差异。 |
| 字体族 8 种保持不变，只修复接线 | 同时收敛为 4 种 | 楷体/仿宋/宋体/黑体在 Android 上并无独立可解析的 family，接线后 8 个 chip 里有 4 个会渲染出完全相同的字形——那是另一批假开关。旧值按语义迁移（SONG/KAI/FANGSONG → SERIF，HEI → SANS_SERIF），需要真正楷体的用户走自定义字体导入。 |

### v52 额外清出的两处死代码（计划书未列出）

- `SettingsViewModel.onExportReady()` / `onImportReady()` **没有任何调用点**——备份与恢复按钮点下去只是把 `isExporting` 置真，什么都不会发生。已接上系统文件选择器。
- `SettingsRepository` 有 9 个从未被调用的方法（`updateReaderTheme`、`updateFontSize`、`updateLineSpacing`、`updatePageMode`、`updateKeepScreenOn`、`updateScreenTimeout`、`updateEyeProtectionInterval`、`updateAutoNightMode`、`updateGestureSettings`）——所有调用方都走 `updateReadingSettings`。已删除。

### 仍未开始（v53 及以后）

字符串外置与四语言切换（`stringResource` 使用率仍为 0）、`:core` 组件库其余 20 余个组件与 `@Preview`、骨架屏、`SelectionContainer` 原生选中、`ReaderViewModel` 拆分、书架双视图与程序化封面、独立搜索目的地、`NavigationSuiteScaffold` 自适应导航、共享元素转场、自定义阅读主题（Room `MIGRATION_6_7`）、截图测试。

---

## 0. 摘要

FlowReader 的功能面已经相当完整（多格式、FTS5 全文检索、标注、统计、TTS、Widget），但 **UI 层从未被系统性设计过**：没有设计系统、没有组件库、没有排版规范、没有自适应布局，六个屏幕各自为政地拼装 Material 3 默认控件。更严重的是，UI 上已经暴露给用户的一批阅读定制能力（字体族、自定义字体、段间距、手势自定义）**在渲染路径上根本没有接线**，属于"看得见、点得动、无效果"的假功能。

本计划书做三件事：

1. **诊断** —— 逐条列出带文件行号的证据，区分"设计缺陷"、"死代码/死设置"、"性能隐患"、"工程缺口"四类问题；
2. **设计** —— 建立 `:core` 设计系统（Token → 组件 → 模式）与阅读排版引擎，重排信息架构与动效规范；
3. **落地** —— 拆成 5 个可独立发布、可独立回滚的版本，每阶段给出交付物、验收标准、以及会踩到的两道 Gradle 门禁的具体应对。

**贯穿全程的硬约束**：不引入网络/账号；不引入 WebView；不新增第三方 UI 框架（Compose + M3 官方组件为唯一依赖来源）；每阶段结束 CI 四步（`verifyKotlinStyle` → `testDebugUnitTest` → `coverageSummary` → `assembleDebug`）必须绿。

---

## 1. 现状诊断（全部附证据）

### 1.1 设计系统层：不存在

| 问题 | 证据 | 影响 |
|------|------|------|
| 品牌色在绝大多数设备上永不生效 | `ui/theme/Theme.kt:77-84` —— SDK ≥ 31 无条件走 `dynamicLightColorScheme/dynamicDarkColorScheme`，`Color.kt:6-55` 精心定义的 50 个 M3 色值只在 Android 11 及以下使用，且用户没有开关 | 应用没有视觉身份；配色随用户壁纸漂移，封面色 + 壁纸色冲突不可控 |
| 阅读器配色与应用主题完全脱节 | `Color.kt:57-63` 的 `ReaderColors` 只有 2 组硬编码值；`ReaderScreen.kt:80-88` 用 `when` 手工映射 | 阅读器无法参与主题体系；新增阅读主题必须改 `when` 分支 |
| 阅读主题只有 2 个 | `domain/model/ReadingSettings.kt` 的 `enum ReaderTheme { LIGHT, DARK }` | 与 `CLAUDE.md` 中"阅读器自有 12 套主题"的描述不符 —— 文档超前于实现，需以实现为准并补齐 |
| 字体系统是英文默认值 | `Typography.kt` 15 个 style 全部 `FontFamily.Default`；`bodyLarge` 的 `letterSpacing = 0.5.sp` | 0.5sp 字距是为拉丁文设计的，中文正文会显得松散；没有为"阅读正文"单列的排版刻度 |
| 无间距/圆角/高度/动效 Token | 全仓库 `.dp` 字面量散落（8/12/16/20/24/32/80/100...） | 每个屏幕的呼吸感全靠手感，无法统一调整 |

### 1.2 死设置清单（UI 已暴露、渲染层未消费）—— 最高优先级

这一类问题对用户是"功能欺骗"，必须在第一阶段清账。

| 设置项 | UI 入口 | 持久化 | 渲染层消费 | 结论 |
|--------|---------|--------|------------|------|
| 字体族（8 种） | `ReaderSettingsDialog.kt:47-54` | ✅ `SettingsRepository` | ❌ `ReaderContent.kt:154-158` 的 `baseStyle` 从未设置 `fontFamily` | **无效** |
| 自定义字体导入（.ttf/.otf） | `SettingsScreen.kt:93-109` | ✅ `SettingsViewModel.kt:206` 落盘到私有目录 | ❌ 无任何 `Font()` 加载代码 | **无效** |
| 段落间距 `paragraphSpacing` | 无 UI（仅数据） | ✅ `SettingsRepository.kt:101` | ⚠️ `ReaderContent.kt:187` 把默认值 `1.0f` 直接当 dp 用 → 段间距 1dp | **等于没有** |
| 手势自定义（7 个动作 + 边缘手势） | `SettingsScreen.kt:475-588` 完整对话框 | ✅ `SettingsRepository.kt:59-78` 全量读写 | ❌ `ReaderScreen.kt:141-149` 只用了 `tapZoneRatio`，左/中/右 行为硬编码；双击、长按、滑动完全没接 | **无效** |
| 翻页模式（5 种） | `ReaderSettingsDialog.kt:98-114`、`SettingsScreen.kt:302-336` | ✅ | ❌ `ReaderContent` 是单一 `verticalScroll`，无任何翻页动画实现 | **无效** |
| 书签列表（书籍详情页） | ❌ Tab 只有"目录/标注"两项 | — | `BookDetailScreen.kt:682` 的 `BookmarkListContent` 已实现但从未被调用；`BookDetailViewModel.kt:66` 仍然每次都查库 | **死代码 + 无谓查询** |

> 说明：翻页模式的完整实现（仿真/卷曲）成本高，本计划的处置方式是**先收敛枚举**（只保留已实现的 `SLIDE` 语义 + `NONE`），把"仿真翻页"降级为 v55 的独立课题，而不是继续挂着假开关。

### 1.3 阅读器体验缺陷

| 问题 | 证据 | 说明 |
|------|------|------|
| **高亮靠手打文字** | `ReaderContent.kt:325-335` —— 长按段落后弹出 `OutlinedTextField`，让用户**自己输入要高亮的文本** | 这是整个应用最反直觉的交互。Compose 的 `SelectionContainer` + 自定义 `TextToolbar` 才是正解 |
| 自动夜间模式不会自动触发 | `ReaderScreen.kt:57-62` —— `Calendar.getInstance()` 在组合期读一次，无时间源驱动重组 | 19:00 到了屏幕不会变；必须退出重进阅读器 |
| 顶栏 8 个图标按钮 | `ReaderControls.kt:68-125` | 小屏必然挤压；且全部是同一视觉权重，无主次 |
| 控制层不处理 insets | `ReaderControls.kt:39` `Box(fillMaxSize)` + `TopAppBar` 直接 `align(TopCenter)` | 边到边模式下顶栏会被状态栏压住 |
| 进度条不是阅读进度 | `ReaderControls.kt:37` `progress = currentChapter / totalChapters` | 一本 5 章的书，读完第 1 章显示 20%，与章内位置无关；拖动直接跳章，无预览 |
| 无沉浸式排版容器 | `ReaderContent.kt:89-91` 固定 `horizontal = 20.dp, vertical = 80.dp` | 平板上行宽会拉到 100+ 字符，严重伤害可读性（理想 CJK 行宽 28–40 字） |

### 1.4 信息架构与导航

- 底部四个 Tab 中，**"转盘"与阅读毫无关系**却占据一级入口（`Navigation.kt:52`），而"书籍详情 → 阅读"这条主链路要多一次跳转。
- `LibraryScreen` 只有**列表视图**，没有封面网格。书架是阅读器的门面，纯列表在视觉上极度平淡（`LibraryScreen.kt:216-291`）。
- 设置项分裂在两处且语义重叠：`SettingsScreen` 的"阅读设置"与 `ReaderSettingsDialog` 各有一套字号/行距/翻页模式入口，改动互相覆盖。
- 转场只有 220ms fade（`Navigation.kt:102-103`），无共享元素、无 predictive back。

### 1.5 状态与反馈一致性

- **错误被静默吞掉**：`LibraryScreen.kt:301-305` —— `uiState.error?.let { LaunchedEffect(error) { viewModel.clearError() } }`，用户永远看不到导入失败的原因。
- Loading / Empty / Error 三态**各屏各写一套**：`LibraryScreen.kt:194-209`、`StatsScreen.kt:42-69`、`BookDetailScreen.kt:70-96`、`ReaderScreen.kt:95-121` 四种不同的措辞、布局和按钮组合。
- 全部使用 `CircularProgressIndicator` 转圈，无骨架屏；书架首屏冷启动是白屏 + 转圈。
- 反馈通道混乱：设置页用 Snackbar，书架用（被吞掉的）error，详情页用 AlertDialog 展示导出结果。

### 1.6 性能隐患

- **千章书籍会卡死详情页**：`BookDetailScreen.kt:142-204` 把 `ChapterListContent` 放进 `LazyColumn` 的**单个 item**，内部是 `Column { chapters.forEach { ... } }`（第 653-680 行区域）。一本 2000 章的 TXT 会一次性组合 2000 个 `ChapterItem`。这是"禁止嵌套 LazyColumn"规则被机械套用的副作用 —— 正解是把章节**扁平化成 LazyColumn 自己的 items**，而不是塞进一个 item。
- `LibraryScreen.kt:377-379`、`457-459` 在组合期做 `File(...).exists()` 磁盘 IO（虽有 `remember` 包裹，首次仍在主线程）。
- 统计图表 `StatsScreen.kt:348-377` 每帧重画两个几何完全相同的矩形（`surfaceVariant` 那层被 `primary` 完全覆盖，纯属浪费）。

### 1.7 无障碍与本地化

- **四套语言资源，零处使用**：`res/values{,-en,-ja,-ko}/strings.xml` 齐备，但全仓库 `stringResource` 使用次数 = **0**，80+ 处中文字面量硬编码在 Kotlin 里。多语言目前是完全不可用的装饰。
- 大量 `contentDescription = null` 用在承载信息的图标上（如 `StatsScreen.kt:305`、`BookDetailScreen` 各处），TalkBack 下统计卡只能读出数字，读不出含义。
- 自绘图表（`ReadTimeBarChart`、`WheelSpinner`）无任何语义信息。
- 颜色对比度从未校验；`onSurface.copy(alpha = 0.5f)` 这类写法（`LibraryScreen.kt:519`、`354`）在浅色主题下接近 3:1，低于 WCAG AA 的 4.5:1。
- 未验证 `fontScale = 2.0` 下的布局（多处固定高度：`LibraryScreen.kt:383` 的 140dp 封面、`ReaderContent.kt:331` 的 120dp 输入框）。

### 1.8 工程与验证缺口

- `:core`、`:feature:library`、`:feature:reader` **三个空模块**已存在但无源文件（`settings.gradle.kts` 已 include，`app/build.gradle.kts` 已依赖）。设计系统正好落在 `:core`。
- 全仓库无一个 `@Preview`，无截图测试，无 Compose UI 测试（`androidTest` 只有依赖，无用例）。
- **两道门禁会在重构中反复咬人**（必须提前设计应对）：
  - `verifyKotlinStyle`：ktlint 作用于**除 `:app` 外**所有模块 → 代码一旦迁入 `:core`/`:feature:*` 就要满足完整 ktlint 规则（当前 `:app` 代码大量 `import ...*` 通配、超长行，直接搬会红）；同时全仓库任何 `.kt/.kts` 出现 Tab 或行尾空格即整体失败。
  - `coverageSummary`：当前 **13/31 = 41.9%**，距 40% 下限只剩 0.6 个百分点。分母口径是 `app/**/data/repository/*Repository*.kt` + `app/**/ui/screens/**/*ViewModel.kt` + `domain/**/repository/*.kt` + `domain/**/model/*.kt`（`build.gradle.kts:45-53`）。**新增 2 个 domain model 且不加测试就会红**（13/33 = 39.4%）。反过来，把 ViewModel 迁往 `:feature:*` 会让它离开分母、而 `feature/*/src/test` 又不计入分子 —— 门禁会被"重构本身"悄悄放水。

---

## 2. 重构目标与原则

### 2.1 目标（可验证）

| # | 目标 | 验收方式 |
|---|------|----------|
| G1 | 消灭全部死设置：UI 上能点的，渲染层必须生效 | 逐项手工冒烟 + 单测覆盖 settings→style 映射函数 |
| G2 | 建立单一设计系统，所有屏幕只消费 Token 与组件 | 全仓库 `.dp` 字面量数量下降 ≥ 80%；`Color(0x...)` 在 `ui/screens/**` 归零 |
| G3 | 阅读器成为产品重心：真实进度、真实选中、真实主题 | 12 套阅读主题可用；文本可原生选中；进度精确到章内位置 |
| G4 | 三态（loading/empty/error）与反馈通道全局统一 | 各屏统一走 `FlowScaffold` + `FlowStateHost`；错误必达用户 |
| G5 | 自适应：手机/平板/折叠屏各有合理布局 | `WindowSizeClass` 三档在 Compact/Medium/Expanded 下均无横向滚动、行宽受控 |
| G6 | 无障碍达 WCAG AA；四语言真正可切换 | 对比度校验脚本通过；`stringResource` 覆盖率 100%；TalkBack 走查 |
| G7 | 大书不卡：2000 章详情页、10MB TXT 阅读流畅 | 详情页首帧 < 300ms；滚动无跳帧（Perfetto 抽查） |

### 2.2 原则

1. **Token 优先** —— 任何颜色/尺寸/时长只能来自 `FlowTokens`，组件不接受裸值。
2. **阅读器优先** —— 冲突时优先保证正文渲染的质量与性能，装饰性动效让路。
3. **无假开关** —— 未实现的能力不留 UI 入口；宁可少一个 chip，不留一个骗人的 chip。
4. **可回滚** —— 每阶段一个版本 tag，新旧组件在过渡期共存，通过替换调用点切换，不做"大爆炸"式全量替换。
5. **不扩表** —— UI 重构原则上不动 Room schema（v52–v54 零迁移）；确需新字段的（自定义阅读主题）集中在 v55 一次迁移。

---

## 3. 目标架构

### 3.1 模块落位

```
:core                      ← 设计系统（本次重构的新增主体）
  designsystem/
    token/                 FlowColors / FlowTypography / FlowSpacing / FlowShapes / FlowMotion / FlowElevation
    theme/                 FlowTheme（含 LocalFlowTokens CompositionLocal）
    component/             约 24 个基础组件（见 3.4）
    reader/                ReaderPalette（12 主题）/ ReaderTypography（排版引擎）
  util/                    UI 无关的纯函数（格式化时长/日期/百分比）
:feature:library           LibraryScreen + LibraryViewModel + 书架专属组件
:feature:reader            ReaderScreen + ReaderViewModel + 阅读器专属组件
:app                       composition root：MainActivity / Navigation / DI / 其余屏幕
```

**迁移顺序（重要）**：`:core` 先建立并被 `:app` 消费 → 屏幕逐个改造为只用 `:core` 组件 → **最后**才把屏幕整体搬进 `:feature:*`。先搬后改会同时踩上 ktlint 门禁和 Hilt/Navigation 依赖缺失（`:feature:*` 当前只有 Compose UI + Material3 + `:domain`）。

### 3.2 Design Token 设计

```kotlin
// :core/designsystem/token/FlowColors.kt
@Immutable
data class FlowColors(
    val brand: ColorScheme,          // 品牌基色方案（保留现有 md_theme_* 值作为品牌身份）
    val readerPalette: ReaderPalette // 阅读器独立配色，见 3.3
)

// 动态取色改为用户可控三态，而不是当前的"S+ 强制动态"
enum class ColorSource { BRAND, DYNAMIC, SYSTEM_FOLLOW }
```

- **修正 `Theme.kt:77-84`**：动态取色变成设置项（默认 `BRAND`，保住视觉身份），而不是不可关闭的行为。
- 间距刻度固定 6 档：`xs=4 / sm=8 / md=12 / lg=16 / xl=24 / xxl=32`，禁止出现刻度外的值。
- 圆角 4 档：`sm=8 / md=12 / lg=16 / xl=28`（对齐 M3 表达式）。
- 动效 4 档时长 + 3 条曲线：`instant=0 / quick=150 / standard=250 / emphasized=400`，曲线取 M3 `emphasized`/`standard`/`emphasizedDecelerate`。

### 3.3 阅读排版引擎（核心增量）

这是本次重构技术含量最高的一块，也是死设置的解药。

```kotlin
// :core/designsystem/reader/ReaderPalette.kt
@Immutable
data class ReaderPalette(
    val id: String,            // "paper" / "sepia" / "night" / "eink" ...
    val displayName: String,
    val background: Color,
    val text: Color,
    val secondaryText: Color,
    val highlightTint: Color,
    val isDark: Boolean
)

// 12 套内置主题：纸白 / 米黄 / 护眼绿 / 亚麻 / 晨雾 / 冷灰
//                 夜黑 / 墨蓝 / 深棕 / 曜石 / OLED 纯黑 / 电子墨水
```

```kotlin
// :core/designsystem/reader/ReaderTypography.kt —— 纯函数，可单测（同时喂饱 coverageSummary 分子）
fun readerTextStyle(
    settings: ReadingSettings,
    customFont: FontFamily?,   // ← 修复自定义字体死设置
    palette: ReaderPalette
): TextStyle = TextStyle(
    fontFamily = customFont ?: settings.fontFamily.toComposeFontFamily(), // ← 修复字体族死设置
    fontSize = settings.fontSize.sp,
    lineHeight = (settings.fontSize * settings.lineSpacing).sp,
    letterSpacing = 0.sp,                    // ← CJK 正文不加字距
    textAlign = TextAlign.Justify,
    color = palette.text
)

fun paragraphSpacingDp(settings: ReadingSettings): Dp =
    (settings.fontSize * settings.paragraphSpacing * 0.5f).dp   // ← 改为跟随字号的倍数，修复 1dp 问题
```

**行宽控制**（解决平板可读性）：正文容器宽度 = `min(maxWidth - 2 * horizontalPadding, 理想行宽)`，理想行宽按 `fontSize * 34`（约 34 个汉字）计算，超出部分留白居中。

**中文排版细则**：
- 首行缩进 2 字符（可关闭），替代当前完全无缩进的观感；
- 标点避头尾（`LineBreak.Paragraph` + `Hyphens.None`）；
- 中英混排时 `TextAlign.Justify` 需配合 `LineBreak.Heading` 避免西文单词被拉伸得过散。

### 3.4 组件清单（`:core/designsystem/component`）

| 分类 | 组件 | 替代现状 |
|------|------|----------|
| 骨架 | `FlowScaffold`、`FlowTopBar`、`FlowStateHost`（loading/empty/error/content 四态统一） | 4 套各写一遍的三态 |
| 反馈 | `FlowSnackbarHost`、`FlowMessage`（全局消息总线，取代被吞掉的 error） | `clearError()` 静默丢弃 |
| 内容 | `BookCover`（含占位/失败/进度环）、`BookGridCard`、`BookListRow`、`ChapterRow`、`AnnotationRow`、`BookmarkRow` | `LibraryScreen` / `BookDetailScreen` 内联私有组合 |
| 数据 | `StatTile`、`TrendBarChart`（带坐标轴/数值/空态/语义）、`GoalProgress` | `StatCard` + 手绘 Canvas |
| 输入 | `FlowChipGroup`（单选/多选）、`FlowSlider`（带数值气泡）、`SettingRow`、`SettingSwitchRow`、`SettingNavRow` | `SettingsItem` / 裸 `Slider` |
| 容器 | `FlowSection`（带标题的分组）、`FlowBottomSheet`（取代阅读器里的 AlertDialog） | `SettingsSection` |
| 骨架屏 | `SkeletonBox`、`BookShelfSkeleton` | 无 |

每个组件配 `@Preview`（浅色 / 深色 / `fontScale=2.0` / RTL 四组），Preview 本身就是设计走查的载体。

---

## 4. 分屏重构方案

### 4.1 书架 Library（门面，改动最大）

**现状**：单一列表 + 顶部 4 个图标按钮 + 常驻式 SearchBar。

**目标**：

- **双视图**：网格（默认，2/3/4 列随 WindowSizeClass 变化）与列表可切换，偏好持久化到 DataStore。
- **封面驱动**：`BookCover` 组件统一处理 —— 有封面走 Coil，无封面生成**书名首字 + 从书名哈希取色**的程序化封面（不再是千篇一律的灰色书本图标）。
- **继续阅读**：置顶一张大卡（当前书 + 章节名 + 进度条 + "继续"主按钮），点击直达阅读器（跳过详情页）。这是使用频率最高的路径，当前需要两跳。
- **顶栏瘦身**：搜索 + 视图切换保留为图标，排序/分类/导入收进 overflow 或底部 sheet。导入改为 `ExtendedFloatingActionButton`。
- **搜索**：改为独立的搜索目的地（`Screen.Search`），支持"书名/作者"与"全文"两段结果、结果分页（不再 `take(8)` 截断）、搜索历史。
- **错误必达**：导入失败走 `FlowSnackbarHost` + "重试/查看原因"操作。
- **骨架屏**：冷启动首屏用 `BookShelfSkeleton` 而非转圈。

### 4.2 阅读器 Reader（体验重心）

| 维度 | 改造 |
|------|------|
| 主题 | 接入 12 套 `ReaderPalette`；设置面板改为色板缩略图网格，所见即所得 |
| 排版 | 接入 `readerTextStyle`，字体族/自定义字体/段间距/行宽全部真实生效；新增首行缩进开关 |
| 选中 | `SelectionContainer` 包裹正文 + 自定义 `TextToolbar`（高亮 5 色 / 复制 / 加书签 / 全文检索本词），**彻底删除手打高亮的对话框** |
| 控制层 | 顶栏收敛为 4 个高频动作（目录 / 书签 / 朗读 / 更多）；其余进底部 `FlowBottomSheet`；全部改用 `WindowInsets` 正确避让 |
| 进度 | 进度 = `(已读章节字符数 + 章内位置) / 全书字符数`；滑块拖动时显示章节名浮层，松手才跳转 |
| 设置面板 | `AlertDialog` → `ModalBottomSheet`，分「排版 / 主题 / 手势 / 朗读」四段，改动实时预览（面板半透明，正文可见） |
| 手势 | 真正消费 `GestureSettings`：左/中/右 点击、双击、长按、左右滑各自映射到 `GestureAction`；边缘手势按 `leftEdgeWidth/rightEdgeWidth` 生效 |
| 自动夜间 | 改为 `produceState` + 定时器（每分钟检查一次）或系统时间广播驱动，确保 19:00 到点即切 |
| 沉浸 | 进入阅读器默认隐藏系统栏，单击中部唤出控制层并自动 3s 收起 |

**翻页模式的处置**：v52 收敛枚举为 `SLIDE`（滚动，现状）+ `PAGED`（分页，新实现），删除 `SIMULATION/CURL/SLIDE_OVER` 三个未实现项及其 UI。分页翻页作为 v54 的独立课题落地（涉及文本分页测量，非纯 UI 工作）。

> ⚠️ 删除 `PageMode` 枚举值会影响 `SettingsRepository` 的反序列化 —— 必须保留 `valueOf` 的 `try/catch` 兜底（现有代码已有此模式），旧值回落到 `SLIDE`。

### 4.3 书籍详情 BookDetail

- **章节列表扁平化**：从"LazyColumn 的一个 item 里塞 Column"改为**章节直接作为 LazyColumn 的 items**，Tab 用 `stickyHeader` 固定。这是性能问题的根治（1.6 节）。
- Tab 补齐第三项「书签」，接上已经存在但从未被渲染的 `BookmarkListContent`；否则删掉它并停掉 ViewModel 里的书签查询。二选一，不留死代码。
- 头部改为**折叠式**（`TopAppBarScrollBehavior.exitUntilCollapsed`）：封面 + 标题随滚动收进顶栏。
- 标签编辑从"逗号分隔的裸文本框"升级为 chip 输入（可删可选历史标签）。
- 导出标注结果从 `AlertDialog` 截断展示改为系统分享 Intent + Snackbar 确认。

### 4.4 统计 Stats

- `TrendBarChart` 重写：坐标轴、数值标签、今日高亮、空态、以及 `semantics { contentDescription = "7 月 20 日阅读 35 分钟" }` 逐柱语义。
- 时间范围切换：7 天 / 30 天 / 全年（`FlowChipGroup`）。
- 「目标」独立成卡片组，周/月目标合并展示，达成时有一次性庆祝动效（`emphasized` 曲线，不超过 400ms）。
- 删除重复的 `formatDate`（`StatsScreen.kt:418` 与 `399` 逻辑完全相同且前者未被使用），格式化函数统一进 `:core/util`。

### 4.5 转盘 Wheel

- **降级为二级入口**：从底部 Tab 移出，改挂在书架 overflow 或统计页。理由：与"沉浸式阅读"的产品定位无关，却占据 25% 的一级导航面积。
- 保留现有 60fps 实现（`WheelScreen.kt:32-46` 的 `derivedStateOf` 隔离与 `Animatable` 驱动是正确的，不要动）。
- 仅做视觉对齐：配色改用 Token，结果展示改用 `FlowBottomSheet`。

### 4.6 设置 Settings

- **消除双入口重叠**：`SettingsScreen` 只保留「应用级」设置（主题来源、语言、提醒、目标、备份、关于）；所有「阅读级」设置（字号/行距/字体/主题/手势）统一收归阅读器内的设置面板，`SettingsScreen` 里改为一个"阅读偏好 →"的跳转项。
- 行间距那个"点一下 +0.25，到顶回绕"的交互（`SettingsScreen.kt:146-157`）删除，改为 `FlowSlider`。
- 新增：语言切换（配合 4.7 的本地化）、动态取色开关、重置为默认。

### 4.7 全局：本地化与无障碍

- **字符串外置**：80+ 处硬编码中文迁入 `strings.xml`，四语言补齐（现有 en/ja/ko 文件需要按新 key 集补全）。这项工作机械但量大，建议一次一屏、随各屏重构同步完成，避免独立的大 PR。
- 使用 `AppCompatDelegate.setApplicationLocales` 或 `LocaleManager`（API 33+）实现应用内语言切换，配 `res/xml/locales_config.xml`。
- 所有承载信息的图标补 `contentDescription`；纯装饰保持 `null` 并加注释说明。
- 建立对比度校验：在 `:core` 加一个 JVM 单测，对 12 套 `ReaderPalette` 和品牌 `ColorScheme` 的关键前景/背景组合断言对比度 ≥ 4.5:1（正文）/ 3:1（大字与图标）。**这个测试同时是 `coverageSummary` 的分子**。
- 全部 `alpha` 弱化文字改用 M3 语义色（`onSurfaceVariant`），不再手工 `copy(alpha = 0.5f)`。

---

## 5. 导航、自适应与动效

### 5.1 导航重构

```
Library (start)
 ├── Search               ← 新增独立目的地
 ├── BookDetail/{id}
 │    └── Reader/{id}?chapterIndex=
 ├── Reader/{id}          ← 书架"继续阅读"直达
Stats
 └── Wheel                ← 从一级 Tab 降级
Settings
 └── ReadingPreferences   ← 新增
```

- 底部导航从 4 项收敛为 3 项（书架 / 统计 / 设置）。
- 引入 `NavigationSuiteScaffold`（`material3-adaptive-navigation-suite`）：Compact 用底部栏，Medium 用侧边 rail，Expanded 用抽屉。
- 阅读器与搜索为全屏目的地，不显示导航容器（现有 `showBottomBar` 判断逻辑可直接复用）。
- 启用 predictive back：`AndroidManifest` 加 `android:enableOnBackInvokedCallback="true"`，配合 navigation-compose 2.8 的返回预览动画。

### 5.2 自适应策略

| 断点 | 书架 | 详情 | 阅读器 |
|------|------|------|--------|
| Compact (<600dp) | 2 列网格 | 单栏 | 单栏，行宽受容器限制 |
| Medium (600–840dp) | 3 列 + rail | 单栏 + 更宽留白 | 单栏 + 行宽上限 34 字 |
| Expanded (>840dp) | 4 列 + 抽屉 | **双栏**（左信息 / 右目录） | 单栏居中 + 两侧留白，或双页并排（v55） |

### 5.3 动效规范

| 场景 | 规格 |
|------|------|
| 页面转场 | 淡入淡出 + 4dp 位移，`standard` 250ms（当前纯 fade 220ms 保留量级） |
| 书架 → 详情 | 共享元素（封面）过渡，`SharedTransitionLayout`，`emphasized` 400ms |
| 控制层显隐 | 淡入淡出 + 顶栏上滑/底栏下滑，`quick` 150ms |
| 主题切换 | 正文 `animateColorAsState`，`standard` 250ms，避免闪白 |
| 骨架屏 | shimmer，1200ms 循环，`prefers-reduced-motion` 时降级为静态灰块 |
| 转盘 | 保持现有 4s `FastOutSlowInEasing`，不纳入统一规范 |

**性能红线**：阅读器正文滚动期间不得有任何 `animateXxxAsState` 驱动的重组；控制层与正文必须是兄弟节点，控制层显隐不触发正文重组（当前 `ReaderScreen.kt:161-188` 的结构基本正确，改造时保持）。

---

## 6. 工程化与门禁应对

### 6.1 ktlint 迁移准备（`:core` / `:feature:*` 受全量 ktlint 约束）

搬迁前需批量整改的常见违规（`:app` 现有代码普遍存在）：

- 通配 import（`import androidx.compose.material3.*` 遍地）→ ktlint `no-wildcard-imports` 会报错，需展开；
- 超过 140 列的行（如 `LibraryScreen.kt:183`、`Navigation.kt:47`）；
- 全限定名内联使用（`ReaderContent.kt` 里 `androidx.compose.ui.graphics.Color` 直接写在参数类型上，共 6 处）→ 提为 import；
- 函数参数换行风格、尾随逗号一致性。

**建议**：新建的 `:core` 代码**从第一行起就按 ktlint 写**；旧代码在"改造该屏"的那个阶段一次性整改，不单独开 PR。

### 6.2 coverageSummary 门禁应对（关键）

当前 13/31 = 41.9%，余量仅 0.6pp。本重构会同时推高分母（新 domain model）和改变口径（ViewModel 迁模块）。对策：

1. **口径先修**：在 v52 第一步就把 `build.gradle.kts:45-57` 的 `include` 扩展到 `feature/*/src/main/java/**/*ViewModel.kt` 与 `core/src/main/java/**/*.kt` 的可测部分，测试目录同步纳入 `feature/*/src/test`、`core/src/test`。否则迁移会让门禁失真。
2. **新增 model 必带测试**：`ReaderPalette`、`ColorSource` 等新 domain 类型，每个配一个断言其取值域/默认值的单测。
3. **纯函数优先**：`readerTextStyle`、`paragraphSpacingDp`、行宽计算、对比度校验、时长/日期格式化 —— 全部设计为**无 Compose 依赖的纯函数**放进 `:core`，天然可 JVM 单测。这既提升真实质量，又把比率拉起来。
4. 目标：重构结束时比率 ≥ 55%，给后续开发留出余量。

### 6.3 新增验证手段

- **`@Preview` 全覆盖**：每个 `:core` 组件 4 组 Preview（浅/深/大字号/RTL）。
- **截图测试**（可选，v54 评估）：Roborazzi 或 Paparazzi，纳入 CI 做视觉回归。注意这会引入新依赖，需权衡"不引入第三方 UI 框架"的原则 —— 测试期依赖不进入 APK，可以接受。
- **无障碍走查清单**：每阶段发布前用 TalkBack + `fontScale=2.0` + 深色主题走一遍主链路（书架 → 详情 → 阅读 → 设置）。
- **性能基线**：`assembleRelease` 后记录 APK 体积；用 Macrobenchmark 或手工 Perfetto 记录冷启动与阅读滚动帧率，写入 CHANGELOG。

---

## 7. 分阶段路线图

每个阶段 = 一个版本 = 一个提交 = 一条 CHANGELOG 记录（遵循现有 `vNN.N.N: summary` 约定）。

### v52.0.0 —— 地基与清账（2–3 周）

> **主题：先把假的变成真的，再谈美。**

**范围**
1. 建立 `:core/designsystem`：Token 全套 + `FlowTheme` + 12 套 `ReaderPalette` + `ReaderTypography` 纯函数。
2. 修复动态取色霸权：`ColorSource` 三态，默认品牌色，设置里可切。
3. **清账死设置**：字体族生效、自定义字体加载生效、段间距改为字号倍数、手势设置真实接线、翻页模式枚举收敛。
4. 修复 `LibraryScreen` 静默吞错误；建立 `FlowSnackbarHost` 全局反馈。
5. 修复自动夜间模式不触发。
6. 处置 `BookmarkListContent` 死代码（补 Tab 或删除）。
7. 修 `coverageSummary` 口径 + 补齐新增类型的单测。

**交付物**：`:core` 模块骨架 + 约 8 个 Token/引擎文件 + 6 个 bug 修复。
**验收**：逐项手工验证每个阅读设置真实生效；CI 四步绿；比率 ≥ 45%。
**风险**：`ReadingSettings` 的字段语义变了（`paragraphSpacing` 从 dp 变倍数），旧用户的存量偏好会突变 —— 需要在 `SettingsRepository` 读取时做一次值域迁移（`if (value <= 2f) value else 1.0f`）。

### v53.0.0 —— 组件库与状态统一（3–4 周）

**范围**
1. 落地 `:core` 全部 24 个组件 + Preview。
2. 三态统一：`FlowStateHost` 替换四屏各自的 loading/empty/error。
3. 骨架屏替换转圈。
4. 字符串外置第一批（书架 + 设置），四语言补齐，语言切换可用。
5. `BookDetail` 章节列表扁平化（性能根治）。
6. 统计图表重写（坐标轴 + 语义 + 空态）。

**交付物**：组件库 + 四屏状态层替换 + 两个性能/可访问性修复。
**验收**：2000 章书籍详情页首帧 < 300ms；TalkBack 可完整朗读统计页；`fontScale=2.0` 无截断。
**风险**：组件替换面广，容易引入视觉回归 —— 靠 Preview 逐个比对，且保留旧组件直到全部调用点切换完成。

### v54.0.0 —— 阅读器重塑（4–6 周，最重）

**范围**
1. `SelectionContainer` + 自定义 `TextToolbar`，删除手打高亮对话框。
2. 控制层重做：insets 正确、顶栏瘦身、底部 sheet 设置面板、实时预览。
3. 真实阅读进度（章内位置纳入计算）+ 拖动预览。
4. 行宽控制 + 首行缩进 + CJK 排版细则。
5. 12 主题色板选择器。
6. 沉浸模式默认化 + 控制层自动收起。

**交付物**：阅读器完整改版。
**验收**：选中→高亮全流程无需键盘；平板行宽 ≤ 40 字；主题切换无闪白；滚动无跳帧。
**风险**：`ReaderViewModel` 已 783 行，改动集中且耦合高。**必须先拆分**：把设置映射、进度计算、TTS 协调抽成可单测的独立类，再动 UI。`goToChapter()` 必须先加载内容再赋值 `currentChapter` 的既有铁律不能破。

### v55.0.0 —— 书架门面与自适应（3–4 周）

**范围**
1. 书架双视图（网格/列表）+ 程序化封面 + 继续阅读大卡。
2. 独立搜索目的地（两段结果 + 历史 + 分页）。
3. `NavigationSuiteScaffold` 自适应导航；转盘降级；底部三 Tab。
4. 共享元素转场（封面）+ predictive back。
5. 详情页折叠头部 + Expanded 断点双栏。
6. 字符串外置第二批（阅读器 + 详情 + 统计），本地化收尾。
7. 自定义阅读主题（用户自定义背景/文字色）—— **本次重构唯一的 Room 迁移点**（`MIGRATION_6_7`，手写，无 destructive 兜底）。

**交付物**：书架改版 + 自适应 + 本地化 100%。
**验收**：三档断点布局正确；共享元素转场无闪烁；四语言全屏可切换无遗漏字面量。
**风险**：Room 迁移必须手写并测试（`room-testing` 已在依赖里，用 `MigrationTestHelper`）。

### v56.0.0 —— 打磨、无障碍与性能门禁（2–3 周）

**范围**
1. 对比度全量校验并修正；TalkBack 全链路走查修复。
2. 动效规范落地与 `reduced motion` 降级。
3. 截图测试接入 CI（评估后决定）。
4. 性能基线：冷启动、滚动帧率、APK 体积写入 CI 报告。
5. 文档同步：`CLAUDE.md` / `AGENTS.md` / `ROADMAP.md` 更新（含修正"12 套阅读主题"这条 —— v55 之后它才成立）。

**验收**：WCAG AA 全通过；CI 含性能与视觉回归门禁。

---

## 8. 风险登记

| # | 风险 | 概率 | 影响 | 缓解 |
|---|------|------|------|------|
| R1 | `ReaderViewModel`（783 行）改动引发回归 | 高 | 严重 | v54 先做无 UI 的逻辑抽取 + 单测，再改 UI；保持 `goToChapter` 先加载后赋值、进度 3s 防抖、统计 30s 自动保存三条铁律 |
| R2 | `coverageSummary` 在迁移中失真或红灯 | 高 | 中 | v52 第一步修口径；每个新 model/纯函数强制配测 |
| R3 | ktlint 在代码迁入 `:core`/`:feature` 时大面积红 | 高 | 中 | 新代码从零合规；旧代码随屏整改，不单开 PR |
| R4 | `ReadingSettings` 语义变更导致老用户偏好突变 | 中 | 中 | `SettingsRepository` 读取侧做值域迁移；未知枚举值 `try/catch` 回落（现有模式） |
| R5 | 12 套主题 + 自定义主题需要 Room 迁移 | 中 | 中 | 集中到 v55 一次迁移，`MigrationTestHelper` 覆盖 |
| R6 | 共享元素/自适应导航依赖较新 API | 中 | 低 | Compose BOM 2024.12.01 已含所需 API；`material3-adaptive-navigation-suite` 需新增依赖（官方库，不违反原则） |
| R7 | 重构周期长（14–20 周），期间功能开发停滞 | 中 | 中 | 每阶段独立可发布；紧急功能需求插队时以阶段为单位暂停，不在阶段中途切换 |
| R8 | 视觉回归无自动化手段 | 中 | 中 | v53 起 Preview 全覆盖；v56 评估截图测试 |

---

## 9. 验收指标总表

| 维度 | 基线 (v51) | 目标 (v56) |
|------|-----------|-----------|
| 死设置数量 | 6 项 | 0 |
| 阅读主题数 | 2 | 12 + 自定义 |
| `ui/screens/**` 中的裸 `.dp` 字面量 | 数百处 | 下降 ≥ 80% |
| `stringResource` 使用率 | 0% | 100% |
| 可用语言 | 名义 4 / 实际 1 | 实际 4 |
| loading/empty/error 实现套数 | 4 | 1 |
| 测试广度比率 | 41.9% | ≥ 55% |
| Compose Preview 数 | 0 | ≥ 96（24 组件 × 4） |
| 2000 章详情页首帧 | 卡顿（全量组合） | < 300ms |
| WCAG AA 对比度 | 未校验 | 100% 通过（自动化断言） |
| 断点适配 | 仅 Compact | Compact / Medium / Expanded |

---

## 10. 明确不做

沿用 `ROADMAP.md` 第七节的否决清单，并针对 UI 层补充：

| 不做 | 原因 |
|------|------|
| 引入第三方 UI 组件库（Accompanist 已废弃部分、各类 Compose UI kit） | 官方 M3 足够；减少维护面 |
| WebView 渲染 EPUB（含 Readium 的 WebView 导航器） | 已在否决清单；破坏原生体验与性能 |
| 拟物化仿真翻页作为 v52–v55 范围内的目标 | 实现成本高于收益；降级为 v55+ 独立课题，期间不留假开关 |
| 自定义字体的在线字体库 | 违反离线优先 |
| 主题市场 / 主题分享（需要网络分发） | 属 v55+ 长期愿景，本次仅做本地自定义主题 |
| 为了视觉效果牺牲正文滚动帧率的任何动效 | 阅读器优先原则 |

---

## 附录 A：文件级改动索引（v52 起点）

| 文件 | 动作 |
|------|------|
| `ui/theme/Color.kt` `Theme.kt` `Typography.kt` | 迁入 `:core/designsystem/token`，重写为 Token 体系；`:app` 保留薄壳转发直到全部调用点切换 |
| `ui/screens/reader/components/ReaderContent.kt:154-158,187` | 接入 `readerTextStyle` / `paragraphSpacingDp` |
| `ui/screens/reader/ReaderScreen.kt:57-62,141-149` | 修自动夜间模式；接入 `GestureSettings` |
| `ui/screens/library/LibraryScreen.kt:301-305` | 删除静默 `clearError`，改走 `FlowSnackbarHost` |
| `ui/screens/bookdetail/BookDetailScreen.kt:142-204,682` | 章节扁平化；处置 `BookmarkListContent` |
| `ui/screens/stats/StatsScreen.kt:348-377,418` | 图表重写；删除重复 `formatDate` |
| `ui/screens/settings/SettingsScreen.kt:131-172` | 阅读级设置迁往阅读器面板，此处改为跳转项 |
| `domain/model/ReadingSettings.kt` | `PageMode` 收敛；新增 `ColorSource`（配单测） |
| `build.gradle.kts:41-64` | `coverageSummary` 口径扩展到 `:core` / `:feature:*` |
| `settings.gradle.kts` / `core/build.gradle.kts` | `:core` 补 Compose + Material3 依赖（当前仅最小依赖） |

---

## 附录 B：每阶段的 CI 检查清单

```bash
./gradlew verifyKotlinStyle     # ktlint（非 :app）+ 全仓库 Tab/行尾空格
./gradlew testDebugUnitTest     # JVM 单测
./gradlew coverageSummary       # 测试广度 ≥ 40%
./gradlew assembleDebug         # 构建
```

外加人工走查：TalkBack 主链路 / `fontScale=2.0` / 深色主题 / 平板断点 / 四语言切换。
