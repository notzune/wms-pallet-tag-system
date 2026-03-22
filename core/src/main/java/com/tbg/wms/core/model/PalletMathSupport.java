package com.tbg.wms.core.model;

/**
 * Shared pallet-count math so preview rows and aggregate planning cannot drift.
 */
public final class PalletMathSupport {

    private PalletMathSupport() {
    }

    public static PalletCounts calculate(int totalUnits, Integer unitsPerPallet) {
        int units = Math.max(0, totalUnits);
        if (unitsPerPallet == null || unitsPerPallet <= 0) {
            return new PalletCounts(units, 0, 0, 0);
        }
        int fullPallets = units / unitsPerPallet;
        int partialPallets = units % unitsPerPallet > 0 ? 1 : 0;
        return new PalletCounts(units, fullPallets, partialPallets, fullPallets + partialPallets);
    }

    public record PalletCounts(
            int units,
            int fullPallets,
            int partialPallets,
            int estimatedPallets
    ) {
    }
}
