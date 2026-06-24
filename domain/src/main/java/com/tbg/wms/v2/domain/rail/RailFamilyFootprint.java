package com.tbg.wms.v2.domain.rail;

import java.util.Locale;

/**
 * Carries rail family footprint data across WMS 2.0 module boundaries.
 *
 * @param itemNumber the item number.
 * @param familyCode the family code.
 * @param casesPerPallet the cases per pallet.
 */
public record RailFamilyFootprint(String itemNumber, String familyCode, int casesPerPallet) {
    public RailFamilyFootprint {
        itemNumber = upper(itemNumber);
        familyCode = upper(familyCode);
        if (itemNumber.isBlank()) {
            throw new IllegalArgumentException("Item number is required.");
        }
        if (casesPerPallet <= 0) {
            throw new IllegalArgumentException("Cases per pallet must be positive.");
        }
    }

    private static String upper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
