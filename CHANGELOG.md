# 版本变更记录

本记录按照语义化版本控制规范编制，按版本号倒序列示功能、修复、工程治理及安全相关变更。除特别说明外，各项变更均以对应版本源码和测试结果为准。

---

## [v56.6.2] - 2026-08-20

> 本版本依据第三方静态审查报告（`SECURITY_REVIEW_2026-08-20.md`）实施安全整改。报告列明的 7 项事项已全部完成处理，范围包括外部文件名路径校验、读取容量上限、局域网地址校验、备份服务器连接超时、Release 正式签名、导出 provider 权限门禁，以及一项敏感凭据记录核查（实际位置与报告所述不一致，详见“说明”）。文档核对期间另发现并修复一项用户可见缺陷：`:core` 模块缺少 5 种语言资源。

### 安全

**审查事项一：外部传入文件名可突破应用私有目录边界（审查编号 #1，风险等级：中）**

经核查，恶意应用向 FlowReader 发送 `ACTION_VIEW` 意图时，其 `ContentProvider` 可将显示名称构造为 `../../databases/flowreader_db`，导入流程据此覆盖数据库；构造为 `datastore/settings.preferences_pb` 即可覆盖设置文件。上述操作对用户呈现为普通导入。成因如下：`getFileName()` 原样返回 `ContentResolver` 提供的 `DISPLAY_NAME`，`copyFileToInternal()` 将其直接用于构造 `File(booksDir, fileName)`，全程未对该字符串进行校验。

处理措施：新增 `util/ImportFileName.kt`。其中 `sanitize()` 仅保留路径最后一段（`/` 与 `\` 均视为分隔符），并将字母、数字、`.`、`_`、`-`、空格之外的字符（含控制字符）替换，去除开头的点号，长度超过 120 字符时截断并保留扩展名。文件写入前通过 `resolveWithin()` 以 `canonicalFile` 复核目标路径位于目标目录内；无法取得规范路径时返回 null，终止导入。

说明：上述处理有意保留 Unicode 字符。仓库中原有仅保留 ASCII 的 `sanitizeFileName()`，其用途为漫画解压目录名；若用于书名，将使《三体.epub》变为 `_.epub`。两者适用范围不同，未作复用。

同时修复一处竞态问题（TOCTOU）：`copyFileToInternal()` 此前自行再次查询 `ContentResolver` 获取文件名，同一 Uri 两次查询可能返回不同结果，造成通过校验的名称与实际写入的名称不一致。现由调用方（`LibraryViewModel`／`OpdsViewModel`）解析并传入文件名，全流程仅解析一次。

**审查事项二：伪造 EPUB 文件可导致内存溢出或磁盘写满（审查编号 #2，风险等级：中）**

经核查，EPUB 的 `META-INF/container.xml` 与 OPF 文件以 `readText()` 无上限读取，整包、图片及压缩包以 `copyTo()` 复制。上述输入内容由文件制作者控制，读取方无从干涉，例如将 `container.xml` 构造至数百 MB 时，`readText()` 将完整读取。

处理措施：相关路径统一改用 `copyCapped`／`readCappedBytes`／`readCappedText`，并设置两档容量上限：整包（EPUB／CBZ／任意选中文件）256MB，EPUB 结构性元数据 4MB；封面图沿用既有 24MB 上限。超过上限时终止处理并返回明确提示，同时删除半成品文件。`parseEpubStream` 与 `extractEpubCover` 增加 `finally { tempFile.delete() }`，防止提前返回造成临时文件残留。

**审查事项三：局域网接收端未校验对方地址，公网链接可被拉取（审查编号 #3，风险等级：低）**

经核查，`LanTransferClient.download()` 仅校验 `http://` 协议前缀。`http://evil.example.com/backup/…` 将被视为同一 WiFi 网络下的其他设备，从公网拉取至多 200MB 内容并交由备份导入器处理，而备份导入将整体覆盖书库。此行为与「`INTERNET` 权限仅用于局域网」的定位相冲突。

处理措施：复用 OPDS 客户端的 `OpdsAddress.isPrivateHost()` 地址校验（回环／RFC1918／RFC4193／`.local` 类名称），路径严格匹配 `/backup/` 及 16 位十六进制令牌。鉴于本应用仅声明一项 `INTERNET` 权限，各请求发起点均须自行完成地址校验，不得依赖 OPDS 客户端代为校验。

**审查事项四：备份服务器可被低速连接占用（审查编号 #4，风险等级：低）**

经核查，`serve()` 运行于单线程执行器，唯一工作线程将在 `readLine()` 处挂起等待（slow loris 攻击模式），导致对话框开启期间服务器无法响应其他请求。现已为每个连接设置 5 秒 `soTimeout`，最坏情况由「无限期等待」变为「每个滞留连接等待 5 秒」。

另经核查发现路径匹配规则与类文档不一致：类文档约定仅应答精确的 `/backup/<token>` 路径，而代码使用 `requestLine.contains("/backup/$token")`，导致 `GET /anything/backup/<token>` 与 `GET /backup/<token>extra` 亦可获得 200 响应。该情形仍须猜中令牌，单独不构成漏洞，但文档边界应与代码执行边界一致。现 `isBackupRequest()` 按整行精确匹配，并同时接受 HTTP/1.1 与 HTTP/1.0（`HttpURLConnection` 可能降级协议版本）。

**审查事项五：Release 构建使用 debug 签名（审查编号 #6，风险等级：信息）**

debug keystore 随 Android SDK 分发，属于公开材料。此前 `buildTypes.release` 直接 `signingConfig = signingConfigs.getByName("debug")`，任何人均可构造签名相同的升级包并尝试覆盖安装。

处理措施：新增 `signingConfigs { create("release") }`，从仓库根目录的 `keystore.properties`（已 git-ignore，密钥库本身由 `*.jks` 规则覆盖）读取密钥库路径与密码；该文件不存在时回退到 debug 签名，使 CI 与新克隆仍能产出可测试的 `app-release.apk`，单元测试不受影响。同时新增 `keystore.properties.example` 说明配置项与生成方式。

已通过实测验证：`apksigner verify --print-certs` 显示 `Signer #1 certificate DN: CN=HuZaiGong, OU=Dev, O=flowreader`，与 debug 密钥的 `CN=Android Debug` 及其证书指纹均不相同。验证时须确认 APK 构建时间晚于 `keystore.properties` 创建时间；首次验证使用的 APK 早于配置落地，故产物仍为旧 debug 签名。

**审查事项六：导出的 ContentProvider 未声明权限（审查编号 #7，风险等级：低）**

`content://com.flowreader.app.provider` 以 `exported="true"` 导出且无任何权限声明，故同机任意应用无需申请、无需用户同意，即可读取完整书单与各书阅读进度。该导出本身是有意设计（供自动化工具与桌面小组件读取只读元数据，不含文件路径与正文），问题在于「有意导出」被实现成了「对所有应用静默开放」。

