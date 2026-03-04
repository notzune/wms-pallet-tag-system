/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * Groups flattened rail rows into railcar-level aggregates.
 */
public final class RailAggregationService {

    /**
     * Aggregates rows by train + sequence + vehicle.
     *
     * @param rows flattened rail rows
     * @return deterministic railcar aggregates in key order
     */
    public List<RailCarAggregate> aggregateByRailcar(List<RailStopRecord> rows) {
        Objects.requireNonNull(rows, "rows cannot be null");
        Map<String, MutableAggregate> grouped = new LinkedHashMap<>();

        for (RailStopRecord row : rows) {
            if (row == null) {
                continue;
            }
            String key = row.getTrainNumber() + "|" + row.getSequence() + "|" + row.getVehicleId();
            MutableAggregate aggregate = grouped.computeIfAbsent(key, ignored -> new MutableAggregate(
                    row.getDate(),
                    row.getSequence(),
                    row.getTrainNumber(),
                    row.getVehicleId(),
                    row.getWarehouse()
            ));
            if (!row.getLoadNumber().isBlank()) {
                aggregate.loadNumbers.add(row.getLoadNumber());
            }
            for (RailStopRecord.ItemQuantity item : row.getItems()) {
                if (item == null || !item.isValid()) {
                    continue;
                }
                aggregate.casesByItem.merge(item.getItemNumber(), item.getCases(), Integer::sum);
            }
        }

        List<RailCarAggregate> result = new ArrayList<>(grouped.size());
        for (MutableAggregate aggregate : grouped.values()) {
            result.add(new RailCarAggregate(
                    aggregate.date,
                    aggregate.sequence,
                    aggregate.trainNumber,
                    aggregate.vehicleId,
                    aggregate.warehouse,
                    aggregate.loadNumbers,
                    aggregate.casesByItem
            ));
        }
        return Collections.unmodifiableList(result);
    }

    private static final class MutableAggregate {
        private final String date;
        private final String sequence;
        private final String trainNumber;
        private final String vehicleId;
        private final String warehouse;
        private final Set<String> loadNumbers = new TreeSet<>();
        private final Map<String, Integer> casesByItem = new LinkedHashMap<>();

        private MutableAggregate(String date,
                                 String sequence,
                                 String trainNumber,
                                 String vehicleId,
                                 String warehouse) {
            this.date = date == null ? "" : date.trim();
            this.sequence = sequence == null ? "" : sequence.trim();
            this.trainNumber = trainNumber == null ? "" : trainNumber.trim();
            this.vehicleId = vehicleId == null ? "" : vehicleId.trim();
            this.warehouse = warehouse == null ? "" : warehouse.trim();
        }
    }
}
