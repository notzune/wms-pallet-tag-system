package com.tbg.wms.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OutDirectoryRetentionServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void prune_shouldDeleteOldFilesAndEmptyDirectoriesOnly() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("out"));
        Path oldDir = Files.createDirectory(root.resolve("old-job"));
        Path oldFile = Files.writeString(oldDir.resolve("label.zpl"), "^XA^XZ");
        Path freshDir = Files.createDirectory(root.resolve("fresh-job"));
        Path freshFile = Files.writeString(freshDir.resolve("label.zpl"), "^XA^XZ");

        FileTime oldTime = FileTime.from(Instant.now().minus(Duration.ofDays(30)));
        Files.setLastModifiedTime(oldFile, oldTime);

        OutDirectoryRetentionService.CleanupResult result =
                new OutDirectoryRetentionService().prune(root, Duration.ofDays(14));

        assertEquals(1, result.getDeletedFiles());
        assertEquals(1, result.getDeletedDirectories());
        assertFalse(Files.exists(oldFile));
        assertFalse(Files.exists(oldDir));
        assertTrue(Files.exists(freshFile));
        assertTrue(Files.exists(freshDir));
    }

    @Test
    void prune_shouldRespectConfiguredRetentionWindow() throws Exception {
        Path root = Files.createDirectory(tempDir.resolve("configured-out"));
        Path oldFile = Files.writeString(root.resolve("older-than-configured-threshold.zpl"), "^XA^XZ");
        Files.setLastModifiedTime(oldFile, FileTime.from(Instant.now().minus(Duration.ofDays(10))));

        RuntimeSettings runtimeSettings = new RuntimeSettings();
        runtimeSettings.setOutRetentionDays(7);

        OutDirectoryRetentionService.CleanupResult result =
                new OutDirectoryRetentionService(runtimeSettings).prune(root, Duration.ofDays(runtimeSettings.outRetentionDays(14)));

        assertEquals(1, result.getDeletedFiles());
        assertFalse(Files.exists(oldFile));
    }

    @Test
    void pruneDefaultOutDirectory_withExplicitRetentionDaysShouldHonorPassedValue() throws Exception {
        Path appHome = Files.createDirectory(tempDir.resolve("app-home"));
        Path outDir = Files.createDirectory(appHome.resolve("out"));
        Path staleFile = Files.writeString(outDir.resolve("stale.zpl"), "^XA^XZ");
        Path recentFile = Files.writeString(outDir.resolve("recent.zpl"), "^XA^XZ");
        Files.setLastModifiedTime(staleFile, FileTime.from(Instant.now().minus(Duration.ofDays(10))));
        Files.setLastModifiedTime(recentFile, FileTime.from(Instant.now().minus(Duration.ofDays(2))));

        String previousAppHome = System.getProperty(RuntimePathResolver.APP_HOME_PROP);
        System.setProperty(RuntimePathResolver.APP_HOME_PROP, appHome.toString());
        try {
            OutDirectoryRetentionService.CleanupResult result =
                    new OutDirectoryRetentionService().pruneDefaultOutDirectory(OutDirectoryRetentionServiceTest.class, 7);

            assertEquals(1, result.getDeletedFiles());
            assertFalse(Files.exists(staleFile));
            assertTrue(Files.exists(recentFile));
        } finally {
            if (previousAppHome == null) {
                System.clearProperty(RuntimePathResolver.APP_HOME_PROP);
            } else {
                System.setProperty(RuntimePathResolver.APP_HOME_PROP, previousAppHome);
            }
        }
    }
}
