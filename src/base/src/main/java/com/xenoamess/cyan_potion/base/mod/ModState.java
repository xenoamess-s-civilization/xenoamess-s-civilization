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

/**
 * Enumeration of possible states for a mod during its lifecycle.
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
public enum ModState {

    /**
     * Initial state - mod is registered but not loaded.
     */
    UNLOADED,

    /**
     * Mod is currently loading (parsing metadata, compiling scripts).
     */
    LOADING,

    /**
     * Mod is loaded but not yet enabled.
     */
    LOADED,

    /**
     * Mod is active and running.
     */
    ACTIVE,

    /**
     * Mod is disabled by user or system.
     */
    DISABLED,

    /**
     * Mod encountered an error during loading or execution.
     */
    ERROR,

    /**
     * Mod is being unloaded/cleaned up.
     */
    UNLOADING
}
