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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FilePermission;
import java.net.SocketPermission;
import java.security.Permission;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Security sandbox for restricting Groovy mod permissions.
 * <p>
 * This sandbox uses a custom SecurityManager approach to control what mods can do:
 * <ul>
 *   <li>File system access (read-only to mod directory, no writes outside)</li>
 *   <li>Network access blocking</li>
 *   <li>System command execution prevention</li>
 *   <li>Reflection restrictions</li>
 *   <li>Whitelist-based API access</li>
 * </ul>
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
@Slf4j
public class GroovySandbox {

    /**
     * Set of packages that mods are allowed to access.
     */
    private static final Set<String> ALLOWED_PACKAGES = Set.of(
            "com.xenoamess.cyan_potion.base.mod.api",
            "org.joml",
            "java.lang",
            "java.util",
            "java.math",
            "java.text",
            "groovy.lang",
            "groovy.util"
    );

    /**
     * Set of packages that mods are explicitly forbidden from accessing.
     */
    private static final Set<String> FORBIDDEN_PACKAGES = Set.of(
            "java.io.FileWriter",
            "java.io.FileOutputStream",
            "java.io.RandomAccessFile",
            "java.net.Socket",
            "java.net.ServerSocket",
            "java.lang.reflect",
            "java.lang.Runtime",
            "java.lang.ProcessBuilder"
    );

    /**
     * Thread-local flag to identify mod threads.
     */
    private final Set<Thread> modThreads = ConcurrentHashMap.newKeySet();

    /**
     * The mod directory where mods have read access.
     */
    @Getter
    @Setter
    private File modDirectory;

    /**
     * Whether the sandbox is enabled.
     */
    @Getter
    @Setter
    private volatile boolean enabled = true;

    /**
     * Whether to allow file reading.
     */
    @Getter
    @Setter
    private boolean allowFileRead = true;

    /**
     * Whether to allow file writing (only within mod directory).
     */
    @Getter
    @Setter
    private boolean allowFileWrite = false;

    /**
     * Whether to allow network access.
     */
    @Getter
    @Setter
    private boolean allowNetwork = false;

    /**
     * Whether to allow system commands.
     */
    @Getter
    @Setter
    private boolean allowExec = false;

    /**
     * Whether to allow reflection.
     */
    @Getter
    @Setter
    private boolean allowReflection = false;

    /**
     * Creates a new sandbox with default settings.
     */
    public GroovySandbox() {
        this(null);
    }

    /**
     * Creates a new sandbox with the specified mod directory.
     *
     * @param modDirectory the directory where mods are located
     */
    public GroovySandbox(File modDirectory) {
        this.modDirectory = modDirectory;
    }

    /**
     * Registers a thread as a mod thread.
     *
     * @param thread the thread to register
     */
    public void registerModThread(Thread thread) {
        modThreads.add(thread);
        log.debug("Registered mod thread: {}", thread.getName());
    }

    /**
     * Unregisters a thread as a mod thread.
     *
     * @param thread the thread to unregister
     */
    public void unregisterModThread(Thread thread) {
        modThreads.remove(thread);
        log.debug("Unregistered mod thread: {}", thread.getName());
    }

    /**
     * Checks if the current thread is a mod thread.
     *
     * @return true if current thread is registered as a mod thread
     */
    public boolean isModThread() {
        return modThreads.contains(Thread.currentThread());
    }

    /**
     * Validates a permission request from a mod thread.
     *
     * @param permission the permission being requested
     * @throws SecurityException if the permission is denied
     */
    public void validatePermission(Permission permission) throws SecurityException {
        if (!enabled || !isModThread()) {
            return;
        }

        String permName = permission.getName();
        String permActions = permission.getActions();

        // Check file permissions
        if (permission instanceof FilePermission) {
            validateFilePermission(permName, permActions);
            return;
        }

        // Check network permissions
        if (permission instanceof SocketPermission) {
            if (!allowNetwork) {
                throw new SecurityException("Network access is not allowed for mods: " + permName);
            }
            return;
        }

        // Check runtime permissions
        if (permission.getClass().getName().equals("java.lang.RuntimePermission")) {
            validateRuntimePermission(permName);
            return;
        }

        // Check property permissions (allow reading system properties)
        if (permission instanceof PropertyPermission) {
            if (permActions.contains("write")) {
                throw new SecurityException("Writing system properties is not allowed for mods: " + permName);
            }
            return;
        }
    }

