package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Shared printer-routing lookups for the label workflow service.
 */
final class LabelWorkflowRoutingSupport {

    List<LabelWorkflowService.PrinterOption> loadPrinters(String siteCode, LabelWorkflowAssetSupport assetSupport)
            throws Exception {
        Objects.requireNonNull(siteCode, "siteCode cannot be null");
        Objects.requireNonNull(assetSupport, "assetSupport cannot be null");
        PrinterRoutingService routing = assetSupport.loadRouting(siteCode);
        List<LabelWorkflowService.PrinterOption> options = new ArrayList<>();
        for (PrinterConfig printer : routing.getPrinters().values()) {
            if (printer.isEnabled()) {
                options.add(new LabelWorkflowService.PrinterOption(
                        printer.getId(),
                        printer.getName(),
                        printer.getEndpoint(),
                        printer.getCapabilities()
                ));
            }
        }
        options.sort(Comparator.comparing(LabelWorkflowService.PrinterOption::getId));
        return options;
    }

    PrinterConfig resolvePrinter(String siteCode, String printerId, LabelWorkflowAssetSupport assetSupport)
            throws Exception {
        Objects.requireNonNull(siteCode, "siteCode cannot be null");
        Objects.requireNonNull(assetSupport, "assetSupport cannot be null");
        if (printerId == null || printerId.isBlank()) {
            return null;
        }
        return assetSupport.resolvePrinter(siteCode, printerId);
    }
}
