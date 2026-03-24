/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.core;

import java.util.Map;
import java.util.Objects;

/**
 * Shared config lookup and scalar parsing helpers.
 */
final class ConfigValueSupport {

    private final Map<String, String> envVars;
    private final Map<String, String> fileValues;
    private final Map<String, String> classpathDefaults;
    private final String defaultFileName;

    ConfigValueSupport(
            Map<String, String> envVars,
            Map<String, String> fileValues,
            Map<String, String> classpathDefaults,
            String defaultFileName
    ) {
        this.envVars = Map.copyOf(Objects.requireNonNull(envVars, "envVars cannot be null"));
        this.fileValues = Map.copyOf(Objects.requireNonNull(fileValues, "fileValues cannot be null"));
        this.classpathDefaults = Map.copyOf(Objects.requireNonNull(classpathDefaults, "classpathDefaults cannot be null"));
        this.defaultFileName = Objects.requireNonNull(defaultFileName, "defaultFileName cannot be null");
    }

    String required(String key) {
        String value = raw(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required config key: " + key
                    + " (set env var, or define in " + defaultFileName + "/.env)");
        }
        return value.trim();
    }

    int parseInt(String key, String defaultValue) {
        String value = get(key, defaultValue);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid integer config for " + key + ": '" + value + "'", ex);
        }
    }

    long parseLong(String key, String defaultValue) {
        String value = get(key, defaultValue);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid long config for " + key + ": '" + value + "'", ex);
        }
    }

    double parseDouble(String key, String defaultValue) {
        String value = get(key, defaultValue);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            throw new IllegalStateException("Invalid decimal config for " + key + ": '" + value + "'", ex);
        }
    }

    String get(String key, String defaultValue) {
        String value = raw(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    String raw(String key) {
        String fromEnv = envVars.get(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromFile = fileValues.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }

        String fromDefaults = classpathDefaults.get(key);
        return (fromDefaults == null || fromDefaults.isBlank()) ? null : fromDefaults.trim();
    }

    String rawFromEnvOrFile(String key) {
        String fromEnv = envVars.get(key);
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String fromFile = fileValues.get(key);
        if (fromFile != null && !fromFile.isBlank()) {
            return fromFile.trim();
        }
        return null;
    }
}
