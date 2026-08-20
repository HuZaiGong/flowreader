# FlowReader 安全复查记录（2026-08-20）

看了一遍 `app` / `core` / `data` / `domain` / `feature` 的源码，纯静态审查，没跑构建也没做动态验证。基线是 v56.6.1（versionCode 5661）。

先说结论：底子干净。离线优先、只申请了一个 `INTERNET` 权限、没有账号也没有分析和同步，攻击面本来就小。v56.3/v56.4/v56.4.1 三轮审计留下的整改项我逐条对了源码，都还在（FileProvider 收窄、ContentProvider 去掉了 `runBlocking`、LAN token 生成修好了、FTS 转义、ZIP 防穿越、备份上限、全程无 WebView）。

问题都出在同一个地方：导入管线对"外面塞进来的文件"信得太多。一共 7 条，2 中 2 低 3 信息。

| # | 等级 | 一句话 | 文件 | 状态 |
|---|------|--------|------|------|
| 1 | 中 | 导入文件名不消毒，能穿越目录覆盖应用私有文件 | `util/BookParser.kt` | 已修复 (v56.6.2) |
| 2 | 中 | EPUB/漫画导入好几处读取没有上限 | `util/BookParser.kt` | 已修复 (v56.6.2) |
| 3 | 低 | `LanTransferClient` 只看 scheme，不管主机是不是局域网 | `util/LanTransferClient.kt` | 已修复 (v56.6.2) |
| 4 | 低 | LAN 服务器单线程无超时，能被慢连接卡死；token 用 `contains` 匹配 | `util/LanTransferServer.kt` | 已修复 (v56.6.2) |
| 5 | 信息 | `providers.yaml` 里有明文 API key，还进了 git | `providers.yaml` | **定位有误**，实际在 `V56.5.0_PLAN.md`，已脱敏 (v56.6.2) |
| 6 | 信息 | release 用 debug 签名 | `app/build.gradle.kts` | 已修复 (v56.6.2) |
| 7 | 信息 | 导出的 ContentProvider 没加权限（有意为之，但是个隐私口子） | `AndroidManifest.xml` | 已修复 (v56.6.2) |

---

## 中危

### 1. 文件名不消毒 → 路径穿越

位置：`app/src/main/java/com/flowreader/app/util/BookParser.kt:598-611`，病根在 `getFileName()`（`:222-232`）。

`getFileName()` 拿到 `ContentResolver` 给的 `DISPLAY_NAME` 就直接用，`copyFileToInternal()` 又用 `File(booksDir, fileName)` 拼路径，中间没人拦 `/` 和 `..`。麻烦的是，一次 `ACTION_VIEW` 导入会调用 `getFileName()` 好几遍——`displayName()`、`parseBook()`、`copyFileToInternal()` 各一次。这就给了恶意 ContentProvider 做手脚的空间：第一次返回 `good.epub` 骗过格式检测，等到真正写盘那次再返回 `../../databases/flowreader_db`。这是个典型的 TOCTOU。

能造成什么后果？设备上另一个恶意应用，诱导用户点"用 FlowReader 打开"（或者干脆对已导出的 `MainActivity` 发个显式 Intent），就能把自己的内容写到 Room 数据库或 DataStore 设置文件上，把用户数据冲掉，或者塞一个构造好的 SQLite 文件进去。不是远程代码执行，但足够恶心。

怎么修：显示名过一遍消毒，只留 `[\p{L}\p{N}._-]`，跟 `OpdsClient.sanitizeFileName()` 一个标准；写盘前用 `canonicalPath` 确认目标真的落在 `filesDir/books` 里面；整个导入过程认准同一个已消毒的文件名，别给"多次查询返回不同结果"留机会。

### 2. EPUB/漫画导入有好几处读取不设上限

都在 `util/BookParser.kt` 里：

- `extractEpubCover()`：`:376` 的 `zipInput.readBytes()` 和 `:388` 的 `getInputStream(coverEntry).readBytes()`，封面图直接整个读进内存，多大都读。塞个几百 MB 的 `cover.jpg` 就能把进程撑爆。
- `parseEpubStream()`：`:252` 把整个 EPUB `copyTo()` 到缓存，不看大小；`:264`、`:269` 的 `container.xml` 和 opf 直接 `readText()`，也不设限。
- `extractEpubCover()` 的 `:364`，又是一次整包无上限复制。
- `parseComicZipStream()` `:698` 整个 CBZ 无上限复制；`parseComicImageStream()` `:672` 单图无上限复制。
- `copyFileToInternal()` `:610` 无上限复制。

这些入口全都能被 `ACTION_VIEW` 或显式 Intent 打到。文档里写着"导入全链路有上限"，但 EPUB/CBZ 的整包复制和封面提取恰好是漏掉的两块，结果就是磁盘能被写满、内存能被撑爆。顺带一提，`parseEpubStream` 失败时那个临时文件还不删，会在缓存目录里越攒越多。

