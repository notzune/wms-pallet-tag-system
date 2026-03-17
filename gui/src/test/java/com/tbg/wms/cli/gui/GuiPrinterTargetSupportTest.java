package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPrinterTargetSupportTest {

    @Test
    void filterLabelScreenPrinters_shouldKeepOfficeAndDispatchOnly() {
        List<LabelWorkflowService.PrinterOption> filtered = GuiPrinterTargetSupport.filterLabelScreenPrinters(List.of(
                new LabelWorkflowService.PrinterOption("OFFICE", "Office", "1"),
                new LabelWorkflowService.PrinterOption("ORDER_PICK", "Order Pick", "2"),
                new LabelWorkflowService.PrinterOption("DISPATCH", "Dispatch", "3")
        ));

        assertEquals(List.of("OFFICE", "DISPATCH"),
                filtered.stream().map(LabelWorkflowService.PrinterOption::getId).collect(Collectors.toList()));
    }

    @Test
    void filterRailToolPrinters_shouldKeepRailTargetsOnly() {
        List<LabelWorkflowService.PrinterOption> filtered = GuiPrinterTargetSupport.filterRailToolPrinters(List.of(
                new LabelWorkflowService.PrinterOption("OFFICE", "Office", "1"),
                new LabelWorkflowService.PrinterOption("ORDER_PICK", "Order Pick", "2"),
                new LabelWorkflowService.PrinterOption("RAIL_OFFICE", "Rail", "3")
        ));

        assertEquals(List.of("ORDER_PICK", "RAIL_OFFICE"),
                filtered.stream().map(LabelWorkflowService.PrinterOption::getId).collect(Collectors.toList()));
    }

    @Test
    void buildPrintToFileOption_shouldUseFileSentinel() {
        LabelWorkflowService.PrinterOption option = GuiPrinterTargetSupport.buildPrintToFileOption(Path.of("out"));

        assertEquals(GuiPrinterTargetSupport.FILE_PRINTER_ID, option.getId());
        assertTrue(GuiPrinterTargetSupport.isPrintToFile(option));
    }
}
