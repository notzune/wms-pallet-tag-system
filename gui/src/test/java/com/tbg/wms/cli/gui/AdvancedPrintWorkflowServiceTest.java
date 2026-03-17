package com.tbg.wms.cli.gui;

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

        List<Lpn> filtered = AdvancedPrintWorkflowService.filterLpnsForPrint(
                List.of(first, second, third),
                List.of(third, first)
        );

        assertEquals(List.of("LPN-1", "LPN-3"), filtered.stream().map(Lpn::getLpnId).collect(Collectors.toList()));
    }

    @Test
    void filterLpnsForPrint_shouldRejectEmptySelection() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> AdvancedPrintWorkflowService.filterLpnsForPrint(List.of(), List.of()));

        assertEquals("Select at least one label to print.", ex.getMessage());
    }
}
