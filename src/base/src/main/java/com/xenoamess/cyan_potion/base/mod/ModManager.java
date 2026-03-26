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

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Central manager for mod discovery, loading, and lifecycle management.
 * <p>
 * The ModManager is responsible for:
 * <ul>
 *   <li>Scanning the mod directory for mods</li>
 *   <li>Resolving and loading mod dependencies</li>
 *   <li>Managing mod lifecycle (load, enable, disable, unload)</li>
 *   <li>Providing access to loaded mods</li>
 *   <li>Error handling and recovery</li>
 * </ul>
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
@Slf4j
public class ModManager implements AutoCloseable {

    /**
     * The default mods directory name.
     */
    public static final String DEFAULT_MODS_DIR = "mods";

    /**
     * Map of loaded mods by ID.
     */
    @Getter
    private final Map<String, ModContainer> loadedMods;

    /**
     * The Groovy script engine for compiling and executing mod code.
     */
    @Getter
    private final GroovyScriptEngine scriptEngine;

    /**
     * The security sandbox for mod execution.
     */
    @Getter
    private final GroovySandbox sandbox;

    /**
     * The mods directory.
     */
    @Getter
    private Path modsDirectory;

    /**
     * Whether the manager has been initialized.
     */
    @Getter
    private boolean initialized;

    /**
     * Creates a new ModManager with default settings.
     */
    public ModManager() {
        this(Paths.get(DEFAULT_MODS_DIR));
    }

    /**
     * Creates a new ModManager with the specified mods directory.
     *
     * @param modsDirectory the directory to scan for mods
     */
    public ModManager(Path modsDirectory) {
        this.loadedMods = new ConcurrentHashMap<>();
        this.modsDirectory = Objects.requireNonNull(modsDirectory, "modsDirectory cannot be null");
        this.sandbox = new GroovySandbox(modsDirectory.toFile());
        this.scriptEngine = new GroovyScriptEngine(ModManager.class.getClassLoader(), sandbox);
        this.initialized = false;
    }

    /**
     * Initializes the mod manager.
     * Creates the mods directory if it doesn't exist.
     *
     * @throws IOException if initialization fails
     */
    public void initialize() throws IOException {
        if (initialized) {
            return;
        }

        // Create mods directory if it doesn't exist
        if (!Files.exists(modsDirectory)) {
            Files.createDirectories(modsDirectory);
            log.info("Created mods directory: {}", modsDirectory.toAbsolutePath());
        }

        initialized = true;
        log.info("ModManager initialized");
    }

