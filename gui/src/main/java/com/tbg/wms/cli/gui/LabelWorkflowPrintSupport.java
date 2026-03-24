/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.template.ZplTemplateEngine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Shared shipment-label rendering and dispatch helpers for the GUI workflow.
 */
final class LabelWorkflowPrintSupport {

    private final PrintDispatcher printDispatcher;

    LabelWorkflowPrintSupport() {
        this(new NetworkPrintDispatcher());
    }

    LabelWorkflowPrintSupport(PrintDispatcher printDispatcher) {
        this.printDispatcher = Objects.requireNonNull(printDispatcher, "printDispatcher cannot be null");
    }

    LabelWorkflowService.PrintResult print(
            LabelWorkflowService.PreparedJob job,
            PrinterConfig printer,
            Path outputDir,
            boolean printToFile
    ) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        Objects.requireNonNull(outputDir, "outputDir cannot be null");

        Files.createDirectories(outputDir);

        LabelDataBuilder builder = new LabelDataBuilder(job.getSkuMapping(), job.getSiteConfig(), job.getFootprintBySku());
        Shipment shipmentForLabels = LabelingSupport.buildShipmentForLabeling(job.getShipment(), job.getLpnsForLabels());

        int printedCount = 0;
        for (int i = 0; i < job.getLpnsForLabels().size(); i++) {
            Lpn lpn = job.getLpnsForLabels().get(i);
            Map<String, String> labelData = builder.build(shipmentForLabels, lpn, i, LabelType.WALMART_CANADA_GRID);
            if (job.isUsingVirtualLabels()) {
                labelData = new HashMap<>(labelData);
                labelData.put("palletSeq", String.valueOf(i + 1));
                labelData.put("palletTotal", String.valueOf(job.getLpnsForLabels().size()));
            }

            String zpl = ZplTemplateEngine.generate(job.getTemplate(), labelData);
            String fileName = String.format("%s_%s_%d_of_%d.zpl",
                    job.getShipmentId(), lpn.getLpnId(), i + 1, job.getLpnsForLabels().size());
            Path zplFile = outputDir.resolve(fileName);
            Files.writeString(zplFile, zpl);
            if (!printToFile) {
                printDispatcher.print(printer, zpl, lpn.getLpnId());
            }
            printedCount++;
        }

        if (printToFile) {
            return LabelWorkflowService.PrintResult.printToFile(printedCount, outputDir.toAbsolutePath());
        }
        return new LabelWorkflowService.PrintResult(
                printedCount,
                outputDir.toAbsolutePath(),
                printer.getId(),
                printer.getEndpoint()
        );
    }

    interface PrintDispatcher {
        void print(PrinterConfig printer, String zpl, String labelId);
    }

    private static final class NetworkPrintDispatcher implements PrintDispatcher {
        private final NetworkPrintService printService = new NetworkPrintService();

        @Override
        public void print(PrinterConfig printer, String zpl, String labelId) {
            printService.print(printer, zpl, labelId);
        }
    }
}
