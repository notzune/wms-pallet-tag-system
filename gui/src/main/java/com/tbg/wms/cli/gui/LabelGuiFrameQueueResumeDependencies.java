package com.tbg.wms.cli.gui;

import javax.swing.JFrame;
import javax.swing.text.JTextComponent;
import java.util.Objects;

/**
 * Frame-backed dependency adapter for queue/resume dialogs.
 */
final class LabelGuiFrameQueueResumeDependencies implements QueueResumeDialogSupport.Dependencies {

    private final JFrame ownerFrame;
    private final AdvancedPrintWorkflowService workflow;
    private final LabelPreviewFormatter previewFormatter;
    private final SelectedPrinterSupplier selectedPrinterSupplier;
    private final PrintToFileSelector printToFileSelector;
    private final ClipboardInstaller clipboardInstaller;
    private final MessageSink showError;
    private final RootMessageResolver rootMessageResolver;
    private final MessageSink setReady;

    LabelGuiFrameQueueResumeDependencies(
            JFrame ownerFrame,
            AdvancedPrintWorkflowService workflow,
            LabelPreviewFormatter previewFormatter,
            SelectedPrinterSupplier selectedPrinterSupplier,
            PrintToFileSelector printToFileSelector,
            ClipboardInstaller clipboardInstaller,
            MessageSink showError,
            RootMessageResolver rootMessageResolver,
            MessageSink setReady
    ) {
        this.ownerFrame = Objects.requireNonNull(ownerFrame, "ownerFrame cannot be null");
        this.workflow = Objects.requireNonNull(workflow, "workflow cannot be null");
        this.previewFormatter = Objects.requireNonNull(previewFormatter, "previewFormatter cannot be null");
        this.selectedPrinterSupplier = Objects.requireNonNull(selectedPrinterSupplier, "selectedPrinterSupplier cannot be null");
        this.printToFileSelector = Objects.requireNonNull(printToFileSelector, "printToFileSelector cannot be null");
        this.clipboardInstaller = Objects.requireNonNull(clipboardInstaller, "clipboardInstaller cannot be null");
        this.showError = Objects.requireNonNull(showError, "showError cannot be null");
        this.rootMessageResolver = Objects.requireNonNull(rootMessageResolver, "rootMessageResolver cannot be null");
        this.setReady = Objects.requireNonNull(setReady, "setReady cannot be null");
    }

    @Override
    public JFrame ownerFrame() {
        return ownerFrame;
    }

    @Override
    public AdvancedPrintWorkflowService workflow() {
        return workflow;
    }

    @Override
    public LabelPreviewFormatter previewFormatter() {
        return previewFormatter;
    }

    @Override
    public LabelWorkflowService.PrinterOption selectedPrinterOption() {
        return selectedPrinterSupplier.get();
    }

    @Override
    public boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected) {
        return printToFileSelector.isPrintToFile(selected);
    }

    @Override
    public void installClipboardBehavior(JTextComponent... fields) {
        clipboardInstaller.install(fields);
    }

    @Override
    public void showError(String message) {
        showError.accept(message);
    }

    @Override
    public String rootMessage(Throwable throwable) {
        return rootMessageResolver.resolve(throwable);
    }

    @Override
    public void setReady(String message) {
        setReady.accept(message);
    }

    interface SelectedPrinterSupplier {
        LabelWorkflowService.PrinterOption get();
    }

    interface PrintToFileSelector {
        boolean isPrintToFile(LabelWorkflowService.PrinterOption selected);
    }

    interface ClipboardInstaller {
        void install(JTextComponent... fields);
    }

    interface MessageSink {
        void accept(String message);
    }

    interface RootMessageResolver {
        String resolve(Throwable throwable);
    }
}
