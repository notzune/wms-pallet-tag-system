package com.tbg.wms.cli.gui.analyzers.openloads;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenLoadsQueryServiceTest {

    @Test
    void sql_shouldExposeOpenLoadsQueryWithExpectedProjectionAndOrdering() {
        String sql = OpenLoadsSql.query();

        assertTrue(sql.contains("ssv.wh_id"));
        assertTrue(sql.contains("pickers.ordnum"));
        assertTrue(sql.contains("casepicks"));
        assertTrue(sql.contains("order by appt,car_move_id,stop_seq"));
    }

    @Test
    void rowMapper_shouldMapPlatformAndStagedCounts() {
        OpenLoadsRowMapper mapper = new OpenLoadsRowMapper();

        OpenLoadsRow row = mapper.map(new OpenLoadsQueryRow(
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

    @Test
    void rowMapper_shouldDefaultMissingNumericValuesToZero() {
        OpenLoadsRowMapper mapper = new OpenLoadsRowMapper();

        OpenLoadsRow row = mapper.map(new OpenLoadsQueryRow(
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
                null,
                null,
                null,
                "PA",
                "Y",
                "RCV",
                "note",
                null,
                null,
                null
        ));

        assertEquals(0, row.casePicks());
        assertEquals(0, row.completedCasePicks());
        assertEquals(0, row.picksRemaining());
        assertEquals(0, row.staged());
        assertEquals(0, row.stopSequence());
    }
}
