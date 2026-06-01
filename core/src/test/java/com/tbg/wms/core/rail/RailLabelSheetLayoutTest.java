/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RailLabelSheetLayoutTest {

    @Test
    void defaultLayout_shouldUseApprovedPhysicalMedia() {
        RailLabelSheetLayout layout = RailLabelSheetLayout.defaultLayout();

        assertEquals(612.0f, layout.pageWidthPoints(), 0.001f);
        assertEquals(792.0f, layout.pageHeightPoints(), 0.001f);
        assertEquals(288.0f, layout.labelWidthPoints(), 0.001f);
        assertEquals(144.0f, layout.labelHeightPoints(), 0.001f);
        assertEquals(10, layout.slotsPerPage());
    }

    @Test
    void slotBounds_shouldMatchTwoByFiveSheet() {
        RailLabelSheetLayout layout = RailLabelSheetLayout.defaultLayout();

        RailLabelSheetLayout.LabelSlot first = layout.slot(0);
        RailLabelSheetLayout.LabelSlot second = layout.slot(1);
        RailLabelSheetLayout.LabelSlot rowTwo = layout.slot(2);

        assertEquals(7.65f, first.left(), 0.001f);
        assertEquals(756.0f, first.top(), 0.001f);
        assertEquals(612.0f, first.bottom(), 0.001f);
        assertEquals(309.15f, second.left(), 0.001f);
        assertEquals(756.0f, second.top(), 0.001f);
        assertEquals(612.0f, second.bottom(), 0.001f);
        assertEquals(7.65f, rowTwo.left(), 0.001f);
        assertEquals(612.0f, rowTwo.top(), 0.001f);
        assertEquals(468.0f, rowTwo.bottom(), 0.001f);
    }

    @Test
    void slotBounds_shouldShiftEntireTemplateFiveHundredthsInchLeft() {
        RailLabelSheetLayout layout = RailLabelSheetLayout.defaultLayout();

        assertEquals(7.65f, layout.slot(0).left(), 0.001f);
        assertEquals(309.15f, layout.slot(1).left(), 0.001f);
        assertEquals(7.65f, layout.slot(8).left(), 0.001f);
        assertEquals(309.15f, layout.slot(9).left(), 0.001f);
    }

    @Test
    void slot_shouldRejectOutOfRangeSlotIndex() {
        RailLabelSheetLayout layout = RailLabelSheetLayout.defaultLayout();

        assertThrows(IllegalArgumentException.class, () -> layout.slot(-1));
        assertThrows(IllegalArgumentException.class, () -> layout.slot(10));
    }
}
