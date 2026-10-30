# Tasks: 城市面板与控制人导航

**Input**: Design documents from `/specs/004-city-panel-ruler/`
**Prerequisites**: plan.md, spec.md, data-model.md, contracts/, quickstart.md

**Tests**: Included (specification mandates ≥80% coverage and end-to-end integration test)

**Organization**: Tasks grouped by user story for independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel
- **[Story]**: US1, US2, US3
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify project compiles and test infrastructure is ready

- [x] T001 Verify existing project compiles: `cd src/xenoamess-s-civilization && mvn clean compile -DskipTests`
- [x] T002 [P] Verify existing tests pass: `cd src/xenoamess-s-civilization && mvn test`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data model extension that MUST be complete before user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 [P] Add `rulerId` field, getter, setter, and `getRuler()` to `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/map/City.java`
- [x] T004 [P] Write unit tests for City ruler logic in `src/xenoamess-s-civilization/src/test/java/com/xenoamess/cyan_potion/civilization/map/CityTest.java`
- [x] T005 Update `WorldMapGenerator` or city assignment logic in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/map/WorldMapGenerator.java` to set initial `rulerId` for generated cities

**Checkpoint**: Foundation ready - `City` has `rulerId`, tests pass, generated cities have rulers

---

## Phase 3: User Story 1 - 查看城市面板 (Priority: P1) 🎯 MVP

**Goal**: 玩家点击地图上的城市后，系统展示该城市的专属面板

**Independent Test**: 在地图上点击任意城市，验证城市面板窗口弹出并显示该城市的名称、类型、人口等基本信息；点击另一城市时面板内容切换；关闭后可再次打开。

### Tests for User Story 1

- [x] T006 [P] [US1] Write unit test for `CityPanelComponent.show(City)` and `hide()` in `src/xenoamess-s-civilization/src/test/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponentTest.java`

### Implementation for User Story 1

- [x] T007 [P] [US1] Create `CityPanelComponent.java` in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponent.java` with background, title, and basic city stats rendering
- [x] T008 [US1] Implement `MapComponent` callback binding in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/PersonBrowserDemo.java` to open `cityPanelWindow` on city click
- [x] T009 [US1] Add `cityPanelComponent` and `cityPanelWindow` fields and lifecycle management in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/PersonBrowserDemo.java`
- [x] T010 [US1] Ensure `CityPanelComponent` closes properly and can be reopened with updated city info

**Checkpoint**: At this point, clicking a city opens the panel with basic info. User Story 1 is testable independently.

---

## Phase 4: User Story 2 - 查看城市控制人 (Priority: P1)

**Goal**: 城市面板中显示该城市的控制人信息；无控制人时显示"无主"状态

**Independent Test**: 打开城市面板，验证有控制人的城市显示控制人姓名，无控制人的城市显示"无主"且控制人区域不可交互。

### Tests for User Story 2

- [x] T011 [P] [US2] Add test cases for ruler display and "no ruler" state in `src/xenoamess-s-civilization/src/test/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponentTest.java`

### Implementation for User Story 2

- [x] T012 [US2] Add `rulerButton` and ruler rendering logic to `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponent.java`
- [x] T013 [US2] Implement "无主" placeholder and disable/hide ruler button when `city.getRulerId()` is null in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponent.java`
- [x] T014 [US2] Ensure ruler info refreshes from `PersonCache` each time `show(City)` is called

**Checkpoint**: City panel shows ruler name when present, shows "无主" when absent. User Stories 1 and 2 both work.

---

## Phase 5: User Story 3 - 跳转到控制人详情 (Priority: P2)

**Goal**: 点击城市面板中的控制人后，导航到该控制人的人物详情页面

**Independent Test**: 在城市面板中点击控制人姓名，验证人物详情窗口打开并显示正确的人物信息；悬停时有视觉反馈；点击"无主"区域无反应。

### Tests for User Story 3

- [x] T015 [P] [US3] Add integration test for ruler-click-to-person-detail flow in `src/xenoamess-s-civilization/src/test/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelIntegrationTest.java`

### Implementation for User Story 3

- [x] T016 [US3] Add `setOnRulerClick(Consumer<Person>)` API and click handling to `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponent.java`
- [x] T017 [US3] Wire `CityPanelComponent.onRulerClick` to `PersonBrowserDemo.onPersonSelected` in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/PersonBrowserDemo.java`
- [x] T018 [US3] Add hover visual feedback (highlight/color change) for the ruler clickable area in `src/xenoamess-s-civilization/src/main/java/com/xenoamess/cyan_potion/civilization/gui/CityPanelComponent.java`
- [x] T019 [US3] Guard against duplicate detail window when clicking the same ruler repeatedly (leverage existing `PersonBrowserDemo` window reuse logic)

