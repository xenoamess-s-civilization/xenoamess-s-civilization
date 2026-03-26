/*
 * MIT License
 *
 * Copyright (c) 2020 XenoAmess
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.xenoamess.cyan_potion.base.mod;

import com.xenoamess.cyan_potion.base.GameManager;
import com.xenoamess.cyan_potion.base.events.Event;
import com.xenoamess.cyan_potion.base.game_window_components.AbstractGameWindowComponent;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Base class for Groovy mods.
 * <p>
 * All Groovy mods must extend this class and implement the required methods.
 * Provides hooks for initialization, update, and shutdown.
 * </p>
 *
 * <p>
 * Example usage in Groovy:
 * <pre>
 * {@code
 * import com.xenoamess.cyan_potion.base.mod.GroovyExtension
 * import com.xenoamess.cyan_potion.base.mod.annotation.Mod
 *
 * @Mod(id = "my-mod", version = "1.0.0")
 * class MyMod extends GroovyExtension {
 *     @Override
 *     void onInitialize(GameManager gameManager) {
 *         log.info("My mod initialized!")
 *     }
 *
 *     @Override
 *     void onUpdate(float deltaTime) {
 *         // Update logic
 *     }
 * }
 * }
 * </pre>
 * </p>
 *
 * @author XenoAmess
 * @version 0.167.4
 * @since 2026-03-23
 */
@Slf4j
public abstract class GroovyExtension {

    /**
     * Reference to the game manager.
     */
    @Getter
    @Setter
    protected GameManager gameManager;

    /**
     * Reference to the mod container.
     */
    @Getter
    @Setter
    protected ModContainer modContainer;

    /**
     * List of registered event handlers.
     */
    private final List<Function<Event, Event>> eventHandlers = new ArrayList<>();

    /**
     * Logger for this mod.
     */
    @Getter
    protected Logger modLogger = log;

    /**
     * Called when the mod is initialized.
     * <p>
     * Override this method to perform initialization logic.
     * </p>
     *
     * @param gameManager the game manager instance
     */
    public abstract void onInitialize(GameManager gameManager);

    /**
     * Called every frame during the update cycle.
     * <p>
     * Override this method to perform per-frame updates.
     * Default implementation does nothing.
     * </p>
     *
     * @param deltaTime time elapsed since last frame in seconds
     */
    public void onUpdate(float deltaTime) {
        // Default: do nothing
    }

    /**
     * Called when the mod is being shut down.
     * <p>
     * Override this method to perform cleanup.
     * Default implementation does nothing.
     * </p>
     */
    public void onShutdown() {
        // Default: do nothing
    }

    /**
     * Registers an event handler.
     * <p>
     * The handler should return the event (possibly modified) or null to consume it.
     * </p>
     *
     * @param handler the event handler function
     */
    public void registerEventHandler(Function<Event, Event> handler) {
        eventHandlers.add(handler);
        log.debug("Registered event handler for mod: {}", 
            modContainer != null ? modContainer.getId() : "unknown");
    }

    /**
     * Processes an event through all registered handlers.
     *
     * @param event the event to process
     * @return the processed event or null if consumed
     */
    public Event processEvent(Event event) {
        Event currentEvent = event;
        for (Function<Event, Event> handler : eventHandlers) {
            if (currentEvent == null) {
                break;
            }
            try {
                currentEvent = handler.apply(currentEvent);
            } catch (Exception e) {
                log.error("Event handler threw exception in mod: {}",
                    modContainer != null ? modContainer.getId() : "unknown", e);
            }
        }
        return currentEvent;
    }

    /**
     * Adds a component to the game window.
     *
     * @param component the component to add
     */
    public void addToGameWindow(AbstractGameWindowComponent component) {
        if (gameManager != null && gameManager.getGameWindowComponentTree() != null) {
            component.addToGameWindowComponentTree(
                gameManager.getGameWindowComponentTree().getRoot()
            );
            log.debug("Added component to game window for mod: {}",
                modContainer != null ? modContainer.getId() : "unknown");
        } else {
            log.warn("Cannot add component - game manager or component tree not available");
        }
    }

    /**
     * Gets the mod logger.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return modLogger;
    }

    /**
     * Logs an info message.
     *
     * @param message the message
     */
    public void info(String message) {
        modLogger.info("[{}] {}", modContainer != null ? modContainer.getId() : "?", message);
    }

    /**
     * Logs a debug message.
     *
     * @param message the message
     */
    public void debug(String message) {
        modLogger.debug("[{}] {}", modContainer != null ? modContainer.getId() : "?", message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message
     */
    public void warn(String message) {
        modLogger.warn("[{}] {}", modContainer != null ? modContainer.getId() : "?", message);
    }

    /**
     * Logs an error message.
     *
     * @param message the message
     */
    public void error(String message) {
        modLogger.error("[{}] {}", modContainer != null ? modContainer.getId() : "?", message);
    }

    /**
     * Gets the mod configuration directory.
     *
     * @return path to mod config directory
     */
    public java.nio.file.Path getConfigDirectory() {
        if (modContainer != null && modContainer.getModDirectory() != null) {
            return modContainer.getModDirectory().resolve("config");
        }
        return null;
    }

    /**
     * Gets the mod data directory.
     *
     * @return path to mod data directory
     */
    public java.nio.file.Path getDataDirectory() {
        if (modContainer != null && modContainer.getModDirectory() != null) {
            return modContainer.getModDirectory().resolve("data");
        }
        return null;
    }
}
