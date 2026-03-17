/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.core;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Resolves runtime-relative filesystem paths from the executing class/jar location.
 *
 * <p><strong>Why this helper exists:</strong> multiple CLI/GUI entry points previously duplicated
 * the same jar-location resolution logic to derive print-to-file output directories.
 * Centralizing this behavior keeps fallback semantics consistent and removes copy-paste drift.</p>
 */
public final class RuntimePathResolver {
    public static final String APP_HOME_PROP = "wms.app.home";

    private RuntimePathResolver() {
        // Utility class.
    }

    /**
     * Resolves a child directory next to the executing class/jar location.
     *
     * <p>If runtime location cannot be resolved, this falls back to a relative path
     * with the provided child directory name.</p>
     *
     * @param anchorType   class used to locate the runtime code source
     * @param childDirName target child directory name (for example {@code "out"})
     * @return resolved runtime-adjacent directory path
     */
    public static Path resolveJarSiblingDir(Class<?> anchorType, String childDirName) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(childDirName, "childDirName cannot be null");

        String normalizedChild = childDirName.trim();
        if (normalizedChild.isEmpty()) {
            throw new IllegalArgumentException("childDirName cannot be blank");
        }

        Path configuredAppHome = resolveConfiguredAppHome();
        if (configuredAppHome != null) {
            return configuredAppHome.resolve(normalizedChild);
        }

        try {
            Path codeSource = Paths.get(Objects.requireNonNull(anchorType
                            .getProtectionDomain()
                            .getCodeSource())
                    .getLocation()
                    .toURI());
            Path baseDir = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            return baseDir == null ? Paths.get(normalizedChild) : baseDir.resolve(normalizedChild);
        } catch (Exception e) {
            return Paths.get(normalizedChild);
        }
    }

    /**
     * Resolves a runtime directory by preferring the current working directory when it exists,
     * then falling back to a jar-adjacent directory.
     *
     * <p>This keeps local development behavior intact while making packaged executable launches
     * independent of the caller's working directory.</p>
     *
     * @param anchorType   class used to locate the runtime code source
     * @param childDirName target child directory name (for example {@code "config"})
     * @return existing working-directory child when present, otherwise jar-adjacent fallback
     */
    public static Path resolveWorkingDirOrJarSiblingDir(Class<?> anchorType, String childDirName) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Objects.requireNonNull(childDirName, "childDirName cannot be null");

        String normalizedChild = childDirName.trim();
        if (normalizedChild.isEmpty()) {
            throw new IllegalArgumentException("childDirName cannot be blank");
        }

        Path workingDirCandidate = Paths.get(normalizedChild);
        if (Files.exists(workingDirCandidate) && Files.isDirectory(workingDirCandidate)) {
            return workingDirCandidate;
        }
        return resolveJarSiblingDir(anchorType, normalizedChild);
    }

    public static Path resolveAppHome(Class<?> anchorType) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        Path configuredAppHome = resolveConfiguredAppHome();
        if (configuredAppHome != null) {
            return configuredAppHome;
        }
        return resolveJarSiblingDir(anchorType, ".");
    }

    private static Path resolveConfiguredAppHome() {
        String configured = System.getProperty(APP_HOME_PROP, "").trim();
        if (configured.isEmpty()) {
            return null;
        }
        try {
            Path path = Paths.get(configured).toAbsolutePath().normalize();
            return Files.isDirectory(path) ? path : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
