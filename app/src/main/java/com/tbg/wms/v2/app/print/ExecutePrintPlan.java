package com.tbg.wms.v2.app.print;

import com.tbg.wms.v2.app.errors.WmsAppException;
import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.app.ports.PrintDispatcher;
import com.tbg.wms.v2.app.ports.PrinterCatalog;
import com.tbg.wms.v2.app.ports.PrinterRef;
import com.tbg.wms.v2.domain.print.PrintTask;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Executes prepared print tasks through storage and optional live dispatch ports.
 */
public final class ExecutePrintPlan {
    private final ArtifactStore artifactStore;
    private final PrintDispatcher printDispatcher;
    private final PrinterCatalog printerCatalog;

    /**
     * Creates a print-plan execution use case.
     *
     * @param artifactStore the artifact store.
     * @param printDispatcher the print dispatcher.
     * @param printerCatalog the printer catalog.
     */
    public ExecutePrintPlan(
            ArtifactStore artifactStore,
            PrintDispatcher printDispatcher,
            PrinterCatalog printerCatalog
    ) {
        this.artifactStore = Objects.requireNonNull(artifactStore, "artifactStore");
        this.printDispatcher = Objects.requireNonNull(printDispatcher, "printDispatcher");
        this.printerCatalog = Objects.requireNonNull(printerCatalog, "printerCatalog");
    }

    /**
     * Executes the requested workflow operation.
     *
     * @param request the request.
     * @return print execution totals
     */
    public PrintExecutionResult execute(PrintExecutionRequest request) {
        Objects.requireNonNull(request, "request");

        PrinterRef printer = request.printToFile() ? null : resolvePrinter(request.printerId());
        List<Path> artifactPaths = writeArtifacts(request.tasks());
        int dispatched = 0;

        if (!request.printToFile()) {
            dispatched = dispatchAll(printer, request.tasks());
        }

        return new PrintExecutionResult(artifactPaths.size(), dispatched, artifactPaths);
    }

    private PrinterRef resolvePrinter(String printerId) {
        if (printerId == null || printerId.isBlank()) {
            throw WmsAppException.printerNotFound(printerId);
        }
        return printerCatalog.findEnabledById(printerId)
                .orElseThrow(() -> WmsAppException.printerNotFound(printerId));
    }

    private List<Path> writeArtifacts(List<PrintTask> tasks) {
        List<Path> paths = new ArrayList<>();
        for (PrintTask task : tasks) {
            paths.add(artifactStore.write(task));
        }
        return paths;
    }

    private int dispatchAll(PrinterRef printer, List<PrintTask> tasks) {
        int dispatched = 0;
        for (PrintTask task : tasks) {
            try {
                printDispatcher.dispatch(printer, task);
                dispatched++;
            } catch (RuntimeException ex) {
                throw WmsAppException.printDispatchFailed(printer.id(), ex);
            }
        }
        return dispatched;
    }
}
