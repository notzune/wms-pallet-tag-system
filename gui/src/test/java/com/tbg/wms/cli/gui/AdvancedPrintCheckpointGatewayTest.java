package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.print.PrinterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdvancedPrintCheckpointGatewayTest {

    @TempDir
    Path tempDir;

    @Test
    void createAndExecuteTasks_shouldDelegateThroughCheckpointSupport() throws Exception {
        PrintCheckpointSupport checkpointSupport = new PrintCheckpointSupport(
                new JobCheckpointStore(tempDir.resolve("checkpoints")),
                new LabelWorkflowService(new AppConfig()),
                10,
                100
        );
        AdvancedPrintCheckpointGateway gateway = new AdvancedPrintCheckpointGateway(checkpointSupport);
        PrinterConfig printer = new PrinterConfig("P1", "Printer 1", "10.0.0.1", 9100, List.of(), List.of(), "Dock", true);
        AdvancedPrintWorkflowService.PrintTask task = new AdvancedPrintWorkflowService.PrintTask(
                AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL,
                "label.zpl",
                "^XA^XZ",
                LabelSelectionRef.forShipment(1, "SHIP1", "LPN-1").toString()
        );

        AdvancedPrintWorkflowService.JobCheckpoint checkpoint = gateway.createCheckpoint(
                "job-1",
                AdvancedPrintWorkflowService.InputMode.SHIPMENT,
                "SHIP1",
                tempDir.resolve("out"),
                true,
                printer,
                List.of(task)
        );
        gateway.executeTasks(checkpoint, printer, 0);

        AdvancedPrintWorkflowService.JobCheckpoint persisted = checkpointSupport.readCheckpoint("job-1");
        assertTrue(persisted.completed);
        assertEquals(1, persisted.nextTaskIndex);
        assertEquals("^XA^XZ", Files.readString(tempDir.resolve("out").resolve("label.zpl")));
    }
}
