/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Shared print execution policy for the GUI shell.
 */
final class GuiPrintExecutionSupport {

    private final GuiPrintFlowSupport printFlowSupport;

    GuiPrintExecutionSupport(GuiPrintFlowSupport printFlowSupport) {
        this.printFlowSupport = Objects.requireNonNull(printFlowSupport, "printFlowSupport cannot be null");
    }

    PreparedExecution prepareExecution(PrintRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        GuiPrintFlowSupport.PrintPlan plan = printFlowSupport.planPrint(
                request.carrierMoveMode(),
                request.preparedJob(),
                request.preparedCarrierJob(),
                request.selectedPrinter(),
                request.printToFile(),
                request.selection(),
                request.includeInfoTags(),
                request.defaultOutputDir(),
                request.outputTimestamp()
        );
        String confirmationMessage = request.printToFile()
                ? null
                : printFlowSupport.buildConfirmationMessage(plan, request.selectedPrinter());
        return new PreparedExecution(
                plan,
                request.preparedJob(),
                request.preparedCarrierJob(),
                confirmationMessage
        );
    }

    AdvancedPrintWorkflowService.PrintResult execute(
            PreparedExecution execution,
            PrintRunner printRunner
    ) throws Exception {
        Objects.requireNonNull(execution, "execution cannot be null");
        Objects.requireNonNull(printRunner, "printRunner cannot be null");
        GuiPrintFlowSupport.PrintPlan plan = execution.plan();
        if (plan.carrierMoveMode()) {
            return printRunner.printCarrierMove(
                    execution.preparedCarrierJob(),
                    plan.selectedCarrierLabels(),
                    plan.printerId(),
                    plan.outputDir(),
                    plan.printToFile(),
                    plan.includeInfoTags()
            );
        }
        return printRunner.printShipment(
                execution.preparedJob(),
                plan.selectedShipmentLpns(),
                plan.printerId(),
                plan.outputDir(),
                plan.printToFile(),
                plan.includeInfoTags()
        );
    }

    CompletionOutcome buildCompletionOutcome(AdvancedPrintWorkflowService.PrintResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return new CompletionOutcome(
                printFlowSupport.buildCompletionStatus(result),
                printFlowSupport.buildCompletionDialogMessage(result)
        );
    }

    FailureOutcome buildFailureOutcome(Exception ex) {
        Objects.requireNonNull(ex, "ex cannot be null");
        return new FailureOutcome("Print failed.", GuiExceptionMessageSupport.rootMessage(ex));
    }

    record PrintRequest(
            boolean carrierMoveMode,
            LabelWorkflowService.PreparedJob preparedJob,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            LabelWorkflowService.PrinterOption selectedPrinter,
            boolean printToFile,
            PreviewSelectionSupport.SelectionSnapshot selection,
            boolean includeInfoTags,
            Path defaultOutputDir,
            String outputTimestamp
    ) {
    }

    record PreparedExecution(
            GuiPrintFlowSupport.PrintPlan plan,
            LabelWorkflowService.PreparedJob preparedJob,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            String confirmationMessage
    ) {
        boolean requiresConfirmation() {
            return confirmationMessage != null;
        }
    }

    record CompletionOutcome(
            String statusMessage,
            String dialogMessage
    ) {
    }

    record FailureOutcome(
            String statusMessage,
            String errorMessage
    ) {
    }

    interface PrintRunner {
        AdvancedPrintWorkflowService.PrintResult printShipment(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) throws Exception;

        AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) throws Exception;
    }
}