怎么修：整包复制前先用 `getFileSize()` 预检，或者干脆改成流式 `copyCapped` 带个上限（比如 256MB）；封面和 OPF/container 改用现成的 `readCappedBytes()`（封面可以直接复用那个 24MB 的上限）；`parseEpubStream` 的异常分支补一个 `tempFile.delete()`。

---

## 低危

### 3. `LanTransferClient` 不校验主机

位置：`app/src/main/java/com/flowreader/app/util/LanTransferClient.kt:22`。

这里只判断了 `uri.scheme != "http"`，主机是什么完全不管，`http://<任意公网地址>/...` 照样下。OPDS 那边用 `OpdsAddress` 卡得死死的，备份接收这一端却是敞开的。用户要是粘了个恶意公网链接，应用就会从公网拉最多 200MB 回来当备份 JSON 导入，书库和书签数据都可能被污染——也跟"INTERNET 只服务局域网"这个承诺对不上。

怎么修：复用 `OpdsAddress.isLanUrl()`（或者抽个 `LanHostPolicy`）校验 `uri.host`；顺便把下载路径也卡精确了，只认 `/backup/<16 位十六进制>`。

### 4. LAN 服务器单线程、无超时，token 匹配太松

位置：`app/src/main/java/com/flowreader/app/util/LanTransferServer.kt:75-88`。

`executor` 就一条线程，accept 下来的 socket 又没设 `soTimeout`。同一个网段里随便谁连上来、连上之后不发数据，就能把这唯一的工作线程占死，对话框开着的这段时间里别人全都连不上。另外 `requestLine.contains(expectedPath)` 不是精确匹配，`/x/backup/<token>` 这种路径也能混过去。

风险不高——服务是手动开的、开一小会儿、token 有 64 位随机——但代码注释自己说"只应答精确的 `/backup/<token>`"，现在没做到。

怎么修：accept 之后设 `client.soTimeout = 5000`；请求行跟 `"GET $expectedPath HTTP/1.1"` 精确比；超时或异常就关掉这条连接接着循环。

---

## 信息级

### 5. `providers.yaml` 里有明文 key —— 定位有误，已按实际位置处理（v56.6.2）

原文位置：`providers.yaml:17`（原文引了完整 key 字符串，此处不复述），并称 `git ls-files` 已确认跟踪。

> **v56.6.2 核查结论（保留原文，仅追加）：结论方向对，位置不对，而且不是一次就查对的。**
>
> 报告最初把路径写成 `.claude/providers.yaml`——`.claude/` 自 v56.6.1 起整体 git-ignore。修正为根目录 `providers.yaml` 之后，第一轮核查又断言「明文 key 自 v45.0.2 起在提交历史中」，同样不成立。
>
> 实际情况：`providers.yaml` **被跟踪，但其提交历史中从未出现明文 key**，各版本均为 `{env:DEEPSEEK_API_KEY}`。本机工作区那份确有真 key，但该文件被 `git update-index --skip-worktree` 标记（`git ls-files -v providers.yaml` 输出 `S` 前缀），故 git 完全忽略工作区副本，`git status` 恒显示干净——两轮核查的误判均源于此：读了工作区文件，而 git 看到的是另一份内容。
>
> 全历史扫描（`git rev-list --all` 逐树 `git grep -F`）确认，该 key 字面量在整个仓库仅有一处提交记录：`V56.5.0_PLAN.md:128`。该段文字正在论证「此 key 是公共免费占位值、不构成泄露」——结论无误，但把字面量抄进了正文，于是这份计划文档成了唯一真正提交了 key 的位置，并随 v56.5.0 进入公开的 `main`。现已改为不复述。
>
> 处置：key 系公共免费中转站共享值，未轮换；改写已公开分支的历史属破坏性操作，留待仓库主人决定。方法论记录于 `AGENTS.md`：判断「凭证是否已提交」须依据 `git ls-files -v` 与全历史扫描，不能读工作区文件。

### 6. release 用 debug 签名 —— 已修复（2026-08-20）

位置：`app/build.gradle.kts` 的 `signingConfigs`/`buildTypes` 块。

原来 release 直接用 debug 密钥签名。debug keystore 是公开的，谁拿到 APK 都能伪造一个同签名的升级包糊上去，所以这样的包不能对外发。

现在改成：仓库根目录放一个 `keystore.properties`（已被 git 忽略）配置正式密钥库，存在就用它签，缺失就回退到 debug。回退是留给没有密钥库的 CI 用的——它照样能产出可测试的 `app-release.apk`；只有在持有密钥库的机器上构建才会拿到正式签名。另附 `keystore.properties.example` 说明字段与密钥库生成方式。

