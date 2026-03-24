/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.cli.gui.LabelWorkflowService;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailDialogSupportTest {
    private final RailDialogSupport support = new RailDialogSupport();

    @Test
    void buildPrinterModel_shouldIncludeSystemDefaultAndPrintToFile() {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = support.buildPrinterModel(
                List.of(new LabelWorkflowService.PrinterOption("RAIL1", "Rail 1", "10.0.0.1", List.of("ZPL"))),
                Path.of("out", "rail-gui")
        );

        assertEquals(3, model.getSize());
        assertEquals("SYSTEM_DEFAULT", model.getElementAt(0).getId());
        assertEquals("RAIL1", model.getElementAt(1).getId());
        assertEquals("FILE", model.getElementAt(2).getId());
    }

    @Test
    void syncPrintTargetState_shouldDisablePrintNowForPrintToFile() {
        RailDialogSupport.PrintTargetState state = support.syncPrintTargetState(
                new LabelWorkflowService.PrinterOption("FILE", "Print to File", ""),
                true
        );

        assertFalse(state.printNowSelected());
        assertFalse(state.printNowEnabled());
        assertEquals("Save PDF", state.printButtonText());
    }

    @Test
    void buildGenerationMessage_shouldIncludePrinterWhenPrinted() throws Exception {
        RailWorkflowService.GenerationResult result = generationResult(
                Path.of("out", "rail-gui"),
                Path.of("out", "rail-gui", "rail-cards-TRAIN1.pdf"),
                true,
                "RAIL1"
        );

        String message = support.buildGenerationMessage(result);

        assertTrue(message.contains("rail-cards-TRAIN1.pdf"));
        assertTrue(message.contains("Print command sent to RAIL1."));
        assertEquals("PDF generated and print command sent.", support.buildReadyMessage(result));
    }

    private RailWorkflowService.GenerationResult generationResult(
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
