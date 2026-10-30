# Quickstart: 城市面板与控制人导航

## 本地运行与验证步骤

### 1. 编译项目

```bash
cd src/xenoamess-s-civilization
mvn clean compile -DskipTests
```

### 2. 运行单元测试

```bash
mvn test
```

关键测试用例：
- `CityTest`: 验证 `rulerId` 的 getter/setter 及 `getRuler()` 解析逻辑
- `CityPanelComponentTest`: 验证面板 show/hide、控制人展示、点击回调

### 3. 启动游戏演示

```bash
mvn exec:java -Dexec.mainClass="com.xenoamess.cyan_potion.civilization.CivilizationGame"
```

或直接运行主类 `CivilizationGame`。

### 4. 功能验证清单

| 步骤 | 操作 | 预期结果 |
|------|------|----------|
| 1 | 点击主界面的"生成100人"按钮 | 地图上出现多个城市 |
| 2 | 点击任意城市 | 弹出"城市详情"窗口，显示城市名称、类型、人口、控制人 |
| 3 | 点击有控制人的城市中的控制人姓名 | 弹出"人物详情"窗口，显示该控制人的信息 |
| 4 | 点击另一座城市 | 城市详情窗口内容切换为新城市的信息 |
| 5 | 点击控制人，再点击同一控制人 | 不重复打开多个人物详情窗口（复用现有窗口） |
| 6 | 关闭城市详情窗口，再点击城市 | 窗口重新打开并显示最新信息 |

### 5. 代码规范检查

```bash
mvn p3c:p3c
```

确保新增代码零 ERROR 级别警告。

### 6. 覆盖率检查

```bash
mvn jacoco:report
```

查看 `target/site/jacoco/index.html`，确认新增代码覆盖率达到目标。
