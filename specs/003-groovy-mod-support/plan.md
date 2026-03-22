# Implementation Plan: Groovy Mod Support

**Branch**: `003-groovy-mod-support` | **Date**: 2026-03-23 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/003-groovy-mod-support/spec.md`

---

## Summary

为cyan_potion引擎添加Groovy脚本支持，允许Mod开发者使用Groovy（类Java语法）编写非核心功能Mod。实现包括Groovy脚本引擎集成、安全沙箱、Mod管理器和API暴露。

技术方案：
- 使用Groovy 4.x作为脚本引擎
- 实现自定义SecurityManager进行沙箱隔离
- 创建ModManager管理Mod生命周期
- 提供GroovyExtension基类和API绑定

---

## Technical Context

**Language/Version**: Java 17+ (兼容 JDK 21), Groovy 4.0+  
**Primary Dependencies**: 
- Groovy 4.0.18 (groovy, groovy-jsr223)
- Apache Commons VFS (用于Mod文件管理)
  
**Testing**: JUnit 5 + Spock Framework (Groovy测试)  
**Target Platform**: Windows 10+, Linux  
**Project Type**: 游戏引擎Mod系统扩展  
**Performance Goals**: Groovy执行速度 >= 50% Java, Mod加载 < 5s  
**Constraints**: 
- 安全沙箱必须阻止文件/网络/系统访问
- 保持与现有Java核心模块的兼容性
- Groovy语法需类Java风格

---

## Constitution Check

### I. 代码质量检查
- [X] 新模块符合 P3C 规范
- [X] 公共 API 已设计 JavaDoc 注释
- [X] 沙箱安全策略经过安全审查

### II. 测试标准检查
- [X] 规划单元测试覆盖（目标 ≥80%）
- [X] 测试可在独立环境中运行
- [X] 已识别集成测试点

### III. 用户体验一致性检查
- [X] 复用现有UI组件库
- [X] 交互符合既有模式
- [X] 错误信息用户友好

### IV. 性能要求检查
- [X] 明确性能预算
- [X] 识别性能瓶颈（Groovy反射开销）
- [X] 目标帧率可维持

---

## Project Structure

### Documentation
```
specs/003-groovy-mod-support/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # API contracts
└── tasks.md             # Phase 2 output
```

### Source Code
```
src/
├── base/src/main/java/.../base/mod/
│   ├── GroovyScriptEngine.java       # Groovy引擎管理
│   ├── ModManager.java               # Mod生命周期管理
│   ├── ModContainer.java             # Mod容器
│   ├── GroovySandbox.java            # 安全沙箱
│   └── api/
│       ├── ModAPI.java               # Mod API接口
│       └── GroovyExtension.java      # Groovy扩展基类
├── base/src/main/resources/
│   └── mods/                         # Mod目录
│       └── example-mod/
│           ├── mod.json              # Mod元数据
│           └── main.groovy           # Mod入口脚本
└── base/src/test/groovy/.../base/mod/
    └── GroovyModTest.groovy          # Groovy测试
```

---

## Implementation Phases

### Phase 0: Research
- Groovy 4.x JSR-223 脚本引擎研究
- SecurityManager沙箱最佳实践
- 现有游戏引擎Mod系统参考（Minecraft Forge, Stardew Valley SMAPI）

### Phase 1: Design
- Mod API设计
- 安全沙箱策略
- 文件结构和元数据格式

### Phase 2: Implementation
1. Groovy引擎集成
2. 安全沙箱实现
3. Mod管理器
4. API绑定
5. 示例Mod

### Phase 3: Documentation
- Mod开发指南
- API参考文档
- 安全最佳实践
