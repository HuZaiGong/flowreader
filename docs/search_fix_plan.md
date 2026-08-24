# 搜索功能整体整改方案

状态：**分析完成，已于 v56.6.3 实施**。本文件为分析与方案记录，实施结果详见 `CHANGELOG.md` v56.6.3 条目。
分析基线：`077ea56`（`app/build.gradle.kts` 为 5652 / "56.5.2"）。
分析日期：2026-08-24。

> 本文档全部结论均以源码实测为准，不以既有文档表述为准。分析当时 `CLAUDE.md` 的版本簿记仅记至 v56.4.4，而仓库已为 v56.5.2。

---

## 0. 结论

用户报告「全项目各种地方各个用途的搜索功能没有一个能用的」。经核查，该判断基本成立，但成因并非四个彼此独立的缺陷，而是一项总病根附加若干独立缺陷。

**总病根：`book_content_fts` 采用 FTS5 默认 `unicode61` 分词器，该分词器将连续汉字整体视为一个词。**

已用真实 SQLite（sqlite-jdbc 3.41.2.2，见 §5）对当时 schema 实测。索引 `"这是一个关于心流阅读的故事，江南的春天格外温柔。"` 后，索引内实际存储的 token 为：

```
这是一个关于心流阅读的故事      ← 整句一个 token
江南的春天格外温柔              ← 整句一个 token
第一章 / 风起
```

当时代码发出的 `"心流"*` 一类查询，实测结果如下：

| 查询 | 结果 | 原因 |
|------|------|------|
| `心流` | **0 条** | 位于 token 中部，前缀匹配无法触及 |
| `阅读` | **0 条** | 同上 |
| `故事` | **0 条** | 同上 |
| `春天` | **0 条** | 同上 |
| `江南` | 1 条 | 恰为 token 起始位置，侥幸命中 |
| `第一章` | 1 条 | 恰为 token 起始位置 |
| `flow`（英文） | 1 条 | 英文按空白正常分词 |

据此可解释「全文搜索为何看似完全不可用」：中文用户检索任何不处于句首或标点之后首位的词，一律 0 条。v56.4.3 修正的 `CAST(? AS INTEGER)` 仅解决了「书内搜索连英文亦无结果」这一层，分词层面的问题于其后显现。书内搜索与全局搜索共用同一索引，故两个入口同时失效。

除此之外另有 **11 项独立缺陷**，其中 3 项属「功能并不存在而 UI 暗示其存在」，2 项属安全或正确性问题。完整清单见 §2。

---

## 1. 全项目搜索入口盘点

全仓 `grep -i search` 命中 26 个非测试文件，其中用户可见的搜索入口实为 4 个，另有 1 个被误认为存在：

| # | 入口 | 位置 | 底层 | 当时状况 |
|---|------|------|------|------|
| 1 | 全库搜索页（书籍段） | `SearchScreen` → `SearchViewModel.search()` | Room `BookDao.searchBooks` (LIKE) | **部分可用**，存在通配符注入（§2.4） |
| 2 | 全库搜索页（章节段） | 同上 → `SearchRepositoryImpl.searchChapters` | FTS5 `searchAll()` | **中文完全失效**（§2.1），且每次搜索重建全库索引（§2.5） |
| 3 | 阅读器内搜索 | `ReaderControls` 菜单 → `SearchDialog` → `ReaderViewModel.searchInBook()` | FTS5 `search()` | **中文完全失效**（§2.1） |
| 4 | 笔记页跨书搜索 | `NotesScreen` → `NotesViewModel.updateQuery()` | 内存 `String.contains` | **可用**，为唯一真正可用者，但与 DAO 内的 SQL 版本重复（§2.9） |
| — | 书架内搜索 | `LibraryScreen` | — | **不存在**：`SearchBar` 为死 import（§2.3） |

另经核查，`AnnotationDao.searchAnnotations` 与 `searchAllAnnotations` 两个 SQL 查询无任何生产调用方（§2.9）；OPDS 无搜索功能。

