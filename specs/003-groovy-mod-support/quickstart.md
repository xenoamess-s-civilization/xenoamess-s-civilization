# Quick Start: Groovy Mod Development

**Feature**: Groovy Mod Support  
**预计时间**: 20分钟  
**日期**: 2026-03-23

---

## 目标

在本指南结束时，你将：
1. 搭建Groovy Mod开发环境
2. 创建并运行第一个Groovy Mod
3. 理解Mod开发的基本模式

---

## 前置要求

- 已安装Cyan Potion引擎
- 基本Java/Groovy编程知识
- 文本编辑器或IDE（推荐IntelliJ IDEA）

---

## 步骤 1: 创建Mod目录结构 (5分钟)

### 1.1 创建Mod文件夹

在引擎的 `mods/` 目录下创建你的Mod文件夹：

```
mods/
└── my-first-mod/
    ├── mod.json          # Mod元数据
    └── main.groovy       # Mod入口脚本
```

### 1.2 编写 mod.json

```json
{
    "id": "my-first-mod",
    "name": "My First Mod",
    "version": "1.0.0",
    "author": "Your Name",
    "description": "A simple hello world mod",
    "entryPoint": "main.groovy",
    "minEngineVersion": "0.167.4",
    "dependencies": []
}
```

---

## 步骤 2: 编写第一个Groovy Mod (10分钟)

### 2.1 创建 main.groovy

```groovy
import com.xenoamess.cyan_potion.base.GameManager
import com.xenoamess.cyan_potion.base.events.Event
import com.xenoamess.cyan_potion.base.mod.GroovyExtension
import com.xenoamess.cyan_potion.base.mod.annotation.Mod

/**
 * My First Mod - Hello World
 */
@Mod(id = "my-first-mod", version = "1.0.0")
class MyFirstMod extends GroovyExtension {
    
    @Override
    void onInitialize(GameManager gameManager) {
        println "Hello from Groovy Mod!"
        
        // 注册事件处理器
        registerEventHandler { Event event ->
            // 处理事件
            return event
        }
    }
    
    @Override
    void onUpdate(float deltaTime) {
        // 每帧更新
    }
    
    @Override
    void onShutdown() {
        println "Goodbye from Groovy Mod!"
    }
}
```

### 2.2 添加自定义功能

```groovy
    @Override
    void onInitialize(GameManager gameManager) {
        println "Mod initialized!"
        
        // 访问游戏窗口
        def window = gameManager.getGameWindow()
        def width = window.getWidth()
        def height = window.getHeight()
        
        println "Game window size: ${width}x${height}"
        
        // 注册键盘事件
        registerKeyboardHandler { keyEvent ->
            if (keyEvent.key == 'H' && keyEvent.pressed) {
                println "Hello key pressed!"
            }
            return keyEvent
        }
    }
```

---

## 步骤 3: 运行Mod (5分钟)

### 3.1 启动游戏

正常启动Cyan Potion引擎游戏，Mod会自动加载。

### 3.2 查看Mod加载日志

在控制台输出中查找：

```
[INFO] Loading mod: my-first-mod v1.0.0
[INFO] Hello from Groovy Mod!
[INFO] Mod my-first-mod loaded successfully
```

### 3.3 使用Mod管理器

在游戏中按 `F10` 打开Mod管理器，可以：
- 查看已加载的Mod列表
- 启用/禁用Mod
- 查看Mod详情

---

## 示例Mod: 自定义UI组件

```groovy
import com.xenoamess.cyan_potion.base.GameManager
import com.xenoamess.cyan_potion.base.game_window_components.controllable_game_window_components.Label
import com.xenoamess.cyan_potion.base.mod.GroovyExtension
import com.xenoamess.cyan_potion.base.mod.annotation.Mod

@Mod(id = "custom-ui-mod", version = "1.0.0")
class CustomUIMod extends GroovyExtension {
    
    private Label customLabel
    
    @Override
    void onInitialize(GameManager gameManager) {
        // 创建自定义标签
        customLabel = new Label(gameManager.getGameWindow())
        customLabel.setText("Powered by Groovy!")
        customLabel.setLeftTopPos(100, 100)
        customLabel.setSize(200, 30)
        
        // 添加到游戏窗口
        addToGameWindow(customLabel)
    }
    
    @Override
    void onUpdate(float deltaTime) {
        // 动态更新文本
        def time = System.currentTimeMillis() / 1000
        customLabel.setText("Time: ${time.intValue()}")
    }
}
```

---

## 下一步

### 深入学习

1. **Mod API参考** - 查看 `docs/api/mod/` 了解完整API
2. **事件系统** - 学习如何处理游戏事件
3. **UI开发** - 创建复杂的Mod界面
4. **数据存储** - 学习Mod配置和存档

### 示例Mod

查看 `mods/examples/` 目录中的官方示例：
- `hello-world/` - 基础示例
- `event-handler/` - 事件处理示例
- `custom-ui/` - UI组件示例
- `data-storage/` - 数据存储示例

### 社区资源

- GitHub: https://github.com/xenoamess/cyan_potion
- Mod开发论坛: [链接]
- API文档: [链接]

---

## 故障排除

### Mod未加载

**检查清单**:
- [ ] `mod.json` 格式正确
- [ ] 文件位于 `mods/your-mod/` 目录
- [ ] `entryPoint` 指向正确的 `.groovy` 文件

### Groovy编译错误

**诊断**:
```bash
# 查看详细错误日志
tail -f logs/mod-loader.log
```

**常见问题**:
- 语法错误：检查Groovy语法（类似Java）
- 类找不到：确认import语句正确
- 方法不存在：检查API版本兼容性

### 安全问题

**沙箱限制**:
- 不能访问文件系统（除mods目录）
- 不能执行系统命令
- 不能进行网络请求

如需扩展权限，请在 `mod.json` 中声明：
```json
{
    "permissions": ["file.read", "network.local"]
}
```

---

## 恭喜！

你已经创建了第一个Groovy Mod！🎉

现在你可以：
- 探索更多Mod API功能
- 创建复杂的Mod逻辑
- 分享给其他玩家

遇到问题？查看完整文档或提交Issue获取帮助。
