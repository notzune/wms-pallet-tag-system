package com.tbg.wms.v2.domain.rail;

import java.util.Locale;

/**
 * Carries rail item quantity data across WMS 2.0 module boundaries.
 *
 * @param itemNumber the item number.
 * @param cases the cases.
 */
public record RailItemQuantity(String itemNumber, int cases) {
    public RailItemQuantity {
        itemNumber = normalizeItem(itemNumber);
        if (itemNumber.isBlank()) {
            throw new IllegalArgumentException("Item number is required.");
        }
        if (cases < 0) {
            throw new IllegalArgumentException("Cases cannot be negative.");
        }
    }

    /**
     * Returns whether usable.
     *
     * @return {@code true} when the item has an item number and positive case count
     */
    public boolean isUsable() {
        return cases > 0;
    }

    private static String normalizeItem(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
