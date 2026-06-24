package com.tbg.wms.v2.domain.rail;

import java.util.List;
import java.util.Locale;

/**
 * Carries rail stop record data across WMS 2.0 module boundaries.
 *
 * @param date the date.
 * @param sequence the sequence.
 * @param trainNumber the train number.
 * @param vehicleId the vehicle id.
 * @param warehouse the warehouse.
 * @param loadNumber the load number.
 * @param items the items.
 */
public record RailStopRecord(
        String date,
        String sequence,
        String trainNumber,
        String vehicleId,
        String warehouse,
        String loadNumber,
        List<RailItemQuantity> items
) {
    public RailStopRecord {
        date = clean(date);
        sequence = clean(sequence);
        trainNumber = upper(trainNumber);
        vehicleId = upper(vehicleId);
        warehouse = upper(warehouse);
        loadNumber = upper(loadNumber);
        items = List.copyOf(items == null ? List.of() : items);
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String upper(String value) {
        return clean(value).toUpperCase(Locale.ROOT);
    }
}
