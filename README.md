# FlowReader 心流阅读

一款简洁优雅的 Android 电子书阅读应用，基于 Jetpack Compose 构建，支持 EPUB、TXT 格式，提供沉浸式的阅读体验。

## 特性

### 📚 书库管理
- 预装精品图书（神秘复苏、诡秘之主）
- 导入本地书籍（EPUB、TXT）
- 书架展示与分类浏览
- 最近阅读记录
- 阅读进度自动保存（3秒延迟写入）
- 书籍搜索与删除管理
- 智能筛选（按状态：正在阅读/想读/已读完/弃读/收藏）
- 标签系统支持

### 📖 阅读体验
- **10+阅读主题**: 浅色、深色、护眼、羊皮纸、纯黑（AMOLED）、晨读、午读、暮读、夜读、E Ink三模式
- **字体大小**: 12sp - 32sp 可调
- **行间距**: 1.0 - 2.5 可调
- **段落排版**: 首行缩进、两端对齐
- **翻页模式**: 滑动、仿真、无动画
- **智能点击区域**: 左30%上一页，中间呼出菜单，右30%下一页（可自定义）
- **章节位置记忆**: 切换章节自动恢复滚动位置
- **保持屏幕常亮**
- **控制栏自动隐藏**
- **背景纹理**: 纯色、纸张、布艺、大理石、渐变
- **环境音效**: 静音、雨声、风声、柴火声、咖啡馆

### 🔖 书签功能
- 随时添加书签
- 书签列表管理
- 快速跳转书签

### 📑 目录导航
- 章节目录展示
- 快速跳转章节
- 当前章节指示

### 📊 阅读统计
- 阅读时长统计
- 阅读进度追踪
- 字数统计与预计阅读时间

### 🎯 阅读目标
- 每日阅读时长目标
- 每周阅读书籍数量目标
- 目标完成情况可视化
- 阅读提醒设置

### 🏆 成就系统
- 阅读里程碑（100/500/1000章）
- 阅读时长成就（10/50/100小时）
- 书籍完读成就
- 阅读等级：初级/中级/高级/达人/大师

### 🖥️ E Ink 设备优化
- E Ink专属主题（纯墨/灰度/暖色）
- 刷新模式选择（自动/全局/局部/快速）
- E Ink模式开关

### ♿ 无障碍功能
- 大字模式（支持更大字体）
- 高对比度模式
- 简化模式（更大点击区域、更少界面元素）

### ⚙️ 个性化设置
- 应用主题跟随系统
- 默认阅读设置
- 自动夜间模式（根据时间自动切换）
- 阅读计划设置
- 简体中文界面

## 性能优化

- **章节内存缓存**: 智能缓存已读取章节，减少数据库查询
- **流式书籍解析**: 支持进度回调，大文件不再卡顿
- **图片缓存优化**: 内存+磁盘双缓存，滚动更流畅
- **进度延迟写入**: 3秒 debounce 减少数据库写入
- **UI 渲染优化**: 功能栏悬浮设计，内容区无遮挡
- **R8代码压缩**: Release构建启用混淆压缩

## 技术栈

| 类别 | 技术 |
|------|------|
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Clean Architecture |
| 依赖注入 | Hilt 2.50 |
| 数据库 | Room 2.6.1 |
| 图片加载 | Coil 2.6.0 |
| 异步 | Kotlin Coroutines + Flow |
| 导航 | Navigation Compose 2.7.7 |
| 设置存储 | DataStore Preferences |
| Kotlin | 1.9.22 |

## 项目结构

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
```

## 支持格式

| 格式 | 状态 |
|------|------|
| EPUB | ✅ 已支持 |
| TXT  | ✅ 已支持 |
| PDF  | ⏳ 规划中 |

## 预装图书

应用首次启动时会自动导入以下图书：

- **神秘复苏** - 作者：我会睡觉
- **诡秘之主** - 作者：爱潜水的乌贼

## 构建

```bash
# 克隆项目
git clone https://github.com/HuZaiGong/flowreader.git

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

### 低内存环境构建

如果构建过程中出现 `Gradle build daemon disappeared unexpectedly` 错误（通常发生在内存不足 4GB 的环境），请尝试以下方法：

1. **创建 Swap 空间**（推荐）：
   ```bash
   sudo fallocate -l 4G /swapfile
   sudo chmod 600 /swapfile
   sudo mkswap /swapfile
   sudo swapon /swapfile
   ```

2. **或使用更低的内存配置**，修改 `gradle.properties`：
   ```properties
   org.gradle.jvmargs=-Xmx1024m -Dfile.encoding=UTF-8
   org.gradle.parallel=false
   org.gradle.caching=true
   ```

## 要求

- Android SDK 34+
- Kotlin 1.9.22+
- Android Gradle Plugin 8.1+
- JDK 17
- 建议可用内存 ≥ 2GB（或配置 Swap）

## 版本

| 版本 | 日期 | 说明 |
|------|------|------|
| v10.0.0 | 2026-04-05 | 全新v10版本：阅读成就、E Ink优化、无障碍、阅读计划 |
| v9.1.0 | 2024-xx-xx | 阅读统计、底部导航 |
| v9.0.0 | 2024-xx-xx | 初始版本 |

## 许可证

MIT License