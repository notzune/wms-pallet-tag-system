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
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailDialogActionSupportTest {

    private final RailDialogActionSupport support =
            new RailDialogActionSupport(new RailDialogSupport(), new RailDialogExecutionSupport());

    @Test
    void buildPrinterLoadOutcome_shouldReturnModelAndReadyMessage() {
        RailDialogActionSupport.PrinterLoadOutcome outcome = support.buildPrinterLoadOutcome(
                List.of(new LabelWorkflowService.PrinterOption("RAIL1", "Rail 1", "10.0.0.1", List.of("RAIL"))),
                Path.of("out", "rail-gui")
        );

        assertEquals(3, outcome.model().getSize());
        assertTrue(outcome.shouldSelectFirst());
        assertEquals("Ready.", outcome.readyMessage());
    }

    @Test
    void buildPreviewOutcome_shouldExposeCardsAndDiagnostics() throws Exception {
        RailWorkflowService.PreparedRailJob job = preparedJob(List.of(card("1", "CAR-1")));

        RailDialogActionSupport.PreviewOutcome outcome = support.buildPreviewOutcome(job, "Rail Diagnostics");

        assertEquals(1, outcome.cards().size());
        assertEquals("Rail Diagnostics", outcome.diagnosticsText());
        assertTrue(outcome.shouldSelectFirstRow());
        assertEquals("Preview ready.", outcome.readyMessage());
    }

    @Test
    void buildPreviewOutcome_shouldAvoidSelectingWhenNoCardsPresent() throws Exception {
        RailDialogActionSupport.PreviewOutcome outcome = support.buildPreviewOutcome(preparedJob(List.of()), "none");

        assertFalse(outcome.shouldSelectFirstRow());
    }

    @Test
    void buildGenerationOutcome_shouldAppendMessageAndReadyState() throws Exception {
        RailDialogActionSupport.GenerationOutcome outcome = support.buildGenerationOutcome(
                generationResult(Path.of("out"), Path.of("out", "rail.pdf"), true, "RAIL1")
        );

        assertTrue(outcome.diagnosticsAppend().contains("Generation Result"));
        assertTrue(outcome.diagnosticsAppend().contains("rail.pdf"));
        assertEquals("PDF generated and print command sent.", outcome.readyMessage());
    }

    private static RailWorkflowService.PreparedRailJob preparedJob(List<RailCarCard> cards) throws Exception {
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
                        "TRAIN1", List.of(), List.of(), cards, Map.of(), Set.of(), Set.of()
                )
        );
    }

    private static RailCarCard card(String sequence, String vehicleId) {
        return new RailCarCard("TRAIN1", sequence, vehicleId, "", List.of(), 1, 2, 3, List.of(), List.of());
    }

    private static RailWorkflowService.GenerationResult generationResult(
            Path outputDirectory,
            Path pdfPath,
            boolean printed,
            String printerId
    ) throws Exception {
        Constructor<RailWorkflowService.GenerationResult> constructor =
                RailWorkflowService.GenerationResult.class.getDeclaredConstructor(Path.class, Path.class, boolean.class, String.class);
        constructor.setAccessible(true);
        return constructor.newInstance(outputDirectory, pdfPath, printed, printerId);
    }
}
