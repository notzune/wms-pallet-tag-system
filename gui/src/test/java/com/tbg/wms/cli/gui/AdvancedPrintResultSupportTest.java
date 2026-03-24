/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AdvancedPrintResultSupportTest {
    private final AdvancedPrintResultSupport support = new AdvancedPrintResultSupport();

    @Test
    void resolvePrinterForPrint_shouldRespectPrintToFileAndValidateIds() {
        PrinterConfig printer = new PrinterConfig("P1", "Printer 1", "10.0.0.1", 9100, List.of("TEST"), List.of("ZPL"), "", true);
        PrinterRoutingService routing = new PrinterRoutingService(Map.of("P1", printer), List.of(), "P1", "TBG3002");

        assertNull(support.resolvePrinterForPrint(routing, "P1", true));
        assertEquals(printer, support.resolvePrinterForPrint(routing, "P1", false));
        assertThrows(IllegalArgumentException.class, () -> support.resolvePrinterForPrint(routing, "", false));
    }

    @Test
    void toResult_shouldCountLabelAndInfoTasks() {
        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = new AdvancedPrintWorkflowService.JobCheckpoint();
        checkpoint.id = "job-1";
        checkpoint.mode = AdvancedPrintWorkflowService.InputMode.SHIPMENT;
        checkpoint.sourceId = "SHIP123";
        checkpoint.outputDirectory = "out\\gui-SHIP123";
        checkpoint.printToFile = false;
        checkpoint.printerId = "P1";
        checkpoint.printerEndpoint = "10.0.0.1";
        checkpoint.createdAt = LocalDateTime.now();
        checkpoint.updatedAt = LocalDateTime.now();
        checkpoint.tasks = List.of(
                new AdvancedPrintWorkflowService.PrintTask(AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL, "a.zpl", "^XA", "L1"),
                new AdvancedPrintWorkflowService.PrintTask(AdvancedPrintWorkflowService.TaskKind.STOP_INFO_TAG, "b.zpl", "^XA", "S1"),
                new AdvancedPrintWorkflowService.PrintTask(AdvancedPrintWorkflowService.TaskKind.FINAL_INFO_TAG, "c.zpl", "^XA", "F1")
        );

        AdvancedPrintWorkflowService.PrintResult result = support.toResult(checkpoint);

        assertEquals(1, result.getLabelsPrinted());
        assertEquals(2, result.getInfoTagsPrinted());
        assertEquals("P1", result.getPrinterId());
    }
}
