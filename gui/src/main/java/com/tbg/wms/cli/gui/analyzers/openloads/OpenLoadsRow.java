package com.tbg.wms.cli.gui.analyzers.openloads;

import java.time.LocalDateTime;

public record OpenLoadsRow(
        String warehouseId,
        String carrierMoveId,
        String orderNumber,
        String shipmentId,
        String dropLive,
        String carrierCode,
        String trailerNumber,
        String yardLocation,
        String shipmentStatus,
        LocalDateTime appointment,
        String destinationLocation,
        String customer,
        String carrierName,
        int casePicks,
        int completedCasePicks,
        int picksRemaining,
        String platform,
        String shippingDockFlag,
        String trailerCode,
        String noteText,
        int staged,
        int stopSequence,
        String shortFlag
) {
}
