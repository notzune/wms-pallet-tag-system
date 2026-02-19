/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Calculates pallet planning summary metrics from SKU footprint data.
 *
 * <p>Guidance: keep the planning logic explicit and easy to audit. The output
 * is used for label counts and must match the per-pallet labeling rules.</p>
 */
public final class PalletPlanningService {

    /**
     * Produces a pallet plan summary for a shipment footprint dataset.
     *
     * @param footprintRows shipment SKU footprint rows (one per SKU)
     * @return plan result with totals and missing-footprint diagnostics
     */
    public PlanResult plan(List<ShipmentSkuFootprint> footprintRows) {
        if (footprintRows == null || footprintRows.isEmpty()) {
            return new PlanResult(0, 0, 0, 0, List.of());
        }

        int totalUnits = 0;
        int fullPallets = 0;
        int partialPallets = 0;
        List<String> missing = new ArrayList<>();

        for (ShipmentSkuFootprint row : footprintRows) {
            if (row == null) {
                continue;
            }

            int units = Math.max(0, row.getTotalUnits());
            totalUnits += units;

            Integer unitsPerPallet = row.getUnitsPerPallet();
            if (unitsPerPallet == null || unitsPerPallet <= 0) {
                // Missing footprint: treat as one partial pallet for visibility.
                if (units > 0 && row.getSku() != null && !row.getSku().isBlank()) {
                    missing.add(row.getSku());
                    partialPallets += 1;
                }
                continue;
            }

            int full = units / unitsPerPallet;
            int remainder = units % unitsPerPallet;
            fullPallets += full;
            if (remainder > 0) {
                partialPallets += 1;
            }
        }

        int estimatedPallets = fullPallets + partialPallets;
        return new PlanResult(totalUnits, fullPallets, partialPallets, estimatedPallets, missing);
    }

    /**
     * Summary metrics used by CLI/GUI previews.
     */
    public static final class PlanResult {
        private final int totalUnits;
        private final int fullPallets;
        private final int partialPallets;
        private final int estimatedPallets;
        private final List<String> skusMissingFootprint;

        private PlanResult(int totalUnits,
                           int fullPallets,
                           int partialPallets,
                           int estimatedPallets,
                           List<String> skusMissingFootprint) {
            this.totalUnits = totalUnits;
            this.fullPallets = fullPallets;
            this.partialPallets = partialPallets;
            this.estimatedPallets = estimatedPallets;
            this.skusMissingFootprint = skusMissingFootprint == null
                    ? List.of()
                    : Collections.unmodifiableList(new ArrayList<>(skusMissingFootprint));
        }

        public int getTotalUnits() {
            return totalUnits;
        }

        public int getFullPallets() {
            return fullPallets;
        }

        public int getPartialPallets() {
            return partialPallets;
        }

        public int getEstimatedPallets() {
            return estimatedPallets;
        }

        public List<String> getSkusMissingFootprint() {
            return skusMissingFootprint;
        }
    }
}
