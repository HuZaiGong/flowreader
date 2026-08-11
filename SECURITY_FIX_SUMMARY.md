# 安全漏洞修复总结 - FlowReader v56.4.1

**修复日期**: 2026-08-07  
**修复版本**: v56.4.1 (待发布)  
**基线版本**: v56.3.0 / v56.4.0

---

## 修复的漏洞

### 🟡 [已修复] CVE-LOCAL-001: LAN 传输令牌生成算法缺陷（潜伏，非活跃）

**严重程度**: Low（潜伏缺陷）→ **已解决**

> 定级说明：初评为 High/CVSS 7.5，复核后下调。`TOKEN_CHARS` 是 16 个 hex 字符，
> 调用点又恰好是 `generateToken(16)`，两个常量相等使错误的索引上限碰巧算出正确结果，
> **现有令牌的 16^16 熵并未受损，无可利用路径**。这是等待被触发的正确性缺陷，
> 而不是已上线的漏洞。

#### 问题
令牌生成使用了错误的随机数范围 `random.nextInt(length)`（目标长度）而不是
`random.nextInt(charset.length)`（字符集长度）。当前二者相等所以结果正确，但熵的正确性
依赖于这个巧合：
- 令牌长度调高到 > 16 → `StringIndexOutOfBoundsException` 崩溃
- 令牌长度调低到 < 16 → 静默退化为只用字符集前 N 个字符，无任何报错

#### 修复
**文件**: `app/src/main/java/com/flowreader/app/util/LanTransferServer.kt:141-146`

```diff
  private fun String.generateToken(length: Int): String {
      val random = SecureRandom()
+     val charset = this
      return buildString {
-         repeat(length) { append(this@generateToken[random.nextInt(length)]) }
+         repeat(length) { append(charset[random.nextInt(charset.length)]) }
      }
  }
```

#### 验证
新增安全回归测试（`LanTransferServerTest.kt`）：
- ✅ `tokenGenerationUsesFullCharsetEntropy()` - 验证使用全部 16 个十六进制字符
- ✅ `tokenUniquenessAcrossMultipleInstances()` - 验证 50 个令牌全部唯一
- ✅ 所有测试通过

---

### 🟠 [已修复] MED-001: ContentProvider 使用 runBlocking 阻塞调用线程

**严重程度**: Medium → **已解决**

#### 问题
`FlowReaderContentProvider.query()` 使用 `runBlocking` 调用 Flow 数据源，阻塞 Binder 线程可能导致：
- 第三方应用调用超时
- ANR 风险
- 性能下降

#### 修复
**文件**: 
- `data/src/main/java/com/flowreader/app/data/local/dao/BookDao.kt` (新增同步方法)
- `app/src/main/java/com/flowreader/app/provider/FlowReaderContentProvider.kt` (移除 runBlocking)

```diff
+ // BookDao.kt - 新增同步查询方法
+ @Query("SELECT * FROM books ORDER BY lastReadTime DESC, addedTime DESC")
+ fun getAllBooksSync(): List<BookEntity>
+
+ @Query("SELECT * FROM books WHERE id = :id")
+ fun getBookByIdSync(id: Long): BookEntity?

  // FlowReaderContentProvider.kt
  override fun query(...): Cursor? {
      return when (MATCHER.match(uri)) {
          MATCH_BOOKS -> {
-             val books = runBlocking { bookDao.getAllBooks().first() }
+             val books = bookDao.getAllBooksSync()
              matrixCursor(COLUMNS, books.map { ... })
          }
          MATCH_BOOK_BY_ID -> {
              val bookId = uri.lastPathSegment?.toLongOrNull() ?: return null
-             val book = runBlocking { bookDao.getBookById(bookId) } ?: return null
+             val book = bookDao.getBookByIdSync(bookId) ?: return null
              matrixCursor(...)
          }
      }
  }
```

#### 优势
- 使用 Room 原生同步查询，无协程开销
- 不再阻塞 Binder 线程
- ContentProvider API 本质是同步的，匹配其设计

#### 验证
- ✅ 编译通过
- ✅ 所有单元测试通过
- ✅ 移除了 `kotlinx.coroutines.runBlocking` 和 `kotlinx.coroutines.flow.first` 导入

---

### 🟠 [已修复] MED-002: FileProvider 路径配置过于宽松

**严重程度**: Medium → **已解决**

#### 问题
FileProvider 暴露整个目录树 (`path="."`)，违反最小权限原则：