处理措施：新增自定义权限 `com.flowreader.app.permission.READ_LIBRARY` 并置于 `android:readPermission`。调用方须声明该权限且经用户运行时授权方可读取，导出能力本身保留。保护级别取 `dangerous` 而非 `signature`：后者会将该接口限定为与本仓库同签名的应用，等同于删除该功能而非为其加门禁。另以 `signature` 级 `WRITE_LIBRARY` 覆盖 `android:writePermission` 作为第二道防线——`insert`／`update`／`delete` 本身已一律抛异常。权限的标签与说明文案随应用支持的 9 种语言一并提供（该文案会出现在系统授权对话框中）。

该变更将影响现有外部集成方：已读取该 provider 的应用在升级后将收到 `SecurityException`，须声明权限并请求授权。经核查，仓库内外均无已知集成方，`README` 与 `ROADMAP` 亦未将其作为对外接口宣传，故按收紧权限处理。Android 在应用类之外执行权限检查，单元测试无法覆盖该门禁，manifest 属性为实际执行点。

### 修复

**`:core` 模块缺少 5 种语言，德／西／法／葡／俄界面回退为中文**

核对文档时发现：`:core` 自有 9 条会直接显示给用户的字符串（加载中／暂无内容／出错了／重试，以及若干无障碍描述），由 `FlowStateHost`／`FlowTopBar`／`BookCover`／`SkeletonBox` 使用，但该模块只有 `values`／`values-en`／`values-ja`／`values-ko` 四套资源。`:app` 在 v56 扩至 9 种语言时未同步扩 `:core`，故在德、西、法、葡、俄五种语言下，所有加载态、空态与错误态均显示中文。现已补齐五套资源，9 个 locale 各 9 条。往 `:core` 新增字符串时，9 个 locale 目录须一并增加。

### 技术实现

`BookParser` 的 `copyCapped`／`readCappedBytes`／`readCappedText` 移入 companion object 并改为 `internal` 访问级别，实例保留同名单行转发，以避免改动十余处调用点。理由在于容量上限须可测试：实际上限为 256MB，测试中触发须构造 256MB 夹具；改为 `internal` 后，测试可直接传入 1000 字节上限与 1001 字节流。`LanTransferServer.isBackupRequest()` 同理，设为 `internal` 以便脱离 socket 单独测试请求行匹配。

### 测试

- 依审查建议，前四项修复分别配置回归测试：`ImportFileNameTest`（15 项）、`BookParserCapsTest`（10 项）、`LanTransferClientTest`（8 项）、`LanTransferServerTest` 新增 5 项。签名与 provider 权限两项无法通过单元测试覆盖：前者以 `apksigner verify --print-certs` 对产物进行验证，后者的执行点位于 Android 框架而非应用代码；两项均记录实测结果，不以单元测试替代。
- 容量上限测试同时固定三项易被改坏的边界：恰好等于上限时应通过（判定为 `> limit` 而非 `>= limit`，否则体积恰在边界的书籍将被拒绝）；超限后不得读完整条流（上限的意义在于 80GB 的流仅消耗一个缓冲区的代价）；`copyCapped` 不删除残留文件（该行为属注释明示的契约，删除由各调用方负责，若将来在 `copyCapped` 内追加删除逻辑，`filesDir/books` 中将残留无人清理的文件）。
- 各项修复均经变异验证：撤销 `soTimeout`、将精确匹配改回 `contains`、删除主机校验后，对应 7 项测试均失败。slow loris 测试设置客户端 20 秒读超时，回归时将报告测试失败，不会造成测试套件无限等待。
- 全量测试 335 → 373 项（+38），0 失败；测试广度 80.9% → 85.3%（58/68）。

### 说明

**审查事项七：敏感 API key 记录核查（审查编号 #5，风险等级：信息）——实际位置与报告及前次结论均不相同。**

审查报告将其定位于 `.claude/providers.yaml`。该路径有误：`.claude/` 自 v56.6.1 起已整体 git-ignore。本仓库根目录的 `providers.yaml` 确实被 git 跟踪，但**其提交历史中从未出现过明文 key**，各版本均为 `{env:DEEPSEEK_API_KEY}` 形式的环境变量引用。

本机工作区的该文件另存有一个真实 key，但被 `git update-index --skip-worktree` 标记为隐藏（`git ls-files -v providers.yaml` 输出前缀为 `S`），因此 `git status` 始终显示干净。这也是前次核查误判“key 自 v45.0.2 起存在于历史记录”的原因：工作区内容与 HEAD 内容不同，而 git 未提示该差异。该标记保存在 `.git/index`，仅对当前 clone 有效，克隆者不会继承；当拉取的提交涉及该文件时，`git pull` 可能直接失败。`.gitignore` 中的相关注释已据此更正。

全历史扫描（`git rev-list --all` 逐树 `git grep`）确认，该 key 字面量在整个仓库中仅有一处提交记录：`V56.5.0_PLAN.md` 第 128 行。该处正在论证「此 key 系公共免费占位值、不构成泄露」——结论无误，但论证过程将该字面量抄入正文，使这份计划文档成为仓库中唯一真正提交了 key 的位置，且随 v56.5.0 进入公开的 `main` 分支。现已改为不复述字面量。

该 key 为某公共免费中转站的共享值，故未作轮换。改写已公开分支提交历史属于破坏性操作，须由仓库所有者另行决定，本版本未执行。

核查结论表明，审查报告、前次核查与最终事实存在差异。此类事项应以 `git ls-files -v` 和全历史扫描结果为依据，不应仅根据工作区文件内容作出判断。

---

## [v56.6.1] - 2026-08-19

> 本版本为 v56.6.0 的补丁版本，处理非中文语言环境下的格式化异常、组合期状态回写及状态显示错误，并更正两份备份规则文件的注释。

### 修复

**事项一：调色台在 de/fr/es/pt/ru 等语言环境下发生异常**

经核查，`SchemePreview` 先以 `String.format("%.1f", ratio)` 将对比度数值转为字符串，再交由 `stringResource` 处理。`String.format` 无 locale 参数时遵循 JVM 默认 locale，而字符串资源按应用内语言设置解析。在小数点为逗号的语言环境下，`"4,7"` 将作为字符串传入资源的数值占位符，直接抛出异常；即便不抛异常，数字格式亦与该语言不符。

处理措施：资源内改为 `%1$.1f` 占位符，`ratio: Double` 原样传入 `stringResource`，由 `Resources.getString(id, args)` 按资源 locale 格式化。全仓库无 locale 参数的 `String.format` 调用仅此一处。

另将 de/es/fr/pt/ru 五份资源中硬编码的「4.5:1」阈值改为「4,5:1」，与同句中现已本地化的实测值对齐。

**事项二：调色台拖动色相环触发重复组合**

经核查，十六进制输入框草稿存储于 `mutableStateOf`，各取色回调在组合期将其改写为新颜色的十六进制串，同一趟组合再读取渲染。此为 Compose 中典型的 backwards write，须多跑一趟方可收敛。

处理措施：草稿改为「用户正在输入」的三态标记（`null` 表示显示取色器颜色），显示值由 `argb` 派生：`val hexDisplay = hexDraft ?: ColorSpaces.toHexString(argb)`。取色回调仅将其清为 `null`，输入框失焦时同样清空。

**事项三：“自调色”副标题显示未生效的颜色**

经核查，用户存储自定义色后切换回内置配色，该项仍显示 `#RRGGBB` 及「使用中」状态。现增加来源判定：`customSeedArgb?.takeIf { colorSource == CUSTOM }`。

