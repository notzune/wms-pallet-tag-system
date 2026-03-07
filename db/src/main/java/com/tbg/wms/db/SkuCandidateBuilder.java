/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.db;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds normalized SKU lookup candidates for cross-table description resolution.
 */
final class SkuCandidateBuilder {

    private SkuCandidateBuilder() {
    }

    static List<String> buildCandidates(String sku) {
        List<String> candidates = new ArrayList<>(3);
        if (sku == null || sku.isBlank()) {
            return candidates;
        }
        String trimmed = sku.trim();
        candidates.add(trimmed);

        if (trimmed.startsWith("100") && trimmed.length() > 3) {
            addIfUnique(candidates, trimmed.substring(3));
        }

        addIfUnique(candidates, trimLeadingZeros(trimmed));
        return candidates;
    }

    private static void addIfUnique(List<String> candidates, String candidate) {
        if (candidate.isBlank()) {
            return;
        }
        for (String existing : candidates) {
            if (existing.equals(candidate)) {
                return;
            }
        }
        candidates.add(candidate);
    }

    private static String trimLeadingZeros(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        int index = 0;
        int maxIndex = value.length() - 1;
        while (index < maxIndex && value.charAt(index) == '0') {
            index++;
        }
        return index == 0 ? value : value.substring(index);
    }
}
