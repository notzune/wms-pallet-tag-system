package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdvancedPrintWorkflowServiceTest {

    @Test
    void filterLpnsForPrint_shouldKeepOriginalOrderForSelectedSubset() {
        Lpn first = new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn second = new Lpn("LPN-2", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn third = new Lpn("LPN-3", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of());

        List<Lpn> filtered = PrintTaskPlanner.filterLpnsForPrint(
                List.of(first, second, third),
                List.of(third, first)
        );

        assertEquals(List.of("LPN-1", "LPN-3"), filtered.stream().map(Lpn::getLpnId).collect(Collectors.toList()));
    }

    @Test
    void filterLpnsForPrint_shouldRejectEmptySelection() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PrintTaskPlanner.filterLpnsForPrint(List.of(), List.of()));

        assertEquals("Select at least one label to print.", ex.getMessage());
    }

    @Test
    void countShipmentInfoTags_shouldRespectSelectionAndToggle() {
        assertEquals(1, PrintTaskPlanner.countShipmentInfoTags(2, true));
        assertEquals(0, PrintTaskPlanner.countShipmentInfoTags(0, true));
        assertEquals(0, PrintTaskPlanner.countShipmentInfoTags(2, false));
    }

    @Test
    void countCarrierMoveInfoTags_shouldCountDistinctSelectedStopsPlusFinalTag() {
        List<LabelSelectionRef> selected = List.of(
                LabelSelectionRef.forCarrierMove(1, "S1", "L1", 1),
                LabelSelectionRef.forCarrierMove(2, "S1", "L2", 1),
                LabelSelectionRef.forCarrierMove(3, "S2", "L3", 3)
        );

        assertEquals(3, PrintTaskPlanner.countCarrierMoveInfoTags(selected, true));
        assertEquals(0, PrintTaskPlanner.countCarrierMoveInfoTags(selected, false));
        assertEquals(0, PrintTaskPlanner.countCarrierMoveInfoTags(List.of(), true));
    }

    @Test
    void countCarrierMoveInfoTags_shouldNotEmitFinalTagWhenSelectionsLackStops() {
        List<LabelSelectionRef> selected = List.of(
                LabelSelectionRef.forShipment(1, "S1", "L1"),
                LabelSelectionRef.forShipment(2, "S1", "L2")
        );

        assertEquals(0, PrintTaskPlanner.countCarrierMoveInfoTags(selected, true));
    }
}
