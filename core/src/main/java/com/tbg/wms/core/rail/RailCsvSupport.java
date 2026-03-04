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
        String trimmed = header.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        StringBuilder normalized = new StringBuilder(trimmed.length());
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if (ch >= 'a' && ch <= 'z') {
                normalized.append((char) (ch - ('a' - 'A')));
            } else if ((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) {
                normalized.append(ch);
            }
        }
        return normalized.toString();
    }

    /**
     * Parses one CSV line with RFC4180-style quote escaping.
     *
     * @param line raw CSV line
     * @return ordered field values
     */
    public static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>(8);
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