验证落在产物上，不止于构建配置：`apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk` 输出 `Signer #1 certificate DN: CN=HuZaiGong, OU=Dev, O=flowreader`，与 debug 密钥的 `CN=Android Debug` 及其指纹均不同。顺带记一个坑：首次验证时磁盘上的 APK 比 `keystore.properties` 还早，配置无误但产物仍是旧的 debug 签名——验证前先比一下两者的时间戳。

### 7. 导出的 ContentProvider 没加权限 —— 已修复（v56.6.2）

位置：`AndroidManifest.xml:55-59`。

`FlowReaderContentProvider` 是 `exported="true"`，暴露书目和进度元数据，又没配 `android:permission`。按文档，这是为了让第三方桌面挂件能读，属于设计选择。但换个角度看，设备上任何一个应用都能读到用户在读哪些书、读到哪、打了什么标签。至少在 SECURITY.md 里把这个隐私口子写明白，或者考虑改成自定义的 signature 级权限。

> **v56.6.2 处置：**新增自定义权限 `com.flowreader.app.permission.READ_LIBRARY` 挂在 `android:readPermission` 上，调用方须声明权限且经用户运行时授权方可读取；导出能力保留，因为那正是该 provider 的用途。
>
> 保护级别取 `dangerous` 而非报告建议的 `signature`：`signature` 只允许与本仓库同签名的应用读取，等于把功能删掉而不是给它加门禁。写权限另配 `signature` 级 `WRITE_LIBRARY` 兜底（`insert`/`update`/`delete` 本来就一律抛异常）。权限标签与说明文案随应用支持的 9 种语言一并提供，因为它们会出现在系统授权对话框里。
>
> 代价已核实：任何已在读该 provider 的外部应用升级后会拿到 `SecurityException`。核查确认仓库内外均无已知集成方，README/ROADMAP 也从未把该 authority 作为对外接口宣传，故按收紧处理。该门禁由 Android 框架在 provider 类之外执行，单元测试覆盖不到——manifest 属性本身就是执行点。

---

## 顺手确认过、没问题的

- `FullTextSearch.escapeFtsQuery()` 对 FTS5 特殊字符处理正确，引号里 `""` 转义能挡住布尔操作符注入，`book_id` 走的是 `CAST(? AS INTEGER)`。
- `ZipImportRules` 拒绝绝对路径和 `..`，跳过 `__MACOSX` 和隐藏项，条目数和单条大小都有上限。
- `OpdsAddress` 把可达范围限制在 loopback/RFC1918/RFC4193/`.local`，而且每一跳重定向都重新校验；目录 2MB、下载 200MB、MIME 过滤都在。
- `BackupRepository` 导入 200MB 上限，跑在 `withTransaction` 里保证原子；备份 XML 实际上什么都不备份（这也是有意的）。
- `file_paths.xml` 只授权 `share_cards/`，跟 `ShareCardGenerator.SHARE_CARD_DIR` 对得上。
- 没有 WebView，没有 `Runtime.exec`，没有 `getSharedPreferences`；全仓仅剩的那个 `!!` 是 ContentProvider 里的 `context!!`，符合审计门槛。
- MOBI/AZW 遇到 DRM 和 HUFF/CDIC 直接拒绝，不尝试解密。

---

## 修的话，我会按这个顺序

先 #1 那个路径穿越，再 #2 的导入上限——这两个是真能被外部触发、影响也实在的。然后 #3 和 #4 一起收拾（LAN 主机校验、服务端超时和精确匹配）。#5 的凭证轮换掉、改成环境变量注入。

再强调一遍：这份是静态审查的结论，没跑构建也没做动态验证。#1 到 #4 修完之后，最好各补一个单元测试——文件名消毒、上限触发、LAN 主机拒绝、精确路径匹配，一样一个。

---

## v56.6.2 实施结果

7 条全部处置完毕，按上面这个顺序做的。

`#1`–`#4` 各配一个回归测试（`ImportFileNameTest` 15、`BookParserCapsTest` 10、`LanTransferClientTest` 8、`LanTransferServerTest` +5），并逐条做了变异验证：撤销 `soTimeout`、把精确匹配改回 `contains`、删掉主机校验，对应的 7 个测试全部失败。slow loris 那条特意把客户端读超时设成 20 秒，好让回归表现为「测试失败」而不是「套件挂住」。

`#6`、`#7` 无法用单元测试覆盖，所以分别落在别的证据上：前者对产物跑 `apksigner verify --print-certs`，后者的执行点在 Android 框架侧、manifest 属性即是执行本身。两处都在上文写明了验证方式，没有用测试数量充数。

`#5` 的位置报告写错了，我第一轮核查也写错了——详见该节的追加说明。这一条的实际教训不在代码里：**判断凭证有没有进仓库，要读 `git ls-files -v` 和全历史扫描，不能读工作区文件**；以及**论证「某个值不是密钥」不需要引用那个值**。

六道门禁均在本地实跑（CI 只在 `main` 上触发，dev 分支没有远端验证）：373 个测试 0 失败，测试广度 85.3%。
