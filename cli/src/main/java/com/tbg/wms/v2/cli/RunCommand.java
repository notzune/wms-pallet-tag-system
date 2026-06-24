package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.labels.BuildCarrierMovePrintPlan;
import com.tbg.wms.v2.app.labels.BuildShipmentPrintPlan;
import com.tbg.wms.v2.app.ports.PrinterCatalog;
import com.tbg.wms.v2.app.print.ExecutePrintPlan;
import com.tbg.wms.v2.app.print.PrintExecutionRequest;
import com.tbg.wms.v2.app.print.PrintExecutionResult;
import com.tbg.wms.v2.domain.print.PrintTask;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Provides run command behavior for WMS 2.0 workflows.
 */
@Command(name = "run", description = "Generate shipment or carrier-move print artifacts")
final class RunCommand implements Callable<Integer> {
    @ParentCommand
    private WmsCli root;

    @Spec
    private CommandSpec spec;

    @Option(names = {"-s", "--shipment-id"})
    private String shipmentId;

    @Option(names = {"-c", "--carrier-move-id"})
    private String carrierMoveId;

    @Option(names = {"--print-to-file", "--ptf"})
    private boolean printToFile;

    @Option(names = "--output-dir", defaultValue = "out")
    private Path outputDir;

    @Option(names = "--printer")
    private String printerId;

    /**
     * Runs the command and returns a process-style exit code.
     *
     * @return process-style exit code
     */
    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        if ((isBlank(shipmentId) && isBlank(carrierMoveId)) || (!isBlank(shipmentId) && !isBlank(carrierMoveId))) {
            err.println("Error: specify exactly one of --shipment-id or --carrier-move-id.");
            return 2;
        }
        if (!printToFile) {
            err.println("Error: live printing requires a printer adapter; use --print-to-file for this 2.0 CLI track.");
            return 2;
        }
        try {
            List<PrintTask> tasks = isBlank(shipmentId) ? carrierMoveTasks() : shipmentTasks();
            ExecutePrintPlan executor = new ExecutePrintPlan(
                    new OutputDirArtifactStore(outputDir),
                    (printer, task) -> {
                    },
                    emptyPrinterCatalog()
            );
            PrintExecutionResult result = executor.execute(new PrintExecutionRequest(tasks, true, printerId));
            out.println(isBlank(shipmentId) ? "=== Carrier Move Plan Summary ===" : "=== Shipment Plan Summary ===");
            out.println("Artifacts written: " + result.artifactsWritten());
            out.println("Output saved to: " + outputDir.toAbsolutePath().normalize());
            out.println("(Print-to-file mode: labels were not sent to printer)");
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println("Error: " + ex.getMessage());
            return 2;
        } catch (RuntimeException ex) {
            err.println("Print workflow failed: " + ex.getMessage());
            return 5;
        }
    }

    private List<PrintTask> shipmentTasks() {
        return new BuildShipmentPrintPlan()
                .build(root.dependencies().shipmentRepository().findByShipmentId(shipmentId), List.of())
                .tasks();
    }

    private List<PrintTask> carrierMoveTasks() {
        return new BuildCarrierMovePrintPlan()
                .build(root.dependencies().carrierMoveRepository().findByCarrierMoveId(carrierMoveId))
                .tasks();
    }

    private static PrinterCatalog emptyPrinterCatalog() {
        return new PrinterCatalog() {
            /**
             * Lists enabled.
             *
             * @return enabled printers
             */
            @Override
            public List<com.tbg.wms.v2.app.ports.PrinterRef> listEnabled() {
                return List.of();
            }

            /**
             * Finds enabled by id.
             *
             * @param printerId the printer id.
             * @return matching enabled printer, when present
             */
            @Override
            public Optional<com.tbg.wms.v2.app.ports.PrinterRef> findEnabledById(String printerId) {
                return Optional.empty();
            }
        };
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
