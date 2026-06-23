package com.tbg.wms.v2.domain.rail;

import java.util.List;
import java.util.Locale;

public record RailCarCard(
        String trainId,
        String sequence,
        String vehicleId,
        String loadNumbers,
        String routeHeader,
        List<RailItemQuantity> itemLines,
        int canPallets,
        int domPallets,
        int kevPallets,
        List<String> topFamilies,
        List<String> missingFootprintItems
) {
    public RailCarCard {
        trainId = upper(trainId);
        sequence = clean(sequence);
        vehicleId = upper(vehicleId);
        loadNumbers = clean(loadNumbers);
        routeHeader = clean(routeHeader);
        itemLines = List.copyOf(itemLines == null ? List.of() : itemLines);
        if (canPallets < 0 || domPallets < 0 || kevPallets < 0) {
            throw new IllegalArgumentException("Pallet counts cannot be negative.");
        }
        topFamilies = List.copyOf(topFamilies == null ? List.of() : topFamilies);
        missingFootprintItems = List.copyOf(missingFootprintItems == null ? List.of() : missingFootprintItems);
    }

    public int totalPallets() {
        return canPallets + domPallets + kevPallets;
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }
}
