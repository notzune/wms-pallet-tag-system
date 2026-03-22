package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.rail.RailCarCard;

import javax.swing.DefaultComboBoxModel;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Builds dialog-ready state transitions for rail preview, generation, and printer loading.
 */
final class RailDialogActionSupport {
    private final RailDialogSupport dialogSupport;
    private final RailDialogExecutionSupport executionSupport;

    RailDialogActionSupport(RailDialogSupport dialogSupport, RailDialogExecutionSupport executionSupport) {
        this.dialogSupport = Objects.requireNonNull(dialogSupport, "dialogSupport cannot be null");
        this.executionSupport = Objects.requireNonNull(executionSupport, "executionSupport cannot be null");
    }

    PrinterLoadOutcome buildPrinterLoadOutcome(
            List<LabelWorkflowService.PrinterOption> printers,
            Path defaultOutputDir
    ) {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model =
                dialogSupport.buildPrinterModel(printers, defaultOutputDir);
        return new PrinterLoadOutcome(model, model.getSize() > 0, "Ready.");
    }

    PreviewOutcome buildPreviewOutcome(
            RailWorkflowService.PreparedRailJob preparedJob,
            String diagnosticsText
    ) {
        Objects.requireNonNull(preparedJob, "preparedJob cannot be null");
        List<RailCarCard> cards = preparedJob.getCards();
        return new PreviewOutcome(
                cards,
                diagnosticsText,
                !cards.isEmpty(),
                executionSupport.previewReadyMessage()
        );
    }

    GenerationOutcome buildGenerationOutcome(RailWorkflowService.GenerationResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        String message = dialogSupport.buildGenerationMessage(result);
        String diagnosticsAppend = "\n\nGeneration Result\n-----------------\n" + message + '\n';
        return new GenerationOutcome(diagnosticsAppend, dialogSupport.buildReadyMessage(result));
    }

    record PrinterLoadOutcome(
            DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model,
            boolean shouldSelectFirst,
            String readyMessage
    ) {
    }

    record PreviewOutcome(
            List<RailCarCard> cards,
            String diagnosticsText,
            boolean shouldSelectFirstRow,
            String readyMessage
    ) {
    }

    record GenerationOutcome(
            String diagnosticsAppend,
            String readyMessage
    ) {
    }
}
