package com.tbg.wms.cli.gui.analyzers.openloads;

import java.time.LocalDateTime;

record OpenLoadsQueryRow(
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
        Integer casePicks,
        Integer completedCasePicks,
        Integer picksRemaining,
        String platform,
        String shippingDockFlag,
        String trailerCode,
        String noteText,
        Integer staged,
        Integer stopSequence,
        String shortFlag
) {
}
