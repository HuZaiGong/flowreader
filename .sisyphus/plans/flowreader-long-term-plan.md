# FlowReader 心流阅读 - 长期更新规划

## 一、项目现状总览

### 1.1 当前版本状态

| 指标 | 状态 |
|------|------|
| 版本号 | 34.0.0 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.02.00 |
| AGP | 8.6.0 |
| Room | 2.6.1 |
| Hilt | 2.50 |
| Min SDK | 26 |
| Target SDK | 35 |

### 1.2 代码规模

```
app/src/main/java/com/flowreader/app/
├── data/           # 26 文件 - 数据库、Repository 实现
│   ├── local/      # 8 DAOs, 8 Entities, AppDatabase
│   └── repository/ # 10 Repository 实现
├── domain/         # 8 文件 - Model、Repository 接口
├── ui/             # 24 文件 - Screens、Theme、Components
│   └── screens/    # 8 个 Screen 模块
│       ├── library/       (书库)
│       ├── reader/        (阅读器 + 组件)
│       ├── bookdetail/    (书籍详情)
│       ├── settings/      (设置)
│       ├── stats/         (统计)
│       ├── auth/          (登录注册)
│       └── onboarding/    (新用户引导)
├── util/          # 6 文件 - BookParser、CacheManager、TTS等
└── di/             # 1 文件 - AppModule
```

### 1.3 已实现功能清单

**已完成（来自 README）**：
- ✅ EPUB、TXT、PDF、Markdown 格式支持
- ✅ 5 种阅读主题（浅色、深色、护眼、羊皮纸、AMOLED）
- ✅ 字体、字号、行间距调节
- ✅ 多种翻页模式（滑动、仿真、无动画、卷曲）
- ✅ 书签功能
- ✅ 目录导航
- ✅ 笔记与批注
- ✅ 全文搜索
- ✅ TTS 文本朗读
- ✅ 阅读统计
- ✅ 阅读目标
- ✅ 每日提醒
- ✅ 备份与恢复
- ✅ Material You 动态配色
- ✅ 账号系统与云端同步
- ✅ Onboarding 引导
- ✅ Markdown 格式支持

**计划中但未完成**：
- ❌ E Ink 设备优化
- ❌ 社交阅读功能
- ❌ 智能书籍分类
- ❌ 3D 书籍封面墙

---

## 二、待解决问题清单

### 2.1 代码质量问题（Critical）

| 问题 | 优先级 | 预估工时 |
|------|--------|----------|
| **命名冲突**：FlowReaderApp 同时作为 Application 类和 Composable | P0 | 2h |
| **缺少代码规范**：无 .editorconfig、lint.xml、detekt 配置 | P1 | 4h |
| **测试覆盖率低**：仅 BookParserTest 一个测试 | P1 | 40h+ |
| **CI 权限过宽**：permissions: contents: write | P2 | 1h |

### 2.2 架构改进（High）

| 问题 | 优先级 | 预估工时 |
|------|--------|----------|
| **ViewModel 膨胀**：部分 ViewModel 超过 500 行 | P1 | 20h |
| **Repository 冗余**：部分业务逻辑未抽离到 UseCase | P1 | 16h |
| **异常处理不统一**：缺少全局 Error 处理机制 | P1 | 8h |
| **状态管理不一致**：StateFlow vs remember 不统一 | P2 | 12h |

### 2.3 性能问题（High）

| 问题 | 优先级 | 预估工时 |
|------|--------|----------|
| **大文件解析卡顿**：>10MB 文件一次性加载 | P1 | 8h |
| **长章节渲染慢**：无分页，长内容滚动卡顿 | P1 | 16h |
| **图片缓存策略缺失**：封面重复读取 | P2 | 4h |
| **进度保存过于频繁**：翻页即写数据库 | P2 | 2h |

### 2.4 功能缺陷（Medium）

| 问题 | 优先级 | 预估工时 |
|------|--------|----------|
| **EPUB 解析不稳定**：Readium 配置复杂 | P2 | 16h |
| **Markdown 渲染粗糙**：基础支持 | P2 | 8h |
| **搜索功能局限**：仅支持单本书 | P2 | 12h |
| **缺少深色模式边缘手势**：夜间模式手势冲突 | P3 | 4h |

---

## 三、分阶段实施计划

### Phase 0：代码治理（1-2 周）

**目标**：解决阻碍长期开发的代码质量问题

