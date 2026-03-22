package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueResumeExecutionSupportTest {

    private final QueueResumeExecutionSupport support = new QueueResumeExecutionSupport();

    @Test
    void buildResumeOptions_shouldFormatModeSourceAndProgress() {
        List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = List.of(
                resumeCandidate("checkpoint-1", AdvancedPrintWorkflowService.InputMode.CARRIER_MOVE,
                        "243177", "out/cmid", 3, 12)
        );

        assertEquals(List.of("CARRIER_MOVE 243177 | progress 3/12"),
                List.of(support.buildResumeOptions(candidates)));
    }

    @Test
    void selectionAndPromptHelpers_shouldRemainStable() {
        String[] options = {"SHIPMENT 8001 | progress 1/4", "CARRIER_MOVE 243177 | progress 3/12"};
        AdvancedPrintWorkflowService.ResumeCandidate candidate =
                resumeCandidate("checkpoint-2", AdvancedPrintWorkflowService.InputMode.SHIPMENT,
                        "8000574009", "out/shipment", 5, 18);

        assertEquals(1, support.findSelectedOptionIndex(options, options[1]));
        assertEquals(-1, support.findSelectedOptionIndex(options, "missing"));
        assertEquals(
                "Found incomplete job (SHIPMENT 8000574009, 5/18).\nResume now?",
                support.buildAutoResumePrompt(candidate)
        );
        assertTrue(support.shouldResumeNow(javax.swing.JOptionPane.YES_OPTION));
        assertFalse(support.shouldResumeNow(javax.swing.JOptionPane.NO_OPTION));
    }

    @Test
    void completionAndFailureMessages_shouldBeConsistent() {
        AdvancedPrintWorkflowService.PrintResult result = printResult(4, 2, Path.of("out"));

        assertEquals("No incomplete jobs found.", support.noIncompleteJobsMessage());
        assertEquals(
                "Resume complete.\nLabels: 4\nInfo Tags: 2\nOutput: out",
                support.buildResumeCompletionMessage(result)
        );
        assertEquals(
                "Resumed job. Printed 4 labels and 2 info tags.",
                support.buildAutoResumeStatus(result)
        );
        assertEquals(
                "Resume failed: root",
                support.buildResumeFailureMessage(new IllegalStateException("top", new IllegalArgumentException("root")))
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

    private static AdvancedPrintWorkflowService.PrintResult printResult(
            int labels,
            int infoTags,
            Path outputDir
    ) {
        try {
            var ctor = AdvancedPrintWorkflowService.PrintResult.class.getDeclaredConstructor(
                    int.class, int.class, Path.class, String.class, String.class, boolean.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(labels, infoTags, outputDir, "FILE", "FILE", true);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to construct PrintResult fixture.", ex);
        }
    }
}