---

## 2. 缺陷清单

### 2.1 【P0，影响面最大】FTS5 默认分词器不支持中文，中文全文搜索恒返回空

- **位置**：`app/src/main/java/com/flowreader/app/util/FullTextSearch.kt:101-110`（建表）、`178-249`（两处查询）
- **现象**：书内搜索与全局章节搜索，中文查询几乎全部 0 条。
- **根本原因**：`CREATE VIRTUAL TABLE ... USING fts5(...)` 未指定 `tokenize`，默认为 `unicode61`。该分词器按 Unicode 空白与标点切分，不作 CJK 分词，一段连续汉字成为单个 token；而代码发出的查询为 `"<query>"*`（短语加前缀），前缀仅能匹配 token 起始位置。
- **实测**：见 §0 表格，已用真实 fts5 复现。
- **此前未被发现的原因**：v56.4.3 的 `FullTextSearchQueryTest` 仅断言 SQL 字符串形状（`contains("CAST(? AS INTEGER)")`），从不执行查询，故对分词器层面的缺陷完全免疫。

### 2.2 【P0】`book_id` 与 `chapter_index` 被作为可搜索文本索引，数字查询污染结果

- **位置**：`FullTextSearch.kt:102-109`
- **现象**：检索「5」将命中所有 `book_id = 5` 的章节；检索「0」命中所有第 0 章，即几乎全库每一本书。检索任何纯数字（年份「1949」、章节号）均混入大量无关结果。
- **根本原因**：`book_id` 与 `chapter_index` 两列未标注 `UNINDEXED`，其数值被分词进入全文索引。
- **实测**：当时 schema 下 `MATCH '"5"*'` 命中 `book_id=5` 的行，`'"0"*'` 命中全部行；标注 `UNINDEXED` 后均为 0 条。
- **附带收益**：标注 `UNINDEXED` 可显著缩减索引体积。

### 2.3 【P1】书架页并无搜索框，`SearchBar` 为死 import

- **位置**：`LibraryScreen.kt:69` `import androidx.compose.material3.SearchBar`
- **现象**：`SearchBar` 在整个文件内仅出现于 import 行，无任何使用。书架顶栏仅有一个跳转按钮（`LibraryScreen.kt:204`）指向独立搜索页。
- **判定**：此为 v52「书架不再内嵌全局搜索」重构的残留，不属缺陷，但 import 应予删除。ktlint 不覆盖 `:app` 模块，故无门禁报错。
- **同类**：`LibraryViewModel.kt:16` 的 `import ...SearchRepository` 亦为死 import，构造函数无该参数，全文件无使用。

### 2.4 【P1，安全】书籍搜索存在 LIKE 通配符注入，检索 `%` 返回全部藏书

- **位置**：`data/.../dao/BookDao.kt:38-39`，经 `BookRepositoryImpl.kt:43`
- **现象**：查询串未转义即拼入 `LIKE '%' || :query || '%'`。用户输入 `%` 或 `_` 时，`%` 匹配任意串、`_` 匹配任意单字符，返回整个书库，表现形似「搜索损坏」。
- **实测**：
  ```
  query='%'  -> 3/3 rows（全部）
  query='_'  -> 3/3 rows（全部）
  query='心流' -> 1 row（正常）
  ```
