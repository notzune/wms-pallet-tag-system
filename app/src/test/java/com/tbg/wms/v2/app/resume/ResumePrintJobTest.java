package com.tbg.wms.v2.app.resume;

import com.tbg.wms.v2.app.ports.ArtifactStore;
import com.tbg.wms.v2.app.ports.CheckpointStore;
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

class ResumePrintJobTest {
    @Test
    void listResumeCandidates_returnsIncompleteCheckpointsNewestFirst() {
        InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore(List.of(
                checkpoint("old", 1),
                checkpoint("new", 2)
        ));

        ListResumeCandidates lister = new ListResumeCandidates(checkpoints);

        assertEquals(List.of("new", "old"), lister.list().stream()
                .map(ResumeCandidate::checkpointId)
                .toList());
    }

    @Test
    void resumePrintJob_reprintsLastSuccessfulTaskThenContinues() {
        List<String> events = new ArrayList<>();
        InMemoryCheckpointStore checkpoints = new InMemoryCheckpointStore(List.of(
                checkpoint("job-1", 2)
        ));
        ResumePrintJob resume = new ResumePrintJob(
                checkpoints,
                artifactStore(events),
                dispatcher(events),
                catalog()
        );

        ResumePrintResult result = resume.resume("job-1");

        assertEquals(2, result.artifactsWritten());
        assertEquals(2, result.tasksDispatched());
        assertEquals(List.of(
                "write:second.zpl",
                "write:third.zpl",
                "dispatch:second.zpl:P1",
                "dispatch:third.zpl:P1"
        ), events);
        assertEquals(3, checkpoints.saved().get(checkpoints.saved().size() - 1).nextTaskIndex());
    }

    @Test
    void resumePrintJob_rejectsMissingCheckpoint() {
        ResumePrintJob resume = new ResumePrintJob(
                new InMemoryCheckpointStore(List.of()),
                task -> Path.of(task.artifactName()),
                (printer, task) -> {
                },
                catalog()
        );

        assertThrows(IllegalArgumentException.class, () -> resume.resume("missing"));
    }

    private static CheckpointStore.PrintCheckpoint checkpoint(String id, int nextTaskIndex) {
        return new CheckpointStore.PrintCheckpoint(
                id,
                "source-" + id,
                nextTaskIndex,
                3,
                null,
                false,
                "P1",
                tasks()
        );
    }

    private static List<PrintTask> tasks() {
        return List.of(
                new PrintTask(PrintTask.Kind.PALLET_LABEL, "first.zpl", "1"),
                new PrintTask(PrintTask.Kind.PALLET_LABEL, "second.zpl", "2"),
                new PrintTask(PrintTask.Kind.SHIPMENT_INFO_TAG, "third.zpl", "3")
        );
    }

    private static ArtifactStore artifactStore(List<String> events) {
        return task -> {
            events.add("write:" + task.artifactName());
            return Path.of("out", task.artifactName());
        };
    }

    private static PrintDispatcher dispatcher(List<String> events) {
        return (printer, task) -> events.add("dispatch:" + task.artifactName() + ":" + printer.id());
    }

    private static PrinterCatalog catalog() {
        PrinterRef printer = new PrinterRef("P1", "Printer 1", List.of("zpl"));
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

    private static final class InMemoryCheckpointStore implements CheckpointStore {
        private final List<PrintCheckpoint> checkpoints;
        private final List<PrintCheckpoint> saved = new ArrayList<>();

        private InMemoryCheckpointStore(List<PrintCheckpoint> checkpoints) {
            this.checkpoints = new ArrayList<>(checkpoints);
        }

        @Override
        public void save(PrintCheckpoint checkpoint) {
            saved.add(checkpoint);
        }

        @Override
        public Optional<PrintCheckpoint> findById(String checkpointId) {
            return checkpoints.stream()
                    .filter(checkpoint -> checkpoint.id().equals(checkpointId))
                    .findFirst();
        }

        @Override
        public List<PrintCheckpoint> findIncomplete() {
            return List.copyOf(checkpoints);
        }

        private List<PrintCheckpoint> saved() {
            return saved;
        }
    }
}
