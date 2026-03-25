package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

public record StorageCapacityRow(
        String buildingId,
        Double pallets,
        Double positions,
        Double pctFull,
        Double emptyRacks,
        boolean separator
) {
}
