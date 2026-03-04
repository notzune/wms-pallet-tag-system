/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser for dotenv-style key/value files.
 */
final class EnvStyleConfigParser {

    private EnvStyleConfigParser() {
    }

    static Map<String, String> parseReader(BufferedReader reader) throws IOException {
        Map<String, String> values = new HashMap<>();
        String rawLine;
        while ((rawLine = reader.readLine()) != null) {
            parseLine(values, rawLine);
        }
        return values;
    }

    static Map<String, String> parseLines(List<String> lines) {
        Map<String, String> values = new HashMap<>();
        for (String rawLine : lines) {
            parseLine(values, rawLine);
        }
        return values;
    }

    private static void parseLine(Map<String, String> values, String rawLine) {
        if (rawLine == null) {
            return;
        }

        String line = rawLine.trim();
        if (line.isEmpty() || line.startsWith("#")) {
            return;
        }

        if (line.startsWith("export ")) {
            line = line.substring("export ".length()).trim();
        }

        int sep = line.indexOf('=');
        if (sep <= 0) {
            return;
        }

        String key = line.substring(0, sep).trim();
        String value = line.substring(sep + 1).trim();
        if (key.isEmpty()) {
            return;
        }

        values.put(key, stripQuotes(value));
    }

    private static String stripQuotes(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }
}
