package com.tbg.wms.v2.domain.rail;

import java.util.Locale;

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

    public boolean isUsable() {
        return cases > 0;
    }

    private static String normalizeItem(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