#### Week 1：命名规范 + CI 修复

| 任务 | 产出 | 验证方式 |
|------|------|----------|
| 重命名 Application 类 FlowReaderApp → FlowReaderApplication | 修改 AndroidManifest + 所有引用 | `./gradlew assembleDebug` |
| 重命名 Composable FlowReaderApp → FlowReaderRoot | 修改 MainActivity 引用 | UI 测试 |
| 创建 .editorconfig | 统一代码风格 | CI lint 检查 |
| 创建 lint.xml | 定义检查规则 | `./gradlew lint` |
| 修复 CI permissions | 最小权限原则 | CI 构建成功 |

#### Week 2：测试基础设施

| 任务 | 产出 | 验证方式 |
|------|------|----------|
| 添加 ViewModel 测试框架 | MockK 配置完成 | 测试可运行 |
| 编写 Repository 测试 | BookRepositoryImplTest | 覆盖率报告 |
| 编写 UseCase 测试 | BookUseCasesTest | 覆盖率报告 |
| 编写 ViewModel 测试 | LibraryViewModelTest | 覆盖率报告 |

**覆盖率目标**：
- Repository 层：>80%
- ViewModel 层：>60%
- UseCase 层：>80%

---

### Phase 1：架构优化（2-3 周）

**目标**：建立可持续发展的代码架构

#### Week 3：Clean Architecture 强化

| 任务 | 改动范围 |
|------|----------|
| 创建 domain/usecase/ 包 | 阅读、书籍、统计相关 UseCase |
| 迁移 Repository 业务逻辑到 UseCase | BookRepositoryImpl → GetBookUseCase |
| 统一异常处理 | 创建 AppException + ErrorHandler |
| 统一状态管理模式 | UiState sealed class 规范 |

#### Week 4-5：状态管理与 DI 优化

| 任务 | 改动范围 |
|------|----------|
| 引入 MVI 模式（可选 Screen） | ReaderScreen 试点 |
| 优化 Hilt 模块划分 | 按功能模块拆分 AppModule |
| 添加 Navigation Type Safety | Navigation 2.8+ |
| 状态提升最佳实践 | remember vs rememberSaveable 规范 |

---

### Phase 2：性能优化（2 周）

**目标**：核心阅读体验流畅

#### Week 6：解析与渲染优化

| 任务 | 优化目标 |
|------|----------|
| BookParser 流式解析 | 支持 >50MB 文件 |
| 章节内容分页加载 | 每页 3000 字，预加载 2 页 |
| 图片内存缓存策略 | Coil memoryCacheKey 配置 |
| 进度保存防抖 | 3 秒延迟写入 |

#### Week 7：启动与内存优化

| 任务 | 优化目标 |
|------|----------|
| 启动速度优化 | 冷启动 <2s |
| 内存占用优化 | 阅读时 <200MB |
| LazyColumn 虚拟化 | 书库列表 60fps |
| 后台任务管理 | WorkManager 优化 |

---

### Phase 3：功能增强（持续迭代）

#### v35-v36：用户体验提升

| 功能 | 优先级 |
|------|--------|
| 批量书籍导入 | P1 |
| 书籍元数据自动补全 | P1 |
| 增强搜索（跨书搜索） | P1 |
| 阅读进度分享 | P2 |

#### v37-v38：生态系统

| 功能 | 优先级 |
|------|--------|
| 插件系统（自定义主题） | P2 |
| 书籍评分系统 | P2 |
| 阅读成就系统 | P2 |
| 周报/月报生成 | P3 |

#### v39-v40：高级功能

| 功能 | 优先级 |
|------|--------|
| E Ink 设备优化 | P3 |
| 云端同步增强 | P3 |
| 离线地图（书籍封面） | P3 |
| 深色模式 AI 适配 | P3 |

---

## 四、技术债务清理

### 4.1 依赖版本规划

| 依赖 | 当前版本 | 目标版本 | 计划版本 |
|------|----------|----------|----------|
| Kotlin | 2.0.21 | 2.1.x | v40+ |
| Compose BOM | 2024.02.00 | 2024.xx | v38+ |
| Room | 2.6.1 | 2.7.x | v36+ |
| Hilt | 2.50 | 2.51+ | v36+ |
| Readium | 3.1.2 | 3.2.x | v37+ |
| Coil | 2.6.0 | 3.0.x | v40+ |

