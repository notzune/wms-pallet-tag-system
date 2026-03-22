/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.model.*;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * GUI workflow service that loads shipment data, builds preview math,
 * and executes label printing.
 */
public final class LabelWorkflowService {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AppConfig config;
    private final LabelWorkflowAssetSupport assetSupport;
    private final LabelWorkflowPlanningSupport planningSupport = new LabelWorkflowPlanningSupport();
    private final LabelWorkflowPrintSupport printSupport = new LabelWorkflowPrintSupport();

    public LabelWorkflowService(AppConfig config) {
        this(config, RuntimePathResolver.resolveWorkingDirOrJarSiblingDir(LabelWorkflowService.class, "config"));
    }

    LabelWorkflowService(AppConfig config, Path configBaseDir) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.assetSupport = new LabelWorkflowAssetSupport(config, Objects.requireNonNull(configBaseDir, "configBaseDir cannot be null"));
    }

    /**
     * Loads enabled printers from routing configuration.
     *
     * @return list of available printers sorted by ID
     * @throws Exception when routing config cannot be loaded
     */
    public List<PrinterOption> loadPrinters() throws Exception {
        String siteCode = config.activeSiteCode();
        PrinterRoutingService routing = assetSupport.loadRouting(siteCode);
        List<PrinterOption> options = new ArrayList<>();
        for (PrinterConfig printer : routing.getPrinters().values()) {
            if (printer.isEnabled()) {
                options.add(new PrinterOption(
                        printer.getId(),
                        printer.getName(),
                        printer.getEndpoint(),
                        printer.getCapabilities()
                ));
            }
        }
        options.sort(Comparator.comparing(PrinterOption::getId));
        return options;
    }

    public void clearCaches() {
        assetSupport.clearCaches();
    }

    /**
     * Resolves a printer config by ID using cached routing data.
     */
    public PrinterConfig resolvePrinter(String printerId) throws Exception {
        if (printerId == null || printerId.isBlank()) {
            return null;
        }
        String siteCode = config.activeSiteCode();
        return assetSupport.resolvePrinter(siteCode, printerId);
    }

    /**
     * Loads shipment data and prepares the label plan for GUI preview.
     *
     * @param shipmentId WMS shipment identifier
     * @return prepared job with shipment, labels, and planning results
     * @throws Exception when data is missing or cannot be loaded
     */
    public PreparedJob prepareJob(String shipmentId) throws Exception {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("Shipment ID is required.");
        }

        String normalizedShipmentId = shipmentId.trim();

        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            DbQueryRepository queryRepo = new OracleDbQueryRepository(pool.getDataSource());
            return prepareJob(queryRepo, normalizedShipmentId);
        }
    }

    PreparedJob prepareJob(DbQueryRepository queryRepo, String shipmentId) throws Exception {
        Objects.requireNonNull(queryRepo, "queryRepo cannot be null");
        String normalizedShipmentId = shipmentId == null ? "" : shipmentId.trim();
        if (normalizedShipmentId.isEmpty()) {
            throw new IllegalArgumentException("Shipment ID is required.");
        }

        if (!queryRepo.shipmentExists(normalizedShipmentId)) {
            throw new IllegalArgumentException("Shipment not found: " + normalizedShipmentId);
        }

        Shipment shipment = queryRepo.findShipmentWithLpnsAndLineItems(normalizedShipmentId);
        if (shipment == null) {
            throw new IllegalStateException("Could not retrieve shipment data.");
        }

        List<ShipmentSkuFootprint> footprintRows = queryRepo.findShipmentSkuFootprints(normalizedShipmentId);
        Map<String, ShipmentSkuFootprint> footprintBySku = LabelingSupport.buildFootprintMap(footprintRows);
        PalletPlanningService.PlanResult planResult = new PalletPlanningService().plan(footprintRows);
        // Fallback to virtual rows only when WMS returns no physical LPNs.
        List<Lpn> lpnsForLabels = planningSupport.resolveLpnsForLabeling(shipment, footprintRows);
        boolean usingVirtualLabels = shipment.getLpnCount() == 0 && !lpnsForLabels.isEmpty();
        String stagingLocation = queryRepo.getStagingLocation(normalizedShipmentId);
        SkuMappingService skuMapping = assetSupport.loadSkuMapping();
        List<SkuMathRow> mathRows = planningSupport.buildSkuMathRows(footprintRows, skuMapping);

        String siteCode = config.activeSiteCode();
        PrinterRoutingService routing = assetSupport.loadRouting(siteCode);
        return new PreparedJob(
                normalizedShipmentId,
                shipment,
                routing,
                assetSupport.loadSiteConfig(siteCode),
                skuMapping,
                assetSupport.loadTemplate(),
                footprintBySku,
                planResult,
                lpnsForLabels,
                mathRows,
                usingVirtualLabels,
                stagingLocation
        );
    }

    /**
     * Generates ZPL output and prints labels for the prepared job.
     *
     * @param job         prepared job from preview
     * @param printerId   printer identifier to use
     * @param outputDir   output directory for ZPL artifacts
     * @param printToFile when true, skip network printing
     * @return summary of labels printed and output path
     * @throws Exception when printing fails
     */
    public PrintResult print(PreparedJob job, String printerId, Path outputDir, boolean printToFile) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        String resolvedPrinterId = printerId == null ? "" : printerId.trim();
        if (!printToFile && resolvedPrinterId.isEmpty()) {
            throw new IllegalArgumentException("Printer is required.");
        }

        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-" + job.getShipmentId() + "-" + TIMESTAMP.format(LocalDateTime.now()))
                : outputDir;
        PrinterConfig printer = null;
        if (!printToFile) {
            printer = job.getRouting()
                    .findPrinter(resolvedPrinterId)
                    .orElseThrow(() -> new IllegalArgumentException("Printer not found or disabled: " + resolvedPrinterId));
        }
        return printSupport.print(job, printer, targetDir, printToFile);
    }

    public static final class PrinterOption {
        private final String id;
        private final String name;
        private final String endpoint;
        private final List<String> capabilities;

        public PrinterOption(String id, String name, String endpoint) {
            this(id, name, endpoint, List.of());
        }

        public PrinterOption(String id, String name, String endpoint, List<String> capabilities) {
            this.id = id;
            this.name = name;
            this.endpoint = endpoint;
            this.capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
        }

        public String getId() {
            return id;
        }

        public List<String> getCapabilities() {
            return capabilities;
        }

        @Override
        public String toString() {
            return id + " - " + name + " (" + endpoint + ")";
        }
    }

    public static final class SkuMathRow {
        private final String sku;
        private final String description;
        private final int units;
        private final Integer unitsPerPallet;
        private final int fullPallets;
        private final int partialPallets;
        private final int estimatedPallets;

        public SkuMathRow(String sku, String description, int units, Integer unitsPerPallet, int fullPallets, int partialPallets, int estimatedPallets) {
            this.sku = sku;
            this.description = description;
            this.units = units;
            this.unitsPerPallet = unitsPerPallet;
            this.fullPallets = fullPallets;
            this.partialPallets = partialPallets;
            this.estimatedPallets = estimatedPallets;
        }

        public String getSku() {
            return sku;
        }

        public String getDescription() {
            return description;
        }

        public int getUnits() {
            return units;
        }

        public Integer getUnitsPerPallet() {
            return unitsPerPallet;
        }

        public int getFullPallets() {
            return fullPallets;
        }

        public int getPartialPallets() {
            return partialPallets;
        }

        public int getEstimatedPallets() {
            return estimatedPallets;
        }
    }

    public static final class PreparedJob {
        private final String shipmentId;
        private final Shipment shipment;
        private final PrinterRoutingService routing;
        private final SiteConfig siteConfig;
        private final SkuMappingService skuMapping;
        private final LabelTemplate template;
        private final Map<String, ShipmentSkuFootprint> footprintBySku;
        private final PalletPlanningService.PlanResult planResult;
        private final List<Lpn> lpnsForLabels;
        private final List<SkuMathRow> skuMathRows;
        private final boolean usingVirtualLabels;
        private final String stagingLocation;

        private PreparedJob(String shipmentId,
                            Shipment shipment,
                            PrinterRoutingService routing,
                            SiteConfig siteConfig,
                            SkuMappingService skuMapping,
                            LabelTemplate template,
                            Map<String, ShipmentSkuFootprint> footprintBySku,
                            PalletPlanningService.PlanResult planResult,
                            List<Lpn> lpnsForLabels,
                            List<SkuMathRow> skuMathRows,
                            boolean usingVirtualLabels,
                            String stagingLocation) {
            this.shipmentId = shipmentId;
            this.shipment = shipment;
            this.routing = routing;
            this.siteConfig = siteConfig;
            this.skuMapping = skuMapping;
            this.template = template;
            this.footprintBySku = footprintBySku;
            this.planResult = planResult;
            this.lpnsForLabels = lpnsForLabels;
            this.skuMathRows = skuMathRows;
            this.usingVirtualLabels = usingVirtualLabels;
            this.stagingLocation = stagingLocation;
        }

        public String getShipmentId() {
            return shipmentId;
        }

        public Shipment getShipment() {
            return shipment;
        }

        public PrinterRoutingService getRouting() {
            return routing;
        }

        public SiteConfig getSiteConfig() {
            return siteConfig;
        }

        public SkuMappingService getSkuMapping() {
            return skuMapping;
        }

        public LabelTemplate getTemplate() {
            return template;
        }

        public Map<String, ShipmentSkuFootprint> getFootprintBySku() {
            return footprintBySku;
        }

        public PalletPlanningService.PlanResult getPlanResult() {
            return planResult;
        }

        public List<Lpn> getLpnsForLabels() {
            return lpnsForLabels;
        }

        public List<SkuMathRow> getSkuMathRows() {
            return skuMathRows;
        }

        public boolean isUsingVirtualLabels() {
            return usingVirtualLabels;
        }

        public String getStagingLocation() {
            return stagingLocation;
        }
    }

    public static final class PrintResult {
        private final int labelsPrinted;
        private final Path outputDirectory;
        private final String printerId;
        private final String printerEndpoint;
        private final boolean printToFile;

        public PrintResult(int labelsPrinted, Path outputDirectory, String printerId, String printerEndpoint) {
            this.labelsPrinted = labelsPrinted;
            this.outputDirectory = outputDirectory;
            this.printerId = printerId;
            this.printerEndpoint = printerEndpoint;
            this.printToFile = false;
        }

        private PrintResult(int labelsPrinted, Path outputDirectory) {
            this.labelsPrinted = labelsPrinted;
            this.outputDirectory = outputDirectory;
            this.printerId = "FILE";
            this.printerEndpoint = "FILE";
            this.printToFile = true;
        }

        public static PrintResult printToFile(int labelsPrinted, Path outputDirectory) {
            return new PrintResult(labelsPrinted, outputDirectory);
        }

        public int getLabelsPrinted() {
            return labelsPrinted;
        }

        public Path getOutputDirectory() {
            return outputDirectory;
        }

        public String getPrinterId() {
            return printerId;
        }

        public String getPrinterEndpoint() {
            return printerEndpoint;
        }

        public boolean isPrintToFile() {
            return printToFile;
        }
    }
}