### 变更

`backup_rules.xml`／`data_extraction_rules.xml` 注释此前表述有误，声称会备份「阅读设置、主题、语言」。实际 `<include>` 将备份范围限制为其列出的域，而此处唯一列出的 `sharedpref` 域在本应用中为空：全仓库无任何 `getSharedPreferences`／`PreferenceManager` 调用，设置存储于 DataStore，`dataStoreFile()` 位于 `filesDir/datastore/`，属 `file` 域。即从未备份任何数据。

按离线优先的产品定位，此行为符合预期，故仅更正注释、不改动行为。注释现载明结果为「数据不离开设备」，并提示不得以 `<include domain="file">` 加以修改，否则阅读习惯数据将经 Google 备份通道传出设备。跨设备迁移走应用内备份／导出（SAF 文件或局域网传输），由用户显式触发。

### 技术实现

`FlowTheme` 的 `ColorScheme` 现以 `remember` 包裹，属加固措施而非修复既有缺陷。初步怀疑每次导航均重建 scheme，`staticCompositionLocalOf` 将连带重建整棵子树，但撤销 `remember` 后新增测试仍通过。后经 Compose 编译器报告（`:core` 已支持 `-PcomposeReports=true`）确认，`FlowTheme` 为 `restartable skippable`，五个参数均为 `stable`，输入不变时 Compose 直接跳过整个函数，函数体不执行，故当前不存在该缺陷。保留 `remember` 的理由在于：此路径一旦失去可跳过性（新增不稳定参数或组合期读取即可触发），后果为全应用重组，而代价仅为一次 `remember` 比较。代码注释与测试 KDoc 均已载明该测试"证明了什么、未证明什么"。

`:core/build.gradle.kts` 补充与 `:app` 对齐的 `composeReports` 开关（默认关闭），上述结论即据此得出。

### 测试

- 新增 `FlowThemeStabilityTest`（4 项，Robolectric + Compose），断言重组时 `MaterialTheme.colorScheme` 实例的同一性（`ColorScheme` 无 `equals`，仅可按引用比较）。KDoc 载明其非已发布缺陷的回归门禁。
- 新增 `FlowColorPresetsTest.everySeedIsDarkEnoughForTheWhiteSelectionCheckmark`：配色选中态的白色对勾为硬编码，保证 12 个种子色与白色对比度均 ≥ 3:1（AA 非文本对比度）。
- 全量测试 330 → 335 项（+4 +1），0 失败；测试广度 79.4% → 80.9%（55/68）。

### 文档

- README 重构为面向使用者和贡献者的项目说明，采用“概述／功能／架构／构建／安全约束”分节，先行说明产品定位及本地存储原则，再依书籍管理、阅读、笔记统计等场景归纳功能，并保留模块边界与工程门禁事实。
- 顺带修正 README 中 4 处与代码不符之处：`:core` 载为 12 套阅读色板（实际 18，v56.5.0 增补 6 套后漏改）、`:data` 载为 7 个 Entity（实际 8，`ReadingListItemEntity` 与 `ReadingListEntity` 属同一文件，按文件数计会少算一个）、`coverageSummary` 载为 77.8%（实际 80.9%）、安全小节仍载「云备份只含 shared prefs」（即本版所修正的错述，README 漏改）。
- 另修正 `CLAUDE.md` 一处过期描述：其载 `domain/usecase/` 为「空的遗留目录」，实际该目录已不存在（README 所载「已删除」为正确表述）。
- `README_EN.md` 按同一思路重写，章节结构与中文版逐节对齐（各 15 个标题），并同步修正同样的 4 处失真。英文版为按英文重写，非中文版逐句翻译。

---

## [v56.6.0] - 2026-08-19
> 配色来源从 2 个选项扩展为 12 套内置配色 + 跟随壁纸 + 自调色，并新增色相环／色盘／光谱条调色台。

### 新增
- **12 套内置配色**：`AppColorPreset`（经典紫、靛蓝、晴空蓝、青碧、翠绿、苔绿、琥珀、橘橙、绯红、玫瑰、梅紫、石墨）。选中任一套会重新生成整套 Material 配色，而不是只替换强调色。
  - **经典紫是默认值，并作专门兼容处理**：`FlowColorPresets.schemeOf(VIOLET)` 原样返回手工调整的 `FlowLightColorScheme` / `FlowDarkColorScheme`，不经过生成器。默认值若改用生成算法，将改变未调整过该设置的既有用户界面配色；`FlowColorPresetsTest` 对各颜色角色逐项进行断言。
- **自调色调色台**（设置 → 外观 → 自调色）：`ColorStudioDialog` 提供四种输入方式并绑定同一 HSV 状态，包括色相环及内嵌饱和度／明度色盘、光谱条、明度条与饱和度条、十六进制输入框。各方式可继续调整其他方式产生的未完成颜色。弹窗实时预览生成后的六个角色色块及正文对比度；仅在选择“应用”后写入，取消操作不改变实时主题。
  - 十六进制输入框同时承担无障碍输入通道。由于 Canvas 无法由 TalkBack 操作，两个渐变条配置 `progressBarRangeInfo` 和 `setProgress` 语义。
- **`ColorSource.CUSTOM`**：新增第三种配色来源。三个来源分别读取 `AppSettings` 的不同字段（BRAND 读取 `colorPreset`，CUSTOM 读取 `customSeedArgb`，DYNAMIC 不读取上述字段），不存在功能重复。未增设“跟随系统”来源，原因是运行时无法与 DYNAMIC 作有效区分。

### 技术实现
- **`SeedColorScheme`（`:core/util`，不依赖 Compose）**：由单一种子色生成 24 个 Material 角色的 HSL 色阶。该实现属于 M3 HCT 的近似方案而非移植；项目未引入 `material-color-utilities`，`dynamicLightColorScheme` 仅用于读取壁纸配色。生成器约 150 行，可在 JVM 环境独立测试。
  - **对比度要求**：任意种子色生成的正文文字与背景配对均不得低于 AA 4.5:1。中等亮度种子可能使 `onPrimary` 与 `primary` 处于接近亮度，因此各 `onX` 角色向远离背景的方向扫描亮度，并在必要时回退至纯黑或纯白；最不利背景对纯黑或纯白中较远一侧仍可达到 4.58:1。
  - `onSurfaceVariant` 同时针对 `surfaceVariant` 和 `surface` 校验，`onSurface` 反向执行相同检查，以覆盖副标题在普通 `surface` 上的实际使用场景。
  - 错误色系固定为红色，不随种子变化；不设置饱和度下限，以确保“石墨”中性配色保持中性（由 `graphiteStaysNeutral` 守卫）。
- **`ColorSpaces` / `ColorWheelMath`（`:core/util`，均不依赖 Compose）**：承载 HSV／HSL 转换、十六进制解析及调色台全部几何运算，包括环形命中判定、内嵌方块边长和手柄反算。逆映射偏差会造成操作点与手势位置不一致，相关规则通过 JVM 测试验证。
- **写入语义**：`updateColorPreset()` / `updateCustomSeedColor()` 均在同一个 `edit{}` 中连带写入 `COLOR_SOURCE`。拆分为两次写入会先产生“新来源 + 旧颜色”的中间 `AppSettings`，导致主题短暂显示旧配色。
- 种子色在读与写两侧都强制为不透明：带旧 alpha 的持久化值会让生成的每个角色都半透明，而对比度数学假定不透明合成。