- **同类**：`BookDao.getBooksByTag:41`、`AnnotationDao:44,51-54` 属同一模式。
- **处理方向**：声明 `ESCAPE '\'`，并对 `%`、`_`、`\` 三个字符转义。SQLite 的 `ESCAPE` 仅接受单字符。

### 2.5 【P1，性能】每次输入均可能触发全库索引重建，且被防抖取消后留下残缺索引

- **位置**：`SearchRepositoryImpl.kt:51-59`、`SearchViewModel.kt:53-60`
- **现象**：搜索卡顿，结果时有时无。
- **根本原因链**：
  1. `searchChapters()` 每次调用均比对 `bookIds != indexedBookIds`，不一致即 `rebuildIndexLocked()`，读取全库每一章正文（`getChapterContent` 逐章经 cache 或 DB）。
  2. `indexedBookIds` 与 `hasBuiltIndex` 为纯内存字段，进程重启即丢失，故冷启动后首次搜索必然全量重建。
  3. `SearchViewModel.updateQuery()` 每次按键先 `searchJob?.cancel()`，350ms 后再执行。重建位于该可取消任务内，用户继续输入即被取消，此时 `deleteAllContent()` 已执行而索引仅灌入一部分；若下一次调用的 `bookIds` 恰等于上次赋值的 `indexedBookIds`，则不再重建，索引长期残缺。
  4. `hasBuiltIndex = true` 位于 `rebuildIndexLocked()` 末尾，取消时不执行，此点设计正确；但 `indexedBookIds` 同样位于末尾，故更常见的失败形态为反复重建而非残缺，两者均有出现。
- **附带**：`catch (e: Exception)`（`SearchViewModel.kt:82`、`ReaderViewModel.kt:529`）将吞掉 `CancellationException`（其继承链为 `IllegalStateException → RuntimeException → Exception`），把正常取消当作搜索失败并弹出错误提示。

### 2.6 【P1】FTS 库无版本号，既有安装用户无法获得修正后的 schema

- **位置**：`FullTextSearch.kt:89-134`
- **现象**：即使 §2.1 与 §2.2 修正完成，已安装用户升级后搜索依旧完全不可用。
- **根本原因**：`createTablesAndTriggers()` 全部使用 `CREATE TABLE/VIRTUAL TABLE/TRIGGER IF NOT EXISTS`，且 `flowreader_fts.db` 由 `openOrCreateDatabase()` 裸建，既无 `user_version` 亦无迁移逻辑。旧库已存在时全部 `IF NOT EXISTS` 均跳过，旧 schema 永久保留。
- **处理方向**：引入 `PRAGMA user_version`，版本落后时 `DROP` 重建并全量重新索引。索引属纯派生数据，可安全重建，无数据丢失风险。

### 2.7 【P2】漫画被纳入全局索引，写入 `[COMIC:/path/...]` 无效内容并泄漏内部路径

- **位置**：`SearchRepositoryImpl.kt:32-46`（无格式过滤），对比 `ReaderViewModel.kt:277`（`if (book.format != BookFormat.COMIC)`）
- **现象**：搜索结果出现无意义条目。`BookParser.kt:761` 生成的章节正文为 `content = "[COMIC:$imagePath]"`，即设备绝对路径，该路径会被索引并可能显示于摘要。
- **判定**：两条索引路径对漫画处理不一致，阅读器跳过而全局重建不跳过。此与项目「不泄漏文件路径」的安全约束相冲突（ContentProvider 明确不暴露路径）。

### 2.8 【P2】`escapeFtsQuery` 于特殊字符两侧补空格，静默改变查询语义

- **位置**：`FullTextSearch.kt:161-176`
- **现象**：检索 `well-known` 实际变为短语 `"well - known"`，检索 `C++` 变为 `"C + + "`。
- **实测**：英文语料下上述查询仍可命中，FTS5 短语内的标点被分词器忽略，故危害小于预期；但 `"quoted"` 经转义为 `"""quoted"""*` 后为 0 条。
- **判定**：未达「完全不可用」程度，但新方案中该函数职责将发生变化（§3.2），需一并重写。

### 2.9 【P2】两个 SQL 搜索查询无调用方，与内存过滤实现重复

- **位置**：`AnnotationDao.kt:44-45`（`searchAnnotations`）、`51-55`（`searchAllAnnotations`），经 `AnnotationRepositoryImpl.kt:63,78`
- **现象**：笔记页实际使用 `NotesViewModel.kt:70-76` 的内存 `contains(ignoreCase = true)`，DAO 内两个 SQL 版本无任何生产调用方。
- **后果**：两套语义并存且不一致。SQL 版 `LIKE` 对 ASCII 大小写不敏感、对非 ASCII 敏感；内存版 `ignoreCase = true` 对中文与全角亦生效。且 SQL 版带有 §2.4 的注入问题。
- **判定**：内存过滤对数百条批注的规模属合理选择。建议删除无用 DAO 方法；若保留则须统一走 SQL 并转义。此项列入 §6 待定。

### 2.10 【P3】搜索历史以 `|` 拼接，含 `|` 的查询词被拆碎

- **位置**：`data/repository/SettingsRepository.kt:254-276`
- **现象**：检索 `a|b` 后，历史记录变为 `a` 与 `b` 两条；空串处理亦不完善。
- **处理方向**：改用 JSON 数组，或至少对 `|` 转义。优先级较低。

### 2.11 【P3】搜索结果摘要以 `<<` `>>` 作标记，而 UI 按纯文本直接显示

- **位置**：`FullTextSearch.kt:36,45`（`snippet(..., '<<', '>>', ...)`），经 `SearchScreen.kt:287` 与 `Dialogs.kt`
- **现象**：结果直接显示字面的 `<<心流>>`，而非高亮样式。
- **判定**：功能可用，观感不佳。新方案（§3.3）改为返回原文加命中区间，由 UI 以 `AnnotatedString` 实施真实高亮。

---

## 3. 修复方案

### 3.1 方案选型：未采用 `tokenize='trigram'` 的理由

FTS5 自带 `trigram` 分词器看似为标准解法，但实测不可用：

```
trigram 索引 "这是一个关于心流阅读的故事"
  查询 "心流"（2 字）  -> 0 条   ← 致命
  查询 "心流阅读"（4 字）-> 1 条
  查询 "心"（1 字）    -> 0 条
