/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * Aggregated railcar payload keyed by train + sequence + vehicle.
 */
public final class RailCarAggregate {
    private final String date;
    private final String sequence;
    private final String trainNumber;
    private final String vehicleId;
    private final String warehouse;
    private final Set<String> loadNumbers;
    private final Map<String, Integer> casesByItem;

    RailCarAggregate(String date,
                     String sequence,
                     String trainNumber,
                     String vehicleId,
                     String warehouse,
                     Set<String> loadNumbers,
                     Map<String, Integer> casesByItem) {
        this.date = normalize(date);
        this.sequence = normalize(sequence);
        this.trainNumber = normalize(trainNumber);
        this.vehicleId = normalize(vehicleId);
        this.warehouse = normalize(warehouse);
        this.loadNumbers = Collections.unmodifiableSet(new LinkedHashSet<>(loadNumbers));
        this.casesByItem = Collections.unmodifiableMap(new LinkedHashMap<>(casesByItem));
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public String getDate() {
        return date;
    }

    public String getSequence() {
        return sequence;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public String getWarehouse() {
        return warehouse;
    }

    public Set<String> getLoadNumbers() {
        return loadNumbers;
    }

    public String getLoadNumberDisplay() {
        return loadNumbers.isEmpty() ? "" : String.join(", ", loadNumbers);
    }

    public Map<String, Integer> getCasesByItem() {
        return casesByItem;
    }

    public List<RailStopRecord.ItemQuantity> getSortedItemsByCasesDesc() {
        List<RailStopRecord.ItemQuantity> rows = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : casesByItem.entrySet()) {
            rows.add(new RailStopRecord.ItemQuantity(entry.getKey(), entry.getValue()));
        }
        rows.sort(Comparator.<RailStopRecord.ItemQuantity>comparingInt(RailStopRecord.ItemQuantity::getCases)
                .reversed()
                .thenComparing(RailStopRecord.ItemQuantity::getItemNumber));
        return rows;
    }
}
