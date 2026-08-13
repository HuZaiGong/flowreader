# 安全政策 / Security Policy

## 支持的版本 / Supported versions

| 版本 / Version | 支持情况 / Supported |
|------|------|
| 最新发布版 / Latest release | ✅ |
| 更早版本 / Older versions | ❌ |

安全修复只随最新版本发布。FlowReader 没有自动更新机制，发现问题后请先升级到最新版本，确认问题仍然存在再报告。

Security fixes ship only with the latest release. FlowReader has no auto-update mechanism, so please upgrade to the latest release first and confirm the issue still exists before reporting.

## 报告漏洞 / Reporting a vulnerability

**请勿为安全漏洞开设公开 Issue。**

**Please do NOT open a public issue for security vulnerabilities.**

本仓库启用了 GitHub **私密漏洞报告（Private Vulnerability Reporting）**。请通过以下路径报告：

> 仓库页面 → **Security** 选项卡 → **Report a vulnerability**
>
> Repository page → **Security** tab → **Report a vulnerability**

报告中请尽量包含 / Please include as much of the following as possible:

- FlowReader 版本号（设置页底部）以及 Android 版本、设备型号 / FlowReader version (bottom of the Settings screen), Android version and device model
- 复现步骤或概念验证（PoC）/ Steps to reproduce or a proof of concept
- 影响范围与潜在危害 / Impact and potential harm
- 如有可能，附最小可复现的示例文件；请勿附带包含个人阅读数据的完整备份 / If possible, attach a minimal sample file; do not attach full backups containing personal reading data

我们力争在收到报告后 7 天内响应。项目由个人维护，如有延迟请见谅；修复方案确定后，希望与报告者协商披露时间。本项目不设漏洞赏金，报告者会在更新日志中获得致谢（要求匿名者除外）。

We aim to respond within 7 days. The project is maintained by a single person, so please bear with any delay; once a fix plan is set, we would like to coordinate disclosure timing with the reporter. There is no bug bounty program; reporters are credited in the changelog unless they request anonymity.

## 重点关注的攻击面 / Areas of interest

- **文件导入解析**：EPUB / TXT / PDF / Markdown / FB2 / MOBI / ZIP / CBZ —— zip-slip、路径穿越、大小上限绕过、解析器崩溃
  **File import parsing**: EPUB / TXT / PDF / Markdown / FB2 / MOBI / ZIP / CBZ — zip-slip, path traversal, size-cap bypasses, parser crashes
- **网络面**：局域网 OPDS（SSRF 防护与重定向校验）、局域网备份互传（令牌与网卡绑定）
  **Network surface**: LAN OPDS (SSRF protection and redirect validation), LAN backup transfer (token and NIC binding)
- **IPC 面**：ContentProvider（只读约束）、FileProvider（授权范围）、`ACTION_VIEW` 导入管线
  **IPC surface**: ContentProvider (read-only contract), FileProvider (grant scope), the `ACTION_VIEW` import pipeline
- **备份导入导出**：原子性、200MB 上限、不含可执行内容
  **Backup import/export**: atomicity, the 200MB cap, no executable content
- **权限模型**：全应用仅一个 `INTERNET` 权限
  **Permission model**: the app holds a single `INTERNET` permission

## 已知问题 / Known issues

- OPDS 密码目前以明文存于 DataStore；加密（EncryptedSharedPreferences / Jetpack Security）已列入计划但尚未实现。此项为已知设计债务，无需重复报告。
  OPDS passwords are currently stored in plaintext in DataStore; encryption (EncryptedSharedPreferences / Jetpack Security) is planned but not implemented. This is a known design debt — no need to re-report it.
