package com.tbg.wms.core.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Calculates pallet requirements from shipment SKU totals and footprint setup.
 */
public final class PalletPlanningService {

    /**
     * Computes pallet planning details per SKU and for the full shipment.
     *
     * @param skuFootprints aggregated shipment SKU rows from WMS
     * @return immutable planning result
     */
    public PlanResult plan(List<ShipmentSkuFootprint> skuFootprints) {
        Objects.requireNonNull(skuFootprints, "skuFootprints cannot be null");

        List<SkuPlan> skuPlans = new ArrayList<>();
        List<String> missingFootprints = new ArrayList<>();

        int estimatedPallets = 0;
        int totalUnits = 0;

        for (ShipmentSkuFootprint row : skuFootprints) {
            if (row == null) {
                continue;
            }

            int units = Math.max(0, row.getTotalUnits());
            totalUnits += units;

            Integer unitsPerPallet = row.getUnitsPerPallet();
            int skuPallets = 0;

            if (units > 0 && unitsPerPallet != null && unitsPerPallet > 0) {
                skuPallets = divideRoundUp(units, unitsPerPallet);
                estimatedPallets += skuPallets;
            } else if (units > 0) {
                missingFootprints.add(row.getSku());
            }

            skuPlans.add(new SkuPlan(row, skuPallets));
        }

        return new PlanResult(
                Collections.unmodifiableList(skuPlans),
                Collections.unmodifiableList(missingFootprints),
                totalUnits,
                estimatedPallets
        );
    }

    private int divideRoundUp(int numerator, int denominator) {
        return (numerator + denominator - 1) / denominator;
    }

    public static final class PlanResult {
        private final List<SkuPlan> skuPlans;
        private final List<String> skusMissingFootprint;
        private final int totalUnits;
        private final int estimatedPallets;

        private PlanResult(List<SkuPlan> skuPlans,
                           List<String> skusMissingFootprint,
                           int totalUnits,
                           int estimatedPallets) {
            this.skuPlans = skuPlans;
            this.skusMissingFootprint = skusMissingFootprint;
            this.totalUnits = totalUnits;
            this.estimatedPallets = estimatedPallets;
        }

        public List<SkuPlan> getSkuPlans() {
            return skuPlans;
        }

        public List<String> getSkusMissingFootprint() {
            return skusMissingFootprint;
        }

        public int getTotalUnits() {
            return totalUnits;
        }

        public int getEstimatedPallets() {
            return estimatedPallets;
        }
    }

    public static final class SkuPlan {
        private final ShipmentSkuFootprint footprint;
        private final int estimatedPallets;

        private SkuPlan(ShipmentSkuFootprint footprint, int estimatedPallets) {
            this.footprint = footprint;
            this.estimatedPallets = estimatedPallets;
        }

        public ShipmentSkuFootprint getFootprint() {
            return footprint;
        }

        public int getEstimatedPallets() {
            return estimatedPallets;
        }
    }
}
