package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentsSectionLoaderTest {

    @Test
    void appointmentsLoader_shouldComputeRemainingCountsFromCompletedValues() {
        AppointmentsSectionLoader loader = new AppointmentsSectionLoader();

        AppointmentsRow row = loader.mapRow(new AppointmentsSectionLoader.QueryRow(
                LocalDate.of(2026, 3, 25),
                12,
                10,
                6,
                8,
                3
        ));

        assertEquals(4, row.outRemaining());
        assertEquals(5, row.inboundRemaining());
    }

    @Test
    void appointmentsQueryService_shouldUseDistinctAliasesForDateAndTruckCount() throws Exception {
        Field sqlField = AppointmentsSectionLoader.QueryService.class.getDeclaredField("SQL");
        sqlField.setAccessible(true);
        String sql = (String) sqlField.get(null);

        assertTrue(sql.contains("select a.apptday apptday"), "query should alias appointment day as apptday");
        assertTrue(sql.contains("count(distinct a.car_move_id) trucks"), "query should alias truck count as trucks");
    }
}
