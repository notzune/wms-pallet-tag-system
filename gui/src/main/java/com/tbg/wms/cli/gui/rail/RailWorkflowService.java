/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.GuiPrinterTargetSupport;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.rail.RailCarCard;
import com.tbg.wms.core.rail.RailCardRenderer;
import com.tbg.wms.core.rail.RailDbRepository;
import com.tbg.wms.core.rail.RailPrintService;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.OracleDbQueryRepository;
import com.tbg.wms.db.WmsRailDbRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GUI bridge around the shared core rail workflow.
 */
public final class RailWorkflowService {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private final AppConfig config;

    public RailWorkflowService(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    /**
     * Loads and prepares railcard preview data from WMS.
     *
     * @param trainId train identifier entered by the operator
     * @return prepared immutable preview payload
     */
    public PreparedRailJob prepareRailJob(String trainId) throws Exception {
        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            RailDbRepository repository = new WmsRailDbRepository(new OracleDbQueryRepository(pool.getDataSource()));
            com.tbg.wms.core.rail.RailWorkflowService workflow =
                    new com.tbg.wms.core.rail.RailWorkflowService(repository);
            com.tbg.wms.core.rail.RailWorkflowService.RailWorkflowResult result = workflow.prepare(trainId);
            return new PreparedRailJob(result);
        }
    }

    public List<LabelWorkflowService.PrinterOption> loadRailPrinters() throws Exception {
        PrinterRoutingService routing = PrinterRoutingService.load(config.activeSiteCode(), Path.of("config"));
        List<LabelWorkflowService.PrinterOption> printers = new ArrayList<>();
        for (PrinterConfig printer : routing.getPrinters().values()) {
            if (printer.isEnabled()) {
                printers.add(new LabelWorkflowService.PrinterOption(
                        printer.getId(),
                        printer.getName(),
                        printer.getEndpoint(),
                        printer.getCapabilities()
                ));
            }
        }
        printers.sort(java.util.Comparator.comparing(LabelWorkflowService.PrinterOption::getId));
        return GuiPrinterTargetSupport.filterRailToolPrinters(printers);
    }

