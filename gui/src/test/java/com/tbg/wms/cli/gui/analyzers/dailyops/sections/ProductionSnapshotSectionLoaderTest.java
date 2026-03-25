package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProductionSnapshotSectionLoaderTest {

    @Test
    void productionLoader_shouldMapRecentWorkOrders() {
        ProductionSnapshotSectionLoader loader = new ProductionSnapshotSectionLoader();

        ProductionSnapshotRow row = loader.mapRow(new ProductionSnapshotSectionLoader.QueryRow(
                "WO-1",
                "SKU-1",
                LocalDateTime.of(2026, 3, 25, 10, 30),
                14
        ));

        assertEquals("WO-1", row.workOrder());
        assertEquals("SKU-1", row.itemNumber());
        assertEquals(14, row.palletsProduced());
    }
}
