package com.tbg.wms.v2.app.print;

import com.tbg.wms.v2.app.errors.WmsAppException;
import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.app.ports.PrintDispatcher;
import com.tbg.wms.v2.app.ports.PrinterCatalog;
import com.tbg.wms.v2.app.ports.PrinterRef;
import com.tbg.wms.v2.domain.print.PrintTask;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExecutePrintPlanTest {
    @Test
    void executePrintPlan_writesArtifactsBeforeDispatchingPrintTasks() {
        List<String> events = new ArrayList<>();
        RecordingArtifactStore artifacts = new RecordingArtifactStore(events);
        RecordingDispatcher dispatcher = new RecordingDispatcher(events);
        PrinterRef printer = new PrinterRef("P1", "Dispatch", List.of("zpl"));
        ExecutePrintPlan executor = new ExecutePrintPlan(artifacts, dispatcher, catalogWith(printer));

        PrintExecutionResult result = executor.execute(new PrintExecutionRequest(
                tasks(),
                false,
                "P1"
        ));

        assertEquals(List.of(
                "write:first.zpl",
                "write:second.zpl",
                "dispatch:first.zpl:P1",
                "dispatch:second.zpl:P1"
        ), events);
        assertEquals(2, result.artifactsWritten());
        assertEquals(2, result.tasksDispatched());
        assertEquals(List.of(Path.of("out", "first.zpl"), Path.of("out", "second.zpl")), result.artifactPaths());
    }

    @Test
    void executePrintPlan_printToFileWritesArtifactsWithoutPrinterLookupOrDispatch() {
        List<String> events = new ArrayList<>();
        ExecutePrintPlan executor = new ExecutePrintPlan(
                new RecordingArtifactStore(events),
                new RecordingDispatcher(events),
                explodingCatalog()
        );

        PrintExecutionResult result = executor.execute(new PrintExecutionRequest(tasks(), true, null));

        assertEquals(List.of("write:first.zpl", "write:second.zpl"), events);
        assertEquals(2, result.artifactsWritten());
        assertEquals(0, result.tasksDispatched());
    }

    @Test
    void executePrintPlan_livePrintRequiresKnownPrinter() {
        ExecutePrintPlan executor = new ExecutePrintPlan(
                task -> Path.of("out", task.artifactName()),
                (printer, task) -> {
                },
                emptyCatalog()
        );

        WmsAppException ex = assertThrows(WmsAppException.class,
                () -> executor.execute(new PrintExecutionRequest(tasks(), false, "missing")));

        assertEquals("PRINTER_NOT_FOUND", ex.code());
    }

    private static List<PrintTask> tasks() {
        return List.of(
                new PrintTask(PrintTask.Kind.PALLET_LABEL, "first.zpl", "SHIP-1:LPN-1"),
                new PrintTask(PrintTask.Kind.SHIPMENT_INFO_TAG, "second.zpl", "INFO-SHIPMENT SHIP-1")
        );
    }

    private static PrinterCatalog catalogWith(PrinterRef printer) {
        return new PrinterCatalog() {
            @Override
            public List<PrinterRef> listEnabled() {
                return List.of(printer);
            }

            @Override
            public Optional<PrinterRef> findEnabledById(String printerId) {
                return printer.id().equals(printerId) ? Optional.of(printer) : Optional.empty();
            }
        };
    }

    private static PrinterCatalog emptyCatalog() {
        return new PrinterCatalog() {
            @Override
            public List<PrinterRef> listEnabled() {
                return List.of();
            }

            @Override
            public Optional<PrinterRef> findEnabledById(String printerId) {
                return Optional.empty();
            }
        };
    }

    private static PrinterCatalog explodingCatalog() {
        return new PrinterCatalog() {
            @Override
            public List<PrinterRef> listEnabled() {
                throw new AssertionError("printer lookup should not run for print-to-file");
            }

            @Override
            public Optional<PrinterRef> findEnabledById(String printerId) {
                throw new AssertionError("printer lookup should not run for print-to-file");
            }
        };
    }

    private static final class RecordingArtifactStore implements ArtifactStore {
        private final List<String> events;

        private RecordingArtifactStore(List<String> events) {
            this.events = events;
        }

        @Override
        public Path write(PrintTask task) {
            events.add("write:" + task.artifactName());
            return Path.of("out", task.artifactName());
        }
    }

    private static final class RecordingDispatcher implements PrintDispatcher {
        private final List<String> events;

        private RecordingDispatcher(List<String> events) {
            this.events = events;
        }

        @Override
        public void dispatch(PrinterRef printer, PrintTask task) {
            events.add("dispatch:" + task.artifactName() + ":" + printer.id());
        }
    }
}
