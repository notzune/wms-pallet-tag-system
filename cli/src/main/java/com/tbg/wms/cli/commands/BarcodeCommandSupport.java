/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands;

import org.slf4j.Logger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Validates barcode CLI options and handles deterministic artifact naming/writes.
 */
final class BarcodeCommandSupport {

    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");
    private static final int MAX_SLUG_LENGTH = 40;

    String validateOptions(
            String data,
            int labelWidthDots,
            int labelHeightDots,
            int originX,
            int originY,
            int moduleWidth,
            int moduleRatio,
            int barcodeHeight,
            int copies
    ) {
        if (data == null || data.trim().isEmpty()) {
            return "Error: --data is required.";
        }
        if (labelWidthDots <= 0 || labelHeightDots <= 0) {
            return "Error: label dimensions must be > 0.";
        }
        if (originX < 0 || originY < 0) {
            return "Error: origin offsets must be >= 0.";
        }
        if (moduleWidth <= 0 || moduleRatio <= 0 || barcodeHeight <= 0) {
            return "Error: barcode sizing values must be > 0.";
        }
        if (copies <= 0) {
            return "Error: --copies must be > 0.";
        }
        return null;
    }

    Path writeZplFile(String zpl,
                      String outputDirPath,
                      String data,
                      DateTimeFormatter timestampFormat,
                      Logger log) {
        Path outputPath = Path.of(outputDirPath);
        try {
            Files.createDirectories(outputPath);
        } catch (Exception e) {
            log.error("Failed to create output directory: {}", outputPath, e);
            System.err.println("Error: Unable to create output directory: " + outputPath);
            return null;
        }

        String fileName = String.format("barcode-%s-%s.zpl", timestampFormat.format(LocalDateTime.now()), safeSlug(data));
        Path zplFile = outputPath.resolve(fileName);
        try {
            Files.writeString(zplFile, zpl);
            return zplFile;
        } catch (Exception e) {
            log.error("Failed to write ZPL file: {}", zplFile, e);
            System.err.println("Error: Unable to write ZPL file: " + zplFile);
            return null;
        }
    }

    static String safeSlug(String value) {
        if (value == null) {
            return "data";
        }
        String slug = NON_ALNUM_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        slug = slug.replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) {
            return "data";
        }
        return slug.length() > MAX_SLUG_LENGTH ? slug.substring(0, MAX_SLUG_LENGTH) : slug;
    }
}
