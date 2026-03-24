/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.core.rail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;

/**
 * Builds rail-card pallet totals and top-family summaries in one pass over aggregate items.
 *
 * <p>This support class exists to keep the hot rail-card planning path single-pass and isolated
 * from the broader workflow service so performance tuning and rollup rules can evolve together.</p>
 */
final class RailCardPlanningSupport {
    private final RailFamilyClassifier familyClassifier = new RailFamilyClassifier();
    private final RailFamilyShareSupport familyShareSupport = new RailFamilyShareSupport();

    RailCardPlan plan(RailCarAggregate aggregate, Map<String, RailFamilyFootprint> footprints) {
        Objects.requireNonNull(aggregate, "aggregate cannot be null");
        Objects.requireNonNull(footprints, "footprints cannot be null");

        long canPallets = 0L;
        long domPallets = 0L;
        long kevPallets = 0L;
        TreeSet<String> missingItems = new TreeSet<>();
        Map<String, Double> equivalentByFamily = new java.util.HashMap<>();
        double totalEquivalent = 0.0d;

        for (Map.Entry<String, Integer> entry : aggregate.getCasesByItem().entrySet()) {
            String item = entry.getKey();
            int cases = entry.getValue();
            RailFamilyFootprint footprint = footprints.get(item);
            if (footprint == null || !footprint.isValid()) {
                missingItems.add(item);
                continue;
            }

            int pallets = divideCeiling(cases, footprint.getCasesPerPallet());
            RailFamilyClassifier.FamilyBucket bucket = familyClassifier.classify(footprint.getFamilyCode());
            if (bucket == RailFamilyClassifier.FamilyBucket.CAN) {
                canPallets += pallets;
            } else if (bucket == RailFamilyClassifier.FamilyBucket.KEV) {
                kevPallets += pallets;
            } else {
                domPallets += pallets;
            }

            double equivalent = cases <= 0 ? 0.0d : ((double) cases) / (double) footprint.getCasesPerPallet();
            totalEquivalent += equivalent;
            equivalentByFamily.merge(footprint.getFamilyCode(), equivalent, Double::sum);
        }

        List<String> topFamilies = new ArrayList<>(3);
        List<RailLabelPlanner.FamilyShare> shares =
                familyShareSupport.buildSortedShares(equivalentByFamily, totalEquivalent);
        int limit = Math.min(3, shares.size());
        for (int i = 0; i < limit; i++) {
            RailLabelPlanner.FamilyShare share = shares.get(i);
            topFamilies.add(share.getFamilyCode() + ":" + share.getPercent());
        }

        return new RailCardPlan(
                safeToInt(canPallets, "CAN"),
                safeToInt(domPallets, "DOM"),
                safeToInt(kevPallets, "KEV"),
                List.copyOf(topFamilies),
                Collections.unmodifiableList(new ArrayList<>(missingItems))
        );
    }

    private int divideCeiling(int dividend, int divisor) {
        if (dividend <= 0 || divisor <= 0) {
            return 0;
        }
        long numerator = (long) dividend + (long) divisor - 1L;
        return (int) (numerator / (long) divisor);
    }

    private int safeToInt(long value, String bucket) {
        if (value > Integer.MAX_VALUE) {
            throw new ArithmeticException("Pallet total overflow for bucket " + bucket + ": " + value);
        }
        return (int) value;
    }

    record RailCardPlan(
            int canPallets,
            int domPallets,
            int kevPallets,
            List<String> topFamilies,
            List<String> missingItems
    ) {
    }
}
