package com.tbg.wms.core.label;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LabelSelectionSupportTest {

    @Test
    void parseOneBasedSelection_shouldReturnAllWhenBlank() {
        assertEquals(List.of(1, 2, 3), LabelSelectionSupport.parseOneBasedSelection("", 3));
    }

    @Test
    void parseOneBasedSelection_shouldExpandRangesAndDeduplicateInNaturalOrder() {
        assertEquals(List.of(1, 3, 4, 5), LabelSelectionSupport.parseOneBasedSelection("5,3-4,1,3", 5));
    }

    @Test
    void parseOneBasedSelection_shouldRejectOutOfRangeValue() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> LabelSelectionSupport.parseOneBasedSelection("1,4", 3));

        assertEquals("Label index out of range: 4", ex.getMessage());
    }

    @Test
    void selectByOneBasedIndexes_shouldReturnSelectedItemsInAvailableOrder() {
        List<String> values = LabelSelectionSupport.selectByOneBasedIndexes(
                List.of("A", "B", "C", "D"),
                List.of(1, 3, 4)
        );

        assertEquals(List.of("A", "C", "D"), values);
    }

    @Test
    void buildShipmentSelections_shouldCreateSharedSelectionRefs() {
        List<LabelSelectionRef> selections = LabelSelectionSupport.buildShipmentSelections(
                "SHIP-1",
                List.of(
                        new Lpn("LPN-1", "SHIP-1", null, 0, 0, 0.0, null, null, null, null, null, List.of()),
                        new Lpn("LPN-2", "SHIP-1", null, 0, 0, 0.0, null, null, null, null, null, List.of())
                )
        );

        assertEquals(2, selections.size());
        assertEquals(1, selections.get(0).getOneBasedIndex());
        assertEquals("SHIP-1", selections.get(0).getShipmentId());
        assertEquals("LPN-2", selections.get(1).getLpnId());
    }

    @Test
    void selectByExpression_shouldUseSharedSelectionRefs() {
        List<LabelSelectionRef> selections = List.of(
                LabelSelectionRef.forShipment(1, "SHIP-1", "LPN-1"),
                LabelSelectionRef.forShipment(2, "SHIP-1", "LPN-2"),
                LabelSelectionRef.forCarrierMove(3, "SHIP-2", "LPN-3", 4)
        );

        List<LabelSelectionRef> selected = LabelSelectionSupport.selectByExpression(selections, "1,3");

        assertEquals(List.of("LPN-1", "LPN-3"), selected.stream().map(LabelSelectionRef::getLpnId).collect(Collectors.toList()));
        assertEquals(4, selected.get(1).getStopPosition());
    }

    @Test
    void selectLpnsByRefs_shouldKeepAvailableOrder() {
        Lpn first = new Lpn("LPN-1", "SHIP-1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn second = new Lpn("LPN-2", "SHIP-1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        Lpn third = new Lpn("LPN-3", "SHIP-1", null, 0, 0, 0.0, null, null, null, null, null, List.of());

        List<Lpn> selected = LabelSelectionSupport.selectLpnsByRefs(
                List.of(first, second, third),
                List.of(
                        LabelSelectionRef.forShipment(3, "SHIP-1", "LPN-3"),
                        LabelSelectionRef.forShipment(1, "SHIP-1", "LPN-1")
                )
        );

        assertEquals(List.of("LPN-1", "LPN-3"), selected.stream().map(Lpn::getLpnId).collect(Collectors.toList()));
    }
}
