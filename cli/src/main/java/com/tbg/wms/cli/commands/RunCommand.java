/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.exception.WmsPrintException;
import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;
import com.tbg.wms.core.template.ZplTemplateEngine;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.DbQueryRepository;
import com.tbg.wms.db.OracleDbQueryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Generates shipping labels for a WMS shipment using Zebra ZPL format.
 *
 * This is the primary command for label generation. It:
 * 1. Retrieves shipment data from WMS (shipment header, pallets, line items)
 * 2. Looks up Walmart SKU codes from CSV matrix
 * 3. Builds label data for each pallet
 * 4. Generates ZPL output for printing
 * 5. Saves artifacts (ZPL files, JSON snapshots) for traceability
 *
 * <p>Usage:</p>
 * <ul>
 *   <li>{@code wms-tags run --shipment-id 8000141715}</li>
 *   <li>{@code wms-tags run --shipment-id 8000141715 --dry-run}</li>
 *   <li>{@code wms-tags run --shipment-id 8000141715 --printer DISPATCH}</li>
 * </ul>
 *
 * <p>Exit codes:</p>
 * <ul>
 *   <li>0 – Labels generated and sent successfully</li>
 *   <li>1 – Shipment not found</li>
 *   <li>2 – Configuration error</li>
 *   <li>3 – Database connectivity error</li>
 *   <li>10 – Unexpected error</li>
 * </ul>
 */
