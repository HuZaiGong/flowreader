# FlowReader 心流阅读

一款简洁优雅的 Android 电子书阅读应用，支持 EPUB、TXT 格式，提供沉浸式的阅读体验。

## 功能特性

### 📚 书库管理
- 导入本地书籍（EPUB、TXT）
- 书架展示，分类浏览
- 最近阅读记录
- 阅读进度自动保存
- 书籍删除管理

### 📖 阅读体验
- **多种阅读主题**: 浅色、深色、护眼、羊皮纸、纯黑（AMOLED）
- **字体大小调节**: 12sp - 32sp
- **行间距调整**: 1.0 - 2.5
- **翻页模式**: 滑动、仿真、无动画
- **保持屏幕常亮**
- 支持左右tap翻页

### 🔖 书签功能
- 随时添加书签
- 书签列表管理
- 快速跳转书签

### 📑 目录导航
- 章节目录展示
- 快速跳转章节
- 当前章节指示

### ⚙️ 个性化设置
- 应用主题跟随系统
- 默认阅读设置
- 简体中文界面

## 技术架构

- **UI**: Jetpack Compose
- **依赖注入**: Hilt
- **数据库**: Room
- **图片加载**: Coil
- **架构模式**: MVVM + Clean Architecture

## 项目结构

```
app/
├── src/main/java/com/flowreader/app/
│   ├── data/              # 数据层
│   │   ├── local/         # Room 数据库
│   │   │   ├── dao/       # Data Access Objects
│   │   │   └── entity/   # 数据实体
│   │   └── repository/    # 仓库实现
│   ├── di/                # Hilt 依赖注入模块
│   ├── domain/            # 领域层
│   │   ├── model/         # 领域模型
│   │   └── repository/    # 仓库接口
│   ├── ui/                # 表现层
│   │   ├── screens/       # 页面
│   │   │   ├── library/   # 书库
│   │   │   ├── reader/    # 阅读器
│   │   │   ├── bookdetail/# 书籍详情
│   │   │   └── settings/  # 设置
│   │   └── theme/         # 主题样式
│   └── util/              # 工具类
```

## 支持格式

| 格式 | 状态 |
|------|------|
| EPUB | ✅ 支持 |
| TXT  | ✅ 支持 |
| PDF  | ⏳ 规划中 |

## 主题预览

- **浅色模式**: 简洁明亮的日常阅读
- **深色模式**: 低亮度的夜间阅读
- **护眼模式**: 温和的棕黄色调，减轻眼睛疲劳
- **羊皮纸模式**: 复古纸张质感
- **纯黑模式**: AMOLED 屏幕省电首选

## 编译运行

```bash
# 克隆项目
git clone https://github.com/HuZaiGong/flowreader.git

# 打开 Android Studio
# 等待 Gradle 同步完成
# 运行到模拟器或设备
```

## 要求

- Android SDK 34+
- Kotlin 1.9+
- Android Gradle Plugin 8.1+

## 许可证

MIT License
