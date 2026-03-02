/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.core.rail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * One flattened rail-load row used by the Word merge template.
 */
public final class RailStopRecord {
    private final String date;
    private final String sequence;
    private final String trainNumber;
    private final String vehicleId;
    private final String warehouse;
    private final String loadNumber;
    private final List<ItemQuantity> items;

    public RailStopRecord(String date,
                          String sequence,
                          String trainNumber,
                          String vehicleId,
                          String warehouse,
                          String loadNumber,
                          List<ItemQuantity> items) {
        this.date = normalize(date);
        this.sequence = normalize(sequence);
        this.trainNumber = normalize(trainNumber);
        this.vehicleId = normalize(vehicleId);
        this.warehouse = normalize(warehouse);
        this.loadNumber = normalize(loadNumber);
        this.items = items == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(items));
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

    public String getLoadNumber() {
        return loadNumber;
    }

    public List<ItemQuantity> getItems() {
        return items;
    }

    public static final class ItemQuantity {
        private final String itemNumber;
        private final int cases;

        public ItemQuantity(String itemNumber, int cases) {
            this.itemNumber = normalize(itemNumber);
            this.cases = cases;
        }

        public String getItemNumber() {
            return itemNumber;
        }

        public int getCases() {
            return cases;
        }

        public boolean isValid() {
            return !itemNumber.isBlank() && cases > 0;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ItemQuantity)) {
                return false;
            }
            ItemQuantity that = (ItemQuantity) other;
            return cases == that.cases && itemNumber.equals(that.itemNumber);
        }

        @Override
        public int hashCode() {
            return Objects.hash(itemNumber, cases);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
