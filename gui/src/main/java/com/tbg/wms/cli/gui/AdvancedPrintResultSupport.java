/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;

import java.nio.file.Paths;

/**
 * Helper for printer resolution and checkpoint-to-result mapping.
 */
final class AdvancedPrintResultSupport {
    PrinterConfig resolvePrinterForPrint(PrinterRoutingService routing, String printerId, boolean printToFile) {
        if (printToFile) {
            return null;
        }
        String id = printerId == null ? "" : printerId.trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("Printer is required.");
        }
        return routing.findPrinter(id)
                .orElseThrow(() -> new IllegalArgumentException("Printer not found or disabled: " + id));
    }

    AdvancedPrintWorkflowService.PrintResult toResult(AdvancedPrintWorkflowService.JobCheckpoint checkpoint) {
        int labels = 0;
        int info = 0;
        for (AdvancedPrintWorkflowService.PrintTask task : checkpoint.tasks) {
            if (task.kind == AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL) {
                labels++;
            } else {
                info++;
            }
        }
        return new AdvancedPrintWorkflowService.PrintResult(
                labels,
                info,
                Paths.get(checkpoint.outputDirectory),
                checkpoint.printerId,
                checkpoint.printerEndpoint,
                checkpoint.printToFile
        );
    }
}