### 4.2 废弃代码清理

| 代码 | 状态 | 清理计划 |
|------|------|----------|
| 注释掉的 epublib 依赖 | Deprecated | v35 移除 |
| 旧版 EPub 解析逻辑 | Deprecated | v36 移除 |
| 未使用的 DataStore Key | Deprecated | v35 清理 |

### 4.3 文档更新

| 文档 | 状态 | 更新计划 |
|------|------|----------|
| AGENTS.md | 需同步最新代码 | 每版本更新 |
| README.md | 部分过期 | v35 更新 |
| API 文档 | 缺失 | v38 创建 |
| 贡献指南 | 缺失 | v36 创建 |

---

## 五、测试策略

### 5.1 测试金字塔

```
        ┌─────────┐
        │   E2E   │  5%  (关键路径)
       ┌─────────┐
       │Integration│  15% (模块交互)
      ┌──────────┐
      │  Unit    │  80% (核心逻辑)
```

### 5.2 测试覆盖目标

| 模块 | 目标覆盖率 | 关键测试用例 |
|------|-----------|--------------|
| BookParser | 90% | 格式检测、编码处理、章节解析 |
| Repository | 80% | CRUD 操作、异常处理 |
| ViewModel | 60% | 状态转换、副作用 |
| UseCase | 80% | 业务规则验证 |

### 5.3 CI 测试集成

```yaml
# .github/workflows/build.yml 增强
- name: Run unit tests
  run: ./gradlew testDebugUnitTest --no-daemon

- name: Run UI tests
  run: ./gradlew connectedDebugAndroidTest --no-daemon

- name: Code coverage
  run: ./gradlew jacocoTestReport
```

---

## 六、发布节奏

### 6.1 版本号规则

```
major.minor.patch
  │     │     └── Bug fix, docs
  │     └──────── New features, backwards compatible
  └────────────── Breaking changes, major refactors
```

### 6.2 迭代周期

| 阶段 | 时长 | 内容 |
|------|------|------|
| 开发 | 2 周 | 功能开发 + Code Review |
| 测试 | 1 周 | QA 测试 + Bug 修复 |
| 发布 | 1 周 | Beta 测试 + 灰度发布 |

### 6.3 长期路线图

```
2026 Q2: v35-v36 (代码治理 + 架构优化)
2026 Q3: v37-v38 (性能优化 + 功能增强)
2026 Q4: v39-v40 (生态系统 + 高级功能)
```

---

## 七、风险与缓解

### 7.1 技术风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Readium 升级 breaking change | 高 | 预留 2 周迁移时间 |
| Kotlin 2.1 兼容性 | 中 | 提前在 dev 分支测试 |
| 测试覆盖率目标过高 | 中 | 分阶段目标，灵活调整 |

### 7.2 资源风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 单一维护者 | 高 | 完善文档，降低接手门槛 |
| 外部依赖更新 | 中 | 锁定关键依赖版本 |

### 7.3 用户风险

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 重大重构影响稳定性 | 高 | 保留老功能切换开关 |
| 新功能学习成本 | 中 | 渐进式引导 |

---

## 八、附录

### 8.1 参考文档

- 现有 v10 规划：`.sisyphus/plans/flowreader-v10-plan.md`
- 优化计划：`.sisyphus/plans/flowreader-optimization-plan.md`
- 项目知识库：`AGENTS.md`

### 8.2 关键文件路径

| 用途 | 路径 |
|------|------|
| 添加新页面 | `ui/screens/{module}/` |
| 数据库修改 | `data/local/dao/` + `entity/` |
| 依赖注入 | `di/AppModule.kt` |
| 阅读设置 | `domain/model/ReadingSettings.kt` |
| 书籍解析 | `util/BookParser.kt` |

### 8.3 里程碑检查点

| 版本 | 目标 | 验收标准 |
|------|------|----------|
| v35 | 代码治理完成 | 无命名冲突，有 lint 配置 |
| v36 | 架构优化完成 | UseCase 层完善，测试 >50% |
| v37 | 性能目标达成 | 冷启动 <2s，阅读内存 <200MB |
| v38 | 功能增强完成 | 批量导入、跨书搜索 |
| v40 | 生态系统成型 | 成就系统、周报生成 |

---

*规划版本：v2.0*  
*创建时间：2026-04-10*  
*基于：AGENTS.md + 现有规划文档 + 代码审查*
