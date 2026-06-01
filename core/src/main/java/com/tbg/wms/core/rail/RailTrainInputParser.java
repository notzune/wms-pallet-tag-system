/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Parses operator-entered rail train codes into normalized, ordered train IDs.
 */
public final class RailTrainInputParser {
    private static final Pattern DELIMITERS = Pattern.compile("[,\\s:/;]+");

    /**
     * Splits train-code input on comma, whitespace, colon, slash, semicolon, or any combination.
     *
     * @param input operator-entered train-code text
     * @return uppercase train IDs in first-seen order
     */
    public List<String> parse(String input) {
        if (input == null) {
            throw new IllegalArgumentException("At least one train ID is required.");
        }
        LinkedHashSet<String> values = new LinkedHashSet<>();
        for (String token : DELIMITERS.split(input.trim())) {
            String normalized = token.trim().toUpperCase(Locale.ROOT);
            if (!normalized.isEmpty()) {
                values.add(normalized);
            }
        }
        if (values.isEmpty()) {
            throw new IllegalArgumentException("At least one train ID is required.");
        }
        return List.copyOf(values);
    }
}
