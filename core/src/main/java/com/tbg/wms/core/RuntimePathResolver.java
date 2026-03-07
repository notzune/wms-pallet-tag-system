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
}
