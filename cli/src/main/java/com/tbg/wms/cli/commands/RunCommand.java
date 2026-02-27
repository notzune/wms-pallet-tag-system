/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.AdvancedPrintWorkflowService;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.exception.WmsPrintException;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Generates shipping labels from WMS data.
 *
 * <p>Supports either shipment mode ({@code --shipment-id}) or carrier move mode
 * ({@code --carrier-move-id}) with shared print workflow semantics.</p>
 */
@Command(
        name = "run",
        description = "Generate pallet shipping labels for a shipment or carrier move"
)
public final class RunCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(RunCommand.class);
    private static final int MAX_LABELS_PER_JOB = 10_000;

    @Option(
            names = {"-s", "--shipment-id"},
            description = "Shipment ID to generate labels for"
    )
    private String shipmentId;

    @Option(
            names = {"-c", "--carrier-move-id"},
            description = "Carrier Move ID to generate labels for all stops"
    )
    private String carrierMoveId;

    @Option(
            names = {"-d", "--dry-run"},
            description = "Generate labels but don't print (for testing)",
            defaultValue = "false"
    )
    private boolean dryRun;

    @Option(
            names = {"-p", "--printer"},
            description = "Target printer ID (optional; routing is used when omitted)",
            defaultValue = ""
    )
    private String printerOverride;

    @Option(
            names = {"-o", "--output-dir"},
            description = "Output directory for ZPL files",
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
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        MDC.put("jobId", jobId);

        try {
            AppConfig config = RootCommand.config();
            MDC.put("site", config.activeSiteCode());

            String inputId = resolveInputId();
            boolean carrierMode = isCarrierMoveMode();
            MDC.put(carrierMode ? "carrierMoveId" : "shipmentId", inputId);

            if (printToFile) {
                dryRun = true;
                outputDir = resolveJarOutputDir().toString();
            }

            Path outputPath = prepareOutputDirectory();
            boolean printToFileMode = dryRun || printToFile;
            AdvancedPrintWorkflowService workflow = new AdvancedPrintWorkflowService(config);

            if (carrierMode) {
                return executeCarrierMoveRun(workflow, inputId, outputPath, printToFileMode);
            }
            return executeShipmentRun(workflow, inputId, outputPath, printToFileMode);

        } catch (WmsDbConnectivityException e) {
            log.error("Database connectivity error: {}", e.getMessage(), e);
            System.err.println("Error: Database connectivity issue");
            System.err.println("Details: " + e.getMessage());
            return 3;
        } catch (IllegalArgumentException e) {
            log.error("Validation/configuration error: {}", e.getMessage());
            System.err.println("Error: " + e.getMessage());
            return 2;
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

    private Integer executeShipmentRun(AdvancedPrintWorkflowService workflow,
                                       String id,
                                       Path outputPath,
                                       boolean printToFileMode) throws Exception {
        log.info("Starting shipment print job for {}", id);
        LabelWorkflowService.PreparedJob prepared = workflow.prepareShipmentJob(id);
        printShipmentPlanSummary(prepared);

        int labels = prepared.getLpnsForLabels().size();
        enforceMaxLabels(labels);

        String printerId = resolvePrinterId(
                printToFileMode,
                prepared.getRouting(),
                prepared.getStagingLocation()
        );

        AdvancedPrintWorkflowService.PrintResult result = workflow.printShipmentJob(
                prepared,
                printerId,
                outputPath,
                printToFileMode
        );

        printCompletion(result);
        return 0;
    }

    private Integer executeCarrierMoveRun(AdvancedPrintWorkflowService workflow,
                                          String id,
                                          Path outputPath,
                                          boolean printToFileMode) throws Exception {
        log.info("Starting carrier move print job for {}", id);
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared = workflow.prepareCarrierMoveJob(id);
        int labelCount = countCarrierMoveLabels(prepared);
        enforceMaxLabels(labelCount);
        printCarrierMovePlanSummary(prepared, labelCount);

        LabelWorkflowService.PreparedJob firstShipment = firstCarrierShipment(prepared);
        String printerId = resolvePrinterId(
                printToFileMode,
                firstShipment.getRouting(),
                firstShipment.getStagingLocation()
        );

        AdvancedPrintWorkflowService.PrintResult result = workflow.printCarrierMoveJob(
                prepared,
                printerId,
                outputPath,
                printToFileMode
        );

        printCompletion(result);
        return 0;
    }

    private String resolveInputId() {
        boolean hasShipment = shipmentId != null && !shipmentId.isBlank();
        boolean hasCarrier = carrierMoveId != null && !carrierMoveId.isBlank();
        if (hasShipment == hasCarrier) {
            throw new IllegalArgumentException("Specify exactly one of --shipment-id or --carrier-move-id.");
        }
        return hasCarrier ? carrierMoveId.trim() : shipmentId.trim();
    }

    private boolean isCarrierMoveMode() {
        return carrierMoveId != null && !carrierMoveId.isBlank();
    }

    private Path prepareOutputDirectory() throws Exception {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        log.debug("Output directory: {}", outputPath.toAbsolutePath());
        return outputPath;
    }

    private String resolvePrinterId(boolean printToFileMode,
                                    PrinterRoutingService routing,
                                    String stagingLocation) {
        if (printToFileMode) {
            return null;
        }

        String override = printerOverride == null ? "" : printerOverride.trim();
        if (!override.isEmpty()) {
            return override;
        }

        PrinterConfig routed = routing.selectPrinter(Map.of(
                "stagingLocation",
                stagingLocation == null || stagingLocation.isBlank() ? "UNKNOWN" : stagingLocation
        ));
        if (routed == null || !routed.isEnabled()) {
            throw new IllegalArgumentException("Could not resolve an enabled printer from routing.");
        }
        return routed.getId();
    }

    private int countCarrierMoveLabels(AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared) {
        int total = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : prepared.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                total += shipmentJob.getLpnsForLabels().size();
            }
        }
        return total;
    }

    private LabelWorkflowService.PreparedJob firstCarrierShipment(AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared) {
        if (prepared.getStopGroups().isEmpty() || prepared.getStopGroups().get(0).getShipmentJobs().isEmpty()) {
            throw new IllegalArgumentException("Carrier move has no printable shipments.");
        }
        return prepared.getStopGroups().get(0).getShipmentJobs().get(0);
    }

    private void enforceMaxLabels(int labels) {
        if (labels > MAX_LABELS_PER_JOB) {
            throw new IllegalArgumentException("Label count exceeds safe upper bound: " + MAX_LABELS_PER_JOB);
        }
    }

    private void printShipmentPlanSummary(LabelWorkflowService.PreparedJob prepared) {
        System.out.println();
        System.out.println("=== Shipment Plan Summary ===");
        System.out.println("Shipment: " + prepared.getShipmentId());
        System.out.println("Total units: " + prepared.getPlanResult().getTotalUnits());
        System.out.println("Estimated pallets (footprint): " + prepared.getPlanResult().getEstimatedPallets());
        System.out.println("  Full pallets: " + prepared.getPlanResult().getFullPallets());
        System.out.println("  Partial pallets: " + prepared.getPlanResult().getPartialPallets());
        if (!prepared.getPlanResult().getSkusMissingFootprint().isEmpty()) {
            System.out.println("Missing footprint SKUs: " + String.join(", ", prepared.getPlanResult().getSkusMissingFootprint()));
        }
        System.out.println("Labels: " + prepared.getLpnsForLabels().size());
        System.out.println("Info Tags: 1");
        System.out.println("=============================");
        System.out.println();
    }

    private void printCarrierMovePlanSummary(AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared, int labelCount) {
        int full = 0;
        int partial = 0;
        int totalUnits = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : prepared.getStopGroups()) {
            for (LabelWorkflowService.PreparedJob shipmentJob : stop.getShipmentJobs()) {
                full += shipmentJob.getPlanResult().getFullPallets();
                partial += shipmentJob.getPlanResult().getPartialPallets();
                totalUnits += shipmentJob.getPlanResult().getTotalUnits();
            }
        }

        System.out.println();
        System.out.println("=== Carrier Move Plan Summary ===");
        System.out.println("Carrier Move: " + prepared.getCarrierMoveId());
        System.out.println("Stops: " + prepared.getTotalStops());
        System.out.println("Total units: " + totalUnits);
        System.out.println("Estimated pallets (footprint): " + (full + partial));
        System.out.println("  Full pallets: " + full);
        System.out.println("  Partial pallets: " + partial);
        System.out.println("Labels: " + labelCount);
        System.out.println("Info Tags: " + (prepared.getTotalStops() + 1));
        System.out.println("=================================");
        System.out.println();
    }

    private void printCompletion(AdvancedPrintWorkflowService.PrintResult result) {
        System.out.println("Success! Generated " + result.getLabelsPrinted() + " label(s) and "
                + result.getInfoTagsPrinted() + " info tag(s)");
        System.out.println("Output saved to: " + result.getOutputDirectory().toAbsolutePath());
        if (result.isPrintToFile()) {
            System.out.println("(Print-to-file mode: labels were not sent to printer)");
        } else {
            System.out.println("Printed to: " + result.getPrinterId() + " (" + result.getPrinterEndpoint() + ")");
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
}
