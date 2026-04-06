# FlowReader 心流阅读

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=flat&logo=android" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-blue?style=flat&logo=kotlin" alt="Language">
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange?style=flat" alt="License">
  <img src="https://img.shields.io/badge/MinSDK-26+-red?style=flat" alt="MinSDK">
</p>

<p align="center">
  <b>一款简洁优雅的 Android 电子书阅读应用</b><br>
  基于 Jetpack Compose 构建 | 支持 EPUB、TXT、PDF 格式<br>
  提供沉浸式的阅读体验
</p>

---

## ✨ 特性亮点

### 📚 书库管理
- 预装精品图书（神秘复苏、诡秘之主）
- 导入本地书籍（EPUB、TXT、PDF）
- 批量导入多本书籍
- 书架展示与分类浏览
- 最近阅读记录
- 阅读进度自动保存（3秒延迟写入）
- 书籍搜索与删除管理
- 书籍排序（按添加时间/阅读时间/书名/作者）

### 📖 阅读体验
| 功能 | 说明 |
|------|------|
| **阅读主题** | 浅色、深色、护眼、羊皮纸、纯黑（AMOLED）|
| **字体大小** | 12sp - 32sp 可调 |
| **行间距** | 1.0 - 2.5 可调 |
| **翻页模式** | 滑动、仿真、无动画、卷曲、滑动覆盖 |
| **点击区域** | 左30%上一页，中间呼出菜单，右30%下一页 |
| **章节记忆** | 切换章节自动恢复滚动位置 |
| **进度条** | 底部可拖拽，快速跳转章节 |
| **屏幕常亮** | 阅读时保持屏幕常亮 |
| **自动夜间模式** | 根据时间自动切换深色/浅色主题 |
| **PDF支持** | 流畅阅读，支持缩放和拖拽翻页 |
| **边缘手势** | 自定义边缘滑动区域，避免与系统返回手势冲突 |

### 📊 阅读统计
- 每日阅读时长统计
- 每日阅读页数统计
- 阅读进度自动记录
- 阅读目标设置（每日阅读时长目标）

### 🔖 书签功能
- 随时添加书签
- 书签列表管理
- 快速跳转书签

### 📑 目录导航
- 章节目录展示
- 快速跳转章节
- 当前章节指示

### ✏️ 笔记与批注 (新增)
- 文字高亮划线
- 添加想法/批注
- 多种高亮颜色（黄、绿、蓝、粉、橙）
- 笔记导出功能

### 🔍 全文搜索 (新增)
- 单本书籍内全文检索
- 搜索结果高亮显示
- 快速跳转到搜索位置

### 🔊 文本朗读 (新增)
- 接入系统TTS语音引擎
- 支持调节语速和音调
- 中英文语音识别

### ⏰ 提醒功能
- 每日阅读提醒
- 自定义提醒时间

### 💾 数据管理
- 书籍和阅读进度备份
- 从备份文件恢复数据
- 阅读记录导出

### ⚙️ 个性化设置
- 应用主题跟随系统（Material You 动态颜色）
- 默认阅读设置
- 简体中文界面

---

## 🚀 性能优化

| 优化项 | 说明 |
|--------|------|
| 章节内存缓存 | 智能缓存已读取章节，减少数据库查询 |
| 流式书籍解析 | 支持进度回调，大文件不再卡顿 |
| 图片缓存优化 | 内存+磁盘双缓存，滚动更流畅 |
| 进度延迟写入 | 3秒 debounce 减少数据库写入 |
| UI 渲染优化 | 功能栏悬浮设计，内容区无遮挡 |
| PDF流式渲染 | 大型PDF文件流畅加载 |
| Readium引擎 | 接入Readium成熟EPUB渲染引擎 |

---

## 🛠️ 技术栈

| 类别 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt |
| 数据库 | Room + SQLite FTS5 |
| 图片加载 | Coil |
| 异步 | Kotlin Coroutines + Flow |
| 导航 | Navigation Compose |
| EPUB渲染 | Readium Kotlin Toolkit |

---

## 📂 项目结构

```
app/src/main/java/com/flowreader/app/
├── data/                      # 数据层
│   ├── local/                 # Room 数据库
│   │   ├── dao/               # Data Access Objects
│   │   └── entity/            # 数据实体
│   └── repository/            # 仓库实现
├── di/                        # Hilt 依赖注入
├── domain/                    # 领域层
│   ├── model/                 # 领域模型
│   └── repository/            # 仓库接口
├── ui/                        # 表现层
│   ├── screens/               # 页面
│   │   ├── library/           # 书库
│   │   ├── reader/            # 阅读器
│   │   ├── bookdetail/        # 书籍详情
│   │   └── settings/          # 设置
│   ├── theme/                 # 主题样式
│   └── Navigation.kt          # 导航配置
└── util/                      # 工具类
    ├── BookParser.kt          # 书籍解析
    ├── TtsManager.kt          # TTS文本朗读
    └── FullTextSearch.kt       # 全文搜索
```

---

## 📋 支持格式

| 格式 | 状态 |
|------|------|
| EPUB | ✅ 已支持（Readium引擎）|
| TXT  | ✅ 已支持 |
| PDF  | ✅ 已支持 |

---

## 📚 预装图书

应用首次启动时会自动导入以下图书：

- **神秘复苏** - 作者：我会睡觉
- **诡秘之主** - 作者：爱潜水的乌贼

---

## 📲 构建

```bash
# 克隆项目
git clone https://github.com/HuZaiGong/flowreader.git

# 进入项目目录
cd flowreader

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

### 环境要求

- Android SDK 34+
- Kotlin 1.9+
- Android Gradle Plugin 8.1+
- JDK 17

---

## 📝 更新日志

### v19 (Latest)
- 修复 Kotlin/Compose 版本兼容性问题
- 更新 Android Gradle Plugin 至 8.6.0
- 更新 Kotlin 至 2.0.21
- 更新 Gradle 至 8.7
- 更新 compileSdk/targetSdk 至 35
- 使用新版 Compose Compiler Plugin 替代旧版配置

### v17
- 新增 Readium Kotlin Toolkit EPUB渲染引擎，支持复杂CSS/排版
- 新增边缘手势识别设置，解决滑动翻页与系统返回手势冲突
- 新增笔记/批注功能（划线、想法、导出）
- 新增全文搜索（单本书籍内FTS检索）
- 新增TTS文本朗读功能
- 性能优化：提升大型书籍解析速度

### v15
- Latest release version

### v12.0.0
- 新增阅读目标设置（每日阅读时长目标）
- 新增搜索历史记录功能
- 性能优化：数据库索引优化
- 性能优化：图片缓存优化
- 性能优化：书籍解析流式处理
- UI优化：阅读进度百分比显示增强

### v11.0.0
- 新增 PDF 格式支持
- 新增阅读统计功能（阅读时长、页数）
- 新增底部可拖拽进度条
- 新增时间自动夜间模式
- 新增批量导入书籍
- 新增书籍排序功能
- 新增阅读记录导出
- 新增每日阅读提醒
- 新增备份/恢复功能
- 新增关于页面
- 新增 Material You 动态颜色支持
- 性能优化：启动速度、内存占用优化
- 深色主题对比度优化
- AMOLED纯黑模式增强

---

## 📄 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 开源。

---

## 🌟 Star History

[![Star History Chart](https://api.star-history.com/svg?repos=HuZaiGong/flowreader&type=Date)](https://star-history.com/#HuZaiGong/flowreader&Date)

---

<p align="center"> Made with ❤️ by HuZaiGong </p>