**Checkpoint**: All user stories are independently functional. Clicking ruler opens person details.

---

## Phase 6: 完善与跨领域关注点 (Polish & Cross-Cutting Concerns)

**Purpose**: 确保符合项目宪法和验收标准

### 代码质量与文档

- [x] T020 [P] 补充 `CityPanelComponent` 公共 API 的 JavaDoc 注释
- [x] T021 [P] 运行 P3C 静态代码分析 (`mvn p3c:p3c`) — 项目未配置 P3C Maven 插件，已手动完成代码规范审查
- [x] T022 [P] 代码清理：确保单方法 ≤50 行，圈复杂度 ≤10

### 测试完善

- [x] T023 [P] 补充 `CityPanelComponent` 单元测试，确保新增代码覆盖率 ≥80%
- [x] T024 运行完整测试套件 `mvn test`，验证所有测试通过
- [x] T025 [P] 验证集成测试覆盖从点击城市到跳转人物详情的端到端流程

### 用户体验与性能

- [x] T026 本地化文本检查：将"无主"等用户可见文本提取到外部化资源（若项目已有 i18n 机制）— 项目无 i18n 框架，文本处理与现有 UI 一致
- [x] T027 UX 一致性检查：确认左键点击、窗口拖拽、ESC 关闭等行为与现有人物列表/详情一致
- [x] T028 运行 quickstart.md 验证步骤，确认面板展示 ≤200ms、完整流程 3 次点击内完成

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies
- **Foundational (Phase 2)**: Depends on Setup - BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational; no dependency on other stories
- **User Story 2 (Phase 4)**: Depends on Foundational and US1 (shares `CityPanelComponent`)
- **User Story 3 (Phase 5)**: Depends on Foundational, US1, and US2
- **Polish (Phase 6)**: Depends on all user stories

### User Story Dependencies

- **US1 (P1)**: Needs City model from Foundational
- **US2 (P1)**: Needs US1 (shared UI component) + City ruler field from Foundational
- **US3 (P2)**: Needs US2 (ruler display must exist before it can be clicked)

### Parallel Opportunities

- T003 and T004 can run in parallel (model + test)
- T007 and T006 can run in parallel (component implementation + component test skeleton)
- T012 and T011 can run in parallel (ruler UI + ruler test)
- T016 and T015 can run in parallel (click handling + integration test skeleton)
- All Polish phase tasks marked [P] can run in parallel

---

## Parallel Example: User Story 1

```bash
# Launch model implementation and its test together:
Task: "Add rulerId field to City.java"
Task: "Write CityTest for ruler logic"

# Launch component implementation and its test together:
Task: "Create CityPanelComponent.java"
Task: "Write CityPanelComponentTest for show/hide"
```

---

## Implementation Strategy

### MVP First (User Story 1 + US2)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational
3. Complete Phase 3: User Story 1 (basic city panel)
4. Complete Phase 4: User Story 2 (ruler display)
5. **STOP and VALIDATE**: Test city panel with and without ruler
6. Demo if ready

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. US1 → City panel opens with basic info → Test independently
3. US2 → Ruler info displayed → Test independently
4. US3 → Ruler click navigates to person detail → Test independently
5. Polish → P3C, coverage, UX checks

### Parallel Team Strategy

With multiple developers:
- Developer A: Foundational (City model + tests)
- Developer B: US1 (CityPanelComponent basic structure + MapComponent integration)
- Developer C: US2/US3 (ruler display + click navigation + integration tests)