### 变更
- `settings_color_source_desc` 文案随之更新；新增 27 条字符串 × 9 种语言（每种语言 309 → 336 条）。新 UI 的标签全部走 `stringResource`，不复用 enum 里的中文 `displayName`（那些字段早于 v53 语言切换，仍被阅读器面板使用）。
- README / README_EN 阅读色板数量由 12 更正为 18（v56.5.0 加了 6 套但漏改文档），并新增「外观与主题」小节。

### 测试
- 新增 47 个测试：`ColorSpacesTest`(12)、`SeedColorSchemeTest`(9)、`ColorWheelMathTest`(13)、`FlowColorPresetsTest`(8)、`AppColorPresetTest`(5)；`ColorSourceTest` 随 CUSTOM 更新。
- AA 承诺的覆盖范围：12 套预设 × 明暗两档 × 13 组文字配对，整个色相圈每 5° 一档，以及纯黑／纯白／中灰／近黑等退化种子。
- 全量测试 330 个，0 失败；测试广度 77.8% → 79.4%（54/68）。

---

## [v56.5.2] - 2026-08-14
> 工程优化：通过 domain 模型稳定性配置，使书架卡片和阅读器参数具备跳过无关重组的条件。

### 优化
- **恢复 domain 模型的 skippability**：`:domain` 无 Compose 编译器，所以 `Book`/`Chapter`/`Annotation`/`ReadingSettings` 等参数被推断为不稳定，导致每张可见书卡在任意状态变更时都重新执行，无法通过 `equals()` 检查避免重组。
  - 新增 `compose_compiler_config.conf`（仓库根目录），声明 `com.flowreader.app.domain.model.*` 稳定。
  - 在四个 Compose 模块（`:app`、`:core`、`:feature:library`、`:feature:reader`）通过 `composeCompiler.stabilityConfigurationFiles.add()` 接入。
  - `LibraryMessage` 添加 `@Immutable` 注解（sealed interface 之前只能走 runtime）。
  - 新增 `ModelStabilityContractTest` 守卫承诺：domain 模型不得有 `var` 或可变集合字段。
- **编译器报告**：52 个不稳定类 → 37 个（所有 UiState 现在都稳定）；`Book`/`Chapter`/`Annotation`/`ReadingSettings` 现在通过 skip 检查。
- **报告生成可选**：`./gradlew -PcomposeReports=true` 写入 `app/build/compose_reports/`；普通构建不支付报告生成成本。

### 修复
- **FTS 数据库初始化竞态**：`FullTextSearch.initialize()` 的双重检查锁定缺少互斥锁，三个并发调用点（`ReaderViewModel`、`SearchRepositoryImpl` 的两处）可能同时通过 `database?.isOpen` 检查并各自 `openOrCreateDatabase()`，泄漏先创建的句柄。现由 `initMutex` 串行化，并在锁内重新检查。
- **`CacheManager.evictBookLocked()`**：原名 `evictBook()`，要求调用方持有 `synchronized(chapterCache)` 却未在签名或文档中体现。重命名并注明前置条件，避免后续从锁外调用导致 `LinkedHashMap` 并发损坏。

### 变更
- 替换三处废弃 API：`stabilityConfigurationFile` → `stabilityConfigurationFiles.add()`（四个模块）、`Intent.getParcelableExtra()` → `IntentCompat.getParcelableExtra()`（API 33+）、`Icons.Default.ViewList` → `Icons.AutoMirrored.Filled.ViewList`。

### 测试
- 测试覆盖率：76.2% → 77.8%（49/63）。
- 全量门禁通过。

## [v56.5.1] - 2026-08-14
> 完成全部 UI 字符串的 9 种语言本地化（zh / en / ja / ko / de / es / fr / pt / ru）。

### 新增
- **9 语言全覆盖**：新增 31 个字符串资源键（搜索、分享、滚轮、阅读器控件、PDF 标注模式），完成阅读器、搜索对话框、分享流程、滚轮页面的本地化。
  - 德语（de）、西班牙语（es）、法语（fr）、葡萄牙语（pt）、俄语（ru）补全所有 v56.5.1 新增键。
  - 英语（en）、日语（ja）、韩语（ko）补全 PDF 标注模式 2 键。
- **消除所有硬编码中文字符串**：`ReaderScreen`、`ReaderSettingsSheet`、`PaletteGrid` 全部改用 `stringResource()` 或 `Context.getString()`，支持运行时语言切换。
  - `ReaderScreen.kt`：书签默认标签、分享阅读卡片选择器标题。
  - `ReaderSettingsSheet.kt`：`formatArgb()` 的 fallback 参数化，`ColorSwatch` / `PaletteGrid` 无障碍描述本地化。

### 变更
- 阅读器色板预设名称（`BACKGROUND_PRESETS` / `TEXT_PRESETS`）保留中文，因这些名称是颜色的语义标签，不是 UI 文案。

### 测试
- 全量门禁通过：`verifyKotlinStyle` → `testDebugUnitTest`（281 测试，0 失败）→ `coverageSummary`（48/63 = 76.2%）。

---

## [v56.5.0] - 2026-08-13

> 完成阅读器体验和本地化扩展，新增 6 套阅读色板、阅读器背景图及阅读器、统计和转盘入口的多语言文案。

### 新增

- 阅读色板从 12 套扩展到 18 套，所有色板均通过 WCAG AA 对比度测试。
- 阅读器支持导入本地背景图。背景图始终叠加在色板背景下方的可读性蒙层之下，不提供关闭蒙层的选项；图片会复制到应用私有目录并压缩到受控尺寸。
- 补齐阅读器控制栏、书签、章节列表、统计页和转盘入口的 9 语言文案与无障碍描述。

### 技术实现

- 新增 `ReaderBackgroundImage` 纯逻辑组件，覆盖蒙层 alpha、最坏情况对比度和采样解码的 JVM 测试。
- 阅读背景和蒙层设置通过现有的 `ReaderViewModel.updateReadingSettings(ReadingSettings)` 写入 DataStore，不进入 Room。

---

## [v56.4.4] - 2026-08-12
> 修复书架、统计和详情页面在数据加载完成后内容被顶栏遮挡的问题：`FlowStateHost` 成功分支未应用 `modifier`。

### 修复
- **书架、统计、书籍详情三个页面在数据加载完成后内容被状态栏与顶栏遮挡**。`FlowStateHost` 的 `when` 中，error / isLoading / isEmpty 三个分支均向状态组件传递 `modifier`，成功分支却直接调用 `content()`，导致该参数丢失。这三个页面均通过该参数传入 `Scaffold` 的 inset padding：
  ```kotlin
  ) { paddingValues ->
      FlowStateHost(
          modifier = Modifier.fillMaxSize().padding(paddingValues),   // 成功分支里被丢弃
  ```
  于是 88dp（24dp 状态栏 + 64dp `TopAppBar`）的安全区在成功状态下失效，`LazyColumn` 从 y=0 开始绘制。加载中、空和错误状态仍能正确应用安全区。成功分支现改为 `Box(modifier = modifier) { content() }`。
  - 受影响的调用点只有传了 padding 的三处：`LibraryScreen`、`StatsScreen`、`BookDetailScreen`。`NotesScreen`／`OpdsScreen`／`ReadingListsScreen` 只传 `fillMaxSize()`，且走 `FlowScaffold`（自己用 `Box` 加 padding），不受影响；`ReaderScreen` 不传 `modifier`，全屏出血设计，行为不变。
  - **设置页不受影响**：它直接给 `Column` 加 `padding(paddingValues)`，不经过 `FlowStateHost`。