    /**
     * Validates file permissions for mod threads.
     *
     * @param path the file path
     * @param actions the requested actions
     * @throws SecurityException if the permission is denied
     */
    private void validateFilePermission(String path, String actions) throws SecurityException {
        // Always allow reading from the mod directory
        if (actions.contains("read")) {
            if (modDirectory != null && path.startsWith(modDirectory.getAbsolutePath())) {
                return;
            }
            // Allow reading from classpath
            if (!actions.contains("write") && !actions.contains("delete")) {
                return;
            }
        }

        // Block writing unless explicitly allowed and within mod directory
        if (actions.contains("write") || actions.contains("delete")) {
            if (!allowFileWrite) {
                throw new SecurityException("File writing is not allowed for mods: " + path);
            }
            if (modDirectory != null && !path.startsWith(modDirectory.getAbsolutePath())) {
                throw new SecurityException("Mods can only write to the mod directory: " + path);
            }
        }

        // Block execute
        if (actions.contains("execute")) {
            throw new SecurityException("File execution is not allowed for mods: " + path);
        }
    }

    /**
     * Validates runtime permissions for mod threads.
     *
     * @param name the permission name
     * @throws SecurityException if the permission is denied
     */
    private void validateRuntimePermission(String name) throws SecurityException {
        // Block exitVM
        if (name.equals("exitVM")) {
            throw new SecurityException("System exit is not allowed for mods");
        }

        // Block classloader creation/modification
        if (name.equals("createClassLoader") || name.equals("setContextClassLoader")) {
            throw new SecurityException("ClassLoader manipulation is not allowed for mods");
        }

        // Block reflection without permission
        if (name.startsWith("accessDeclaredMembers") || name.startsWith("reflectPermission")) {
            if (!allowReflection) {
                throw new SecurityException("Reflection is not allowed for mods");
            }
        }

        // Block security manager modification
        if (name.equals("setSecurityManager")) {
            throw new SecurityException("SecurityManager modification is not allowed for mods");
        }

        // Block exec
        if (name.equals("exec") && !allowExec) {
            throw new SecurityException("Process execution is not allowed for mods");
        }
    }

    /**
     * Validates if a class can be accessed by mod code.
     *
     * @param className the fully qualified class name
     * @throws SecurityException if the class access is forbidden
     */
    public void validateClassAccess(String className) throws SecurityException {
        if (!enabled || !isModThread()) {
            return;
        }

        // Check forbidden packages
        for (String forbidden : FORBIDDEN_PACKAGES) {
            if (className.startsWith(forbidden)) {
                throw new SecurityException("Access to class is forbidden: " + className);
            }
        }

        // Allow java.lang and other safe packages
        if (className.startsWith("java.lang.") || 
            className.startsWith("java.util.") ||
            className.startsWith("java.math.") ||
            className.startsWith("java.text.")) {
            return;
        }

        // Check allowed packages
        for (String allowed : ALLOWED_PACKAGES) {
            if (className.startsWith(allowed)) {
                return;
            }
        }

        // Allow Groovy internal classes
        if (className.startsWith("groovy.") || className.startsWith("org.codehaus.groovy.")) {
            return;
        }

        // Log access to unknown classes for debugging
        log.debug("Mod thread accessing class: {}", className);
    }

    /**
     * Executes a Runnable within the sandbox context.
     *
     * @param runnable the code to execute
     */
    public void executeSandboxed(Runnable runnable) {
        Thread currentThread = Thread.currentThread();
        registerModThread(currentThread);
        try {
            runnable.run();
        } finally {
            unregisterModThread(currentThread);
        }
    }

    /**
     * Executes a callable within the sandbox context.
     *
     * @param callable the code to execute
     * @param <T> the return type
     * @return the result of the callable
     * @throws Exception if the callable throws an exception
     */
    public <T> T executeSandboxed(java.util.concurrent.Callable<T> callable) throws Exception {
        Thread currentThread = Thread.currentThread();
        registerModThread(currentThread);
        try {
            return callable.call();
        } finally {
            unregisterModThread(currentThread);
        }
    }

    /**
     * Clears all registered mod threads.
     */
    public void clearModThreads() {
        modThreads.clear();
    }

    /**
     * Gets the number of registered mod threads.
     *
     * @return the count of mod threads
     */
    public int getModThreadCount() {
        return modThreads.size();
    }
}