```

`trigram` 要求查询不少于 3 个字符，而中文最常用的检索词恰为二字词（心流、阅读、春天）。另 `trigram` 需 SQLite 3.34 以上（Android 12 / API 31 起方稳定满足），本项目 `minSdk 26` 无法覆盖。故予否决。

**采用方案：应用层 bigram（二元组）切分。** 与 Android 系统 SQLite 版本无关，`minSdk 26` 全覆盖。

### 3.2 核心设计：折叠流加通用 bigram

实测所得最优形态为先折叠（fold）再切 bigram，中英文统一处理：

- **fold**：丢弃空白与标点，统一小写，仅保留 `Character.isLetterOrDigit` 的码点。
- **bigram**：于折叠后的码点流上切分重叠二元组。

```
原文   这是一个关于心流阅读的故事，江南的春天格外温柔。
折叠   这是一个关于心流阅读的故事江南的春天格外温柔
索引流 这是 是一 一个 个关 关于 于心 心流 流阅 阅读 读的 的故 故事 事江 江南 南的 的春 春天 天格 格外 外温 温柔
```

查询经同样的折叠与 bigram 切分，作为短语（`"..."`）查询。

**实测结果（真实 fts5，18 个查询，含边界、混排、单字）：假阴性 0，假阳性 0。**

统一折叠带来的主要收益：

| 查询 | 旧方案 | 新方案 | 说明 |
|------|--------|--------|------|
| `心流` | 0 | 命中 | 中文句中词 |
| `事，江` | 0 | 命中 | **跨标点**，折叠后标点消失方可命中 |
| `流Flow` | 0 | 命中 | **中英交界** |
| `海1949` | 0 | 命中 | **中数交界** |
| `eading` | 0 | 命中 | 英文**词中**匹配，bigram 附带能力，旧方案不具备 |
| `上海海上` | — | 命中 | 重叠字正确命中 |
| `海上上海` | — | 0 条 | 正确地**不**误命中，短语位置约束生效 |

bigram 的经典风险为假阳性，实测未出现：FTS5 短语查询强制 token 连续且有序，`海上 上上 上海` 在 `上海海上` 中不构成连续序列。此点已验证，故无需额外 `instr()` 二次校验。

**必须写入注释的两项已知限制**（实测确认，非推测）：

1. 单字 CJK 查询须用前缀形式 `"心"*`（匹配以该字起始的 bigram）。若该字在整章折叠流中仅作为末位字符出现，则任何形式均无法触及（如 `柔` 在 `一个字：柔` 中）。此属极端边角情形，可以接受；但不得令单字查询默认走精确短语，否则恒为 0 条。
2. 单字符文本（整章仅一字）索引为该单字本身，依靠上一条的前缀查询覆盖。

**索引体积代价**：中文约 3.0 倍，英文约 2.4 倍（实测）。此为本方案唯一实质成本。缓解手段为 `book_id`、`chapter_index` 及原文列全部标注 `UNINDEXED`（§2.2），仅索引 bigram 流。

### 3.3 新 schema

```sql
CREATE TABLE book_content(
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  book_id INTEGER NOT NULL,
  chapter_index INTEGER NOT NULL,
  chapter_title TEXT,
  content TEXT,              -- 原文，仅用于取摘要
  title_indexed TEXT,        -- 折叠 bigram 流
  content_indexed TEXT,      -- 折叠 bigram 流
  UNIQUE(book_id, chapter_index)
);