### 测试
- `ShellWindowInsetsTest` 新增 2 例，单测 260 → 262（0 失败）。此前的 `tabScreenAppliesEachInsetExactlyOnce` 未覆盖该问题，原因是其 `FakeScreen` 自行向 `Box` 添加 padding，未经过 `FlowStateHost`。
  - `stateHostSuccessContentClearsTheTopBar` 复刻真实组合结构（padding 经 `FlowStateHost` 的 `modifier` 传入），断言内容顶边 = 24dp + 64dp。恢复旧代码后该用例失败，验证回归门禁有效。
  - `stateHostLoadingAndSuccessShareTheSameContentTop` 把加载态钉在同一偏移，防止将来修好一个状态又弄坏另一个。

---

## [v56.4.3] - 2026-08-12
> 功能性问题修复：处理书内全文搜索返回空结果、翻页模式阅读统计未落库等问题，并修复其他缺陷。

### 修复
- **书内全文搜索返回空结果**（影响范围较大）。`FullTextSearch.search()` 通过 `WHERE book_id = ?` 限定书籍，但 `book_content_fts` 为 FTS5 **external content** 表（`content='book_content'`），其列未声明类型，且 `SQLiteDatabase.rawQuery()` 只能绑定 `String` 参数。在该条件下，SQLite 不会将 INTEGER `5` 与 TEXT `'5'` 自动视为相等，查询条件因而恒为假。现改为 `WHERE book_id = CAST(? AS INTEGER)`。
  - 全局搜索 `searchAll()` 不受影响：它不按书籍过滤，只有 `MATCH`。`deleteBookContent()` 也不受影响：它查的是普通表 `book_content`，`book_id INTEGER` 有正常亲和性，TEXT 参数会被转换。
  - 根因已用相同 schema + 触发器在 sqlite3 上实证复现：绑定 TEXT 返回 `[]`，绑定 INTEGER 返回该行，加 `CAST` 后恢复正常。
- **翻页模式与漫画的阅读统计未落库**。`updatePosition()` 接收的 `position` 在 `SLIDE`/`NONE` 下为字符偏移，在 `PAGED` 与漫画下为**已渲染页下标**；此前两者均传入按字符计算的 `ReaderSessionTracker.recordProgress()`。翻页每次仅前进 1，被当作读取 1 个字符，`readPages` 保持为 0，`saveReadingStats()` 的 `if (seconds > 0 && pages > 0)` 条件随之丢弃整个会话数据，包括累计阅读时长。
  - 新增 `ReaderSessionTracker.recordPageProgress(pageIndex, charsPerPage)`：一次前进算一页，`charsPerPage` 只用于把速度估算换算回"字/分钟"。
  - 新增 `ReaderPositionUnit`（`CHARACTERS` / `PAGE_INDEX`）把"位置的单位到底是什么"这条规则从 ViewModel 里抽出来，使其可被单测覆盖；漫画恒为页下标，`PAGED` 为页下标，其余为字符。
  - `updatePosition()` 的移动阈值随单位切换：字符模式仍是 200，页模式为 1——页下标每次只 +1，原来的 200 字阈值在页模式下永远不可能触发。
- **阅读器的按书重建索引绕过了索引锁**。v56.4 只在 `SearchRepositoryImpl` 内部加了 Mutex，而 `ReaderViewModel.indexBookForSearch()` 直接调用 `deleteBookContent` + `indexChapter`，可与全局重建交错，导致一本书在重建已把它计为"已索引"之后又被删掉。现把锁移进 `FullTextSearch`（`withIndexLock`），任何调用方都无法绕过；因 `kotlinx` 的 `Mutex` 不可重入，`rebuildIndex()`（加锁）与 `rebuildIndexLocked()`（已持锁）拆开，每个入口只加一次锁。
- **切换章节时保存了过期的滚动位置**。`goToChapter()` 用 `uiState.currentPosition` 覆盖 `chapterPositions[上一章]`，但 `currentPosition` 经 250ms 节流，`chapterPositions` 始终更新；改为 `getOrPut`，只在该章从未记录过时才回填。
- **`recordProgress()` 中潜伏的除零**。`readChars %= charsPerPage` 在 `charsPerPage` 为 0 时抛 `ArithmeticException`。当前调用方恒传正数（不可达），现统一 `coerceAtLeast(1)`。
- **`CacheManager.estimatedMemory` 只增不减**。`memoryUsage` 在 put 时累加，但 LRU 淘汰（内外两层 `removeEldestEntry`）与 `evictBook()` 都不归还，替换同一章也会重复计数。现在淘汰/替换/逐书清理都会释放对应字符数，计数改在 synchronized 块内用 `put()` 的返回值做差。该值仅用于展示与自适应容量判断，不影响正确性。
- **划词高亮和书签少存一个字符**。`ParagraphContent.rawRange()` 返回**闭区间**（`rawStart..rawEnd`，`rawEnd` 为选区最后一个字符的偏移），而 `Annotation.endPosition` 的消费方均按**开区间**处理。写入方此前将闭区间的 `range.last` 原样传递，导致选中“hello”时实际存储并高亮“hell”，单字符选区的高亮宽度为 0。现新增 `ParagraphContent.selectionSpan()`，在唯一位置完成 `+1` 转换并提取文本；该纯函数可独立测试。
- **重叠或嵌套的高亮会把段落文字重复渲染**。`buildParagraphContent()` 逐条 annotation 追加 `substring(relStart, relEnd)`，重叠区间被追加两次：`abcdefghij` 上高亮 `[0,6)` 与 `[3,9)` 渲染成 `abcdefdefghij`；嵌套时 `lastEnd` 还会**倒退**，连间隙文字一起重复（`abcdefghcdefghij`）。更糟的是 `rawOffsets` 随之与显示文本错位，此后所有划词都会映射到错误的章节偏移。现在按已输出位置钳制起点、跳过被完全覆盖的区间；重叠区取先出现者的颜色，尾部区取后者，文字只输出一次。
- **划词做的书签跳不回原处**。书签的 `position` 会被 `goToBookmark()` → `goToChapter()` 写回 `currentPosition`，而 `ReaderScreen` 把它交给 `ScrollState.scrollTo()`（滚动模式的**像素**）或 `PagedReader` 当**页下标**用；划词书签存的却是**字符偏移**。滚动模式下会滚到一个无意义的像素位置，分页模式下字符偏移远大于页数、被 `coerceIn(0, pages.size - 1)` 钳到章节最后一页。书签现在统一只存阅读器自己的位置（选区就在当前屏/当前页上，回到该位置即可看到它），`addBookmark()` 不再接受字符偏移参数。高亮不受影响——`Annotation` 的位置确实是字符偏移。
- **「最近 7 天趋势」图最多只显示两三天**。`getRecentDailyStats(limit)` 走的是 `getRecentStats(limit)` = `ORDER BY date DESC LIMIT :limit`，`LIMIT` 限的是**行数**；但 `reading_stats` 带 `(bookId, date)` 唯一索引，一天一本书一行。同时读 3 本书的用户请求 7 天，只能拿到 21 行中最新的 7 行，即最近 2–3 天（按天聚合后 `takeLast(7)` 也救不回已经没查出来的日期）。现改为按日期范围查询：新增 DAO 方法 `getStatsSince(startDate)`，起始日取 `今天 - (limit - 1)`（含今天），`yyyy-MM-dd` 的字典序等价于日期序，因此 `date >= :startDate` 是正确的范围扫描。`limit` 另做 `coerceAtLeast(1)`，避免 0 或负数算出未来的起始日期把全部数据过滤掉。
  - 统计页的周报（`getReadingReport(days)`）不受影响：它走 `getAllStats()` + 日期过滤，本来就是按天算的。`getRecentStats()` 这个 repository 方法保留原样——它的语义本就是"最近 N 行"，无调用方受影响。
