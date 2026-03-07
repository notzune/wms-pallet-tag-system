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
        Map<RailcarGroupKey, MutableAggregate> grouped = new LinkedHashMap<>();

        for (RailStopRecord row : rows) {
            if (row == null) {
                continue;
            }
            RailcarGroupKey key = RailcarGroupKey.of(row);
            MutableAggregate aggregate = grouped.computeIfAbsent(key, ignored -> MutableAggregate.fromRow(row));
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

    private static final class RailcarGroupKey {
        private final String trainNumber;
        private final String sequence;
        private final String vehicleId;

        private RailcarGroupKey(String trainNumber, String sequence, String vehicleId) {
            this.trainNumber = trainNumber;
            this.sequence = sequence;
            this.vehicleId = vehicleId;
        }

        private static RailcarGroupKey of(RailStopRecord row) {
            return new RailcarGroupKey(row.getTrainNumber(), row.getSequence(), row.getVehicleId());
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof RailcarGroupKey)) {
                return false;
            }
            RailcarGroupKey other = (RailcarGroupKey) obj;
            return Objects.equals(trainNumber, other.trainNumber)
                    && Objects.equals(sequence, other.sequence)
                    && Objects.equals(vehicleId, other.vehicleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(trainNumber, sequence, vehicleId);
        }
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

        private static MutableAggregate fromRow(RailStopRecord row) {
            return new MutableAggregate(
                    row.getDate(),
                    row.getSequence(),
                    row.getTrainNumber(),
                    row.getVehicleId(),
                    row.getWarehouse()
            );
        }
    }
}
