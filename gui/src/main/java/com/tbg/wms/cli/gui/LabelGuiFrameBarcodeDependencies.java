package com.tbg.wms.cli.gui;

import com.tbg.wms.core.print.PrinterConfig;

import javax.swing.DefaultComboBoxModel;
import javax.swing.text.JTextComponent;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Frame-backed dependency adapter for the barcode dialog factory.
 */
final class LabelGuiFrameBarcodeDependencies implements BarcodeDialogFactory.Dependencies {

    private final PrintTargetModelBuilder printTargetModelBuilder;
    private final PrintToFileSelector printToFileSelector;
    private final OutputDirSupplier outputDirSupplier;
    private final ClipboardInstaller clipboardInstaller;
    private final MessageSink showError;
    private final RootMessageResolver rootMessageResolver;
    private final PrinterResolver printerResolver;
    private final PrintDispatcher printDispatcher;

    LabelGuiFrameBarcodeDependencies(
            PrintTargetModelBuilder printTargetModelBuilder,
            PrintToFileSelector printToFileSelector,
            OutputDirSupplier outputDirSupplier,
            ClipboardInstaller clipboardInstaller,
            MessageSink showError,
            RootMessageResolver rootMessageResolver,
            PrinterResolver printerResolver,
            PrintDispatcher printDispatcher
    ) {
        this.printTargetModelBuilder = Objects.requireNonNull(printTargetModelBuilder, "printTargetModelBuilder cannot be null");
        this.printToFileSelector = Objects.requireNonNull(printToFileSelector, "printToFileSelector cannot be null");
        this.outputDirSupplier = Objects.requireNonNull(outputDirSupplier, "outputDirSupplier cannot be null");
        this.clipboardInstaller = Objects.requireNonNull(clipboardInstaller, "clipboardInstaller cannot be null");
        this.showError = Objects.requireNonNull(showError, "showError cannot be null");
        this.rootMessageResolver = Objects.requireNonNull(rootMessageResolver, "rootMessageResolver cannot be null");
        this.printerResolver = Objects.requireNonNull(printerResolver, "printerResolver cannot be null");
        this.printDispatcher = Objects.requireNonNull(printDispatcher, "printDispatcher cannot be null");
    }

    @Override
    public DefaultComboBoxModel<LabelWorkflowService.PrinterOption> buildPrintTargetModel(boolean includeFileOption) {
        return printTargetModelBuilder.build(includeFileOption);
    }

    @Override
    public boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected) {
        return printToFileSelector.isPrintToFile(selected);
    }

    @Override
    public Path defaultPrintToFileOutputDir() {
        return outputDirSupplier.get();
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
    public PrinterConfig resolvePrinter(String printerId) throws Exception {
        return printerResolver.resolve(printerId);
    }

    @Override
    public void printBarcode(PrinterConfig printerConfig, String zpl) throws Exception {
        printDispatcher.print(printerConfig, zpl);
    }

    interface PrintTargetModelBuilder {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> build(boolean includeFileOption);
    }

    interface PrintToFileSelector {
        boolean isPrintToFile(LabelWorkflowService.PrinterOption selected);
    }

    interface OutputDirSupplier {
        Path get();
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

    interface PrinterResolver {
        PrinterConfig resolve(String printerId) throws Exception;
    }

    interface PrintDispatcher {
        void print(PrinterConfig printerConfig, String zpl) throws Exception;
    }
}
