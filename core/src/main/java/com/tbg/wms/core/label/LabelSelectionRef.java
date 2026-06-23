package com.tbg.wms.core.label;

import java.util.Objects;

/**
 * Shared immutable identity for a selectable label across shipment and carrier workflows.
 */
public final class LabelSelectionRef {
    private final int oneBasedIndex;
    private final String shipmentId;
    private final String lpnId;
    private final Integer stopPosition;

    private LabelSelectionRef(int oneBasedIndex, String shipmentId, String lpnId, Integer stopPosition) {
        if (oneBasedIndex < 1) {
            throw new IllegalArgumentException("oneBasedIndex must be >= 1");
        }
        if (stopPosition != null && stopPosition < 1) {
            throw new IllegalArgumentException("stopPosition must be >= 1");
        }
        this.oneBasedIndex = oneBasedIndex;
        this.shipmentId = normalizeRequired(shipmentId, "shipmentId");
        this.lpnId = normalizeRequired(lpnId, "lpnId");
        this.stopPosition = stopPosition;
    }

    public static LabelSelectionRef forShipment(int oneBasedIndex, String shipmentId, String lpnId) {
        return new LabelSelectionRef(oneBasedIndex, shipmentId, lpnId, null);
    }

    public static LabelSelectionRef forCarrierMove(int oneBasedIndex, String shipmentId, String lpnId, int stopPosition) {
        return new LabelSelectionRef(oneBasedIndex, shipmentId, lpnId, stopPosition);
    }

    public int getOneBasedIndex() {
        return oneBasedIndex;
    }

    public String getShipmentId() {
        return shipmentId;
    }

    public String getLpnId() {
        return lpnId;
    }

    public Integer getStopPosition() {
        return stopPosition;
    }

    public boolean isCarrierMoveSelection() {
        return stopPosition != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof LabelSelectionRef that)) {
            return false;
        }
        return oneBasedIndex == that.oneBasedIndex
                && shipmentId.equals(that.shipmentId)
                && lpnId.equals(that.lpnId)
                && Objects.equals(stopPosition, that.stopPosition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oneBasedIndex, shipmentId, lpnId, stopPosition);
    }

    @Override
    public String toString() {
        return "LabelSelectionRef{"
                + "oneBasedIndex=" + oneBasedIndex
                + ", shipmentId='" + shipmentId + '\''
                + ", lpnId='" + lpnId + '\''
                + ", stopPosition=" + stopPosition
                + '}';
    }

    private static String normalizeRequired(String value, String fieldName) {
        String normalized = Objects.requireNonNull(value, fieldName + " cannot be null").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
        return normalized;
    }
}
