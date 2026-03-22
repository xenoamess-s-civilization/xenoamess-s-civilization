# Mod API Contract: Groovy Mod Support

**Feature**: Groovy Mod Support  
**Date**: 2026-03-23

---

## API Overview

Mod API提供给Groovy脚本访问引擎核心功能的接口。所有API都经过安全沙箱过滤，仅暴露安全操作。

---

## GroovyExtension Base Class

所有Groovy Mod必须继承此类。

```groovy
abstract class GroovyExtension {
    
    /**
     * Called when mod is initialized.
     * @param gameManager the game manager instance
     */
    abstract void onInitialize(GameManager gameManager)
    
    /**
     * Called every frame.
     * @param deltaTime time since last frame in seconds
     */
    void onUpdate(float deltaTime) {}
    
    /**
     * Called when mod is shutdown.
     */
    void onShutdown() {}
    
    /**
     * Register an event handler.
     * @param handler event handler closure
     */
    void registerEventHandler(Closure<Event> handler)
    
    /**
     * Register a keyboard event handler.
     * @param handler keyboard event handler closure
     */
    void registerKeyboardHandler(Closure<KeyboardEvent> handler)
    
    /**
     * Add a component to the game window.
     * @param component the component to add
     */
    void addToGameWindow(AbstractGameWindowComponent component)
    
    /**
     * Get the mod logger.
     */
    Logger getLogger()
}
```

---

## @Mod Annotation

标记Groovy类为Mod入口点。

```groovy
@Mod(
    id = "mod-id",           // Required: unique mod identifier
    version = "1.0.0",       // Required: mod version
    name = "Mod Name",       // Optional: display name
    description = "..."      // Optional: description
)
class MyMod extends GroovyExtension {
    // ...
}
```

---

## GameManager API

暴露给Mod的GameManager方法。

```java
interface ModGameManagerAPI {
    
    /**
     * Get the game window.
     */
    GameWindow getGameWindow()
    
    /**
     * Get the resource manager.
     */
    ResourceManager getResourceManager()
    
    /**
     * Get current FPS.
     */
    int getCurrentFPS()
    
    /**
     * Get game version.
     */
    String getGameVersion()
    
    /**
     * Check if game is paused.
     */
    boolean isGamePaused()
}
```

---

## Event System API

注册和处理游戏事件。

```groovy
// Register event handler
registerEventHandler { Event event ->
    // Handle any event
    return event  // Return null to consume event
}

// Register specific event type
registerEventHandler(KeyboardEvent.class) { KeyboardEvent event ->
    if (event.key == 'ESC') {
        println "ESC pressed!"
    }
    return event
}
```

### Available Event Types

- `KeyboardEvent` - Keyboard input
- `MouseButtonEvent` - Mouse button input
- `MouseMoveEvent` - Mouse movement
- `WindowResizeEvent` - Window resize

---

## UI Component API

创建和操控UI组件。

```groovy
// Create a button
Button button = new Button(gameWindow, "Click Me")
button.setLeftTopPos(100, 100)
button.setSize(120, 40)
button.registerOnMouseButtonLeftDownCallback { event ->
    println "Button clicked!"
    return null
}
addToGameWindow(button)

// Create a label
Label label = new Label(gameWindow)
label.setText("Hello Groovy!")
label.setLeftTopPos(100, 150)
label.setSize(200, 30)
addToGameWindow(label)

// Create a panel
Panel panel = new Panel(gameWindow)
panel.setLeftTopPos(50, 50)
panel.setSize(300, 200)
addToGameWindow(panel)
```

---

## Logging API

Mod日志记录。

```groovy
// Using mod logger
getLogger().info("This is an info message")
getLogger().warn("This is a warning")
getLogger().error("This is an error")

// Shortcut methods
log.info("Info message")
log.debug("Debug: ${someVariable}")
```

---

## Resource API

加载和使用游戏资源。

```groovy
// Load texture
def texture = resourceManager.fetchResource(
    Texture.class,
    "textures/myTexture.png"
)

// Load sound
def sound = resourceManager.fetchResource(
    Sound.class,
    "sounds/effect.wav"
)
```

---

## Configuration API

读取Mod配置。

```groovy
// Read mod config
def config = getModConfig()
def enabled = config.getBoolean("enabled", true)
def maxCount = config.getInteger("maxCount", 100)
def name = config.getString("name", "Default")
```

---

## Security Restrictions

以下操作在Groovy Mod中被禁止：

### Blocked Operations

- `System.exit()` - Cannot terminate JVM
- `Runtime.exec()` - Cannot execute system commands
- `java.io.File` outside mods directory - Cannot access file system
- `java.net.Socket/URL` - Cannot make network requests
- `java.lang.reflect` - Cannot use reflection
- `sun.misc.Unsafe` - Cannot use unsafe operations

### Allowed Operations

- Read files in `mods/your-mod/` directory
- Write to `mods/your-mod/data/` directory
- Access exposed engine APIs
- Create UI components
- Handle game events

---

## Version Compatibility

Mod API版本管理。

```json
// mod.json
{
    "minEngineVersion": "0.167.4",
    "maxEngineVersion": "0.168.0"
}
```

API版本检查：
```groovy
// Check engine version
def engineVersion = gameManager.getGameVersion()
if (engineVersion < "0.167.4") {
    log.error("This mod requires engine 0.167.4 or higher")
    return
}
```

---

## Error Handling

Groovy异常处理。

```groovy
class MyMod extends GroovyExtension {
    
    @Override
    void onInitialize(GameManager gameManager) {
        try {
            // Risky operation
            riskyOperation()
        } catch (ModSecurityException e) {
            log.error("Security violation: ${e.message}")
        } catch (Exception e) {
            log.error("Mod initialization failed: ${e.message}")
            e.printStackTrace()
        }
    }
}
```

---

## Best Practices

1. **Use strong typing** for better performance:
   ```groovy
   // Good
   String name = "value"
   int count = 10
   
   // Avoid
   def name = "value"
   def count = 10
   ```

2. **Clean up resources** in `onShutdown()`:
   ```groovy
   @Override
   void onShutdown() {
       customComponent?.close()
       log.info("Mod cleaned up")
   }
   ```

3. **Check for null** before accessing:
   ```groovy
   if (gameManager?.getGameWindow() != null) {
       // Safe to use
   }
   ```

4. **Use `@CompileStatic`** for performance-critical code:
   ```groovy
   import groovy.transform.CompileStatic
   
   @CompileStatic
   void heavyCalculation() {
       // This method will be statically compiled
   }
   ```

---

## Examples

See `mods/examples/` directory for complete examples:

- `hello-world/` - Basic mod structure
- `event-handler/` - Event handling
- `custom-ui/` - UI components
- `data-storage/` - Configuration and data
