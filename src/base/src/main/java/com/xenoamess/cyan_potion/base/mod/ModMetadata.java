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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Metadata for a mod, parsed from mod.json.
 *
 * @author XenoAmess
 * @version 0.167.3-SNAPSHOT
 * @since 2025-03-22
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModMetadata {

    /**
     * The unique identifier for the mod.
     * Format: lowercase letters, numbers, hyphens, and underscores only.
     */
    @JsonProperty("id")
    private String id;

    /**
     * The human-readable name of the mod.
     */
    @JsonProperty("name")
    private String name;

    /**
     * The version of the mod (semantic versioning recommended).
     */
    @JsonProperty("version")
    private String version;

    /**
     * Description of the mod.
     */
    @JsonProperty("description")
    private String description;

    /**
     * The author(s) of the mod.
     */
    @JsonProperty("authors")
    private List<String> authors = new ArrayList<>();

    /**
     * Dependencies required by this mod.
     * Format: "modid" or "modid@version" or "modid@version-range"
     */
    @JsonProperty("dependencies")
    private List<String> dependencies = new ArrayList<>();

    /**
     * Optional dependencies that enhance functionality if present.
     */
    @JsonProperty("optionalDependencies")
    private List<String> optionalDependencies = new ArrayList<>();

    /**
     * List of entry point classes (fully qualified names).
     */
    @JsonProperty("entryPoints")
    private List<String> entryPoints = new ArrayList<>();

    /**
     * The main entry point class (for single-entry mods).
     */
    @JsonProperty("entryPoint")
    private String entryPoint;

    /**
     * Minimum required game engine version.
     */
    @JsonProperty("minEngineVersion")
    private String minEngineVersion;

    /**
     * Maximum compatible game engine version.
     */
    @JsonProperty("maxEngineVersion")
    private String maxEngineVersion;

    /**
     * URL to the mod's website or repository.
     */
    @JsonProperty("url")
    private String url;

    /**
     * License identifier (e.g., "MIT", "GPL-3.0").
     */
    @JsonProperty("license")
    private String license;

    /**
     * Icon path relative to mod directory.
     */
    @JsonProperty("icon")
    private String icon;

    /**
     * Whether this mod is enabled by default.
     */
    @JsonProperty("enabledByDefault")
    private boolean enabledByDefault = true;

    /**
     * Whether this mod is a library mod (no entry points, just API).
     */
    @JsonProperty("library")
    private boolean library = false;

    /**
     * Gets the effective entry points.
     *
     * @return list of entry point class names
     */
    public List<String> getEffectiveEntryPoints() {
        List<String> points = new ArrayList<>();
        if (entryPoint != null && !entryPoint.isEmpty()) {
            points.add(entryPoint);
        }
        points.addAll(entryPoints);
        return points;
    }

    /**
     * Validates the metadata.
     *
     * @throws IllegalArgumentException if validation fails
     */
    public void validate() {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Mod id is required");
        }
        if (!id.matches("^[a-z][a-z0-9_-]*$")) {
            throw new IllegalArgumentException("Mod id must start with lowercase letter and contain only lowercase letters, numbers, hyphens, and underscores: " + id);
        }
        if (version == null || version.isEmpty()) {
            throw new IllegalArgumentException("Mod version is required");
        }
        if (getEffectiveEntryPoints().isEmpty() && !library) {
            throw new IllegalArgumentException("At least one entry point is required for non-library mods");
        }
    }

    /**
     * Gets the display name (name or id if name is null).
     *
     * @return the display name
     */
    public String getDisplayName() {
        return name != null && !name.isEmpty() ? name : id;
    }
}
