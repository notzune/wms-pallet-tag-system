package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.print.PrinterConfig;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class AdvancedPrintJobExecutionGatewayTest {

    @Test
    void executeShipmentJob_shouldDelegateToExecutionSupport() throws Exception {
        RecordingCheckpointGateway checkpointGateway = new RecordingCheckpointGateway();
        AdvancedPrintExecutionSupport executionSupport = new AdvancedPrintExecutionSupport(
                checkpointGateway,
                new AdvancedPrintResultSupport(),
                DateTimeFormatter.ofPattern("'TS'")
        );
        AdvancedPrintJobExecutionGateway gateway = new AdvancedPrintJobExecutionGateway(executionSupport);
        LabelWorkflowService.PreparedJob job = PreviewSelectionTestData.shipmentJob(
                "SHIP1",
                List.of(new Lpn("LPN-1", "SHIP1", null, 0, 0, 0.0, null, null, null, null, null, List.of()))
        );
        List<AdvancedPrintWorkflowService.PrintTask> tasks = List.of(
                new AdvancedPrintWorkflowService.PrintTask(
                        AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL,
                        "label.zpl",
                        "^XA^XZ",
                        "LPN-1"
                )
        );

        AdvancedPrintWorkflowService.PrintResult result =
                gateway.executeShipmentJob(job, "FILE", Path.of("out"), true, tasks);

        assertEquals("shipment-SHIP1-TS", checkpointGateway.createdCheckpoint.id);
        assertSame(checkpointGateway.createdCheckpoint, checkpointGateway.executedCheckpoint);
        assertEquals(1, result.getLabelsPrinted());
        assertEquals(0, result.getInfoTagsPrinted());
        assertEquals("FILE", result.getPrinterId());
    }

    private static final class RecordingCheckpointGateway implements AdvancedPrintExecutionSupport.CheckpointGateway {
        private AdvancedPrintWorkflowService.JobCheckpoint createdCheckpoint;
        private AdvancedPrintWorkflowService.JobCheckpoint executedCheckpoint;

        @Override
        public AdvancedPrintWorkflowService.JobCheckpoint createCheckpoint(
                String id,
                AdvancedPrintWorkflowService.InputMode mode,
                String sourceId,
                Path outputDir,
                boolean printToFile,
                PrinterConfig printer,
                List<AdvancedPrintWorkflowService.PrintTask> tasks
        ) {
            createdCheckpoint = new AdvancedPrintWorkflowService.JobCheckpoint();
            createdCheckpoint.id = id;
            createdCheckpoint.mode = mode;
            createdCheckpoint.sourceId = sourceId;
            createdCheckpoint.outputDirectory = outputDir.toString();
            createdCheckpoint.printToFile = printToFile;
            createdCheckpoint.printerId = printToFile ? "FILE" : printer.getId();
            createdCheckpoint.printerEndpoint = printToFile ? "FILE" : printer.getEndpoint();
            createdCheckpoint.tasks = tasks;
            return createdCheckpoint;
        }

        @Override
        public void executeTasks(
                AdvancedPrintWorkflowService.JobCheckpoint checkpoint,
                PrinterConfig printer,
                int startIndex
        ) {
            executedCheckpoint = checkpoint;
        }
    }
}
