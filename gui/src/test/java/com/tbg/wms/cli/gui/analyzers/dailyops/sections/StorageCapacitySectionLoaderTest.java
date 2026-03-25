package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageCapacitySectionLoaderTest {

    @Test
    void storageLoader_shouldPreserveNullSeparatorRowsForLayoutBreaks() {
        StorageCapacitySectionLoader loader = new StorageCapacitySectionLoader();

        StorageCapacityRow row = loader.mapRow(new StorageCapacitySectionLoader.QueryRow(
                null,
                null,
                null,
                null,
                null
        ));

        assertTrue(row.separator());
        assertNull(row.buildingId());
    }
}
