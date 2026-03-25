package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CasePickShiftThroughputSectionLoaderTest {

    @Test
    void shiftLoader_shouldMapAllShiftBucketsInDisplayOrder() {
        CasePickShiftThroughputSectionLoader loader = new CasePickShiftThroughputSectionLoader();

        CasePickShiftThroughputRow row = loader.mapRow(new CasePickShiftThroughputSectionLoader.QueryRow(
                LocalDate.of(2026, 3, 25),
                10,
                20,
                30,
                40
        ));

        assertEquals(10, row.thirdA());
        assertEquals(20, row.first());
        assertEquals(30, row.second());
        assertEquals(40, row.thirdB());
    }
}
