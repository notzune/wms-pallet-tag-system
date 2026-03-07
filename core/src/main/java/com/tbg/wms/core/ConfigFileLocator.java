/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.core;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves and validates configuration file locations.
 */
final class ConfigFileLocator {

    private ConfigFileLocator() {
    }

    static Path resolveExplicitConfigPath(String configFileProp, String configFileEnv) {
        String explicit = System.getProperty(configFileProp);
        if (explicit == null || explicit.isBlank()) {
            explicit = System.getenv(configFileEnv);
        }
        if (explicit == null || explicit.isBlank()) {
            return null;
        }
        return validateConfigFile(Paths.get(explicit.trim()));
    }

    static Path validateConfigFile(Path path) {
        if (Files.exists(path) && Files.isRegularFile(path)) {
            return path;
        }
        throw new IllegalStateException("Configured file not found: " + path);
    }

    static Path discoverConfigFile(String defaultFileName, Class<?> anchorClass) {
        List<Path> candidates = new ArrayList<>();
        candidates.add(Paths.get(defaultFileName));
        candidates.add(Paths.get(".env"));
        candidates.add(Paths.get("config", defaultFileName));

        Path executableDir = resolveExecutableDirectory(anchorClass);
        if (executableDir != null) {
            candidates.add(executableDir.resolve(defaultFileName));
            candidates.add(executableDir.resolve(".env"));
            candidates.add(executableDir.resolve("config").resolve(defaultFileName));
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path resolveExecutableDirectory(Class<?> anchorClass) {
        try {
            CodeSource codeSource = anchorClass.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            URI locationUri = codeSource.getLocation().toURI();
            Path location = Paths.get(locationUri);
            if (Files.isDirectory(location)) {
                return location;
            }
            Path parent = location.getParent();
            return parent;
        } catch (Exception ignored) {
            return null;
        }
    }
}
