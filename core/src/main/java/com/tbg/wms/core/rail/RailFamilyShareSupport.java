/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds deterministic family-share summaries for rail labels.
 */
final class RailFamilyShareSupport {

    List<RailLabelPlanner.FamilyShare> buildSortedShares(Map<String, Double> equivalentByFamily, double totalEquivalent) {
        List<RailLabelPlanner.FamilyShare> shares = new ArrayList<>(equivalentByFamily.size());
        if (totalEquivalent <= 0.0d) {
            return shares;
        }

        List<Map.Entry<String, Double>> entries = new ArrayList<>(equivalentByFamily.entrySet());
        entries.sort(Comparator.<Map.Entry<String, Double>>comparingDouble(Map.Entry::getValue).reversed()
                .thenComparing(Map.Entry::getKey));
        Map<String, Integer> percentByFamily = allocatePercentages(entries, totalEquivalent);

        for (Map.Entry<String, Double> entry : entries) {
            int percent = percentByFamily.getOrDefault(entry.getKey(), 0);
            shares.add(new RailLabelPlanner.FamilyShare(entry.getKey(), percent, entry.getValue()));
        }
        return shares;
    }

    /**
     * Allocates integer percentages that always sum to 100 using a largest-remainder policy.
     */
    Map<String, Integer> allocatePercentages(List<Map.Entry<String, Double>> entries, double totalEquivalent) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (entries.isEmpty() || totalEquivalent <= 0.0d) {
            return result;
        }

        List<PercentCandidate> candidates = new ArrayList<>(entries.size());
        int floorSum = 0;
        for (Map.Entry<String, Double> entry : entries) {
            double exact = (entry.getValue() / totalEquivalent) * 100.0d;
            int floor = (int) Math.floor(exact);
            floorSum += floor;
            candidates.add(new PercentCandidate(entry.getKey(), exact - floor));
            result.put(entry.getKey(), floor);
        }

        int remaining = Math.max(0, 100 - floorSum);
        candidates.sort(Comparator.comparingDouble(PercentCandidate::remainder).reversed()
                .thenComparing(PercentCandidate::familyCode));

        for (int i = 0; i < remaining && i < candidates.size(); i++) {
            PercentCandidate candidate = candidates.get(i);
            result.put(candidate.familyCode(), result.get(candidate.familyCode()) + 1);
        }

        return result;
    }

    private static final class PercentCandidate {
        private final String familyCode;
        private final double remainder;

        private PercentCandidate(String familyCode, double remainder) {
            this.familyCode = familyCode;
            this.remainder = remainder;
        }

        private String familyCode() {
            return familyCode;
        }

        private double remainder() {
            return remainder;
        }
    }
}
