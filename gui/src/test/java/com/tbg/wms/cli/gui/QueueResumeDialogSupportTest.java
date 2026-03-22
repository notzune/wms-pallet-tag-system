package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QueueResumeDialogSupportTest {

    @Test
    void buildResumeOptionsShouldFormatModeSourceAndProgress() {
        List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = List.of(
                resumeCandidate("checkpoint-1", AdvancedPrintWorkflowService.InputMode.CARRIER_MOVE,
                        "243177", "out/cmid", 3, 12)
        );

        assertEquals(List.of("CARRIER_MOVE 243177 | progress 3/12"),
                List.of(QueueResumeDialogSupport.buildResumeOptions(candidates)));
    }

    @Test
    void findSelectedOptionIndexShouldReturnMatchingIndex() {
        String[] options = {"SHIPMENT 8001 | progress 1/4", "CARRIER_MOVE 243177 | progress 3/12"};

        assertEquals(1, QueueResumeDialogSupport.findSelectedOptionIndex(options, options[1]));
        assertEquals(-1, QueueResumeDialogSupport.findSelectedOptionIndex(options, "missing"));
    }

    @Test
    void buildAutoResumePromptShouldRemainStable() {
        AdvancedPrintWorkflowService.ResumeCandidate candidate =
                resumeCandidate("checkpoint-2", AdvancedPrintWorkflowService.InputMode.SHIPMENT,
                        "8000574009", "out/shipment", 5, 18);

        assertEquals(
                "Found incomplete job (SHIPMENT 8000574009, 5/18).\nResume now?",
                QueueResumeDialogSupport.buildAutoResumePrompt(candidate)
        );
    }

    private static AdvancedPrintWorkflowService.ResumeCandidate resumeCandidate(
            String checkpointId,
            AdvancedPrintWorkflowService.InputMode mode,
            String sourceId,
            String outputDirectory,
            int nextTaskIndex,
            int totalTasks
    ) {
        try {
            Constructor<AdvancedPrintWorkflowService.ResumeCandidate> constructor =
                    AdvancedPrintWorkflowService.ResumeCandidate.class.getDeclaredConstructor(
                            String.class,
                            AdvancedPrintWorkflowService.InputMode.class,
                            String.class,
                            String.class,
                            int.class,
                            int.class,
                            LocalDateTime.class,
                            String.class
                    );
            constructor.setAccessible(true);
            return constructor.newInstance(
                    checkpointId,
                    mode,
                    sourceId,
                    outputDirectory,
                    nextTaskIndex,
                    totalTasks,
                    LocalDateTime.now(),
                    null
            );
        } catch (Exception ex) {
            throw new AssertionError("Could not construct ResumeCandidate", ex);
        }
    }
}
