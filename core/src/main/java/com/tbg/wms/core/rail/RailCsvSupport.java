/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.2
 */
package com.tbg.wms.core.rail;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared CSV helpers for rail import/export workflows.
 */
public final class RailCsvSupport {

    private RailCsvSupport() {
    }

    /**
     * Normalizes a header to uppercase alphanumeric form.
     *
     * @param header raw header
     * @return normalized header key
     */
    public static String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
    }

    /**
     * Parses one CSV line with RFC4180-style quote escaping.
     *
     * @param line raw CSV line
     * @return ordered field values
     */
    public static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }
            if (ch == ',' && !inQuotes) {
                values.add(current.toString());
                current.setLength(0);
                continue;
            }
            current.append(ch);
        }
        values.add(current.toString());
        return values;
    }
}