@Command(
        name = "run",
        description = "Generate pallet shipping labels for a WMS shipment"
)
public final class RunCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(RunCommand.class);

    @Option(
            names = {"-s", "--shipment-id"},
            description = "Shipment ID to generate labels for",
            required = true
    )
    private String shipmentId;

    @Option(
            names = {"-d", "--dry-run"},
            description = "Generate labels but don't print (for testing)",
            defaultValue = "false"
    )
    private boolean dryRun;

    @Option(
            names = {"-p", "--printer"},
            description = "Target printer name (overrides routing rules)",
            defaultValue = ""
    )
    private String printerOverride;

    @Option(
            names = {"-o", "--output-dir"},
            description = "Output directory for ZPL files and snapshots",
            defaultValue = "./labels"
    )
    private String outputDir;

    @Option(
            names = {"--print-to-file", "--ptf"},
            description = "Write ZPL to /out next to the JAR and skip printing",
            defaultValue = "false"
    )
    private boolean printToFile;

    @Override
    public Integer call() {
        // Generate unique job ID
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("jobId", jobId);
        MDC.put("shipmentId", shipmentId);

        try {
            log.info("Starting label generation for shipment: {}", shipmentId);

            // Load configuration
            AppConfig config = RootCommand.config();
            String site = config.activeSiteCode();
            MDC.put("site", site);

            if (printToFile) {
                dryRun = true;
                outputDir = resolveJarOutputDir().toString();
            }

            // Create output directory
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);
            log.debug("Output directory: {}", outputPath.toAbsolutePath());

            // ── Step 1: Load SKU Mapping ──
            log.info("Loading Walmart SKU mapping CSV...");
            Path csvPath = resolveSkuMatrixCsv();
            if (csvPath == null) {
                log.error("SKU mapping CSV not found: {}", csvPath);
                System.err.println("Error: SKU mapping CSV not found. Expected one of:");
                System.err.println("  - config/walmart-sku-matrix.csv");
                System.err.println("  - config/walmart_sku_matrix.csv");
                System.err.println("  - config/TBG3002/walmart-sku-matrix.csv");
                System.err.println("  - config/TBG3002/walmart_sku_matrix.csv");
                System.err.println("  - walmart-sku-matrix.csv");
                System.err.println("  - walmart_sku_matrix.csv");
                return 2;
            }
            SkuMappingService skuMapping = new SkuMappingService(csvPath);
            log.info("Loaded {} SKU mappings", skuMapping.getMappingCount());

            // ── Step 2: Load ZPL Template ──
            log.info("Loading ZPL template...");
            Path templatePath = Paths.get("config/templates/walmart-canada-label.zpl");
            if (!Files.exists(templatePath)) {
                log.error("ZPL template not found: {}", templatePath);
                System.err.println("Error: ZPL template not found at " + templatePath);
                return 2;
            }
            String templateContent = Files.readString(templatePath);
            LabelTemplate labelTemplate = new LabelTemplate("WALMART_CANADA", templateContent);
            log.info("Loaded template with {} placeholders", labelTemplate.getPlaceholderCount());

            // ── Step 3: Create Database Connection ──
            log.info("Connecting to WMS database (read-only)...");
            try (DbConnectionPool pool = new DbConnectionPool(config)) {
                DbQueryRepository queryRepo = new OracleDbQueryRepository(pool.getDataSource());

                // ── Step 4: Check Shipment Exists ──
                log.info("Checking shipment existence...");
                if (!queryRepo.shipmentExists(shipmentId)) {
                    log.warn("Shipment not found: {}", shipmentId);
                    System.err.println("Error: Shipment not found: " + shipmentId);
                    return 1;
                }

                // ── Step 5: Fetch Shipment Data ──
                log.info("Fetching shipment data from WMS...");
                Shipment shipment = queryRepo.findShipmentWithLpnsAndLineItems(shipmentId);
                if (shipment == null) {
                    log.warn("Could not retrieve shipment data for: {}", shipmentId);
                    System.err.println("Error: Could not retrieve shipment data");
                    return 1;
                }
                log.info("Retrieved shipment with {} pallets", shipment.getLpnCount());

                // ── Step 6: Pull footprint + planning data ──
                List<ShipmentSkuFootprint> footprintRows = queryRepo.findShipmentSkuFootprints(shipmentId);
                Map<String, ShipmentSkuFootprint> footprintBySku = buildFootprintMap(footprintRows);
                PalletPlanningService.PlanResult planResult = new PalletPlanningService().plan(footprintRows);

                printPlanSummary(planResult, shipment.getLpnCount());

                // ── Step 7: Create Site Config ──
                SiteConfig siteConfig = createSiteConfig(site);

                // ── Step 8: Load Printer Routing ──
                log.info("Loading printer routing configuration...");
                Path configDir = Paths.get("config");
                PrinterRoutingService routing = PrinterRoutingService.load(site, configDir);
                log.info("Loaded {} printers and {} routing rules",
                        routing.getPrinters().size(), routing.getRules().size());

                // ── Step 9: Initialize Print Service ──
                NetworkPrintService printService = new NetworkPrintService();

                // ── Step 10: Build Labels ──
                log.info("Building labels for {} pallets...", shipment.getLpnCount());
                LabelDataBuilder builder = new LabelDataBuilder(skuMapping, siteConfig, footprintBySku);
                List<Lpn> lpns = resolveLpnsForLabeling(shipment, footprintRows);

                // Get staging location for routing
                String stagingLocation = queryRepo.getStagingLocation(shipmentId);
                log.info("Shipment staging location: {}", stagingLocation != null ? stagingLocation : "UNKNOWN");

                int labelCount = 0;
                for (int i = 0; i < lpns.size(); i++) {
                    Lpn lpn = lpns.get(i);

                    log.debug("Processing pallet {}/{}: {}", i + 1, lpns.size(), lpn.getLpnId());

                    // Build label data
                    Map<String, String> labelData = new HashMap<>(builder.build(shipment, lpn, i, LabelType.WALMART_CANADA_GRID));
                    if (shipment.getLpnCount() == 0) {
                        labelData.put("palletSeq", String.valueOf(i + 1));
                        labelData.put("palletTotal", String.valueOf(lpns.size()));
                    }

                    // Generate ZPL
                    String zpl = ZplTemplateEngine.generate(labelTemplate, labelData);
                    log.trace("Generated ZPL for pallet {}", lpn.getLpnId());

                    // Save ZPL file
                    String filename = String.format("%s_%s_%d_of_%d.zpl",
                            shipmentId, lpn.getLpnId(), i + 1, lpns.size());
                    Path zplFile = outputPath.resolve(filename);
                    Files.writeString(zplFile, zpl);
                    log.info("Saved ZPL file: {}", zplFile.getFileName());

                    labelCount++;

                    // ── Step 11: Print (if not dry-run) ──
                    if (!dryRun) {
                        PrinterConfig printer;

                        if (!printerOverride.isEmpty()) {
                            // Manual override
                            log.info("Using manual printer override: {}", printerOverride);
                            printer = routing.findPrinter(printerOverride)
                                    .orElseThrow(() -> new IllegalArgumentException(
                                            "Printer not found or disabled: " + printerOverride));
                        } else {
                            // Routing based on staging location
                            Map<String, String> routingContext = new HashMap<>();
                            routingContext.put("stagingLocation", stagingLocation != null ? stagingLocation : "UNKNOWN");
                            printer = routing.selectPrinter(routingContext);
                        }

                        log.info("Printing label {} to printer {} ({})",
                                lpn.getLpnId(), printer.getId(), printer.getEndpoint());

                        try {
                            printService.print(printer, zpl, lpn.getLpnId());
                            System.out.println(String.format("  Printed label %d/%d to %s",
                                    i + 1, lpns.size(), printer.getName()));
                        } catch (WmsPrintException e) {
                            log.error("Failed to print label {}: {}", lpn.getLpnId(), e.getMessage());
                            System.err.println("Warning: Failed to print label " + lpn.getLpnId() + ": " + e.getMessage());
                            System.err.println("ZPL file saved to: " + zplFile);
                            // Continue with other labels instead of failing entirely
                        }
                    }
                }

                log.info("Successfully generated {} labels for shipment {}", labelCount, shipmentId);
                System.out.println("\nSuccess! Generated " + labelCount + " label(s)");
                System.out.println("Output saved to: " + outputPath.toAbsolutePath());

                if (dryRun) {
                    System.out.println("(Dry-run mode: labels were not sent to printer)");
                }

                return 0;
            }

        } catch (WmsDbConnectivityException e) {
            log.error("Database connectivity error: {}", e.getMessage(), e);
            System.err.println("Error: Database connectivity issue");
            System.err.println("Details: " + e.getMessage());
            return 3;
        } catch (WmsPrintException e) {
            log.error("Print error: {}", e.getMessage(), e);
            System.err.println("Error: Printing failed");
            System.err.println("Details: " + e.getMessage());
            System.err.println("Remediation: " + e.getRemediationHint());
            return 5;
        } catch (Exception e) {
            log.error("Unexpected error during label generation", e);
            System.err.println("Error: Unexpected error: " + e.getMessage());
            return 10;
        } finally {
            MDC.clear();
        }
    }

    private static Path resolveJarOutputDir() {
        try {
            Path codeSource = Paths.get(Objects.requireNonNull(RunCommand.class
                    .getProtectionDomain()
                    .getCodeSource())
                    .getLocation()
                    .toURI());
            Path baseDir = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            return baseDir.resolve("out");
        } catch (Exception e) {
            return Paths.get("out");
        }
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

    private void printPlanSummary(PalletPlanningService.PlanResult planResult, int actualPallets) {
        System.out.println();
        System.out.println("=== Pallet Planning Summary ===");
        System.out.println("Total units across shipment: " + planResult.getTotalUnits());
        System.out.println("Estimated pallets from footprint setup: " + planResult.getEstimatedPallets());
        System.out.println("  Full pallets: " + planResult.getFullPallets());
        System.out.println("  Partial pallets: " + planResult.getPartialPallets());
        System.out.println("Actual LPNs in shipment: " + actualPallets);

        if (!planResult.getSkusMissingFootprint().isEmpty()) {
            System.out.println("SKUs missing pallet footprint setup: " + String.join(", ", planResult.getSkusMissingFootprint()));
        }
        if (planResult.getEstimatedPallets() > 0 && planResult.getEstimatedPallets() != actualPallets) {
            System.out.println("Warning: Estimated pallet count differs from actual LPN count.");
        }
        System.out.println("===============================");
        System.out.println();
    }

    private List<Lpn> resolveLpnsForLabeling(Shipment shipment, List<ShipmentSkuFootprint> footprintRows) {
        List<Lpn> lpns = shipment.getLpns();
        if (!lpns.isEmpty()) {
            return lpns;
        }

        List<Lpn> virtualLpns = buildVirtualLpnsFromFootprints(shipment, footprintRows);
        if (!virtualLpns.isEmpty()) {
            log.warn("Shipment {} has no LPNs. Using {} virtual SKU-based labels.", shipment.getShipmentId(), virtualLpns.size());
            System.out.println("Warning: Shipment has no LPNs. Generating SKU-based labels without LPN dependency.");
            return virtualLpns;
        }

        return lpns;
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

            int totalUnits = Math.max(0, row.getTotalUnits());
            if (totalUnits == 0) {
                continue;
            }

            Integer unitsPerPallet = row.getUnitsPerPallet();
            int palletsForSku;
            if (unitsPerPallet == null || unitsPerPallet <= 0) {
                // Missing footprint: fall back to a single pallet label.
                palletsForSku = 1;
            } else {
                // One label per pallet: full pallets + one partial if remainder exists.
                palletsForSku = totalUnits / unitsPerPallet;
                if (totalUnits % unitsPerPallet != 0) {
                    palletsForSku += 1;
                }
            }

            for (int palletIndex = 0; palletIndex < palletsForSku; palletIndex++) {
                int palletUnits;
                if (unitsPerPallet == null || unitsPerPallet <= 0) {
                    palletUnits = totalUnits;
                } else if (palletIndex < palletsForSku - 1) {
                    palletUnits = unitsPerPallet;
                } else {
                    int remainder = totalUnits % unitsPerPallet;
                    // The last pallet carries the remainder.
                    palletUnits = remainder == 0 ? unitsPerPallet : remainder;
                }

                seq++;
                LineItem item = new LineItem(
                        String.valueOf(seq),
                        "0",
                        row.getSku(),
                        isHumanReadableDescription(row.getItemDescription()) ? row.getItemDescription() : null,
                        null,
                        shipment.getOrderId(),
                        null,
                        null,
                        palletUnits,
                        row.getUnitsPerCase() != null ? row.getUnitsPerCase() : 0,
                        "EA",
                        0.0,
                        null,
                        null,
                        null
                );

                String syntheticLpnId = "NO_LPN_" + seq;
                String syntheticSscc = String.format("%018d", seq);

                Lpn virtualLpn = new Lpn(
                        syntheticLpnId,
                        shipment.getShipmentId(),
                        syntheticSscc,
                        0,
                        palletUnits,
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
        }

        return virtualLpns;
    }

    private boolean isHumanReadableDescription(String value) {
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

    /**
     * Creates site-specific configuration based on site code.
     *
     * @param siteCode the site code (e.g., "TBG3002")
     * @return SiteConfig with ship-from address
     */
    private SiteConfig createSiteConfig(String siteCode) {
        AppConfig config = RootCommand.config();
        return new SiteConfig(
                config.siteShipFromName(siteCode),
                config.siteShipFromAddress(siteCode),
                config.siteShipFromCityStateZip(siteCode)
        );
    }
}

