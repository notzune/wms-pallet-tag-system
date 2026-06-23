package com.tbg.wms.v2.domain.label;

/**
 * Reference to one pallet label available for shipment label selection.
 */
public record LabelSelectionRef(String labelId, int sourceSequence) {
    public LabelSelectionRef {
        if (labelId == null || labelId.isBlank()) {
            throw new IllegalArgumentException("labelId is required");
        }
        if (sourceSequence < 1) {
            throw new IllegalArgumentException("sourceSequence must be positive");
        }
        labelId = labelId.trim();
    }

    public static LabelSelectionRef palletLabel(String labelId, int sourceSequence) {
        return new LabelSelectionRef(labelId, sourceSequence);
    }
}
