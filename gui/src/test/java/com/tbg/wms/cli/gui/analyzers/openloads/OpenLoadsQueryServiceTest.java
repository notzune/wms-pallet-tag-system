package com.tbg.wms.cli.gui.analyzers.openloads;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenLoadsQueryServiceTest {

    @Test
    void queryService_shouldMapPlatformAndStagedCounts() {
        OpenLoadsQueryService service = new OpenLoadsQueryService();

        OpenLoadsRow row = service.mapRow(new OpenLoadsQueryService.QueryRow(
                "3002",
                "243177",
                "8000574009",
                "8000574009",
                "DROP",
                "ABCD",
                "TRL123",
                "Y1",
                "Scheduled",
                LocalDateTime.of(2026, 3, 25, 8, 0),
                "ATL",
                "Customer",
                "Carrier",
                120,
                100,
                20,
                "PA",
                "Y",
                "RCV",
                "note",
                7,
                2,
                "SHORT"
        ));

        assertEquals("PA", row.platform());
        assertEquals(7, row.staged());
        assertEquals("SHORT", row.shortFlag());
    }
}