- 顺带清理：删除 `ReaderViewModel` 中已无调用方的私有 `loadChapterContent()`；`toggleTts()` 改为走 `ReaderTtsCoordinator.pause()/speakFrom()`，不再直接操作 `ttsManager`，并在页模式下从章首朗读（页下标不是字符偏移，不能当起点）。

### 测试
- 新增 `FullTextSearchQueryTest`（4 个用例）。Robolectric 自带的 SQLite **没有编译 fts5 模块**（实测报 `no such module: fts5`），离线环境也没有 sqlite-jdbc 可替代，因此无法真的执行生产查询；测试改为两头夹：一头断言 `SEARCH_IN_BOOK_SQL` 常量确实带 `CAST(? AS INTEGER)` 且不含裸 `book_id = ?`，另一头在无声明类型的普通表上实证"TEXT 参数匹配不到 INTEGER 值、加 CAST 才能匹配"这一 SQLite 语义。
- `ReaderSessionTrackerTest` 新增 8 个用例，其中一个专门固化 bug 现象：把页下标喂进字符路径，30 次翻页后 `readPages` 仍为 0。
- 新增 `ReaderPositionUnitTest`（6 个用例）覆盖单位判定规则。
- `CacheManagerTest` 新增 4 个用例覆盖内存计数的淘汰/替换/逐书释放。
- `ReaderTextMappingTest` 从 7 个扩到 16 个用例，覆盖 `selectionSpan` 的开闭区间转换（含单字符选区、跨 markdown 标记、非零段落起点、空选区/缩进字符拒绝）与重叠/嵌套/同起点高亮的去重。两处修复都做了反向验证：还原去重逻辑后 3 个用例失败并打印出重复文本（`expected:<abcdef[]ghij> but was:<abcdef[def]ghij>`），还原 `+1` 后 4 个用例失败（`expected:<5> but was:<4>`）。
- `ReadingStatsRepositoryImplTest` 新增 3 个用例：7 天 × 3 本书 = 21 行时必须聚合出 7 天、DAO 收到的起始日必须是"今天 - 6"（含今天）、`limit = 0` 时不得算出未来日期。反向验证（改回按行取最近 N 条）复现出 `expected:<7> but was:<3>`，另两个用例同时失败。
- 单测总数 227 → 260（0 失败），测试文件 45 → 47。测试广度 72.6% → 75.8%（47/62，实测 `coverageSummary` 输出）。

## [v56.4.2] - 2026-08-11
> 修复 issue #6：各页面顶部出现额外空白区域并造成内容截断。

### 修复
- **窗口 inset 被重复应用两次**（issue #6“安卓版本界面 UI 异常：每个界面的顶部 UI 过宽，导致内容被截断”）。`Navigation.kt` 的外层 shell `Scaffold` 使用默认的 `ScaffoldDefaults.contentWindowInsets`，并通过 `Modifier.padding()` 将 `paddingValues` 传给内容；但 padding 只会应用 inset，不会消费 inset。下层 9 个界面的 `Scaffold` 与 `TopAppBar` 因而再次应用同一份 inset：标题上方增加一条状态栏高度的空白（24dp 空白 + 24dp 重复 inset + 64dp 标题栏 = 112dp），底部导航栏 inset 也被重复计算，造成内容上移或截断。
- 修法：抽出 `FlowShellScaffold`，设 `contentWindowInsets = WindowInsets(0, 0, 0, 0)` 并追加 `.consumeWindowInsets(paddingValues)`。顶部归零后，状态栏 inset 由各界面的 `TopAppBar` 应用**恰好一次**，应用栏绘制到透明状态栏之下——这本来就是 `MainActivity` 里 `enableEdgeToEdge()` 与 `themes.xml` 透明状态栏/导航栏的意图。底部仍需 `consumeWindowInsets`，因为 shell 在底部花掉的 padding 是 `NavigationBar` 的实测高度，其中已经含有导航栏 inset。
- 阅读器路由不在 `bottomNavItems` 中（`showBottomBar` 为 false），shell 现在把 inset 原样透传，`ReaderControls` 自己的 `windowInsetsPadding` 因此也变为应用一次，全屏沉浸式生效。
- 平板/大屏分支（`screenWidthDp >= 600` 的 `NavigationRail`）本就没有外层 `Scaffold`，inset 从来只应用一次，未改动。

### 测试
- 新增 `ShellWindowInsetsTest`（2 个用例）作为回归闸门：断言标签页界面内容顶部恰为 `状态栏 24dp + TopAppBar 64dp = 88dp`，且内容底边正好等于 `NavigationBar` 顶边；另一个用例断言 shell 无底栏时 inset 仍原样透传给自行沉浸的全屏界面。
- 反向验证（故意还原修复）复现出预期数字：`Actual top is 112.0.dp, expected 88.0.dp` 与 `Actual top is 48.0.dp, expected 24.0.dp`。
- 说明这类 bug 为何此前未被发现：Robolectric 下 inset 默认全为 0，且必须把 `WindowInsetsCompat` 派发到 **ComposeView**（`findViewById(android.R.id.content).getChildAt(0)`）才能到达 Compose——派发到 decorView 无效；同时唯一的 Roborazzi 用例只截取 `BookCover`/`BookShelfSkeleton`，不含任何 `Scaffold`。
- 测试广度 71.0% → 72.6%（45/62）。

## [v56.4.1] - 2026-08-07
> 安全加固补丁，在 v56.4.0 审计基础上进一步收紧相关边界。

