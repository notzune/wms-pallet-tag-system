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
import java.util.Map;

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
        return discoverConfigFile(defaultFileName, anchorClass, System.getenv());
    }

    static Path discoverConfigFile(String defaultFileName, Class<?> anchorClass, Map<String, String> envVars) {
        List<Path> candidates = new ArrayList<>();
        Path perUserConfig = resolvePerUserConfig(defaultFileName, envVars);
        if (perUserConfig != null) {
            candidates.add(perUserConfig);
        }
        candidates.add(Paths.get(defaultFileName));
        candidates.add(Paths.get(".env"));
        candidates.add(Paths.get("config", defaultFileName));

        Path executableDir = resolveExecutableDirectory(anchorClass);
        if (executableDir != null) {
            candidates.add(executableDir.resolve(defaultFileName));
            candidates.add(executableDir.resolve(".env"));
            candidates.add(executableDir.resolve("config").resolve(defaultFileName));
        }
        Path appHome = RuntimePathResolver.resolveAppHome(anchorClass);
        if (appHome != null) {
            candidates.add(appHome.resolve(defaultFileName));
            candidates.add(appHome.resolve(".env"));
            candidates.add(appHome.resolve("config").resolve(defaultFileName));
        }

        for (Path candidate : candidates) {
            if (Files.exists(candidate) && Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private static Path resolvePerUserConfig(String defaultFileName, Map<String, String> envVars) {
        if (envVars == null) {
            return null;
        }
        String localAppData = envVars.getOrDefault("LOCALAPPDATA", "").trim();
        if (localAppData.isEmpty()) {
            return null;
        }
        return Paths.get(localAppData)
                .resolve("Tropicana")
                .resolve("WMS-Pallet-Tag-System")
                .resolve(defaultFileName);
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
