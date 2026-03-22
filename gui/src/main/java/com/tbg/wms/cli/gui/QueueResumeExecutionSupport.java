/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import java.util.List;
import java.util.Objects;

/**
 * Pure queue/resume messaging and decision helpers for GUI recovery flows.
 */
final class QueueResumeExecutionSupport {

    String noIncompleteJobsMessage() {
        return "No incomplete jobs found.";
    }

    String[] buildResumeOptions(List<AdvancedPrintWorkflowService.ResumeCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates cannot be null");
        String[] options = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            AdvancedPrintWorkflowService.ResumeCandidate candidate = candidates.get(i);
            options[i] = candidate.mode() + " " + candidate.sourceId() + " | progress " +
                    candidate.nextTaskIndex() + "/" + candidate.totalTasks();
        }
        return options;
    }

    int findSelectedOptionIndex(String[] options, String selected) {
        if (selected == null) {
            return -1;
        }
        for (int i = 0; i < options.length; i++) {
            if (Objects.equals(options[i], selected)) {
                return i;
            }
        }
        return -1;
    }

    String buildAutoResumePrompt(AdvancedPrintWorkflowService.ResumeCandidate latest) {
        Objects.requireNonNull(latest, "latest cannot be null");
        return "Found incomplete job (" + latest.mode() + " " + latest.sourceId() + ", " +
                latest.nextTaskIndex() + "/" + latest.totalTasks() + ").\nResume now?";
    }

    String buildResumeCompletionMessage(AdvancedPrintWorkflowService.PrintResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return "Resume complete.\nLabels: " + result.getLabelsPrinted() +
                "\nInfo Tags: " + result.getInfoTagsPrinted() +
                "\nOutput: " + result.getOutputDirectory();
    }

    String buildAutoResumeStatus(AdvancedPrintWorkflowService.PrintResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        return "Resumed job. Printed " + result.getLabelsPrinted() + " labels and " +
                result.getInfoTagsPrinted() + " info tags.";
    }

    String buildResumeFailureMessage(Throwable throwable) {
        Objects.requireNonNull(throwable, "throwable cannot be null");
        return "Resume failed: " + GuiExceptionMessageSupport.rootMessage(throwable);
    }

    boolean shouldResumeNow(int choice) {
        return choice == javax.swing.JOptionPane.YES_OPTION;
    }
}
