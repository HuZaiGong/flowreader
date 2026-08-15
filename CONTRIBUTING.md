# 贡献指南

首先，感谢你对 FlowReader 心流阅读的关注。无论是报告 bug、提出功能建议、修复代码还是补充翻译，都欢迎。

参与本项目即表示你同意遵守 [行为准则](CODE_OF_CONDUCT.md)。

---

## 提 Issue 之前

1. **先搜索**已有的 Issue，避免重复；重复的报告会被直接关闭。
2. **安全漏洞请勿开设公开 Issue**，请通过私密渠道报告，见 [SECURITY.md](SECURITY.md)。
3. 请使用 Issue 模板：Bug 报告和功能建议各有专门模板，信息越完整，定位越快。
4. 本项目没有任何崩溃收集，如果方便，请随报告附上 logcat 输出。

## 功能建议

FlowReader 是一个有明确主张的项目：**离线优先、隐私优先**。与以下约束冲突的功能建议会被拒绝：

- 不做账号、云同步、遥测、崩溃收集——这不是待办事项，而是设计决定
- 全应用只有一个 `INTERNET` 权限，仅供局域网 OPDS 与局域网备份互传；不新增任何权限
- 全应用无 WebView；不做任何形式的 DRM 破解

建议在提功能前先读一遍 README 的「安全约束」一节。

## 开发环境

