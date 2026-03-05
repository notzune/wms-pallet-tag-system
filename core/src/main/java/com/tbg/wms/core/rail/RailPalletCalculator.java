/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * Calculates per-railcar pallet totals using ceiling(cases / casesPerPallet).
 *
 * <p>Family totals are tracked independently for {@code CAN}, {@code DOM}, and
 * {@code KEV} so reporting can present each bucket explicitly.</p>
 */
public final class RailPalletCalculator {
    private final RailFamilyClassifier familyClassifier = new RailFamilyClassifier();

    /**
     * Computes CAN and DOM pallets for one railcar aggregate.
     *
     * @param aggregate railcar aggregate
     * @param footprints lookup by item number / short code
     * @return pallet result and diagnostics
     */
    public RailPalletResult calculate(RailCarAggregate aggregate, Map<String, RailFamilyFootprint> footprints) {
        Objects.requireNonNull(aggregate, "aggregate cannot be null");
        Objects.requireNonNull(footprints, "footprints cannot be null");

        long canPallets = 0L;
        long domPallets = 0L;
        long kevPallets = 0L;
        List<String> missingFootprints = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : aggregate.getCasesByItem().entrySet()) {
            String item = entry.getKey();
            int cases = entry.getValue();
            RailFamilyFootprint footprint = footprints.get(item);
            if (footprint == null || !footprint.isValid()) {
                missingFootprints.add(item);
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
        }

        Collections.sort(missingFootprints);
        return new RailPalletResult(
                safeToInt(canPallets, "CAN"),
                safeToInt(domPallets, "DOM"),
                safeToInt(kevPallets, "KEV"),
                missingFootprints
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

    /**
     * Result object for per-railcar pallet totals.
     */
    public static final class RailPalletResult {
        private final int canPallets;
        private final int domPallets;
        private final int kevPallets;
        private final List<String> missingItems;

        private RailPalletResult(int canPallets, int domPallets, int kevPallets, List<String> missingItems) {
            this.canPallets = canPallets;
            this.domPallets = domPallets;
            this.kevPallets = kevPallets;
            this.missingItems = Collections.unmodifiableList(new ArrayList<>(missingItems));
        }

        public int getCanPallets() {
            return canPallets;
        }

        public int getDomPallets() {
            return domPallets;
        }

        /**
         * Returns pallet total for KEV family rows.
         */
        public int getKevPallets() {
            return kevPallets;
        }

        public List<String> getMissingItems() {
            return missingItems;
        }
    }
}
