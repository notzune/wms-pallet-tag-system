package com.tbg.wms.core.label;

import org.junit.jupiter.api.Test;

import java.util.List;

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
}
