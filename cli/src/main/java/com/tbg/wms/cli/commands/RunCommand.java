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
import com.tbg.wms.core.exception.WmsDbConnectivityException;
import com.tbg.wms.core.exception.WmsPrintException;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;
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
    private static final int MAX_LABEL_PREVIEW_ROWS = 100;
    private final RunCommandOutputSupport outputSupport = new RunCommandOutputSupport();
    private final RunCommandExecutionSupport executionSupport = new RunCommandExecutionSupport();

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

            String inputId = executionSupport.resolveInputId(shipmentId, carrierMoveId);
            boolean carrierMode = executionSupport.isCarrierMoveMode(carrierMoveId);
            MDC.put(carrierMode ? "carrierMoveId" : "shipmentId", inputId);
            if (carrierMode && labelSelectionExpression != null && !labelSelectionExpression.isBlank()) {
                throw new IllegalArgumentException("--labels is only supported with --shipment-id.");
            }

            boolean printToFileMode = executionSupport.isPrintToFileMode(dryRun, printToFile);
            String effectiveOutputDir = executionSupport.effectiveOutputDir(printToFile, outputDir);
            Path outputPath = executionSupport.prepareOutputDirectory(effectiveOutputDir);
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
        List<Lpn> selectedLpns = executionSupport.resolveSelectedShipmentLpns(prepared, labelSelectionExpression);
        WorkflowPrintPlanSupport.ShipmentPlanSummary plan =
                WorkflowPrintPlanSupport.buildShipmentPlan(prepared, selectedLpns, 1);
        printShipmentPlanSummary(plan, prepared, selectedLpns);

        int labels = selectedLpns.size();
        executionSupport.enforceMaxLabels(labels);

        String printerId = executionSupport.resolvePrinterId(
                printToFileMode,
                printerOverride,
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
        executionSupport.enforceMaxLabels(plan.getTotalLabels());
        printCarrierMovePlanSummary(plan);

        LabelWorkflowService.PreparedJob firstShipment = prepared.firstShipmentJob();
        String printerId = executionSupport.resolvePrinterId(
                printToFileMode,
                printerOverride,
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

    private void printShipmentPlanSummary(
            WorkflowPrintPlanSupport.ShipmentPlanSummary plan,
            LabelWorkflowService.PreparedJob prepared,
            List<Lpn> selectedLpns
    ) {
        System.out.print(outputSupport.buildShipmentPlanSummary(
                plan,
                prepared,
                selectedLpns,
                labelSelectionExpression,
                MAX_LABEL_PREVIEW_ROWS
        ));
    }

    private void printCarrierMovePlanSummary(WorkflowPrintPlanSupport.CarrierMovePlanSummary plan) {
        System.out.print(outputSupport.buildCarrierMovePlanSummary(plan));
    }

    private void printCompletion(AdvancedPrintWorkflowService.PrintResult result) {
        System.out.print(outputSupport.buildCompletionMessage(result));
    }
}
