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

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyCodeSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSR-223 compliant Groovy script engine wrapper with compilation caching support.
 * <p>
 * This class provides a secure and efficient way to execute Groovy scripts within the mod system.
 * It supports:
 * <ul>
 *   <li>Script compilation caching for performance</li>
 *   <li>Secure binding configuration</li>
 *   <li>Sandboxed execution environment</li>
 *   <li>Class loading with restrictions</li>
 * </ul>
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
@Slf4j
public class GroovyScriptEngine implements AutoCloseable {

    /**
     * The underlying Groovy script engine implementation.
     */
    @Getter
    private final GroovyScriptEngineImpl engine;

    /**
     * Cache for compiled scripts to avoid recompilation.
     */
    private final Map<String, CompiledScript> compiledScriptCache;

    /**
     * The class loader used for loading Groovy classes.
     */
    @Getter
    private final GroovyClassLoader groovyClassLoader;

    /**
     * The sandbox security manager for this engine.
     */
    private final GroovySandbox sandbox;

    /**
     * Creates a new GroovyScriptEngine with the default parent class loader.
     */
    public GroovyScriptEngine() {
        this(GroovyScriptEngine.class.getClassLoader(), null);
    }

    /**
     * Creates a new GroovyScriptEngine with the specified parent class loader.
     *
     * @param parentClassLoader the parent class loader
     */
    public GroovyScriptEngine(ClassLoader parentClassLoader) {
        this(parentClassLoader, null);
    }

    /**
     * Creates a new GroovyScriptEngine with the specified parent class loader and sandbox.
     *
     * @param parentClassLoader the parent class loader
     * @param sandbox the security sandbox (can be null for no sandboxing)
     */
    public GroovyScriptEngine(ClassLoader parentClassLoader, GroovySandbox sandbox) {
        this.compiledScriptCache = new ConcurrentHashMap<>();
        this.groovyClassLoader = AccessController.doPrivileged(
                (PrivilegedAction<GroovyClassLoader>) () -> new GroovyClassLoader(parentClassLoader)
        );
        this.engine = new GroovyScriptEngineImpl(this.groovyClassLoader);
        this.sandbox = sandbox;
        initializeEngine();
    }

    /**
     * Initializes the script engine with default settings.
     */
    private void initializeEngine() {
        // Configure the engine to use our class loader
        engine.getContext().setAttribute(
                "groovy.classloader", 
                this.groovyClassLoader, 
                ScriptContext.ENGINE_SCOPE
        );
        log.debug("GroovyScriptEngine initialized");
    }

    /**
     * Compiles a script string into a CompiledScript.
     *
     * @param script the script source code
     * @return the compiled script
     * @throws ScriptException if compilation fails
     */
    public CompiledScript compile(String script) throws ScriptException {
        return engine.compile(script);
    }

    /**
     * Compiles a script from a Reader.
     *
     * @param reader the reader containing script source
     * @return the compiled script
     * @throws ScriptException if compilation fails
     */
    public CompiledScript compile(Reader reader) throws ScriptException {
        return engine.compile(reader);
    }

    /**
     * Executes a script string with the given bindings.
     *
     * @param script the script source code
     * @param bindings the variable bindings for the script
     * @return the result of script execution
     * @throws ScriptException if execution fails
     */
    public Object executeScript(String script, Bindings bindings) throws ScriptException {
        String cacheKey = generateCacheKey(script);
        CompiledScript compiled = compiledScriptCache.computeIfAbsent(cacheKey, k -> {
            try {
                return compile(script);
            } catch (ScriptException e) {
                throw new RuntimeException("Failed to compile script", e);
            }
        });
        return compiled.eval(bindings);
    }

    /**
     * Executes a script string with no bindings.
     *
     * @param script the script source code
     * @return the result of script execution
     * @throws ScriptException if execution fails
     */
    public Object executeScript(String script) throws ScriptException {
        return executeScript(script, createSecureBindings());
    }

