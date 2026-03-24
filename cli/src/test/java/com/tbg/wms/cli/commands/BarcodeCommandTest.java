/*
 * Copyright (c) 2026 Tropicana Brands Group
 */

package com.tbg.wms.cli.commands;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BarcodeCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void dryRunWritesZplFileToRequestedOutputDirectory() throws IOException {
        CommandLine cli = new CommandLine(new BarcodeCommand());

        int exitCode = cli.execute(
                "--data", "HELLO-WORLD-123",
                "--dry-run",
                "--output-dir", tempDir.toString()
        );

        assertEquals(0, exitCode);
        try (Stream<Path> files = Files.list(tempDir)) {
            List<Path> artifacts = files.toList();
            assertEquals(1, artifacts.size());
            Path artifact = artifacts.get(0);
            assertTrue(artifact.getFileName().toString().matches("barcode-\\d{8}-\\d{6}-hello-world-123\\.zpl"));
            assertTrue(Files.readString(artifact).contains("^FDHELLO-WORLD-123^FS"));
        }
    }
}
