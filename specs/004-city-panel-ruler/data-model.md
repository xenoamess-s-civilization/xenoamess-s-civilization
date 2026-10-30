# Data Model: 城市面板与控制人导航

## Entity: City (扩展)

**包**: `com.xenoamess.cyan_potion.civilization.map.City`

### 新增字段

| 字段 | 类型 | 说明 | 约束 |
|------|------|------|------|
| `rulerId` | `String` | 城市控制人（统治者）的人物 ID | 可为 `null`，表示无主 |

### 新增方法

```java
/**
 * 获取城市的控制人 ID。
 *
 * @return 控制人 ID，若城市无主则返回 null
 */
public String getRulerId();

/**
 * 设置城市的控制人 ID。
 *
 * @param rulerId 控制人 ID，可为 null
 */
public void setRulerId(String rulerId);

/**
 * 获取城市的控制人对象。
 *
 * @return 控制人 Person 实例，若未找到或 rulerId 为 null 则返回 null
 */
public Person getRuler();
```

### 不变性约束
- `rulerId` 若不为 null，必须在 `PersonCache` 中能解析出对应的 `Person`
- 修改 `rulerId` 时无需级联修改 `Person`（单向关联，简化一致性管理）

---

## Entity: CityPanelComponent (新增)

**包**: `com.xenoamess.cyan_potion.civilization.gui.CityPanelComponent`

**继承**: `AbstractControllableGameWindowComponent`

### 职责
- 渲染城市面板的背景、标题、基础统计、控制人区域
- 处理控制人区域的鼠标悬停和点击事件
- 向外部通知控制人被点击的事件

### 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `city` | `City` | 当前展示的城市 |
| `rulerButton` | `Button` | 控制人 clickable 区域 |
| `onRulerClick` | `Consumer<Person>` | 控制人被点击时的回调 |
| `backgroundPicture` | `Picture` | 面板背景 |
| `show` | `boolean` | 是否可见 |

### 公共方法

```java
/**
 * 展示指定城市的面板信息。
 *
 * @param city 要展示的城市，不能为 null
 */
public void show(@NotNull City city);

/**
 * 隐藏面板。
 */
public void hide();

/**
 * 设置控制人被点击时的回调。
 *
 * @param onRulerClick 回调函数，接收被点击的控制人
 */
public void setOnRulerClick(Consumer<Person> onRulerClick);
```

---

## Entity: CityRulerComponent (可选子组件)

**包**: `com.xenoamess.cyan_potion.civilization.gui.CityRulerComponent`

**继承**: `AbstractControllableGameWindowComponent`

### 职责
- 封装控制人头像+姓名的展示块
- 提供悬停高亮和点击反馈

### 字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `person` | `Person` | 当前展示的控制人 |
| `onClick` | `Consumer<Person>` | 点击回调 |
| `hovered` | `boolean` | 鼠标是否悬停 |

---

## Entity: PersonBrowserDemo (变更)

**包**: `com.xenoamess.cyan_potion.civilization.gui.PersonBrowserDemo`

### 新增字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `cityPanelComponent` | `CityPanelComponent` | 城市面板主体 |
| `cityPanelWindow` | `DraggableWindowComponent` | 城市面板的窗口外壳 |

### 行为变更
- `MapComponent.onCitySelected` 回调绑定到打开 `cityPanelWindow` 并调用 `cityPanelComponent.show(city)`
- `CityPanelComponent.onRulerClick` 回调绑定到 `this::onPersonSelected`，复用现有人物详情跳转

---

## 状态转换图

```
MapComponent (玩家点击城市)
    │
    ▼
cityPanelWindow.setVisible(true)
cityPanelComponent.show(city)
    │
    ├── city.getRulerId() == null
    │       └── 显示"无主"，rulerButton 禁用/隐藏
    │
    └── city.getRulerId() != null
            └── rulerButton 显示控制人姓名
                    │
                    ▼ (玩家点击控制人)
            onRulerClick.accept(ruler)
                    │
                    ▼
            PersonBrowserDemo.onPersonSelected(ruler)
                    │
                    ▼
            detailWindow.setVisible(true)
            detailComponent.show(ruler)
```
