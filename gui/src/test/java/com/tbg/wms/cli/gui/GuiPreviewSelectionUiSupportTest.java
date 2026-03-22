/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPreviewSelectionUiSupportTest {
    private final GuiPreviewSelectionUiSupport support = new GuiPreviewSelectionUiSupport();

    @Test
    void selectionTextHelpers_shouldDescribeState() {
        assertEquals(
                "Selected 2 of 5 labels | Info Tags 1 | Total Documents 3",
                support.selectionStatusText(2, 5, 1, 3)
        );
        assertEquals("Deselect All", support.selectionToggleText(5, 5));
        assertEquals("Select All", support.selectionToggleText(2, 5));
        assertEquals("Label Selection [expanded]", support.collapseButtonText(true));
        assertEquals("Label Selection [collapsed]", support.collapseButtonText(false));
    }

    @Test
    void shouldEnablePrint_shouldRespectModeAndSelection() {
        assertTrue(support.shouldEnablePrint(true, true, false, 1, 0));
        assertFalse(support.shouldEnablePrint(true, true, false, 0, 2));
        assertTrue(support.shouldEnablePrint(false, false, true, 0, 2));
        assertFalse(support.shouldEnablePrint(false, false, false, 0, 2));
    }
}