```xml
<!-- 修复前 -->
<external-path name="external" path="." />  <!-- ❌ 整个外部存储 -->
<files-path name="files" path="." />        <!-- ❌ 整个 files 目录 -->
<cache-path name="cache" path="." />        <!-- ❌ 整个 cache 目录 -->
```

#### 修复
**文件**: `app/src/main/res/xml/file_paths.xml`、`app/src/main/java/com/flowreader/app/util/ShareCardGenerator.kt`

全仓审计 `getUriForFile()` 调用点后确认：整个应用只有 **一个** 消费方——`ReaderScreen` 的「分享阅读卡片」。因此只需授权一个子目录：

```xml
<!-- 修复后：只授权唯一真实存在的分享路径 -->
<paths>
    <!-- Reader share cards; must match ShareCardGenerator.SHARE_CARD_DIR -->
    <cache-path name="share_cards" path="share_cards/" />
</paths>
```

`ShareCardGenerator` 此前把 PNG 写在 cache **根目录**，因此收窄路径后必须同步改写入位置，否则分享时 `getUriForFile()` 会抛 `IllegalArgumentException`：

```diff
- val file = File(cacheDir, "share_card_${'$'}{System.currentTimeMillis()}.png")
+ val shareDir = File(cacheDir, SHARE_CARD_DIR).apply { mkdirs() }
+ val file = File(shareDir, "share_card_${'$'}{System.currentTimeMillis()}.png")
```

其余三棵目录树没有任何 FileProvider 消费方，直接删除而非收窄：
- `covers/`（filesDir）— 由 Coil 在进程内按文件路径读取，从不经 FileProvider 分享。
- `opds/`（cacheDir）— `OpdsClient.download` 的落地目录，随后在进程内导入。
- LAN 备份 JSON — 位于 cache 根目录，由 `LanTransferServer` 自己的令牌校验 socket 提供，不走 FileProvider。
- 标注导出 — `NotesViewModel.exportVisible` 只把文本放进 UI state，根本不落盘。

#### 优势
- 最小权限：授权面从三棵完整目录树缩到一个子目录
- 消除死配置：不再声明没有消费方的路径，避免后续代码误以为「这里可以分享」
- 审计友好：配置与唯一调用点一一对应，且两侧互相注释指向

#### 验证
- ✅ 配置文件语法正确
- ✅ 编译通过

---

### 🟡 [已修复] LOW-001: 备份规则缺乏安全说明

**严重程度**: Low → **已解决**

#### 问题
备份规则文件缺少注释，未来开发者可能不清楚安全边界。

#### 修复
**文件**: 
- `app/src/main/res/xml/backup_rules.xml`
- `app/src/main/res/xml/data_extraction_rules.xml`

```xml
<!--
  Android Auto Backup rules (API 23+).

  FlowReader backs up user preferences (SharedPreferences/DataStore) but NOT the database:
  - Included: reading settings, theme, language (settings.preferences_pb)
  - Excluded: device.xml (device-specific data), the Room database (books stay local)

  Security note: Currently safe as no credentials are stored. If future versions add
  account/sync features, use EncryptedSharedPreferences and exclude from backup.
-->
<full-backup-content>
    <include domain="sharedpref" path="."/>
    <exclude domain="sharedpref" path="device.xml"/>
</full-backup-content>
```

#### 优势
- 明确当前安全假设（无凭据存储）
- 为未来开发者提供安全指导
- 符合安全审计最佳实践

---

## 验证结果

### CI 流水线检查

| 检查项 | 结果 | 输出 |
|--------|------|------|
| `verifyKotlinStyle` | ✅ 通过 | 无 ktlint 违规，无 tab/尾随空格 |
| `testDebugUnitTest` | ✅ 通过 | 134 tasks, 新增安全测试全部通过 |
| `coverageSummary` | ✅ 通过 | 71.0% (44/62 files) - 保持不变 |
| `assembleDebug` | ✅ 通过 | APK 成功构建 |

### 新增测试

新增 2 个安全回归测试方法：
- `tokenGenerationUsesFullCharsetEntropy()` - 验证令牌熵
- `tokenUniquenessAcrossMultipleInstances()` - 验证唯一性

测试覆盖原有功能测试：
- `servesPayloadAtTokenPath()` - 已存在，继续通过 ✅
- `wrongPathGets404()` - 已存在，继续通过 ✅
- `everyInstanceUsesAFreshToken()` - 已存在，继续通过 ✅