CREATE VIRTUAL TABLE book_content_fts USING fts5(
  book_id UNINDEXED,
  chapter_index UNINDEXED,
  chapter_title UNINDEXED,
  content UNINDEXED,
  title_indexed,             -- 仅索引这两列
  content_indexed,
  content='book_content',
  content_rowid='id'
);
```

三个触发器（ai / ad / au）需同步补充新列。`book_id` 仍须 `CAST(? AS INTEGER)`：实测确认标注 `UNINDEXED` 不改变「FTS5 列无亲和性」这一事实，v56.4.3 的结论依然成立，不可回退。

**摘要生成**：不再使用 `snippet()`，其返回值为切碎的 bigram 流，不具可读性。改为在 SQL 侧以 `instr()` 与 `substr()` 自原文列切片：

```sql
substr(content, MAX(1, instr(content, ?) - 15), length(?) + 30)
```

实测可行，且仅摘要跨越 cursor，不会将 16MB 整章读入内存。此点较为关键，`BookParser` 允许单章 16MB。

**须注意**：`instr()` 作用于原文，而匹配发生于折叠流，两者偏移量不同（实测 `第一章：这是...` 中 `心流` 折叠后偏移 9、原文偏移 10）。因此 `instr(content, 原始查询)` 在跨标点命中时将返回 0。处理方式为返回 0 时退化取章节开头一段作摘要，不影响命中判定本身，命中已由 FTS 决定。若需精确高亮，须建立折叠偏移至原文偏移的映射，项目内 `ReaderTextMapping` 已有同类模式可循；建议本轮不做，列入 §6 待定。

### 3.4 索引重建的正确时机

将重建自搜索路径彻底移出：

- 删除 `searchChapters()` 内 `bookIds != indexedBookIds` 的惰性重建。
- 改为事件驱动的增量维护：导入书籍后索引该书；删除书籍时 `deleteBookContent(bookId)`。当前 `BookRepositoryImpl` 的三个删除路径均未清理 FTS 索引，此为 §2.7 之外另一处遗留问题，需一并补齐。
- 保留一个显式的全量重建入口（设置页「重建搜索索引」），并在 §2.6 的 schema 升级时自动触发一次。
- 重建须运行于不可被 UI 防抖取消的作用域（如注入的 `@ApplicationScope CoroutineScope`），而非 `viewModelScope` 内每次按键即取消的任务。
- 以持久化存储记录索引进度，替代内存字段，避免冷启动全量重建。

### 3.5 其余修复

- **LIKE 转义**（§2.4）：新增 `SqlLike.escape(query)`，对 `\`、`%`、`_` 转义，所有 `LIKE` 查询加 `ESCAPE '\'`。覆盖 `BookDao:38,41` 与 `AnnotationDao:44,51`。
- **不吞 `CancellationException`**（§2.5）：所有 `catch (e: Exception)` 前先行判断并重抛。涉及 `SearchViewModel.kt:82,102`、`ReaderViewModel.kt:529`。
- **漫画跳过**（§2.7）：`rebuildIndexLocked()` 增加 `book.format != BookFormat.COMIC` 过滤，与 `ReaderViewModel.kt:277` 对齐。
- **删除死 import**（§2.3）：`LibraryScreen.kt:69`、`LibraryViewModel.kt:16`。
- **搜索历史改用转义编解码**（§2.10）。
- **UI 真实高亮**（§2.11）：`FtsSearchResult` 增加命中区间，`SearchScreen` 与 `SearchDialog` 以 `AnnotatedString` 渲染，去除 `<<` `>>`。
- **阅读器搜索自行初始化**：`ReaderViewModel.searchInBook():526` 直接调用 `fullTextSearch.search()`，依赖 `indexBookForSearch()` 已先行完成。后者为 fire-and-forget 的 `launch`，用户在索引完成前打开搜索将触发 `FtsNotInitializedException`。应由 `search()` 内部自行确保初始化。

