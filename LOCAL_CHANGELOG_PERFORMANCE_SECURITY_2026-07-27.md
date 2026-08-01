# 本地改动日志：性能、架构与漏洞修复

日期：2026-07-27
提交状态：未提交，仅保留在本地工作区

## 修复概览

- 优化阅读进度 Widget 更新路径，移除 `runBlocking`，避免 AppWidget 广播回调阻塞主线程。
- Widget 异步更新使用 `goAsync()` 保持广播生命周期，确保 DataStore 读取和 RemoteViews 更新完成后再结束接收器。
- 漫画纵向拼接阅读从 `Column.verticalScroll` 改为 `LazyColumn`，避免图片包页数较多时一次组合整本漫画造成内存和首帧压力。
- OPDS 目录读取增加 2MB 上限，避免局域网服务返回异常大 XML 导致内存峰值或卡顿。
- OPDS 解析下载链接时过滤不支持的 acquisition 类型，避免把 APK/任意二进制误暴露为可下载书籍。
- 为 OPDS 不支持下载类型补充单元测试。

## 影响文件

- `app/src/main/java/com/flowreader/app/widget/ReadingProgressWidgetProvider.kt`
- `app/src/main/java/com/flowreader/app/ui/screens/reader/components/ComicReader.kt`
- `app/src/main/java/com/flowreader/app/util/OpdsClient.kt`
- `app/src/test/java/com/flowreader/app/util/OpdsClientTest.kt`

## 验证结果

- `./gradlew assembleDebug`：通过
- `./gradlew testDebugUnitTest`：通过
- `./gradlew verifyKotlinStyle`：通过
- `./gradlew coverageSummary`：通过，测试广度 `34/58 = 58.6%`

## 后续建议

- `BookParser` 仍存在 EPUB/FB2/MOBI 解析中整段 `readText()` / `readBytes()` 的内存峰值风险，建议下一轮把大文件解析改为分段或有上限读取。
- `ReaderViewModel` 仍偏大，建议继续按 ROADMAP v54 拆分进度计算、TTS 协调和设置映射。
- 备份导入当前一次性读取 JSON，后续可增加备份文件大小上限和结构版本校验。
