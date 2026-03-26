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

package com.xenoamess.cyan_potion.base.mod.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for marking Groovy classes as Mod entry points.
 * <p>
 * This annotation should be applied to the main class of a Groovy mod.
 * The annotated class will be instantiated and managed by the mod system.
 * <p>
 * Example usage in Groovy:
 * <pre>
 * &#64;Mod(id = "mymod", version = "1.0.0", name = "My Mod")
 * class MyMod {
 *     void init() {
 *         // Called when mod is enabled
 *     }
 *     
 *     void cleanup() {
 *         // Called when mod is disabled
 *     }
 * }
 * </pre>
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Mod {

    /**
     * The unique identifier for this mod.
     * <p>
     * Must start with a lowercase letter and contain only lowercase letters,
     * numbers, hyphens, and underscores.
     *
     * @return the mod ID
     */
    String id();

    /**
     * The version of this mod.
     * <p>
     * Semantic versioning (e.g., "1.0.0") is recommended.
     *
     * @return the mod version
     */
    String version();

    /**
     * The human-readable name of this mod.
     *
     * @return the mod name
     */
    String name() default "";

    /**
     * A description of this mod.
     *
     * @return the mod description
     */
    String description() default "";

    /**
     * The author(s) of this mod.
     *
     * @return the mod authors
     */
    String[] authors() default {};

    /**
     * Dependencies required by this mod.
     * <p>
     * Format: "modid" or "modid@version" or "modid@[min,max]"
     *
     * @return the mod dependencies
     */
    String[] dependencies() default {};

    /**
     * Optional dependencies that enhance functionality if present.
     *
     * @return the optional dependencies
     */
    String[] optionalDependencies() default {};

    /**
     * Minimum required game engine version.
     *
     * @return the minimum engine version
     */
    String minEngineVersion() default "";

    /**
     * Maximum compatible game engine version.
     *
     * @return the maximum engine version
     */
    String maxEngineVersion() default "";

    /**
     * Whether this mod should be enabled by default.
     *
     * @return true if enabled by default
     */
    boolean enabledByDefault() default true;
}