---

## 未修复的低优先级问题

### LOW-002: FullTextSearch 数据库未在应用退出时关闭

**决策**: 接受现状，不修复

**理由**:
1. SQLite 有 WAL 和 journal 保护，异常关闭不会损坏数据库
2. Android 进程退出时操作系统自动回收文件描述符
3. v56.4 运行至今未发现数据库损坏问题
4. 修复收益小（优雅性提升），风险不存在

**缓解措施**: 在 `FullTextSearch.kt` 顶部添加注释说明设计决策（已完成）。

---

## 修改文件清单

| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `app/src/main/java/com/flowreader/app/util/LanTransferServer.kt` | 🔧 修复 | 令牌生成算法 |
| `app/src/test/java/com/flowreader/app/util/LanTransferServerTest.kt` | ➕ 新增 | 安全回归测试 |
| `data/src/main/java/com/flowreader/app/data/local/dao/BookDao.kt` | ➕ 新增 | 同步查询方法 |
| `app/src/main/java/com/flowreader/app/provider/FlowReaderContentProvider.kt` | 🔧 修复 | 移除 runBlocking |
| `app/src/main/res/xml/file_paths.xml` | 🔧 修复 | 收窄至唯一分享子目录 |
| `app/src/main/java/com/flowreader/app/util/ShareCardGenerator.kt` | 🔧 修复 | 改写入 `share_cards/`，与收窄后的路径对齐 |
| `app/src/main/res/xml/backup_rules.xml` | 📝 文档 | 添加安全注释 |
| `app/src/main/res/xml/data_extraction_rules.xml` | 📝 文档 | 添加安全注释 |
| `SECURITY_AUDIT_REPORT.md` | ➕ 新增 | 完整审计报告 |
| `SECURITY_FIX_SUMMARY.md` | ➕ 新增 | 本文档 |

**总计**: 8 个文件修复，2 个文件新增，10 个文件变更

---

## 版本更新建议

### 推荐版本号: v56.4.1

**理由**:
- 包含 2 个中危问题修复（ContentProvider Binder 阻塞、FileProvider 授权面）
- 包含 1 个潜伏正确性缺陷修复（令牌生成索引上限）与 1 个低危文档补充
- 无破坏性变更；`ShareCardGenerator` 的写入目录变更对用户不可见（分享卡片是一次性缓存产物）
- 符合语义化版本 PATCH 级别（bug 修复）

### CHANGELOG.md 条目建议

```markdown
## [v56.4.1] - 2026-08-07
> 安全修复版本（基于 v56.4.0 审计发现）

### 修复
- **令牌生成的潜伏缺陷**：LanTransferServer 用目标长度而非字符集长度作索引上限；当前两者恰好都是 16，熵未受损，但改长度即会越界或静默降熵。已修正并添加回归测试
- **ContentProvider 性能优化**：移除 runBlocking，改用 Room 同步查询避免阻塞 Binder 线程
- **FileProvider 权限收紧**：三棵完整目录树收至唯一真实分享路径 `share_cards/`，`ShareCardGenerator` 同步改写入该子目录
- **备份规则文档化**：为 Android 备份配置添加安全注释，明确当前范围和未来注意事项

### 测试
- 新增令牌熵和唯一性安全回归测试
- 测试覆盖率保持 71.0%
```

---

## 后续建议

### 短期（v57）
1. ✅ 已完成全部中危修复及潜伏缺陷加固
2. 考虑添加 Dependabot 或 Renovate 进行依赖监控
3. 在 CI 中添加静态安全扫描工具（如 Android Lint security 检查）

### 中期（v58-v60）
1. 定期安全审计（每季度或每 5 个大版本）
2. 如果添加账号/云同步功能，立即：
   - 启用 SQLCipher 数据库加密
   - 使用 EncryptedSharedPreferences 存储凭据
   - 排除敏感数据在备份外

### 长期
1. 考虑通过 HackerOne/Bugcrowd 启动负责任披露计划
2. 获取外部安全公司的专业渗透测试
3. 在 README.md 添加 SECURITY.md 链接（GitHub 安全政策）

---

## 联系信息

**修复执行**: Claude Code Agent  
**项目维护者**: HuZaiGong  
**审计报告**: 见 `SECURITY_AUDIT_REPORT.md`

---

**文档版本**: 1.0  
**最后更新**: 2026-08-07
