/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Execution helpers for barcode dialog validation, output, and completion text.
 */
final class BarcodeDialogExecutionSupport {

    private final Path defaultPrintToFileOutputDir;
    private final DateTimeFormatter timestampFormatter;
    private final int maxSlugLength;

    BarcodeDialogExecutionSupport(Path defaultPrintToFileOutputDir, DateTimeFormatter timestampFormatter, int maxSlugLength) {
        this.defaultPrintToFileOutputDir = Objects.requireNonNull(defaultPrintToFileOutputDir, "defaultPrintToFileOutputDir cannot be null");
        this.timestampFormatter = Objects.requireNonNull(timestampFormatter, "timestampFormatter cannot be null");
        this.maxSlugLength = maxSlugLength;
    }

    String requireBarcodeData(String rawData) {
        String data = rawData == null ? "" : rawData.trim();
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Barcode data is required.");
        }
        return data;
    }

    String buildZpl(BarcodeRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        return BarcodeZplBuilder.build(request);
    }

    Path resolveOutputPath(String outputDir, String data, boolean printToFileSelected) {
        String dir;
        if (printToFileSelected) {
            dir = (outputDir == null || outputDir.isBlank())
                    ? defaultPrintToFileOutputDir.toString()
                    : outputDir.trim();
        } else {
            dir = defaultPrintToFileOutputDir.toString();
        }
        Path outputPath = Paths.get(dir);
        String fileName = String.format("barcode-%s-%s.zpl",
                timestampFormatter.format(LocalDateTime.now()),
                ArtifactNameSupport.safeSlug(data, "data", maxSlugLength));
        return outputPath.resolve(fileName);
    }

    String buildGeneratedMessage(Path outputPath) {
        return "ZPL saved to " + outputPath;
    }

    String buildPrintedMessage(Path outputPath) {
        return "Printed barcode label.\nZPL saved to " + outputPath;
    }

    String buildWriteFailureMessage(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return "Failed to write ZPL file: " + GuiExceptionMessageSupport.rootMessage(throwable);
    }

    String buildPrintFailureMessage(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return "Failed to print barcode: " + GuiExceptionMessageSupport.rootMessage(throwable);
    }
}
