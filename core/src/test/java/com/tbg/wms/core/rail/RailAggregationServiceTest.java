package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class RailAggregationServiceTest {

    @Test
    void aggregateByRailcarCombinesRowsForSameRailcarOnly() {
        RailStopRecord row1 = new RailStopRecord(
                "03-04-26", "142", "0124", "TPIX3004", "BR", "LOAD1",
                List.of(new RailStopRecord.ItemQuantity("01830", 100))
        );
        RailStopRecord row2 = new RailStopRecord(
                "03-04-26", "142", "0124", "TPIX3004", "BR", "LOAD2",
                List.of(new RailStopRecord.ItemQuantity("01830", 50), new RailStopRecord.ItemQuantity("01831", 20))
        );
        RailStopRecord row3 = new RailStopRecord(
                "03-04-26", "143", "0124", "TPIX3010", "BR", "LOAD3",
                List.of(new RailStopRecord.ItemQuantity("01830", 60))
        );

        List<RailCarAggregate> cars = new RailAggregationService().aggregateByRailcar(List.of(row1, row2, row3));

        assertEquals(2, cars.size());
        assertEquals("142", cars.get(0).getSequence());
        assertEquals(150, cars.get(0).getCasesByItem().get("01830"));
        assertEquals(20, cars.get(0).getCasesByItem().get("01831"));
        assertEquals("143", cars.get(1).getSequence());
        assertEquals(60, cars.get(1).getCasesByItem().get("01830"));
    }
}
