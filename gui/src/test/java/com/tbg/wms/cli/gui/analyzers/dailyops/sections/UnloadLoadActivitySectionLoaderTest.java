package com.tbg.wms.cli.gui.analyzers.dailyops.sections;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UnloadLoadActivitySectionLoaderTest {

    @Test
    void unloadLoadLoader_shouldMapRailAndTruckSeriesSeparately() {
        UnloadLoadActivitySectionLoader loader = new UnloadLoadActivitySectionLoader();

        List<UnloadLoadActivityRow> rows = List.of(
                loader.mapMetricRow("Rail Loads", new UnloadLoadActivitySectionLoader.QueryRow(
                        LocalDate.of(2026, 3, 25), 1, 2, 3, 4
                )),
                loader.mapMetricRow("Truck Loads", new UnloadLoadActivitySectionLoader.QueryRow(
                        LocalDate.of(2026, 3, 25), 5, 6, 7, 8
                ))
        );

        assertEquals("Rail Loads", rows.get(0).metric());
        assertEquals(4, rows.get(0).thirdB());
        assertEquals("Truck Loads", rows.get(1).metric());
        assertEquals(6, rows.get(1).first());
    }
}
