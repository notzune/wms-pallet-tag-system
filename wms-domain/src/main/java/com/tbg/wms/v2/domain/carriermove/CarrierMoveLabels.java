package com.tbg.wms.v2.domain.carriermove;

import java.util.List;

/**
 * Carrier move label data that has already been resolved into stop shipment groups.
 */
public record CarrierMoveLabels(
        String carrierMoveId,
        List<PreparedStopGroup> stops,
        boolean includeInfoTags
) {
    public CarrierMoveLabels {
        if (carrierMoveId == null || carrierMoveId.isBlank()) {
            throw new IllegalArgumentException("carrierMoveId is required");
        }
        if (stops == null) {
            throw new IllegalArgumentException("stops is required");
        }
        carrierMoveId = carrierMoveId.trim();
        stops = List.copyOf(stops);
    }

    public static CarrierMoveLabels of(
            String carrierMoveId,
            List<PreparedStopGroup> stops,
            boolean includeInfoTags
    ) {
        return new CarrierMoveLabels(carrierMoveId, stops, includeInfoTags);
    }
}