### 安全修复
- **LanTransferServer 令牌生成的潜伏缺陷**：`String.generateToken(length)` 用目标长度而非字符集长度作为索引上限（`random.nextInt(length)`）。当前 `TOKEN_CHARS`（16 个 hex 字符）与 token 长度恰好都是 16，所以现有令牌的熵并未受损——这是一个潜伏缺陷而非活跃漏洞：一旦令牌长度调高就会抛 `StringIndexOutOfBoundsException`，调低则会静默地只取字符集前 N 个字符。现改为 `random.nextInt(charset.length)`，让熵不再依赖两个常量的巧合相等。新增两个回归测试（字符集覆盖度 + 50 个令牌唯一性）。
- **消除 ContentProvider 中的 runBlocking**：`FlowReaderContentProvider` 此前在 Binder 线程上调用 `runBlocking { bookDao.getAllBooks().first() }` 和 `runBlocking { bookDao.getBookById(bookId) }`，阻塞 Binder 线程池导致系统范围 ANR 风险。现新增同步 DAO 方法 `getAllBooksSync()` / `getBookByIdSync()`，避免阻塞。
- **收紧 FileProvider 路径权限**：`file_paths.xml` 此前把三棵完整目录树（`<external-path path="." />`、`<files-path path="." />`、`<cache-path path="." />`）全部授权给 FileProvider，违反最小权限原则。全仓审计确认 `getUriForFile()` 只有一个调用点——阅读器的「分享阅读卡片」——因此现在只授权 `share_cards/` 一个子目录，`ShareCardGenerator` 相应改为写入该子目录（此前写在 cache 根目录）。其余三棵树均无消费方：`covers/` 由 Coil 在进程内按路径读取，`opds/` 下载在进程内导入，LAN 备份 JSON 走 `LanTransferServer` 自己的令牌校验 socket，都不经过 FileProvider。
- **为 backup_rules.xml / data_extraction_rules.xml 补充安全文档注释**：说明 Android 备份仅含 DataStore Preferences（不含 DB），并标注 OPDS 密码平文持久化需后续引入加密（EncryptedSharedPreferences 或 Jetpack Security）。

### 文档
- 同步更新 `AGENTS.md`（新增"Security"部分）与 `CLAUDE.md`（"Version bookkeeping"记录安全修复）。
- 新增 `SECURITY_AUDIT_REPORT.md`（完整审计报告）与 `SECURITY_FIX_SUMMARY.md`（修复总结）。

## [v56.4.0] - 2026-08
> 开展第二轮安全审计，对 v56.3 收尾后的实现进行全面复查。

### 修复
- **「用 FlowReader 打开」不再静默失败**：MainActivity 的 `ACTION_VIEW` intent-filter（epub/text）此前无任何处理——其他应用分享的文件会丢失。现在按 URI 走受限导入管线（解析上限/zip-slip 防护全数生效），intent 消费一次即清空，配置变更不会重复导入。
- **FTS 索引重建竞态**：并发搜索可能交错执行 `deleteAllContent`/`indexChapter` 导致索引不一致；`SearchRepositoryImpl` 用 Mutex 串行化重建。
- **LAN 服务只绑局域网接口**：不再绑定 `0.0.0.0`，蜂窝网等非局域网接口无法访问；令牌防护不变。

### 复查结论（未发现新问题）
- 无危险 API/硬编码密钥；无 WebView；`!!` 仅剩 ContentProvider 的标准 `context!!`（onCreate 保证非空）。
- OPDS：目录地址、重定向每一跳、acquisition 下载均单独校验局域网；目录 2MB / 下载 200MB 上限。
- ZIP：路径穿越（`..`/绝对路径/`__MACOSX`）拒绝、条目数与单条目上限。
- 备份导入：SAF 与 LAN 路径均 200MB 上限、事务原子写入；导出/导入不含可执行内容。
- FTS 查询转义、HTML 导出转义；Android 备份仅含 settings（DB 不外传）。
- ContentProvider 只读、无文件路径/正文；FileProvider 路径白名单。

## [v56.3.0] - 2026-08
> 完成 v54–v56 阶段的安全问题和无效代码清理。

### 修复
- **“屏幕常亮”设置恢复实际生效**：此前仅持久化而未应用（v52 清理遗留）；现阅读器按设置应用或清除 `FLAG_KEEP_SCREEN_ON`。
- **退出时进度和统计数据得到保存**：`ReaderViewModel.onCleared()` 此前在 `viewModelScope` 取消后仍启动协程保存，现改为通过独立 IO 作用域同步落库。
- **备份导入读取上限**：SAF 备份导入与 LAN 导入一致增加 200MB 上限，超限明确报错而非 OOM。
- **LAN 服务生命周期**：关闭局域网传输对话框即停止 HTTP 服务，不再后台常驻。
- 阅读器选中引擎消除 `!!`（合规门禁要求无新增 `!!`）；删除 v54 拆分后遗留的死函数 `saveProgressImmediately`；修复语言枚举单测（校验新增五种语言资源目录存在）。

## [v56.0.0] - 2026-08
> 完善无障碍能力、性能基线及持续集成门禁，确保应用功能可用性和构建结果可验证。

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

## [v55.0.0] - 2026-08

> 完善书架视图、自适应导航、内容分享及离线局域网传输能力。

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

## [v54.1.0] - 2026-08

> 完成阅读器重构第一阶段，涉及原生文本选择、分页翻页、ViewModel 拆分、启动性能和缓存策略。

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

> 启动阅读器重构，新增图片及漫画阅读入口，使图像内容使用独立渲染路径。

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
> 完成 UI 重构第二阶段及 v52 顺延的书架管理能力，重点涉及组件库建设、字符串资源化和书架批量操作。

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
- 测试 81 → **159 个**，测试广度 55.8% → **58.6%（34/58）**：MOBI 解压与尾部裁剪、DRM 拒绝、FB2 解析、zip slip 防护、OPDS 局域网边界（包括 `fcbooks.com` 等易被误判为 IPv6 前缀的域名）、批量元数据的 null/空白语义、拖拽排序的越界及元素保留性质。

## [v52.0.0] - 2026-07
> 完成 UI 全面重构第一阶段，重点处理基础设计系统、无效设置清理和实际功能接线。

### 无效设置清理与渲染接入
- **字体族设置接入实际渲染**：`:core` 新增 `ReaderTypography`，`ReaderContent` 改为使用 `readerBodyStyle`。字体枚举由 8 项收敛为 4 项（默认/衬线/无衬线/等宽）；楷体、仿宋在系统上无法可靠解析，旧值按语义迁移至最接近的真实字体。
- **自定义字体加载流程完善**：导入的 `.ttf/.otf` 经 `Typeface.createFromFile` 校验后加载，读取失败时回退至内置字体，并保留明确的容错路径。
- **段间距语义修正**：`paragraphSpacing` 从被当作 dp 使用（默认值 1.0f → 1dp 间距，等于没有间距）改为字号倍数，读取侧对旧值做值域迁移。设置面板新增段间距滑块与首行缩进开关。
- **手势设置接入渲染层**：左／中／右点击、双击、长按、左右滑动全部映射到 `GestureAction`；边缘热区宽度 `leftEdgeWidth/rightEdgeWidth` 可在设置中调节。此前仅读取 `tapZoneRatio`，且左右行为为硬编码。
- **翻页模式收敛**：删除未实现的 `SIMULATION`（仿真）、`CURL`（卷曲）、`SLIDE_OVER`（滑动覆盖）三个界面选项，只保留存在实际实现差异的 `SLIDE`（动画翻页）与 `NONE`（瞬时切换）；旧持久化值回退至 `SLIDE`。仿真翻页调整为 v55+ 独立课题。
- **备份和恢复功能接入**：`onExportReady` / `onImportReady` 此前无调用点，备份与恢复按钮无法触发操作；现已接入系统文件选择器。
- **书签死代码处置**：书籍详情页补齐第三个「书签」Tab，渲染此前已实现但从未被调用的书签列表。

