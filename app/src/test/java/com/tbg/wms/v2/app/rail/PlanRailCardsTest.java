package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlanRailCardsTest {
    private final PlanRailCards planner = new PlanRailCards();

    @Test
    void planRailCards_groupsRowsAndComputesCeilingPalletsByFamily() {
        List<RailStopRecord> rows = List.of(
                row("03-22-26", "142", "train1", "car1", "BR", "LOAD2",
                        List.of(new RailItemQuantity("DOM1", 150), new RailItemQuantity("CAN1", 76))),
                row("03-22-26", "142", "train1", "car1", "BR", "LOAD1",
                        List.of(new RailItemQuantity("DOM1", 51), new RailItemQuantity("KEV1", 25))),
                row("03-22-26", "143", "train1", "car2", "BR", "LOAD3",
                        List.of(new RailItemQuantity("DOM1", 10)))
        );

        RailPlan plan = planner.plan("train1", rows, Map.of(
                "DOM1", new RailFamilyFootprint("DOM1", "DOM", 100),
                "CAN1", new RailFamilyFootprint("CAN1", "CAN", 75),
                "KEV1", new RailFamilyFootprint("KEV1", "KEV", 25)
        ));

        assertEquals("TRAIN1", plan.trainId());
        assertEquals(2, plan.cards().size());
        assertEquals("142", plan.cards().get(0).sequence());
        assertEquals("LOAD1, LOAD2", plan.cards().get(0).loadNumbers());
        assertEquals(List.of("DOM1", "CAN1", "KEV1"), plan.cards().get(0).itemLines().stream()
                .map(RailItemQuantity::itemNumber)
                .toList());
        assertEquals(3, plan.cards().get(0).domPallets());
        assertEquals(2, plan.cards().get(0).canPallets());
        assertEquals(1, plan.cards().get(0).kevPallets());
        assertEquals("143", plan.cards().get(1).sequence());
        assertEquals(1, plan.cards().get(1).domPallets());
    }

    @Test
    void planRailCards_tracksMissingFootprints() {
        RailPlan plan = planner.plan("train1", List.of(
                row("03-22-26", "142", "TRAIN1", "CAR1", "BR", "LOAD1",
                        List.of(new RailItemQuantity("MISS1", 20)))
        ), Map.of());

        assertEquals(List.of("MISS1"), plan.missingFootprintItems());
        assertEquals(List.of("MISS1"), plan.cards().get(0).missingFootprintItems());
    }

    private static RailStopRecord row(
            String date,
            String sequence,
            String trainNumber,
            String vehicleId,
            String warehouse,
            String loadNumber,
            List<RailItemQuantity> items
    ) {
        return new RailStopRecord(date, sequence, trainNumber, vehicleId, warehouse, loadNumber, items);
    }
}
