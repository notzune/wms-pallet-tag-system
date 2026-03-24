/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Loads config values from classpath defaults and env-style files.
 */
final class ConfigSourceLoader {

    private ConfigSourceLoader() {
    }

    static Map<String, String> loadClasspathDefaults(Class<?> owner, String resourceName) {
        InputStream stream = owner.getClassLoader().getResourceAsStream(resourceName);
        if (stream == null) {
            return Map.of();
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            return EnvStyleConfigParser.parseReader(reader);
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    static Map<String, String> loadEnvStyleFile(Path path) {
        try {
            List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            return EnvStyleConfigParser.parseLines(lines);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read config file: " + path, e);
        }
    }
}