---

## 4. 实施顺序

分四批，每批独立可验证、可回滚。

**批次 1（基础设施，无行为变化）**
1. 新建 `core/util/CjkTokenizer.kt`（`fold()` / `bigrams()` / `buildMatchQuery()`），置于 `:core` 且不依赖 Compose，以便纯 JVM 测试。
2. 建立 sqlite-jdbc 测试源集（§5）。
3. 为 `CjkTokenizer` 补齐单元测试，含 §3.2 两条已知限制。

**批次 2（索引层，核心修复）**
4. `FullTextSearch`：新 schema、`UNINDEXED`、`PRAGMA user_version` 迁移（§2.1 / 2.2 / 2.6）。
5. `indexChapter()` 写入 bigram 流；查询改折叠短语加单字前缀分支。
6. 摘要改 `instr` 与 `substr`（§3.3）。
7. 端到端测试：中文二字词、跨标点、中英混排、单字、数字污染、书内范围过滤。

**批次 3（调用方与生命周期）**
8. 索引重建移出搜索路径，改事件驱动加 `@ApplicationScope`（§3.4）。
9. 删除书籍时清理 FTS 索引。
10. 漫画过滤；`searchInBook()` 自行初始化；不吞 `CancellationException`。

**批次 4（周边与观感）**
11. LIKE 转义（§2.4）。
12. 死 import 与无用 DAO 方法（§2.3 / 2.9，后者待定）。
13. 搜索历史转义存储（§2.10）、UI 真实高亮（§2.11）。

---

## 5. 测试策略

分析当时 `AGENTS.md:68` 与 `CHANGELOG.md:95` 均载明「Robolectric 自带 SQLite 未编译 fts5 模块，离线环境亦无 sqlite-jdbc 可替代，因此无法真正执行生产查询」。

**该结论已经过期。** 实测：

```
org.xerial:sqlite-jdbc:3.41.2.2 已存在于本机 Gradle 缓存
  ~/.gradle/caches/modules-2/files-2.1/org.xerial/sqlite-jdbc/3.41.2.2/
探针结果：sqlite_version=3.41.2，CREATE VIRTUAL TABLE ... USING fts5 成功，MATCH 返回行
```

「Robolectric 无 fts5」这半句仍然成立，但以 sqlite-jdbc 作纯 JVM 测试完全可行。本文档 §0 至 §3 的每一项结论均据此实测得出。

