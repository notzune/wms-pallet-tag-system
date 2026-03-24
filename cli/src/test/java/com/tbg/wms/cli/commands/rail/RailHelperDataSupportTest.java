/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.rail.RailFamilyFootprint;
import com.tbg.wms.core.rail.RailStopRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailHelperDataSupportTest {

    private static final DateTimeFormatter DEFAULT_DATE = DateTimeFormatter.ofPattern("MM-dd-yy");
    private final RailHelperDataSupport support = new RailHelperDataSupport();

    @TempDir
    Path tempDir;

    @Test
    void loadRailRecordsBuildsSortedStopsAndFallsBackMissingDate() throws Exception {
        Path inputCsv = tempDir.resolve("input.csv");
        Files.write(inputCsv, List.of(
                "SEQ,TRAIN_NBR,VEHICLE_ID,DCS_WHSE,LOAD_NBR,ITEM_NBR_2,TOTAL_CS_ITM_2,ITEM_NBR_1,TOTAL_CS_ITM_1",
                "143,TRAIN-B,CAR-200,D-2,LOAD-2,01832,500,00818,200",
                "142,TRAIN-A,CAR-100,D-2,LOAD-1,01832,100,,0"
        ));

        List<RailStopRecord> records = support.loadRailRecords(inputCsv);

        assertEquals(2, records.size());
        assertEquals("TRAIN-A", records.get(0).getTrainNumber());
        assertEquals(LocalDate.now().format(DEFAULT_DATE), records.get(0).getDate());
        assertEquals(List.of("01832"), itemNumbers(records.get(0).getItems()));
        assertEquals(List.of("00818", "01832"), itemNumbers(records.get(1).getItems()));
    }

    @Test
    void loadFootprintMapAcceptsAlternateHeaders() throws Exception {
        Path footprintCsv = tempDir.resolve("footprint.csv");
        Files.write(footprintCsv, List.of(
                "ITEM,FAMILY,CASES_PER_PALLET",
                "01832,DOM,100"
        ));

        Map<String, RailFamilyFootprint> footprints = support.loadFootprintMap(footprintCsv);

        assertEquals(1, footprints.size());
        assertEquals(100, footprints.get("01832").getCasesPerPallet());
    }

    private static List<String> itemNumbers(List<RailStopRecord.ItemQuantity> items) {
        return items.stream().map(RailStopRecord.ItemQuantity::getItemNumber).toList();
    }
}
