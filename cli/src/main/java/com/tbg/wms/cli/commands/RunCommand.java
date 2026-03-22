/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.cli.gui.AdvancedPrintWorkflowService;
import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.cli.gui.WorkflowPrintPlanSupport;
import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.exception.WmsPrintException;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.label.LabelSelectionSupport;
import com.tbg.wms.core.model.Lpn;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Generates pallet labels from WMS data for a single shipment or a carrier move.
 *
 * <p>Exactly one identifier mode is required per invocation:
 * {@code --shipment-id} or {@code --carrier-move-id}.</p>
 */
@Command(
        name = "run",
        description = "Generate pallet shipping labels for a shipment or carrier move"
)
public final class RunCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(RunCommand.class);
    private static final int JOB_ID_LENGTH = 8;
    private static final int MAX_LABELS_PER_JOB = 10_000;
    private static final int MAX_LABEL_PREVIEW_ROWS = 100;

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

    @Option(
            names = {"--labels"},
            description = "Shipment label selection expression. Use 'all' or 1-based indexes/ranges like 1,3,5-7"
    )
    private String labelSelectionExpression;

    /**
     * Executes shipment/carrier print workflow for CLI mode.
     *
     * @return exit code (0 success, non-zero failure category)
     */
    @Override
    public Integer call() {
        String jobId = UUID.randomUUID().toString().substring(0, JOB_ID_LENGTH);
        MDC.put("jobId", jobId);

        try {
            AppConfig config = RootCommand.config();
            MDC.put("site", config.activeSiteCode());

            String inputId = resolveInputId();
            boolean carrierMode = isCarrierMoveMode();
            MDC.put(carrierMode ? "carrierMoveId" : "shipmentId", inputId);
            if (carrierMode && labelSelectionExpression != null && !labelSelectionExpression.isBlank()) {
                throw new IllegalArgumentException("--labels is only supported with --shipment-id.");
            }

            boolean printToFileMode = dryRun || printToFile;
            String effectiveOutputDir = printToFile
                    ? RuntimePathResolver.resolveJarSiblingDir(RunCommand.class, "out").toString()
                    : outputDir;
            Path outputPath = prepareOutputDirectory(effectiveOutputDir);
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

    /**
     * Executes shipment-mode preview/print workflow and emits an operator summary.
     */
    private Integer executeShipmentRun(AdvancedPrintWorkflowService workflow,
                                       String id,
                                       Path outputPath,
                                       boolean printToFileMode) throws Exception {
        log.info("Starting shipment print job for {}", id);
        LabelWorkflowService.PreparedJob prepared = workflow.prepareShipmentJob(id);
        List<Lpn> selectedLpns = resolveSelectedShipmentLpns(prepared);
        WorkflowPrintPlanSupport.ShipmentPlanSummary plan =
                WorkflowPrintPlanSupport.buildShipmentPlan(prepared, selectedLpns, 1);
        printShipmentPlanSummary(plan, prepared, selectedLpns);

        int labels = selectedLpns.size();
        enforceMaxLabels(labels);

        String printerId = resolvePrinterId(
                printToFileMode,
                prepared.getRouting(),
                prepared.getStagingLocation()
        );

        AdvancedPrintWorkflowService.PrintResult result = workflow.printShipmentJob(
                prepared,
                selectedLpns,
                printerId,
                outputPath,
                printToFileMode
        );

        printCompletion(result);
        return 0;
    }

    /**
     * Executes carrier-move workflow across all mapped stops in deterministic stop order.
     */
    private Integer executeCarrierMoveRun(AdvancedPrintWorkflowService workflow,
                                          String id,
                                          Path outputPath,
                                          boolean printToFileMode) throws Exception {
        log.info("Starting carrier move print job for {}", id);
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepared = workflow.prepareCarrierMoveJob(id);
        WorkflowPrintPlanSupport.CarrierMovePlanSummary plan = WorkflowPrintPlanSupport.buildCarrierMovePlan(prepared);
        enforceMaxLabels(plan.getTotalLabels());
        printCarrierMovePlanSummary(plan);

        LabelWorkflowService.PreparedJob firstShipment = prepared.firstShipmentJob();
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

    /**
     * Validates mutually-exclusive input mode and returns the selected ID.
     */
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

    private List<Lpn> resolveSelectedShipmentLpns(LabelWorkflowService.PreparedJob prepared) {
        if (labelSelectionExpression == null || labelSelectionExpression.isBlank()) {
            return prepared.getLpnsForLabels();
        }
        List<LabelSelectionRef> availableSelections = LabelSelectionSupport.buildShipmentSelections(
                prepared.getShipmentId(),
                prepared.getLpnsForLabels()
        );
        List<LabelSelectionRef> selectedRefs = LabelSelectionSupport.selectByExpression(
                availableSelections,
                labelSelectionExpression
        );
        return LabelSelectionSupport.selectLpnsByRefs(prepared.getLpnsForLabels(), selectedRefs);
    }

    private Path prepareOutputDirectory(String outputDirectory) throws Exception {
        Path outputPath = Paths.get(outputDirectory);
        Files.createDirectories(outputPath);
        log.debug("Output directory: {}", outputPath.toAbsolutePath());
        return outputPath;
    }

    /**
     * Resolves final printer ID, honoring print-to-file mode and explicit override first.
     */
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
        if (!routed.isEnabled()) {
            throw new IllegalArgumentException("Could not resolve an enabled printer from routing.");
        }
        return routed.getId();
    }

    private void enforceMaxLabels(int labels) {
        if (labels > MAX_LABELS_PER_JOB) {
            throw new IllegalArgumentException("Label count exceeds safe upper bound: " + MAX_LABELS_PER_JOB);
        }
    }

    private void printShipmentPlanSummary(
            WorkflowPrintPlanSupport.ShipmentPlanSummary plan,
            LabelWorkflowService.PreparedJob prepared,
            List<Lpn> selectedLpns
    ) {
        System.out.println();
        System.out.println("=== Shipment Plan Summary ===");
        System.out.println("Shipment: " + plan.getShipmentId());
        System.out.println("Total units: " + plan.getTotalUnits());
        System.out.println("Estimated pallets (footprint): " + plan.getEstimatedPallets());
        System.out.println("  Full pallets: " + plan.getFullPallets());
        System.out.println("  Partial pallets: " + plan.getPartialPallets());
        if (!plan.getMissingFootprintSkus().isEmpty()) {
            System.out.println("Missing footprint SKUs: " + String.join(", ", plan.getMissingFootprintSkus()));
        }
        System.out.println("Labels selected: " + plan.getSelectedLabels() + " / " + plan.getTotalLabels());
        if (labelSelectionExpression != null && !labelSelectionExpression.isBlank()) {
            System.out.println("Selection: " + labelSelectionExpression.trim());
        }
        System.out.println("Info Tags: " + plan.getInfoTagCount());
        printShipmentLabelPreview(prepared, selectedLpns);
        System.out.println("=============================");
        System.out.println();
    }

    private void printShipmentLabelPreview(LabelWorkflowService.PreparedJob prepared, List<Lpn> selectedLpns) {
        System.out.println("Label Preview:");
        java.util.Set<String> selectedIds = new java.util.HashSet<>();
        for (Lpn selectedLpn : selectedLpns) {
            if (selectedLpn != null && selectedLpn.getLpnId() != null) {
                selectedIds.add(selectedLpn.getLpnId());
            }
        }
        for (int i = 0; i < prepared.getLpnsForLabels().size() && i < MAX_LABEL_PREVIEW_ROWS; i++) {
            Lpn lpn = prepared.getLpnsForLabels().get(i);
            boolean selected = lpn != null && selectedIds.contains(lpn.getLpnId());
            System.out.printf("  [%s] %d. %s%n", selected ? "x" : " ", i + 1, renderLpnPreviewId(lpn));
        }
        if (prepared.getLpnsForLabels().size() > MAX_LABEL_PREVIEW_ROWS) {
            System.out.println("  ... showing first " + MAX_LABEL_PREVIEW_ROWS + " labels");
        }
    }

    private static String renderLpnPreviewId(Lpn lpn) {
        if (lpn == null || lpn.getLpnId() == null || lpn.getLpnId().isBlank()) {
            return "UNKNOWN";
        }
        return lpn.getLpnId();
    }

    private void printCarrierMovePlanSummary(WorkflowPrintPlanSupport.CarrierMovePlanSummary plan) {
        System.out.println();
        System.out.println("=== Carrier Move Plan Summary ===");
        System.out.println("Carrier Move: " + plan.getCarrierMoveId());
        System.out.println("Stops: " + plan.getTotalStops());
        System.out.println("Total units: " + plan.getTotalUnits());
        System.out.println("Estimated pallets (footprint): " + plan.getEstimatedPallets());
        System.out.println("  Full pallets: " + plan.getFullPallets());
        System.out.println("  Partial pallets: " + plan.getPartialPallets());
        System.out.println("Labels: " + plan.getTotalLabels());
        System.out.println("Info Tags: " + plan.getInfoTagCount());
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
}
