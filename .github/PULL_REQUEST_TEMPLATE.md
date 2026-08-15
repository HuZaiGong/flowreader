<!--
  中文与英文说明见 CONTRIBUTING.md。一个 PR 只处理一件事，目标分支为 main。
  See CONTRIBUTING.md for details. One concern per PR, targeting main.
-->

## 关联 Issue / Related issue

<!-- 例如 closes #12；没有则删除本节 / e.g. closes #12; remove if none -->

## 改了什么，为什么 / What changed and why

## 如何验证 / How to verify

<!-- 复现步骤或测试方式 / Steps to reproduce or test procedure -->

## 检查清单 / Checklist

- [ ] 已阅读 CONTRIBUTING.md / I have read CONTRIBUTING.md
- [ ] 本地依次通过六项 CI 门禁（verifyKotlinStyle → testDebugUnitTest → coverageSummary → assembleDebug → verifyRoborazziDebug → performanceBaseline）/ All six CI gates pass locally in order
- [ ] 未新增权限，未引入遥测 / 统计 / 崩溃收集 / 账号 / 云同步 / No new permissions; no telemetry, analytics, crash collection, accounts or cloud sync
- [ ] 未引入 WebView，未新增 `!!` / No WebView introduced; no new `!!`
- [ ] 模块依赖方向正确，`:domain` 未引入 app / Room / Compose / Hilt / Module dependency direction respected; `:domain` stays clean
- [ ] 新增的领域模型 / `:core` / `:feature:*` 源文件均带测试（coverageSummary 门禁）/ New domain, `:core` or `:feature:*` source files come with tests (coverageSummary gate)
- [ ] Room schema 变更附有手写迁移（如适用）/ Room schema changes include a hand-written migration (if applicable)
- [ ] 新增用户可见文案已加入全部 9 种语言，或在 PR 中说明了缺译情况 / New user-facing strings are added in all 9 locales, or missing translations are noted
- [ ] 无 tab、无行尾空格，4 空格缩进，LF / No tabs, no trailing whitespace, 4-space indents, LF