    /**
     * Discovers all mods in the mods directory.
     *
     * @return list of discovered mod containers
     */
    public List<ModContainer> discoverMods() {
        if (!initialized) {
            throw new IllegalStateException("ModManager not initialized");
        }

        List<ModContainer> discovered = new ArrayList<>();

        try (Stream<Path> paths = Files.list(modsDirectory)) {
            paths.filter(Files::isDirectory)
                    .forEach(modDir -> {
                        try {
                            ModContainer mod = ModContainer.loadFromDirectory(modDir);
                            discovered.add(mod);
                        } catch (ModLoadException e) {
                            log.warn("Failed to discover mod in: {}", modDir, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Failed to scan mods directory: {}", modsDirectory, e);
        }

        log.info("Discovered {} mods", discovered.size());
        return discovered;
    }

    /**
     * Loads all discovered mods with dependency resolution.
     *
     * @throws ModLoadException if loading fails
     */
    public void loadAllMods() throws ModLoadException {
        List<ModContainer> discovered = discoverMods();
        loadMods(discovered);
    }

    /**
     * Loads mods with dependency resolution.
     *
     * @param mods the mods to load
     * @throws ModLoadException if loading fails
     */
    public void loadMods(List<ModContainer> mods) throws ModLoadException {
        // Sort by dependencies
        List<ModContainer> sorted = resolveLoadOrder(mods);

        for (ModContainer mod : sorted) {
            loadMod(mod);
        }
    }

    /**
     * Loads a single mod.
     *
     * @param mod the mod to load
     * @throws ModLoadException if loading fails
     */
    public void loadMod(ModContainer mod) throws ModLoadException {
        String modId = mod.getId();

        if (loadedMods.containsKey(modId)) {
            log.warn("Mod already loaded: {}", modId);
            return;
        }

        // Check dependencies
        for (String dependency : mod.getMetadata().getDependencies()) {
            String depId = parseDependencyId(dependency);
            if (!loadedMods.containsKey(depId)) {
                throw new ModLoadException("Missing dependency for mod " + modId + ": " + depId);
            }
        }

        // Load within sandbox
        sandbox.executeSandboxed(() -> {
            try {
                mod.load(scriptEngine);
            } catch (ModLoadException e) {
                throw new RuntimeException(e);
            }
        });

        loadedMods.put(modId, mod);
        log.info("Loaded mod: {} v{}", modId, mod.getVersion());
    }

    /**
     * Enables all loaded mods that are enabled by default.
     */
    public void enableDefaultMods() {
        List<ModContainer> toEnable = loadedMods.values().stream()
                .filter(m -> m.getMetadata().isEnabledByDefault())
                .collect(Collectors.toList());

        for (ModContainer mod : toEnable) {
            try {
                enableMod(mod.getId());
            } catch (ModLoadException e) {
                log.error("Failed to enable mod: {}", mod.getId(), e);
            }
        }
    }

    /**
     * Enables a mod by ID.
     *
     * @param modId the mod ID
     * @throws ModLoadException if enabling fails
     */
    public void enableMod(String modId) throws ModLoadException {
        ModContainer mod = loadedMods.get(modId);
        if (mod == null) {
            throw new ModLoadException("Mod not found: " + modId);
        }

        if (mod.getState() == ModState.ACTIVE) {
            return;
        }

        // Enable within sandbox
        sandbox.executeSandboxed(() -> {
            try {
                mod.enable();
            } catch (ModLoadException e) {
                throw new RuntimeException(e);
            }
        });

        // Move to top of load order (for event priority)
        // This is handled by the game loop/event system
    }

    /**
     * Disables a mod by ID.
     *
     * @param modId the mod ID
     */
    public void disableMod(String modId) {
        ModContainer mod = loadedMods.get(modId);
        if (mod == null) {
            log.warn("Cannot disable - mod not found: {}", modId);
            return;
        }

        sandbox.executeSandboxed(mod::disable);
    }

    /**
     * Unloads a mod by ID.
     *
     * @param modId the mod ID
     */
    public void unloadMod(String modId) {
        ModContainer mod = loadedMods.remove(modId);
        if (mod == null) {
            log.warn("Cannot unload - mod not found: {}", modId);
            return;
        }

        sandbox.executeSandboxed(mod::unload);
    }

    /**
     * Gets a loaded mod by ID.
     *
     * @param modId the mod ID
     * @return the mod container, or null if not found
     */
    public ModContainer getMod(String modId) {
        return loadedMods.get(modId);
    }

    /**
     * Gets all loaded mods.
     *
     * @return collection of loaded mods
     */
    public Collection<ModContainer> getAllMods() {
        return Collections.unmodifiableCollection(loadedMods.values());
    }

    /**
     * Gets all active mods.
     *
     * @return list of active mods
     */
    public List<ModContainer> getActiveMods() {
        return loadedMods.values().stream()
                .filter(m -> m.getState() == ModState.ACTIVE)
                .collect(Collectors.toList());
    }

    /**
     * Checks if a mod is loaded.
     *
     * @param modId the mod ID
     * @return true if loaded
     */
    public boolean isModLoaded(String modId) {
        return loadedMods.containsKey(modId);
    }

    /**
     * Checks if a mod is active.
     *
     * @param modId the mod ID
     * @return true if active
     */
    public boolean isModActive(String modId) {
        ModContainer mod = loadedMods.get(modId);
        return mod != null && mod.getState() == ModState.ACTIVE;
    }

    /**
     * Resolves the load order for mods based on dependencies.
     *
     * @param mods the mods to order
     * @return sorted list of mods
     * @throws ModLoadException if there's a circular dependency
     */
    public List<ModContainer> resolveLoadOrder(List<ModContainer> mods) throws ModLoadException {
        Map<String, ModContainer> modMap = mods.stream()
                .collect(Collectors.toMap(ModContainer::getId, m -> m));

        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();
        List<ModContainer> result = new ArrayList<>();

        for (ModContainer mod : mods) {
            if (!visited.contains(mod.getId())) {
                visitMod(mod, modMap, visited, visiting, result);
            }
        }

        return result;
    }

    /**
     * Visits a mod during topological sort.
     *
     * @param mod the current mod
     * @param modMap map of all mods
     * @param visited set of visited mod IDs
     * @param visiting set of currently visiting mod IDs (for cycle detection)
     * @param result the result list
     * @throws ModLoadException if circular dependency detected
     */
    private void visitMod(ModContainer mod, Map<String, ModContainer> modMap,
                         Set<String> visited, Set<String> visiting,
                         List<ModContainer> result) throws ModLoadException {
        String modId = mod.getId();

        if (visiting.contains(modId)) {
            throw new ModLoadException("Circular dependency detected involving mod: " + modId);
        }

        if (visited.contains(modId)) {
            return;
        }

        visiting.add(modId);

        // Visit dependencies first
        for (String dependency : mod.getMetadata().getDependencies()) {
            String depId = parseDependencyId(dependency);
            ModContainer depMod = modMap.get(depId);
            if (depMod != null) {
                visitMod(depMod, modMap, visited, visiting, result);
            }
        }

        visiting.remove(modId);
        visited.add(modId);
        result.add(mod);
    }

    /**
     * Parses a dependency string to extract the mod ID.
     *
     * @param dependency the dependency string (e.g., "modid@1.0.0")
     * @return the mod ID
     */
    private String parseDependencyId(String dependency) {
        int atIndex = dependency.indexOf('@');
        return atIndex >= 0 ? dependency.substring(0, atIndex) : dependency;
    }

    /**
     * Reloads a mod by unloading and loading it again.
     *
     * @param modId the mod ID
     * @throws ModLoadException if reload fails
     */
    public void reloadMod(String modId) throws ModLoadException {
        ModContainer oldMod = loadedMods.get(modId);
        if (oldMod == null) {
            throw new ModLoadException("Mod not loaded: " + modId);
        }

        Path modDir = oldMod.getModDirectory();
        boolean wasEnabled = oldMod.isEnabled();

        unloadMod(modId);

        ModContainer newMod = ModContainer.loadFromDirectory(modDir);
        loadMod(newMod);

        if (wasEnabled) {
            enableMod(modId);
        }

        log.info("Reloaded mod: {}", modId);
    }

    /**
     * Shuts down all mods and cleans up resources.
     */
    public void shutdown() {
        log.info("Shutting down ModManager");

        // Unload all mods
        List<String> modIds = new ArrayList<>(loadedMods.keySet());
        for (String modId : modIds) {
            unloadMod(modId);
        }

        // Clear the map
        loadedMods.clear();

        initialized = false;
        log.info("ModManager shutdown complete");
    }

    @Override
    public void close() {
        shutdown();
        if (scriptEngine != null) {
            scriptEngine.close();
        }
    }
}