此事直接决定修复能否成立：仅断言 SQL 字符串形状的测试对分词器层面的缺陷完全免疫，此即 §2.1 得以在 v56.4.3「功能性问题修复」之后依然存续的原因。因此：

- 新增 `FullTextSearchEngineTest`（纯 JVM，sqlite-jdbc），建立真实 fts5 表，断言查询结果而非 SQL 文本。必测项：中文二字词、跨标点、中英与中数交界、单字前缀、数字污染为 0、`book_id` 范围过滤、重叠字不误命中。
- 保留 `FullTextSearchQueryTest` 的形状断言，作为 `CAST` 回归网。
- 相应更正 `AGENTS.md:68` 与本节结论，避免后续再被该表述误导。

**覆盖率门禁**：新增 `CjkTokenizer`（`:core` 主源码）将拉低 `coverageSummary` 的文件比值（分析时为 71.0%，阈值 40%），须同批补充测试文件，否则 CI 直接失败。

**验证命令**（按 CI 顺序）：

```bash
./gradlew verifyKotlinStyle testDebugUnitTest coverageSummary assembleDebug
```

`verifyKotlinStyle` 的空白门禁对全仓 `.kt` 与 `.kts` 生效，任何 tab 或行尾空格即失败；ktlint 不覆盖 `:app`，故 `CjkTokenizer` 置于 `:core` 将受 ktlint 全量检查，须注意 140 列上限。

---

## 6. 待定事项及最终处理

以下三项在方案阶段列为待定，实施时按下列结论处理，详见 `CHANGELOG.md` v56.6.3 条目。

1. **索引体积 3 倍是否接受**（§3.2）。此为 bigram 方案的固有成本，亦是中文搜索可用的唯一代价。替代方案仅有「牺牲二字词检索改用 trigram」或「放弃 FTS 改纯 `LIKE` 扫描」，两者体验均更差。**结论：接受。**
2. **`AnnotationDao` 两个无调用方的 SQL 查询是删除还是接入**（§2.9）。**结论：删除。** 笔记页的内存过滤在数百条规模下更为简单，且大小写语义更正确。
3. **本轮是否实施精确高亮**（§3.3 末）。**结论：实施。** 索引返回命中偏移与长度（`matchStart` / `matchLength`）；命中仅存在于折叠流中时（跨标点等情形）偏移为 -1，此时按原文渲染，不作偏移错位的高亮。

---

## 7. 附：本文档结论的验证方式

| 结论 | 验证手段 |
|------|----------|
| `unicode61` 整段汉字成一 token | sqlite-jdbc 加 `fts5vocab` 直接 dump token 表 |
| 中文查询 0 条、`江南` 侥幸命中 | 真实 fts5 执行当时生产 SQL |
| `trigram` 需 3 字符以上 | 真实 fts5，二字与单字查询均 0 条 |
| bigram 方案 0 假阴性 0 假阳性 | 真实 fts5，18 个查询含边界、混排、重叠字 |
| 数字污染及 `UNINDEXED` 修正有效 | 真实 fts5，前后对比 |
| `CAST` 在 `UNINDEXED` 下仍属必需 | 真实 fts5 对比 `book_id = ?` 与 `CAST(? AS INTEGER)` |
| LIKE 通配符注入 | SQLite 执行 `%` 与 `_` 查询，返回全表 |
| `substr` 摘要不读整章 | SQLite 执行，确认服务端切片 |
| 折叠偏移不等于原文偏移 | 实测 `第一章：这是...`，偏移 9 对 10 |
| `CancellationException` 被 `catch (Exception)` 捕获 | JVM 类型层级：`CancellationException → IllegalStateException → RuntimeException → Exception` |
| FTS 库无版本号 | 源码扫描：无 `user_version`、无迁移、全部 `IF NOT EXISTS` |
| sqlite-jdbc 具备 fts5 | JDBC 探针，`sqlite_version=3.41.2` |
| 死 import 与无调用方 | 全仓 grep 调用点 |
