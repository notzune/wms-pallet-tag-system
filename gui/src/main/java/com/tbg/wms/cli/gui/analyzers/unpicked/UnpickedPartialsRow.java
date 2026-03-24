package com.tbg.wms.cli.gui.analyzers.unpicked;

import java.time.LocalDateTime;

public record UnpickedPartialsRow(
        String warehouseId,
        LocalDateTime appointment,
        String orderNumber,
        String soldToCustomer,
        int orderedQuantity,
        int allocatedQuantity,
        int unallocatedQuantity,
        int completedQuantity,
        int remainingQuantity,
        String warehouseName,
        LocalDateTime fetchedAt,
        String soldToName,
        String addressLine1,
        String addressCity,
        String addressState,
        UnpickedPartialsRule rule
) {
}