- **JDK 17**、**Android SDK 35**（compileSdk 35 / minSdk 26）、AGP 8.6.0
- Gradle wrapper（9.6.1）从腾讯云镜像拉取；wrapper 下载失败通常是镜像问题，不是项目问题

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader
./gradlew assembleDebug          # 开发包
./gradlew testDebugUnitTest      # 全部 JVM 单测（无需模拟器）
./gradlew verifyKotlinStyle      # ktlint + 全仓空白字符门禁
```

## CI 门禁

CI（`.github/workflows/ci.yml`）严格按顺序跑以下六项，发往 `main` 的 PR 必须全部通过：

```bash
./gradlew verifyKotlinStyle      # ktlint（:app 以外的模块）+ 全仓空白字符检查
./gradlew testDebugUnitTest      # 单元测试
./gradlew coverageSummary        # 测试广度文件比 ≥ 40%
./gradlew assembleDebug          # 编译
./gradlew verifyRoborazziDebug   # 截图回归
./gradlew performanceBaseline    # APK 体积对比
```

请在开 PR 前本地完整跑一遍。两个最常踩的点：

- **空白字符门禁**：全仓任意 `.kt` / `.kts` 文件中出现一个 tab 或行尾空格，整个构建就失败。`.editorconfig` 规定 4 空格缩进、LF 换行、行宽上限 140。
- **`coverageSummary` 是文件数量比，不是行覆盖率**：新增一个领域模型或 `:core` 源文件却不加测试，就算别处一行没动也会挂。

## 代码约定

- 代码标识符与注释使用**英文**；用户可见文案走 `strings.xml`，需同时添加全部 9 种语言（默认中文 + en/ja/ko/de/es/fr/pt/ru）。无法提供全部译文时，请至少给出 zh + en 并在 PR 中注明。
- 架构数据流：`Composable → ViewModel → domain 仓库接口 → data 实现 → Room DAO`。模块依赖方向只允许 `feature:* → core/domain`、`data → core/domain`、`app → 全部`；`:domain` 不得引入 app / Room / Compose / Hilt 任何依赖。
- 项目**有意不给 ViewModel 写单测**：业务规则应抽取为纯函数/纯类放入 `:core` 或 `:feature:*`，在那里测试。
- Room schema 变更（当前 version 7）必须**手写迁移**，项目没有 `fallbackToDestructiveMigration()`。
- 不新增 `!!`。
- 逐条行为陷阱（FTS、进度单位、窗口 inset 等）见 `AGENTS.md`，动手前值得一读。

## 安全红线

以下是项目的硬约束，违反它们的 PR 会被拒绝：

- 不新增任何权限；网络请求仅限局域网 OPDS 与局域网备份互传
- 不引入遥测、统计、崩溃收集、账号或云同步
- 不引入 WebView；保持 FTS 查询与 HTML/Markdown 导出的转义
- 不做 DRM 破解；保持导入路径的大小上限与 zip-slip 防护
- FileProvider 继续只授权 `share_cards/` 子目录；ContentProvider 保持只读

## 提交与 PR

- PR 目标分支为 `main`，一个 PR 只处理一件事。
- 仓库自身的提交风格是 `vX.Y.Z: 简述`，开发中的工作用 `wip(vX.Y.Z): 简述`。外部 PR 不必严格遵循——合并时维护者会按发布版本统一整理标题。
- `CHANGELOG.md` 由维护者随发布维护；你只需在 PR 里说清楚改了什么、为什么改。
- 项目由个人维护，响应可能较慢，请耐心等待；超过两周没有回复可以在原帖礼貌地顶一下。

---

# Contributing Guide (English)

First off, thank you for your interest in FlowReader. Bug reports, feature requests, code fixes and translations are all welcome.

By participating in this project you agree to abide by the [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Before opening an issue

1. **Search** existing issues first; duplicates are closed.
2. **Security vulnerabilities must not be public issues** — report them privately, see [SECURITY.md](SECURITY.md).
3. Use the issue templates: there are dedicated templates for bug reports and feature requests. The more complete the information, the faster the triage.
4. The app collects no crash data at all; if you can, attach logcat output with your report.

## Feature requests

FlowReader is an opinionated project: **offline-first, privacy-minded**. Requests conflicting with the following constraints will be declined:

- No accounts, no cloud sync, no telemetry, no crash collection — these are design decisions, not backlog items
- The app holds a single `INTERNET` permission, used only for LAN OPDS and LAN backup transfer; no new permissions will be added
- No WebView anywhere; no DRM circumvention of any kind

Please read the "安全约束 / Security constraints" section of the README before proposing a feature.

## Development environment

- **JDK 17** and **Android SDK 35** (compileSdk 35 / minSdk 26), AGP 8.6.0
- The Gradle wrapper (9.6.1) is fetched from a Tencent Cloud mirror; a failing wrapper download is usually a mirror problem, not a project problem

```bash
git clone https://github.com/HuZaiGong/flowreader.git
cd flowreader
./gradlew assembleDebug          # development APK
./gradlew testDebugUnitTest      # all JVM unit tests (no emulator needed)
./gradlew verifyKotlinStyle      # ktlint + repo-wide whitespace gate
```

## CI gates

CI (`.github/workflows/ci.yml`) runs these six steps in strict order; a PR to `main` must pass all of them:

```bash
./gradlew verifyKotlinStyle      # ktlint (all modules except :app) + whitespace check
./gradlew testDebugUnitTest      # unit tests
./gradlew coverageSummary        # file-count test breadth ≥ 40%
./gradlew assembleDebug          # build
./gradlew verifyRoborazziDebug   # screenshot regression
./gradlew performanceBaseline    # APK size comparison
```

Run the full set locally before opening the PR. The two most common traps:

- **Whitespace gate**: a single tab or trailing-space character in any `.kt`/`.kts` file fails the whole build. `.editorconfig` mandates 4-space indents, LF and a 140-char line cap.
- **`coverageSummary` is a file-count ratio, not line coverage**: adding a domain model or a `:core` source file without tests fails the build even if nothing else changed.

## Code conventions

- Code identifiers and comments are **English**. User-facing strings live in `strings.xml` and must be added to **all 9 locales** (Chinese default plus en/ja/ko/de/es/fr/pt/ru). If you cannot provide every translation, add zh + en and say so in the PR.
- Architecture data flow: `Composable → ViewModel → domain repository interface → data impl → Room DAO`. Allowed dependency direction: `feature:* → core/domain`, `data → core/domain`, `app → everything`; `:domain` must stay free of app/Room/Compose/Hilt.
- ViewModels are **deliberately not unit-tested**: extract business rules into pure classes in `:core` or `:feature:*` and test those instead.
- Room schema changes (currently version 7) require **hand-written migrations** — there is no `fallbackToDestructiveMigration()`.
- Do not introduce new `!!`.
- `AGENTS.md` documents the behavioral pitfalls one by one (FTS, progress units, window insets, …); it is worth reading before you start.

## Security hard constraints

These are the project's hard constraints; PRs violating them will be rejected:

- No new permissions; network traffic only for LAN OPDS and LAN backup transfer
- No analytics, crash reporting, accounts or cloud sync
- No WebView; keep the escaping in FTS queries and HTML/Markdown export
- No DRM circumvention; keep the import-path size caps and zip-slip protection
- FileProvider keeps authorizing only the `share_cards/` subdirectory; ContentProvider stays read-only

## Commits and PRs

- Open PRs against the `main` branch, one concern per PR.
- The repo's own commit style is `vX.Y.Z: summary`, with `wip(vX.Y.Z): summary` for work in progress. External PRs don't need to match it exactly — the maintainer rewrites titles to the release convention when merging.
- `CHANGELOG.md` is maintained by the maintainer per release; just explain what changed and why in the PR.
- The project has a single maintainer, so responses may be slow — thanks for your patience; a polite bump after two weeks of silence is fine.
