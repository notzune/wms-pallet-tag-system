/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiPrinterSelectionSupportTest {
    private final GuiPrinterSelectionSupport support = new GuiPrinterSelectionSupport();

    @Test
    void resolveSelectionIndex_shouldRestoreMatchingPrinterOrFallbackToFirst() {
        List<LabelWorkflowService.PrinterOption> candidates = List.of(
                new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1"),
                new LabelWorkflowService.PrinterOption("P2", "Printer 2", "10.0.0.2")
        );

        assertEquals(1, support.resolveSelectionIndex(new LabelWorkflowService.PrinterOption("P2", "Printer 2", "10.0.0.2"), candidates));
        assertEquals(0, support.resolveSelectionIndex(new LabelWorkflowService.PrinterOption("PX", "Printer X", "10.0.0.9"), candidates));
        assertEquals(0, support.resolveSelectionIndex(null, candidates));
    }

    @Test
    void printerLoadStatusMessage_shouldDescribePrinterAvailability() {
        assertEquals("No enabled printers found in routing config.", support.printerLoadStatusMessage(0, 0));
        assertEquals("No enabled printers found. Print to file available.", support.printerLoadStatusMessage(0, 1));
        assertEquals("Printers loaded.", support.printerLoadStatusMessage(2, 3));
    }
}
