package com.tbg.wms.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Deletes stale generated artifacts from the runtime {@code out} directory.
 */
public final class OutDirectoryRetentionService {
    public static final int DEFAULT_RETENTION_DAYS = 14;

    private static final Logger LOG = LoggerFactory.getLogger(OutDirectoryRetentionService.class);
    private final RuntimeSettings runtimeSettings;

    public OutDirectoryRetentionService() {
        this(new RuntimeSettings());
    }

    OutDirectoryRetentionService(RuntimeSettings runtimeSettings) {
        this.runtimeSettings = Objects.requireNonNull(runtimeSettings, "runtimeSettings cannot be null");
    }

    public CleanupResult pruneDefaultOutDirectory(Class<?> anchorType) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        int retentionDays = runtimeSettings.outRetentionDays(DEFAULT_RETENTION_DAYS);
        return pruneDefaultOutDirectory(anchorType, retentionDays);
    }

    public CleanupResult pruneDefaultOutDirectory(Class<?> anchorType, int retentionDays) {
        Objects.requireNonNull(anchorType, "anchorType cannot be null");
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("retentionDays must be positive");
        }
        return prune(RuntimePathResolver.resolveJarSiblingDir(anchorType, "out"), Duration.ofDays(retentionDays));
    }

    public CleanupResult prune(Path rootDir, Duration retention) {
        Objects.requireNonNull(rootDir, "rootDir cannot be null");
        Objects.requireNonNull(retention, "retention cannot be null");
        if (retention.isNegative() || retention.isZero()) {
            throw new IllegalArgumentException("retention must be positive");
        }
        if (!Files.exists(rootDir) || !Files.isDirectory(rootDir)) {
            return new CleanupResult(rootDir, 0, 0);
        }

        Instant cutoff = Instant.now().minus(retention);
        List<Path> descendants = new ArrayList<>();
        try (var walk = Files.walk(rootDir)) {
            walk.filter(path -> !path.equals(rootDir))
                    .sorted(Comparator.reverseOrder())
                    .forEach(descendants::add);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to scan output directory: " + rootDir, ex);
        }

        int deletedFiles = 0;
        int deletedDirectories = 0;
        for (Path path : descendants) {
            try {
                if (Files.isDirectory(path)) {
                    if (isDirectoryEmpty(path)) {
                        Files.deleteIfExists(path);
                        deletedDirectories++;
                    }
                    continue;
                }
                Instant modified = Files.getLastModifiedTime(path).toInstant();
                if (modified.isBefore(cutoff)) {
                    Files.deleteIfExists(path);
                    deletedFiles++;
                }
            } catch (IOException ex) {
                LOG.warn("Failed to prune stale output entry {}", path, ex);
            }
        }
        if (deletedFiles > 0 || deletedDirectories > 0) {
            LOG.info("Pruned stale output artifacts from {}: filesDeleted={}, directoriesDeleted={}, retentionDays={}",
                    rootDir, deletedFiles, deletedDirectories, retention.toDays());
        }
        return new CleanupResult(rootDir, deletedFiles, deletedDirectories);
    }

    private boolean isDirectoryEmpty(Path directory) throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            return !stream.iterator().hasNext();
        }
    }

    public static final class CleanupResult {
        private final Path rootDir;
        private final int deletedFiles;
        private final int deletedDirectories;

        public CleanupResult(Path rootDir, int deletedFiles, int deletedDirectories) {
            this.rootDir = Objects.requireNonNull(rootDir, "rootDir cannot be null");
            this.deletedFiles = deletedFiles;
            this.deletedDirectories = deletedDirectories;
        }

        public Path getRootDir() {
            return rootDir;
        }

        public int getDeletedFiles() {
            return deletedFiles;
        }

        public int getDeletedDirectories() {
            return deletedDirectories;
        }
    }
}