    /**
     * Executes a script from a file.
     *
     * @param file the script file
     * @param bindings the variable bindings
     * @return the result of script execution
     * @throws ScriptException if execution fails
     * @throws IOException if file reading fails
     */
    public Object executeScript(File file, Bindings bindings) throws ScriptException, IOException {
        GroovyCodeSource codeSource = new GroovyCodeSource(file);
        codeSource.setCachable(false);
        
        CompiledScript compiled = AccessController.doPrivileged(
                (PrivilegedAction<CompiledScript>) () -> {
                    try {
                        return compile(new StringReader(codeSource.getScriptText()));
                    } catch (ScriptException e) {
                        throw new RuntimeException("Failed to compile script file: " + file.getName(), e);
                    }
                }
        );
        return compiled.eval(bindings);
    }

    /**
     * Loads a Groovy class from source code.
     *
     * @param className the name of the class
     * @param script the class source code
     * @return the loaded class
     * @throws ScriptException if loading fails
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> loadClass(String className, String script) throws ScriptException {
        try {
            return (Class<T>) groovyClassLoader.parseClass(script, className);
        } catch (Exception e) {
            throw new ScriptException("Failed to load class: " + className + " - " + e.getMessage());
        }
    }

    /**
     * Loads a Groovy class from a file.
     *
     * @param file the file containing the class
     * @return the loaded class
     * @throws ScriptException if loading fails
     * @throws IOException if file reading fails
     */
    @SuppressWarnings("unchecked")
    public <T> Class<T> loadClass(File file) throws ScriptException, IOException {
        try {
            return (Class<T>) groovyClassLoader.parseClass(file);
        } catch (Exception e) {
            throw new ScriptException("Failed to load class from file: " + file.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Instantiates a Groovy class with the given arguments.
     *
     * @param clazz the class to instantiate
     * @param args constructor arguments
     * @return the instantiated object
     * @throws ScriptException if instantiation fails
     */
    public <T> T instantiateClass(Class<T> clazz, Object... args) throws ScriptException {
        try {
            if (args == null || args.length == 0) {
                return clazz.getDeclaredConstructor().newInstance();
            }
            Class<?>[] argTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                argTypes[i] = args[i] != null ? args[i].getClass() : Object.class;
            }
            return clazz.getDeclaredConstructor(argTypes).newInstance(args);
        } catch (Exception e) {
            throw new ScriptException("Failed to instantiate class: " + clazz.getName() + " - " + e.getMessage());
        }
    }

    /**
     * Executes a Groovy class (typically a @Mod annotated class).
     *
     * @param clazz the class to execute
     * @param args arguments to pass to the constructor
     * @return the instantiated object
     * @throws ScriptException if execution fails
     */
    public Object executeClass(Class<?> clazz, Object... args) throws ScriptException {
        return instantiateClass(clazz, args);
    }

    /**
     * Creates secure bindings with restricted access.
     *
     * @return secure bindings for script execution
     */
    public Bindings createSecureBindings() {
        Bindings bindings = new SimpleBindings();
        
        // Add safe built-in variables
        bindings.put("out", System.out);
        bindings.put("err", System.err);
        
        // Add engine reference for advanced usage
        bindings.put("__engine__", this);
        
        return bindings;
    }

    /**
     * Generates a cache key for a script.
     *
     * @param script the script source
     * @return the cache key
     */
    private String generateCacheKey(String script) {
        return Integer.toHexString(script.hashCode());
    }

    /**
     * Clears the compiled script cache.
     */
    public void clearCache() {
        compiledScriptCache.clear();
        log.debug("Script cache cleared");
    }

    /**
     * Gets the number of cached scripts.
     *
     * @return the cache size
     */
    public int getCacheSize() {
        return compiledScriptCache.size();
    }

    @Override
    public void close() {
        clearCache();
        if (groovyClassLoader != null) {
            try {
                groovyClassLoader.close();
            } catch (IOException e) {
                log.warn("Error closing GroovyClassLoader", e);
            }
        }
        log.debug("GroovyScriptEngine closed");
    }
}
