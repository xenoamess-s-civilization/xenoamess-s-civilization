# UI Event Contract: 城市面板与控制人导航

## Event Flow

```
MapComponent ──onCitySelected(City)──► PersonBrowserDemo
                                            │
                                            ▼
                              cityPanelComponent.show(City)
                                            │
                                            ▼
CityPanelComponent ──onRulerClick(Person)──► PersonBrowserDemo
                                            │
                                            ▼
                              onPersonSelected(Person)
                                            │
                                            ▼
                              detailComponent.show(Person)
```

## Callback Interfaces

### MapComponent → Consumer<City>

```java
/**
 * 当玩家点击地图上的城市时触发。
 *
 * @param city 被点击的城市实例，不为 null
 */
Consumer<City> onCitySelected;
```

### CityPanelComponent → Consumer<Person>

```java
/**
 * 当玩家点击城市面板中的控制人区域时触发。
 *
 * @param person 被点击的控制人实例，不为 null
 */
Consumer<Person> onRulerClick;
```

## Preconditions

- `onCitySelected` 仅在玩家左键点击城市圆圈内时触发
- `onRulerClick` 仅在城市有控制人（`city.getRulerId() != null`）且玩家点击控制人区域时触发

## Postconditions

- 触发 `onCitySelected` 后，`CityPanelComponent` 必须进入可见状态并展示对应城市信息
- 触发 `onRulerClick` 后，必须导航到对应人物详情页面（由 `PersonBrowserDemo` 负责打开 `detailWindow`）
