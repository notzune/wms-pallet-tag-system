package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiPrinterTargetSupportTest {

    @Test
    void filterLabelScreenPrinters_shouldKeepZplCapablePrintersOnly() {
        List<LabelWorkflowService.PrinterOption> filtered = GuiPrinterTargetSupport.filterLabelScreenPrinters(List.of(
                new LabelWorkflowService.PrinterOption("OFFICE", "Office", "1", List.of("ZPL")),
                new LabelWorkflowService.PrinterOption("ORDER_PICK", "Order Pick", "2", List.of("RAIL")),
                new LabelWorkflowService.PrinterOption("DISPATCH", "Dispatch", "3", List.of("ZPL"))
        ));

        assertEquals(List.of("OFFICE", "DISPATCH"),
                filtered.stream().map(LabelWorkflowService.PrinterOption::getId).collect(Collectors.toList()));
    }

    @Test
    void filterRailToolPrinters_shouldKeepRailCapablePrintersOnly() {
        List<LabelWorkflowService.PrinterOption> filtered = GuiPrinterTargetSupport.filterRailToolPrinters(List.of(
                new LabelWorkflowService.PrinterOption("OFFICE", "Office", "1", List.of("ZPL")),
                new LabelWorkflowService.PrinterOption("ORDER_PICK", "Order Pick", "2", List.of("RAIL")),
                new LabelWorkflowService.PrinterOption("RAIL_OFFICE", "Rail", "3", List.of("RAIL"))
        ));

        assertEquals(List.of("ORDER_PICK", "RAIL_OFFICE"),
                filtered.stream().map(LabelWorkflowService.PrinterOption::getId).collect(Collectors.toList()));
    }

    @Test
    void hasCapability_shouldMatchCaseInsensitively() {
        LabelWorkflowService.PrinterOption option =
                new LabelWorkflowService.PrinterOption("OFFICE", "Office", "1", List.of("zpl"));

        assertTrue(GuiPrinterTargetSupport.hasCapability(option, "ZPL"));
    }

    @Test
    void buildPrintToFileOption_shouldUseFileSentinel() {
        LabelWorkflowService.PrinterOption option = GuiPrinterTargetSupport.buildPrintToFileOption(Path.of("out"));

        assertEquals(GuiPrinterTargetSupport.FILE_PRINTER_ID, option.getId());
        assertTrue(GuiPrinterTargetSupport.isPrintToFile(option));
    }
}
