package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
