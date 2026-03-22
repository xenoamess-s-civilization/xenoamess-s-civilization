# Tasks: Groovy Mod Support

**Input**: Design documents from `/specs/003-groovy-mod-support/`  
**Prerequisites**: plan.md, spec.md

---

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project setup and dependency configuration

- [X] T001 Add Groovy 4.0.18 dependency to parent/pom.xml
- [X] T002 [P] Create `src/base/src/main/java/.../base/mod/` package structure
- [X] T003 [P] Create `src/base/src/test/groovy/` for Groovy tests

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core Groovy engine and safety infrastructure

- [ ] T004 Create `GroovyScriptEngine.java` - JSR-223 script engine wrapper
- [ ] T005 Create `GroovySandbox.java` - SecurityManager-based sandbox
- [ ] T006 Create `ModContainer.java` - Mod metadata and script holder
- [ ] T007 Create `ModManager.java` - Mod lifecycle management

---

## Phase 3: US1 - Groovy Mod Loading (Priority: P1) 🎯 MVP

**Goal**: Load and execute Groovy scripts

### Tests for US1

- [ ] T008 [P] [US1] Create `GroovyScriptEngineTest.groovy`
- [ ] T009 [P] [US1] Test simple Groovy script execution
- [ ] T010 [P] [US1] Test Groovy class import from Java

### Implementation for US1

- [ ] T011 [US1] Implement script compilation caching
- [ ] T012 [US1] Implement mod.json metadata parsing
- [ ] T013 [US1] Create example Groovy Mod structure

---

## Phase 4: US2 - Security Sandbox (Priority: P1)

**Goal**: Safe Groovy execution

### Tests for US2

- [ ] T014 [P] [US2] Test file system access blocking
- [ ] T015 [P] [US2] Test system command blocking
- [ ] T016 [P] [US2] Test network access blocking

### Implementation for US2

- [ ] T017 [US2] Implement SecurityManager policy
- [ ] T018 [US2] Implement Groovy AST transformation for security
- [ ] T019 [US2] Add secure binding for allowed APIs only

---

## Phase 5: US3 - API Binding (Priority: P1)

**Goal**: Groovy-Java interop

### Tests for US3

- [ ] T020 [P] [US3] Test calling Java API from Groovy
- [ ] T021 [P] [US3] Test event handler registration from Groovy

### Implementation for US3

- [ ] T022 [US3] Create `ModAPI.java` interface
- [ ] T023 [US3] Create `GroovyExtension.java` base class
- [ ] T024 [US3] Bind GameManager, EventSystem to Groovy
- [ ] T025 [US3] Create `@Mod` annotation for Groovy classes

---

## Phase 6: US4 - Mod Manager UI (Priority: P2)

**Goal**: User-friendly mod management

### Tests for US4

- [ ] T026 [P] [US4] Test ModManager UI interactions

### Implementation for US4

- [ ] T027 [US4] Create Mod list UI component
- [ ] T028 [US4] Add enable/disable mod functionality
- [ ] T029 [US4] Add mod loading error display

---

## Phase 7: Documentation & Examples

**Purpose**: Developer documentation and examples

- [X] T030 [P] Create `ModDevelopmentGuide.md`
- [X] T031 [P] Create `APIReference.md`
- [ ] T032 [P] Create example Mod: HelloWorld
- [ ] T033 [P] Create example Mod: CustomEventHandler
- [ ] T034 [P] Create example Mod: CustomUIComponent
- [X] T035 [P] Create `quickstart.md` for mod developers

---

## Dependencies & Execution Order

```
Phase 1 (Setup)
    ↓
Phase 2 (Foundational)
    ↓
    ├──→ Phase 3 (US1: Mod Loading)
    │
    ├──→ Phase 4 (US2: Security) ──→ Phase 5 (US3: API Binding)
    │
    └──→ Phase 6 (US4: UI)
            ↓
        Phase 7 (Docs)
```

---

## Task Statistics

| Category | Count |
|----------|-------|
| **Total Tasks** | 35 |
| **Setup** | 3 |
| **Foundational** | 4 |
| **US1 (Mod Loading)** | 6 |
| **US2 (Security)** | 6 |
| **US3 (API Binding)** | 6 |
| **US4 (UI)** | 4 |
| **Documentation** | 6 |

---

## Implementation Strategy

### MVP First

1. Complete Phase 1-2: Setup + Foundational
2. Complete Phase 3: Basic Groovy loading (US1)
3. Complete Phase 4: Security sandbox (US2)
4. **STOP and VALIDATE**: Can load and safely run Groovy scripts

### Full Feature Set

1. MVP (above)
2. Phase 5: API binding (US3)
3. Phase 6: Mod Manager UI (US4)
4. Phase 7: Documentation
