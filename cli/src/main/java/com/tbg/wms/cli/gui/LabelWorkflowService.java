package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.model.WalmartSkuMapping;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import com.tbg.wms.core.template.ZplTemplateEngine;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class LabelWorkflowService {

    private final AppConfig config;

    public LabelWorkflowService(AppConfig config) {
        this.config = Objects.requireNonNull(config, "config cannot be null");
    }

    public List<PrinterOption> loadPrinters() throws Exception {
        PrinterRoutingService routing = PrinterRoutingService.load(config.activeSiteCode(), Paths.get("config"));
        List<PrinterOption> options = new ArrayList<>();
        for (PrinterConfig printer : routing.getPrinters().values()) {
            if (printer.isEnabled()) {
                options.add(new PrinterOption(printer.getId(), printer.getName(), printer.getEndpoint()));
            }
        }
        options.sort(Comparator.comparing(PrinterOption::getId));
        return options;
    }

    public PreparedJob prepareJob(String shipmentId) throws Exception {
        if (shipmentId == null || shipmentId.isBlank()) {
            throw new IllegalArgumentException("Shipment ID is required.");
        }

        String normalizedShipmentId = shipmentId.trim();
        Path csvPath = resolveSkuMatrixCsv();
        if (csvPath == null) {
            throw new IllegalStateException("SKU mapping CSV not found.");
        }
        SkuMappingService skuMapping = new SkuMappingService(csvPath);

        Path templatePath = Paths.get("config/templates/walmart-canada-label.zpl");
        if (!Files.exists(templatePath)) {
            throw new IllegalStateException("ZPL template not found: " + templatePath);
        }
        LabelTemplate template = new LabelTemplate("WALMART_CANADA", Files.readString(templatePath));

        String siteCode = config.activeSiteCode();
        SiteConfig siteConfig = createSiteConfig(siteCode);
        PrinterRoutingService routing = PrinterRoutingService.load(siteCode, Paths.get("config"));

        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            DbQueryRepository queryRepo = new OracleDbQueryRepository(pool.getDataSource());
            if (!queryRepo.shipmentExists(normalizedShipmentId)) {
                throw new IllegalArgumentException("Shipment not found: " + normalizedShipmentId);
            }

            Shipment shipment = queryRepo.findShipmentWithLpnsAndLineItems(normalizedShipmentId);
            if (shipment == null) {
                throw new IllegalStateException("Could not retrieve shipment data.");
            }

            List<ShipmentSkuFootprint> footprintRows = queryRepo.findShipmentSkuFootprints(normalizedShipmentId);
            Map<String, ShipmentSkuFootprint> footprintBySku = buildFootprintMap(footprintRows);
            PalletPlanningService.PlanResult planResult = new PalletPlanningService().plan(footprintRows);
            List<Lpn> lpnsForLabels = resolveLpnsForLabeling(shipment, footprintRows);
            boolean usingVirtualLabels = shipment.getLpnCount() == 0 && !lpnsForLabels.isEmpty();
            String stagingLocation = queryRepo.getStagingLocation(normalizedShipmentId);
            List<SkuMathRow> mathRows = buildSkuMathRows(footprintRows, skuMapping);

            return new PreparedJob(
                    normalizedShipmentId,
                    shipment,
                    routing,
                    siteConfig,
                    skuMapping,
                    template,
                    footprintBySku,
                    planResult,
                    lpnsForLabels,
                    mathRows,
                    usingVirtualLabels,
                    stagingLocation
            );
        }
    }

    public PrintResult print(PreparedJob job, String printerId, Path outputDir) throws Exception {
        Objects.requireNonNull(job, "job cannot be null");
        if (printerId == null || printerId.isBlank()) {
            throw new IllegalArgumentException("Printer is required.");
        }

        Path targetDir = outputDir == null
                ? Paths.get("out", "gui-" + job.getShipmentId() + "-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()))
                : outputDir;
        Files.createDirectories(targetDir);

        PrinterConfig printer = job.getRouting()
                .findPrinter(printerId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Printer not found or disabled: " + printerId));

        LabelDataBuilder builder = new LabelDataBuilder(job.getSkuMapping(), job.getSiteConfig(), job.getFootprintBySku());
        NetworkPrintService printService = new NetworkPrintService();

        int printedCount = 0;
        for (int i = 0; i < job.getLpnsForLabels().size(); i++) {
            Lpn lpn = job.getLpnsForLabels().get(i);
            Map<String, String> labelData = new HashMap<>(builder.build(job.getShipment(), lpn, i, LabelType.WALMART_CANADA_GRID));
            if (job.getShipment().getLpnCount() == 0) {
                labelData.put("palletSeq", String.valueOf(i + 1));
                labelData.put("palletTotal", String.valueOf(job.getLpnsForLabels().size()));
            }

            String zpl = ZplTemplateEngine.generate(job.getTemplate(), labelData);
            String fileName = String.format("%s_%s_%d_of_%d.zpl",
                    job.getShipmentId(), lpn.getLpnId(), i + 1, job.getLpnsForLabels().size());
            Path zplFile = targetDir.resolve(fileName);
            Files.writeString(zplFile, zpl);
            printService.print(printer, zpl, lpn.getLpnId());
            printedCount++;
        }

        return new PrintResult(printedCount, targetDir.toAbsolutePath(), printer.getId(), printer.getEndpoint());
    }

    private List<SkuMathRow> buildSkuMathRows(List<ShipmentSkuFootprint> rows, SkuMappingService skuMapping) {
        List<SkuMathRow> mathRows = new ArrayList<>();
        for (ShipmentSkuFootprint row : rows) {
            if (row == null || row.getSku() == null || row.getSku().isBlank()) {
                continue;
            }

            int units = Math.max(0, row.getTotalUnits());
            Integer upp = row.getUnitsPerPallet();
            int fullPallets = 0;
            int partialUnits = 0;
            int estimatedPallets = 0;
            if (upp != null && upp > 0) {
                fullPallets = units / upp;
                partialUnits = units % upp;
                estimatedPallets = fullPallets + (partialUnits > 0 ? 1 : 0);
            }

            String description = row.getItemDescription();
            if (!isHumanReadable(description)) {
                WalmartSkuMapping mapping = skuMapping.findByPrtnum(row.getSku());
                if (mapping != null && isHumanReadable(mapping.getDescription())) {
                    description = mapping.getDescription();
                }
            }

            mathRows.add(new SkuMathRow(
                    row.getSku(),
                    description == null ? "" : description,
                    units,
                    upp,
                    fullPallets,
                    partialUnits,
                    estimatedPallets
            ));
        }
        return mathRows;
    }

    private Path resolveSkuMatrixCsv() {
        List<Path> candidates = List.of(
                Paths.get("config/walmart-sku-matrix.csv"),
                Paths.get("config/walmart_sku_matrix.csv"),
                Paths.get("config/TBG3002/walmart-sku-matrix.csv"),
                Paths.get("config/TBG3002/walmart_sku_matrix.csv"),
                Paths.get("walmart-sku-matrix.csv"),
                Paths.get("walmart_sku_matrix.csv")
        );
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private Map<String, ShipmentSkuFootprint> buildFootprintMap(List<ShipmentSkuFootprint> rows) {
        if (rows == null || rows.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, ShipmentSkuFootprint> bySku = new HashMap<>();
        for (ShipmentSkuFootprint row : rows) {
            if (row != null && row.getSku() != null && !row.getSku().isBlank()) {
                bySku.put(row.getSku(), row);
            }
        }
        return bySku;
    }

    private List<Lpn> resolveLpnsForLabeling(Shipment shipment, List<ShipmentSkuFootprint> footprintRows) {
        List<Lpn> lpns = shipment.getLpns();
        if (!lpns.isEmpty()) {
            return lpns;
        }
        List<Lpn> virtual = buildVirtualLpnsFromFootprints(shipment, footprintRows);
        return virtual.isEmpty() ? lpns : virtual;
    }

    private List<Lpn> buildVirtualLpnsFromFootprints(Shipment shipment, List<ShipmentSkuFootprint> footprintRows) {
        if (footprintRows == null || footprintRows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Lpn> virtualLpns = new ArrayList<>();
        int seq = 0;

        for (ShipmentSkuFootprint row : footprintRows) {
            if (row == null || row.getSku() == null || row.getSku().isBlank()) {
                continue;
            }

            seq++;
            String description = isHumanReadable(row.getItemDescription()) ? row.getItemDescription() : null;
            LineItem item = new LineItem(
                    String.valueOf(seq),
                    "0",
                    row.getSku(),
                    description,
                    null,
                    shipment.getOrderId(),
                    null,
                    null,
                    row.getTotalUnits(),
                    row.getUnitsPerCase() != null ? row.getUnitsPerCase() : 0,
                    "EA",
                    0.0,
                    null,
                    null,
                    null
            );

            Lpn virtualLpn = new Lpn(
                    "NO_LPN_" + seq,
                    shipment.getShipmentId(),
                    String.format("%018d", seq),
                    0,
                    row.getTotalUnits(),
                    0.0,
                    shipment.getDestinationLocation(),
                    null,
                    null,
                    LocalDate.now(),
                    LocalDate.now(),
                    List.of(item)
            );
            virtualLpns.add(virtualLpn);
        }
        return virtualLpns;
    }

    private boolean isHumanReadable(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (Character.isLetter(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private SiteConfig createSiteConfig(String siteCode) {
        if ("TBG3002".equalsIgnoreCase(siteCode)) {
            return new SiteConfig("TROPICANA PRODUCTS, INC.", "155 Broad Street", "Jersey City, NJ 07302");
        }
        return new SiteConfig("TROPICANA PRODUCTS, INC.", "155 Broad Street", "Jersey City, NJ 07302");
    }

    public static final class PrinterOption {
        private final String id;
        private final String name;
        private final String endpoint;

        public PrinterOption(String id, String name, String endpoint) {
            this.id = id;
            this.name = name;
            this.endpoint = endpoint;
        }

        public String getId() {
            return id;
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
        private final int partialUnits;
        private final int estimatedPallets;

        public SkuMathRow(String sku, String description, int units, Integer unitsPerPallet, int fullPallets, int partialUnits, int estimatedPallets) {
            this.sku = sku;
            this.description = description;
            this.units = units;
            this.unitsPerPallet = unitsPerPallet;
            this.fullPallets = fullPallets;
            this.partialUnits = partialUnits;
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

        public int getPartialUnits() {
            return partialUnits;
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

        public PrintResult(int labelsPrinted, Path outputDirectory, String printerId, String printerEndpoint) {
            this.labelsPrinted = labelsPrinted;
            this.outputDirectory = outputDirectory;
            this.printerId = printerId;
            this.printerEndpoint = printerEndpoint;
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
    }
}