    /**
     * Renders cards to PDF and optionally sends the result to printer.
     *
     * @param job       prepared job produced by {@link #prepareRailJob(String)}
     * @param outputDir optional output directory (null for timestamped default)
     * @param printerId printer target ID, `SYSTEM_DEFAULT`, or null/blank/FILE to skip printing
     * @return generation result details
     */
    public GenerationResult generatePdf(PreparedRailJob job, Path outputDir, String printerId) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        Path targetDir = outputDir == null
                ? Path.of("out", "rail-gui-" + job.result.getTrainId() + "-" + TS.format(LocalDateTime.now()))
                : outputDir;
        Files.createDirectories(targetDir);
        Path pdfPath = targetDir.resolve("rail-cards-" + job.result.getTrainId() + ".pdf");
        new RailCardRenderer(
                (float) config.railLabelCenterGapInches(),
                (float) config.railLabelOffsetXInches(),
                (float) config.railLabelOffsetYInches()
        ).renderPdf(job.result.getCards(), pdfPath);
        String targetPrinterId = printerId == null ? "" : printerId.trim();
        if (GuiPrinterTargetSupport.SYSTEM_DEFAULT_PRINTER_ID.equals(targetPrinterId)) {
            new RailPrintService().print(pdfPath);
            return new GenerationResult(targetDir, pdfPath, true, "System default printer");
        }
        if (!targetPrinterId.isEmpty() && !GuiPrinterTargetSupport.FILE_PRINTER_ID.equals(targetPrinterId)) {
            PrinterRoutingService routing = PrinterRoutingService.load(config.activeSiteCode(), Path.of("config"));
            PrinterConfig printer = routing.findPrinter(targetPrinterId)
                    .orElseThrow(() -> new IllegalArgumentException("Printer not found or disabled: " + targetPrinterId));
            new RailPrintService().print(pdfPath, printer);
            return new GenerationResult(targetDir, pdfPath, true, printer.getId());
        }
        return new GenerationResult(targetDir, pdfPath, false, GuiPrinterTargetSupport.FILE_PRINTER_ID);
    }

    /**
     * Builds the monospace card preview text for GUI display.
     */
    public String buildCardPreviewText(RailCarCard card) {
        StringBuilder sb = new StringBuilder();
        sb.append("SEQ: ").append(card.getSequence()).append("   VEHICLE: ").append(card.getVehicleId()).append('\n');
        if (!card.getLoadNumbers().isBlank()) {
            sb.append("LOAD: ").append(card.getLoadNumbers()).append('\n');
        }
        sb.append('\n').append("ITEM LIST").append('\n');
        int shown = Math.min(12, card.getItemLines().size());
        for (int i = 0; i < shown; i++) {
            com.tbg.wms.core.rail.RailStopRecord.ItemQuantity item = card.getItemLines().get(i);
            sb.append(String.format("%-10s %6d%n", item.getItemNumber(), item.getCases()));
        }
        if (card.getItemLines().size() > shown) {
            sb.append("... ").append(card.getItemLines().size() - shown).append(" more items").append('\n');
        }
        sb.append('\n');
        sb.append("CAN: ").append(card.getCanPallets()).append('\n');
        sb.append("DOM: ").append(card.getDomPallets()).append('\n');
        sb.append("KEV: ").append(card.getKevPallets()).append('\n');
        if (!card.getTopFamilies().isEmpty()) {
            sb.append("TOP: ").append(String.join(" ", card.getTopFamilies())).append('\n');
        }
        if (!card.getMissingFootprintItems().isEmpty()) {
            sb.append("Missing: ").append(String.join(", ", card.getMissingFootprintItems())).append('\n');
        }
        sb.append('\n').append("PASS: ______   FUEL: ______   BH: ______").append('\n');
        return sb.toString();
    }

    /**
     * Builds diagnostics text shown in the GUI diagnostics panel.
     */
    public String buildDiagnosticsText(PreparedRailJob job) {
        StringBuilder sb = new StringBuilder();
        sb.append("Rail Diagnostics").append('\n');
        sb.append("================").append('\n');
        sb.append("Train ID: ").append(job.result.getTrainId()).append('\n');
        sb.append("WMS rows: ").append(job.result.getRawRows().size()).append('\n');
        sb.append("Railcars: ").append(job.result.getCards().size()).append('\n');
        sb.append("Resolved footprints: ").append(job.result.getResolvedFootprints().size()).append('\n');
        sb.append("Unresolved short codes: ").append(job.result.getUnresolvedShortCodes().size()).append('\n');
        if (!job.result.getUnresolvedShortCodes().isEmpty()) {
            sb.append("Unresolved: ").append(String.join(", ", job.result.getUnresolvedShortCodes())).append('\n');
        }
        return sb.toString();
    }

    public static final class PreparedRailJob {
        private final com.tbg.wms.core.rail.RailWorkflowService.RailWorkflowResult result;

        private PreparedRailJob(com.tbg.wms.core.rail.RailWorkflowService.RailWorkflowResult result) {
            this.result = result;
        }

        public String getTrainId() {
            return result.getTrainId();
        }

        public List<RailCarCard> getCards() {
            return result.getCards();
        }
    }

    public static final class GenerationResult {
        private final Path outputDirectory;
        private final Path pdfPath;
        private final boolean printed;
        private final String printerId;

        private GenerationResult(Path outputDirectory, Path pdfPath, boolean printed, String printerId) {
            this.outputDirectory = outputDirectory;
            this.pdfPath = pdfPath;
            this.printed = printed;
            this.printerId = printerId;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public Path getPdfPath() {
            return pdfPath;
        }

        public boolean isPrinted() {
            return printed;
        }

        public String getPrinterId() {
            return printerId;
        }
    }
}
