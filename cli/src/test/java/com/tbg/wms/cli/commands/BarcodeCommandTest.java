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

    @Test
    void presetBreakStartWritesExpectedPayload() throws IOException {
        CommandLine cli = new CommandLine(new BarcodeCommand());

        int exitCode = cli.execute(
                "--preset", "BREAK_START",
                "--dry-run",
                "--output-dir", tempDir.toString()
        );

        assertEquals(0, exitCode);
        try (Stream<Path> files = Files.list(tempDir)) {
            List<Path> artifacts = files.toList();
            assertEquals(1, artifacts.size());
            Path artifact = artifacts.get(0);
            assertTrue(artifact.getFileName().toString().matches("barcode-\\d{8}-\\d{6}-break-start\\.zpl"));
            String zpl = Files.readString(artifact);
            assertTrue(zpl.contains("BREAK START"));
            // Short trigger as Code 128; the Velocity scan handler maps it to the key macro.
            assertTrue(zpl.contains("^FDBRKSTART^FS"));
            assertTrue(zpl.contains("^BCN"));
        }
    }

    @Test
    void presetBreakStopWritesExpectedPayload() throws IOException {
        CommandLine cli = new CommandLine(new BarcodeCommand());

        int exitCode = cli.execute(
                "--preset", "BREAK_STOP",
                "--dry-run",
                "--output-dir", tempDir.toString()
        );

        assertEquals(0, exitCode);
        try (Stream<Path> files = Files.list(tempDir)) {
            List<Path> artifacts = files.toList();
            assertEquals(1, artifacts.size());
            Path artifact = artifacts.get(0);
            assertTrue(artifact.getFileName().toString().matches("barcode-\\d{8}-\\d{6}-break-stop\\.zpl"));
            String zpl = Files.readString(artifact);
            assertTrue(zpl.contains("BREAK STOP"));
            // Short trigger as Code 128; the Velocity scan handler maps it to the key macro.
            assertTrue(zpl.contains("^FDBRKSTOP^FS"));
            assertTrue(zpl.contains("^BCN"));
        }
    }

    @Test
    void missingDataAndPresetFailsFast() {
        CommandLine cli = new CommandLine(new BarcodeCommand());

        int exitCode = cli.execute("--dry-run", "--output-dir", tempDir.toString());

        assertEquals(2, exitCode);
    }

    @Test
    void breakSheetWritesBothWorkflowCodesToOneLabel() throws IOException {
        CommandLine cli = new CommandLine(new BarcodeCommand());

        int exitCode = cli.execute(
                "--preset", "BREAK_SHEET",
                "--dry-run",
                "--output-dir", tempDir.toString()
        );

        assertEquals(0, exitCode);
        try (Stream<Path> files = Files.list(tempDir)) {
            List<Path> artifacts = files.toList();
            assertEquals(1, artifacts.size());
            String zpl = Files.readString(artifacts.get(0));
            assertTrue(zpl.contains("BREAK START"));
            assertTrue(zpl.contains("BREAK STOP"));
            assertTrue(zpl.contains("^FDBRKSTART^FS"));
            assertTrue(zpl.contains("^FDBRKSTOP^FS"));
            // Two Code 128 trigger barcodes (START over STOP) on one label.
            assertTrue(zpl.indexOf("^BCN") != zpl.lastIndexOf("^BCN"));
        }
    }
}
