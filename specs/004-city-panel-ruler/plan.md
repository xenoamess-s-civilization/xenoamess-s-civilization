# Implementation Plan: 城市面板与控制人导航

**Branch**: `004-city-panel-ruler` | **Date**: 2026-04-03 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/004-city-panel-ruler/spec.md`

## Summary

为游戏地图添加城市面板 UI，在玩家点击城市后展示该城市的基本信息及控制人。玩家可点击控制人区域跳转至人物详情页面。实现涉及 City 模型扩展、CityPanelComponent UI 组件开发、MapComponent 与城市面板的集成，以及 PersonBrowserDemo 中的事件流转和窗口管理。

## Technical Context

**Language/Version**: Java 17+  
**Primary Dependencies**: cyan_potion 引擎（内部维护）、LWJGL、JOML、Lombok、SLF4J  
**Storage**: N/A（运行时内存模型，无持久化变更）  
**Testing**: JUnit 5（通过 Maven 运行）  
**Target Platform**: Desktop（OpenGL 窗口应用）  
**Project Type**: desktop-game  
**Performance Goals**: 城市面板展示 ≤200ms，维持 60 FPS  
**Constraints**: 单方法 ≤50 行，圈复杂度 ≤10；零 P3C ERROR  
**Scale/Scope**: 单个游戏场景内的 UI 面板扩展，影响 1 个数据模型和 3-4 个 UI 组件

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

基于项目宪法（.specify/memory/constitution.md）的检查项：

### I. 代码质量检查
- [x] 新模块是否符合 P3C 代码规范？
  - 是。CityPanelComponent 与 RulerInfoComponent 将遵循既有 GUI 组件的命名与结构规范。
- [x] 公共 API 是否已设计 JavaDoc 注释？
  - 是。show(City)、setOnRulerClick(Consumer<Person>) 等公共方法将包含完整 JavaDoc。
- [x] 预估的方法/类复杂度是否在限制范围内（方法 ≤50 行，圈复杂度 ≤10）？
  - 是。面板渲染将拆分为 drawHeader、drawRulerSection、drawStats 等小方法；每个方法控制在 50 行内。

### II. 测试标准检查
- [x] 是否规划了单元测试覆盖（目标 ≥80%，关键路径 ≥90%）？
  - 是。City 的 rulerId getter/setter、CityPanelComponent 的状态切换逻辑将包含单元测试。
- [x] 测试用例是否可在独立环境中运行（无外部依赖）？
  - 是。UI 组件测试使用 headless/stub GameWindow；模型测试无外部依赖。
- [x] 是否识别了需要集成测试的模块交互点？
  - 是。MapComponent → onCitySelected → CityPanelComponent → onRulerClick → PersonDetailComponent 的端到端流程将编写集成测试。

### III. 用户体验一致性检查
- [x] UI 组件是否复用现有组件库？
  - 是。CityPanelComponent 继承 AbstractControllableGameWindowComponent；使用 Button、Panel、Picture 等现有基元；以 DraggableWindowComponent 包裹。
- [x] 用户交互流程是否符合既有模式（左键选择、右键菜单等）？
  - 是。左键点击城市打开面板，左键点击控制人跳转详情，与现有人物列表→详情的交互模式一致。
- [x] 操作反馈是否在 100ms 内？耗时操作是否有进度指示？
  - 是。面板展示为同步内存操作，无需异步加载，可在 1 帧内完成。
- [x] 错误场景是否设计了用户友好的提示信息？
  - 是。无控制人时显示“无主”占位文本，不可点击，避免空白困惑。

### IV. 性能要求检查
- [x] 新功能是否明确了性能预算（内存/CPU/渲染）？
  - 是。城市面板为固定尺寸（约 400×500），只绘制文本和简单色块，不产生额外贴图；内存增量为 1 个面板实例 + 1 个窗口外壳。
- [x] 是否识别了潜在的性能瓶颈（大循环、频繁 GC、IO 操作）？
  - 是。无大循环或 IO；控制人信息为直接字段读取； residents 数量大时仅显示计数，不展开渲染列表。
- [x] 目标帧率（60 FPS）是否可维持？
  - 是。面板渲染与现有人物详情窗口同级，无额外复杂特效。

*宪法检查全部通过，无违规项。*

## Project Structure

### Documentation (this feature)

```text
specs/004-city-panel-ruler/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (UI event contract only)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/
├── map/
│   └── City.java                     # 新增 rulerId 字段
├── gui/
│   ├── CityPanelComponent.java       # 城市面板主体
│   ├── CityRulerComponent.java       # 控制人展示子组件（可选，若内联则合并）
│   ├── MapComponent.java             # 回调接口调整
│   └── PersonBrowserDemo.java        # 集成城市面板窗口与详情跳转
└── [tests]
    ├── CityTest.java                 # City ruler 单元测试
    └── CityPanelComponentTest.java   # UI 与集成测试
```

**Structure Decision**: 采用单项目结构。所有变更集中在 `src/xenoamess-s-civilization` 模块内，复用现有的 `gui/` 和 `map/` 包结构，与现有人物浏览器和地图组件保持一致。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

无违规项，本章节留空。