### 设计系统（`:core` 从空壳变成真实模块）
- 新增 `designsystem/token`：`FlowSpacing`（6 档）、`FlowRadius`（4 档）、`FlowElevation`、`FlowMotion`（4 时长 + 3 曲线）、`FlowShapes`、`FlowTypography`（正文去掉为拉丁文设计的 0.5sp 字距）、`FlowBrandColors`。
- 新增 `FlowTheme`：动态取色不作为 Android 12+ 的强制行为。新增 `ColorSource`（品牌配色 / 跟随壁纸），默认使用品牌配色，设置页可切换；`AppThemeMode` 新增“跟随系统”。
- 新增 12 套 `ReaderPalette`（纸白/米黄/护眼绿/亚麻/晨雾/冷灰/电子墨水/夜黑/墨蓝/深棕/曜石/纯黑），阅读设置面板改为色板网格，所见即所得。此前阅读器只有 2 组硬编码配色。
- 新增 `FlowStateHost`：书架/详情/阅读器/统计四套各写一遍的 loading/empty/error 收敛为一套。

### 阅读器
- **自动夜间模式按时间触发**：改为每分钟轮询时间源，19:00 到达后切换至所选夜间色板；此前 `Calendar` 仅在组合期读取一次，需退出并重新进入阅读器后方可生效。
- **进度条反映章节内阅读进度**：进度计算调整为章节序号与章内滚动比例的组合。此前仅使用 `当前章/总章数`，章内滚动不会改变进度。拖动时浮层显示目标章节名，松手后执行跳转。
- **控制层适配系统栏**：顶栏和底栏改用 `WindowInsets`，边到边模式下避免被状态栏覆盖；顶栏操作由 8 个同权重图标调整为 4 个高频动作及 overflow。
- **高亮范围与文本一致**：删除让用户"自己输入要高亮的文本"的对话框（输入内容与实际存储的字符区间可能不一致），改为长按段落弹出底部操作面板，高亮精确覆盖该段落，并支持复制与带备注书签。
- **阅读设置面板**：`AlertDialog` → `ModalBottomSheet`，改动实时预览；`ReaderViewModel` 六个近乎重复的设置写入方法收敛为一个。
- **CJK 排版**：正文行宽上限 34 字（平板不再拉出 100+ 字的长行），首行缩进两字可开关，标点避头尾。

### 信息架构
- **转盘降级**：从底部一级 Tab 移出（底部导航 4 项 → 3 项：书架/统计/设置），改由书架顶栏 overflow 进入。转盘的 60fps 实现原样保留。
- **书架继续阅读直达**：“继续阅读”卡片点击后直接进入阅读器，无需经过详情页。
- 导入改为 `ExtendedFloatingActionButton`，排序/转盘/设置收进 overflow。
- 设置页删除与阅读器面板重复的字号/行间距/翻页模式入口（此前两处互相覆盖），只保留应用级与设备级设置。

### 修复与性能
- **书架导入失败信息得到展示**：`LibraryScreen` 此前通过 `LaunchedEffect { clearError() }` 丢弃错误，现改为使用 Snackbar 呈现失败原因。
- **大章节目录页面避免一次性组合全部内容**：章节由单个 `LazyColumn` item 内的 `Column` 改为 `LazyColumn` 自身的 items，2000 章书籍可按需组合。
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
- **开始阅读异常修复**：TTS 引擎改为在用户点击朗读时懒初始化，避免进入阅读器时因系统 TTS 初始化异常导致应用退出。
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
- **书签系统恢复**：阅读器控制栏恢复书签入口，长按段落可添加带备注的书签，并支持章节内书签列表跳转和删除。
- **TTS 朗读恢复**：新增系统 TextToSpeech 管理器，阅读器支持从当前阅读位置朗读、暂停和释放资源，不引入第三方 SDK。
- **阅读进度 Widget**：新增 Android 主屏幕 Widget，展示最近阅读书籍标题和进度百分比。
- **阅读专注模式**：阅读器新增全屏专注按钮，可隐藏状态栏和导航栏，滑动临时唤出系统栏。
- **夜间模式自动切换**：阅读设置新增自动夜间模式，根据本地时间在夜间切换为深色阅读配色。

## [v48.0.0] - 2026-07
- **阅读统计精确化**：阅读页数改为按章节真实字符位置累计，并按字号/行距估算每页字符数；未满一页的字符会跨滚动累计，暂停超过 5 分钟后切分为新阅读会话。
- **护眼提醒可配置**：阅读设置新增 15/20/30/45/60 分钟护眼提醒间隔，持久化到 DataStore。
- **翻页记忆**：阅读器按章节记忆滚动位置，切换章节再返回时恢复到上次位置。
- **书架筛选增强**：书架页面新增分类 FilterChip 筛选；搜索继续同时匹配书名和作者。
- **错误状态处理**：阅读器加载失败状态新增重试按钮，提供重新加载路径。
- **APK 体积优化**：Release 构建启用 R8 full mode，并保留资源压缩/混淆。

## [v47.0.0] - 2026-07
- **修复搜索"未找到匹配结果"提前显示**：SearchDialog 新增 `hasSearched` 标记，用户点击搜索后才显示"未找到匹配结果"，消除输入即触发的误报
- **修复阅读统计不保存**：`saveReadingStats()` 原仅 `onCleared()` 调用，导航保存状态下 ViewModel 不销毁导致统计不落库；新增每 30 秒定期保存 + 章节切换时保存，保存后重置会话计数器防止重复统计
- **修复阅读统计异常退出**：`saveReadingStats()` 添加 try-catch，防止 DB 写入异常导致应用退出。
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
- **章节切换空白页面修复**：`goToChapter()` 异步加载 content 后未更新 `currentChapter`，现改为在协程内完成 content 加载后再更新状态。

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
*   **决策转盘改进**：`spin()` 改为自动管理协程（`viewModelScope.launch`），无需外部 `LaunchedEffect` 触发；旋转角度基于当前角度叠加，改善连续旋转效果；转盘文字保持正向显示。
*   **Gradle 升级**：Gradle Wrapper 从 `8.7` 升级到 `9.6.1`；重构 `gradle.properties`，添加 `UseParallelGC`、`vfs.watch`、`kotlin.daemon.jvmargs` 等优化项，提升构建性能。
*   **漏洞修复**：
    *   `StatsViewModel`：阅读统计数据不再只快取一次，每次收集时实时刷新
    *   `FullTextSearch`：消除不安全的 `!!` 操作符，使用局部变量确保空安全
    *   `WheelViewModel`：动画协程取消后 `isSpinning` 自动恢复为 false，防止状态永久停留在旋转中
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
*   **异常退出修复**：所有书籍加载流程添加 `try-catch` 和 `bookId > 0` 校验，数据库异常或文件缺失时显示错误提示，不再直接退出应用。

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
*   **缺陷修复**：修复 `ReaderViewModel` 中空安全断言 `!!` 的使用问题。
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
*   **护眼提醒**：按固定间隔提醒用户休息，默认间隔为 20 分钟。
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
*   发布版本记录。

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
