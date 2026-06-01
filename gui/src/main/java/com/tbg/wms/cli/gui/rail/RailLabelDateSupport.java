/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;

/**
 * Parses and formats rail label dates for operator entry and PDF output.
 */
final class RailLabelDateSupport {
    private static final DateTimeFormatter INPUT_FORMATTER = new DateTimeFormatterBuilder()
            .appendValue(ChronoField.MONTH_OF_YEAR, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValue(ChronoField.DAY_OF_MONTH, 1, 2, java.time.format.SignStyle.NOT_NEGATIVE)
            .appendLiteral('-')
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
            .toFormatter()
            .withResolverStyle(java.time.format.ResolverStyle.STRICT);
    private static final DateTimeFormatter OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yy");

    String parseLabelDate(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Label date is required.");
        }
        try {
            return formatDate(LocalDate.parse(normalized, INPUT_FORMATTER));
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Label date must use MM-DD-YY.", ex);
        }
    }

    String todayText() {
        return formatDate(LocalDate.now());
    }

    String formatDate(LocalDate date) {
        return OUTPUT_FORMATTER.format(date);
    }

    LocalDate parseDate(String text) {
        return LocalDate.parse(parseLabelDate(text), OUTPUT_FORMATTER);
    }
}
