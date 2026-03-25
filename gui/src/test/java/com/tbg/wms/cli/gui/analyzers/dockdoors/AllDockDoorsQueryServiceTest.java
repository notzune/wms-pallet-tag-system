package com.tbg.wms.cli.gui.analyzers.dockdoors;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllDockDoorsQueryServiceTest {

    @Test
    void queryService_shouldMapRossiAndShortFlags() {
        AllDockDoorsQueryService service = new AllDockDoorsQueryService();

        AllDockDoorsRow row = service.mapRow(new AllDockDoorsQueryService.QueryRow(
                "D01",
                "DROP",
                "INB",
                "TRL-1",
                "243177",
                LocalDateTime.of(2026, 3, 25, 8, 0),
                LocalDateTime.of(2026, 3, 25, 7, 30),
                LocalDateTime.of(2026, 3, 25, 7, 45),
                15,
                50.0,
                3.0,
                24.0,
                "SHORT",
                "Customer",
                "100003434",
                "100003434|100003434",
                "AIRBAG",
                12.0,
                8.0
        ));

        assertEquals("SHORT", row.shortFlag());
        assertEquals("AIRBAG", row.airbagFlag());
        assertEquals(12.0, row.rossiPallets());
        assertEquals(8.0, row.rossiCompletedPicks());
    }
}
