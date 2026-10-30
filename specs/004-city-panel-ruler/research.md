# Research: 城市面板与控制人导航

## Overview

本功能的技术栈已完全由项目宪法和现有代码基确定，不存在外部技术选型的未知因素。本研究文档记录对现有代码基的分析结论，以及为完成本功能所做的设计决策。

## Existing Codebase Analysis

### UI Framework
- **Engine**: cyan_potion 自研引擎，基于 LWJGL/OpenGL
- **Component Base**: `AbstractControllableGameWindowComponent` 提供事件处理、绘制、生命周期管理
- **Window Wrapper**: `DraggableWindowComponent` 提供可拖拽的窗口外壳，已用于 `PersonListComponent` 和 `PersonDetailComponent`
- **Input Handling**: 左键点击通过 `MouseButtonEvent` 处理器注册；悬停效果通过 `update()` 中的坐标检测实现

### Data Model
- **City**: 已有 `id`, `name`, `x`, `y`, `type`, `connectedCities`, `residents` 字段；缺少控制人关联
- **Person**: 已有 `cityId` 指向所在城市；人物详情页面 `PersonDetailComponent` 已完整实现
- **CityCache**: 全局城市访问入口
- **PersonCache**: 全局人物访问入口

### Integration Points
- `MapComponent.onCitySelected` 回调已存在，当前在 `PersonBrowserDemo` 中未绑定具体行为，只需接入城市面板的展示逻辑
- `PersonBrowserDemo.onPersonSelected(Person)` 已能打开人物详情窗口，城市面板点击控制人后可复用该路径

## Decisions

| Decision | Rationale |
|----------|-----------|
| 在 `City` 中新增 `rulerId` 字段（String，可为 null） | 最轻量的关联方式，与现有 `cityId` 在 `Person` 中的设计对称；不引入新的关联表或复杂对象引用，避免循环依赖 |
| 城市面板使用 `DraggableWindowComponent` 包裹 | 与人物列表、人物详情保持一致的用户体验；无需新建面板类型 |
| 控制人区域使用可点击的 `Button` 或自定义 `AbstractControllableGameWindowComponent` | `Button` 已内置悬停高亮和点击反馈，最符合 UX-002 的 100ms 反馈要求；若需要更复杂的头像+文字布局，可封装为 `CityRulerComponent` |
| 无控制人时显示占位文本并禁用点击 | 避免 null 导致的异常，同时给用户明确的状态反馈 |
| 面板关闭后再次打开时重新从 `CityCache` 读取数据 | 保证显示最新状态，满足 FR-005 |

## Alternatives Considered

1. **在 City 中直接持有 Person 引用而非 rulerId**
   - 拒绝原因：Person 模型采用贫血领域模型，且已有通过 Cache 访问的模式；直接引用会增加序列化/反序列化复杂度，并可能在控制人变更时引入状态同步问题。

2. **将城市面板实现为 MapComponent 内部的固定侧边栏而非浮动窗口**
   - 拒绝原因：与现有人物详情窗口的浮动窗口风格不一致；侧边栏会压缩地图可视区域，且实现复杂度更高（需要处理布局重排）。

3. **为控制人跳转新建独立的导航服务/路由层**
   - 拒绝原因：`PersonBrowserDemo` 中 `onPersonSelected` 已经是成熟的跳转点，新增抽象层属于过度设计。
