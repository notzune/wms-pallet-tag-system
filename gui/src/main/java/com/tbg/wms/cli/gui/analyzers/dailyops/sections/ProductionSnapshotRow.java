package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import java.time.LocalDateTime;

public record ProductionSnapshotRow(
        String workOrder,
        String itemNumber,
        LocalDateTime lastReceivedTime,
        int palletsProduced
) {
}
