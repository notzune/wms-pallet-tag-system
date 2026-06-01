package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.LabelWorkflowService;
import com.tbg.wms.core.rail.RailCarCard;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailDialogExecutionSupportTest {

    private final RailDialogExecutionSupport support = new RailDialogExecutionSupport();

    @Test
    void preparePreviewRequest_shouldRequireTrainId() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> support.preparePreviewRequest(" "));

        assertEquals("At least one train ID is required.", ex.getMessage());
        assertEquals(List.of("JC03182026"), support.preparePreviewRequest(" JC03182026 ").trainIds());
        assertEquals(
                List.of("JC03182026", "JC04152026"),
                support.preparePreviewRequest("JC03182026, JC04152026").trainIds()
        );
    }

    @Test
    void preparePreviewRequest_shouldIncludeValidatedLabelDate() {
        RailDialogExecutionSupport.PreviewRequest request =
                support.preparePreviewRequest("JC03182026", "6-1-26");

        assertEquals(List.of("JC03182026"), request.trainIds());
        assertEquals("06-01-26", request.labelDate());
    }

    @Test
    void prepareGenerationRequest_shouldRespectPrintTargetModes() throws Exception {
        RailWorkflowService.PreparedRailJob job = preparedJob();
        List<RailCarCard> selectedCards = List.of(card("1"));

        RailDialogExecutionSupport.GenerationRequest fileRequest = support.prepareGenerationRequest(
                job,
                selectedCards,
                "",
                new LabelWorkflowService.PrinterOption("FILE", "Print to File", ""),
                false,
                true
        );
        RailDialogExecutionSupport.GenerationRequest printRequest = support.prepareGenerationRequest(
                job,
                selectedCards,
                "out\\rail",
                new LabelWorkflowService.PrinterOption("RAIL1", "Rail 1", "10.0.0.1", List.of("RAIL")),
                false,
                true
        );

        assertNull(fileRequest.outputDirectory());
        assertEquals("FILE", fileRequest.printerId());
        assertFalse(fileRequest.shouldPrint());
        assertEquals(1, fileRequest.selectedCards().size());
        assertEquals(Path.of("out\\rail"), printRequest.outputDirectory());
        assertEquals("RAIL1", printRequest.printerId());
        assertTrue(printRequest.shouldPrint());
    }

    @Test
    void prepareGenerationRequest_shouldRejectEmptySelection() throws Exception {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> support.prepareGenerationRequest(
                        preparedJob(),
                        List.of(),
                        "",
                        new LabelWorkflowService.PrinterOption("FILE", "Print to File", ""),
                        false,
                        false
                )
        );

        assertEquals("Select at least one rail row to generate.", ex.getMessage());
    }

    @Test
    void messaging_shouldRemainStable() {
        assertEquals("Preview ready.", support.previewReadyMessage());
        assertEquals("Preview failed.", support.previewFailedMessage());
        assertEquals("Generating PDF...", support.generationBusyMessage(false));
        assertEquals("Generating PDF and printing...", support.generationBusyMessage(true));
        assertEquals("Generation failed.", support.generationFailedMessage());
        assertEquals("root", support.rootMessage(new IllegalStateException("top", new IllegalArgumentException("root"))));
    }

    private static RailWorkflowService.PreparedRailJob preparedJob() throws Exception {
        Constructor<com.tbg.wms.core.rail.RailWorkflowService.RailWorkflowResult> resultConstructor =
                com.tbg.wms.core.rail.RailWorkflowService.RailWorkflowResult.class.getDeclaredConstructor(
                        String.class,
                        List.class,
                        List.class,
                        List.class,
                        Map.class,
                        Set.class,
                        Set.class
                );
        resultConstructor.setAccessible(true);
        Constructor<RailWorkflowService.PreparedRailJob> constructor =
                RailWorkflowService.PreparedRailJob.class.getDeclaredConstructor(
                        com.tbg.wms.core.rail.RailWorkflowService.RailWorkflowResult.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(
                resultConstructor.newInstance(
                        "JC03182026", List.of(), List.of(), List.of(), Map.of(), Set.of(), Set.of()
                )
        );
    }

    private static RailCarCard card(String sequence) {
        return new RailCarCard("TRAIN1", sequence, "CAR" + sequence, "", List.of(), 1, 0, 0, List.of(), List.of());
    }
}
