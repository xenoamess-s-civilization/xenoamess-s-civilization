/*
 * MIT License
 *
 * Copyright (c) 2025 XenoAmess
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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Container for mod metadata, scripts, and lifecycle state.
 * <p>
 * A ModContainer holds all information about a loaded mod including:
 * <ul>
 *   <li>Metadata from mod.json</li>
 *   <li>Loaded Groovy classes</li>
 *   <li>Lifecycle state</li>
 *   <li>Dependency information</li>
 * </ul>
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
@Slf4j
public class ModContainer {

    /**
     * JSON parser for mod metadata.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * The mod's metadata.
     */
    @Getter
    private final ModMetadata metadata;

    /**
     * The directory containing the mod files.
     */
    @Getter
    private final Path modDirectory;

    /**
     * Current lifecycle state of the mod.
     */
    @Getter
    private ModState state;

    /**
     * Loaded Groovy classes for this mod.
     */
    @Getter
    private final List<Class<?>> loadedClasses;

    /**
     * Instantiated mod objects (entry point instances).
     */
    @Getter
    private final Map<String, Object> instances;

    /**
     * Error message if the mod failed to load.
     */
    @Getter
    private String errorMessage;

    /**
     * Exception that caused the mod to fail.
     */
    @Getter
    private Throwable errorCause;

    /**
     * Whether the mod is currently enabled.
     */
    @Getter
    private boolean enabled;

    /**
     * Creates a new mod container.
     *
     * @param metadata the mod metadata
     * @param modDirectory the directory containing mod files
     */
    public ModContainer(ModMetadata metadata, Path modDirectory) {
        this.metadata = Objects.requireNonNull(metadata, "metadata cannot be null");
        this.modDirectory = Objects.requireNonNull(modDirectory, "modDirectory cannot be null");
        this.state = ModState.UNLOADED;
        this.loadedClasses = new ArrayList<>();
        this.instances = new HashMap<>();
        this.enabled = metadata.isEnabledByDefault();
    }

    /**
     * Loads a mod from a directory.
     *
     * @param modDirectory the directory containing mod.json
     * @return the loaded mod container
     * @throws ModLoadException if loading fails
     */
    public static ModContainer loadFromDirectory(Path modDirectory) throws ModLoadException {
        Path modJsonPath = modDirectory.resolve("mod.json");
        File modJsonFile = modJsonPath.toFile();

        if (!modJsonFile.exists()) {
            throw new ModLoadException("mod.json not found in: " + modDirectory);
        }

        try {
            ModMetadata metadata = OBJECT_MAPPER.readValue(modJsonFile, ModMetadata.class);
            metadata.validate();
            return new ModContainer(metadata, modDirectory);
        } catch (IOException e) {
            throw new ModLoadException("Failed to parse mod.json in: " + modDirectory, e);
        } catch (IllegalArgumentException e) {
            throw new ModLoadException("Invalid mod.json in: " + modDirectory + " - " + e.getMessage(), e);
        }
    }

    /**
     * Compiles and loads the mod's scripts using the provided script engine.
     *
     * @param scriptEngine the script engine to use
     * @throws ModLoadException if loading fails
     */
    public void load(GroovyScriptEngine scriptEngine) throws ModLoadException {
        if (state != ModState.UNLOADED) {
            throw new ModLoadException("Mod is not in UNLOADED state: " + metadata.getId());
        }

        setState(ModState.LOADING);
        loadedClasses.clear();

        try {
            // Load entry point classes
            for (String entryPoint : metadata.getEffectiveEntryPoints()) {
                loadEntryPoint(scriptEngine, entryPoint);
            }

            // Load additional scripts from scripts directory if it exists
            Path scriptsDir = modDirectory.resolve("scripts");
            if (scriptsDir.toFile().isDirectory()) {
                loadScriptsFromDirectory(scriptEngine, scriptsDir.toFile());
            }

            setState(ModState.LOADED);
            log.info("Mod loaded: {} v{}", metadata.getId(), metadata.getVersion());
        } catch (Exception e) {
            setError("Failed to load mod: " + metadata.getId(), e);
            throw new ModLoadException("Failed to load mod: " + metadata.getId(), e);
        }
    }

    /**
     * Loads a single entry point class.
     *
     * @param scriptEngine the script engine
     * @param className the fully qualified class name
     * @throws ModLoadException if loading fails
     */
    private void loadEntryPoint(GroovyScriptEngine scriptEngine, String className) throws ModLoadException {
        // Try to find the Groovy file
        String fileName = className.replace('.', '/') + ".groovy";
        Path scriptPath = modDirectory.resolve(fileName);

        if (!scriptPath.toFile().exists()) {
            // Try without package structure
            fileName = className.substring(className.lastIndexOf('.') + 1) + ".groovy";
            scriptPath = modDirectory.resolve(fileName);
        }

        if (!scriptPath.toFile().exists()) {
            throw new ModLoadException("Entry point not found: " + className + " (looked for " + fileName + ")");
        }

        try {
            Class<?> clazz = scriptEngine.loadClass(scriptPath.toFile());
            loadedClasses.add(clazz);
            log.debug("Loaded entry point class: {}", className);
        } catch (IOException | ScriptException e) {
            throw new ModLoadException("Failed to load entry point: " + className, e);
        }
    }

    /**
     * Loads all Groovy scripts from a directory.
     *
     * @param scriptEngine the script engine
     * @param directory the directory to scan
     */
    private void loadScriptsFromDirectory(GroovyScriptEngine scriptEngine, File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".groovy"));
        if (files == null) {
            return;
        }

        for (File file : files) {
            try {
                Class<?> clazz = scriptEngine.loadClass(file);
                loadedClasses.add(clazz);
                log.debug("Loaded script class: {}", file.getName());
            } catch (Exception e) {
                log.warn("Failed to load script: {}", file.getName(), e);
            }
        }
    }

    /**
     * Enables the mod by instantiating entry points.
     *
     * @throws ModLoadException if enabling fails
     */
    public void enable() throws ModLoadException {
        if (state != ModState.LOADED) {
            throw new ModLoadException("Mod must be in LOADED state to enable: " + metadata.getId());
        }

        try {
            for (Class<?> clazz : loadedClasses) {
                // Check if class has @Mod annotation
                com.xenoamess.cyan_potion.base.mod.annotation.Mod modAnnotation = 
                        clazz.getAnnotation(com.xenoamess.cyan_potion.base.mod.annotation.Mod.class);
                
                Object instance;
                if (modAnnotation != null) {
                    // Instantiate with default constructor
                    instance = clazz.getDeclaredConstructor().newInstance();
                    instances.put(clazz.getName(), instance);
                    
                    // Call init method if exists
                    try {
                        java.lang.reflect.Method initMethod = clazz.getMethod("init");
                        if (initMethod != null && initMethod.getReturnType() == void.class) {
                            initMethod.invoke(instance);
                        }
                    } catch (NoSuchMethodException e) {
                        // No init method, that's fine
                    }
                }
            }

            enabled = true;
            setState(ModState.ACTIVE);
            log.info("Mod enabled: {} v{}", metadata.getId(), metadata.getVersion());
        } catch (Exception e) {
            setError("Failed to enable mod: " + metadata.getId(), e);
            throw new ModLoadException("Failed to enable mod: " + metadata.getId(), e);
        }
    }

    /**
     * Disables the mod.
     */
    public void disable() {
        if (state != ModState.ACTIVE) {
            return;
        }

        // Call cleanup methods
        for (Map.Entry<String, Object> entry : instances.entrySet()) {
            try {
                Object instance = entry.getValue();
                java.lang.reflect.Method cleanupMethod = instance.getClass().getMethod("cleanup");
                if (cleanupMethod != null) {
                    cleanupMethod.invoke(instance);
                }
            } catch (NoSuchMethodException e) {
                // No cleanup method, that's fine
            } catch (Exception e) {
                log.warn("Error calling cleanup for mod: {}", metadata.getId(), e);
            }
        }

        instances.clear();
        enabled = false;
        setState(ModState.DISABLED);
        log.info("Mod disabled: {} v{}", metadata.getId(), metadata.getVersion());
    }

    /**
     * Unloads the mod and cleans up resources.
     */
    public void unload() {
        setState(ModState.UNLOADING);
        
        if (enabled) {
            disable();
        }

        loadedClasses.clear();
        instances.clear();
        setState(ModState.UNLOADED);
        log.info("Mod unloaded: {} v{}", metadata.getId(), metadata.getVersion());
    }

    /**
     * Sets the mod state.
     *
     * @param newState the new state
     */
    private void setState(ModState newState) {
        this.state = newState;
    }

    /**
     * Sets an error state.
     *
     * @param message the error message
     * @param cause the causing exception
     */
    private void setError(String message, Throwable cause) {
        this.errorMessage = message;
        this.errorCause = cause;
        this.state = ModState.ERROR;
        log.error(message, cause);
    }

    /**
     * Gets the mod ID.
     *
     * @return the mod ID
     */
    public String getId() {
        return metadata.getId();
    }

    /**
     * Gets the mod version.
     *
     * @return the version
     */
    public String getVersion() {
        return metadata.getVersion();
    }

    @Override
    public String toString() {
        return String.format("ModContainer[%s@%s, state=%s]", 
                metadata.getId(), metadata.getVersion(), state);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModContainer that = (ModContainer) o;
        return metadata.getId().equals(that.metadata.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata.getId());
    }
}
