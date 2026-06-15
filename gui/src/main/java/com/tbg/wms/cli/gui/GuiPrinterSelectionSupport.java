/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import java.util.List;
import java.util.Objects;

/**
 * Pure helper for printer combo selection and load-status messaging.
 */
final class GuiPrinterSelectionSupport {
    int resolveSelectionIndex(
            LabelWorkflowService.PrinterOption previousSelection,
            List<LabelWorkflowService.PrinterOption> candidates
    ) {
        if (candidates == null || candidates.isEmpty()) {
            return -1;
        }
        if (previousSelection == null) {
            int defaultIndex = resolveDefaultSelectionIndex(candidates);
            return defaultIndex >= 0 ? defaultIndex : firstSelectableIndex(candidates);
        }
        for (int i = 0; i < candidates.size(); i++) {
            LabelWorkflowService.PrinterOption candidate = candidates.get(i);
            if (candidate != null && !candidate.isSeparator() && Objects.equals(candidate.getId(), previousSelection.getId())) {
                return i;
            }
        }
        int defaultIndex = resolveDefaultSelectionIndex(candidates);
        return defaultIndex >= 0 ? defaultIndex : firstSelectableIndex(candidates);
    }

    String printerLoadStatusMessage(int printerCount, int modelSize) {
        if (modelSize <= 0) {
            return "No enabled printers found in routing config.";
        }
        if (printerCount == 0) {
            return "No enabled printers found. Print to file available.";
        }
        return "Printers loaded.";
    }

    private int resolveDefaultSelectionIndex(List<LabelWorkflowService.PrinterOption> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            LabelWorkflowService.PrinterOption candidate = candidates.get(i);
            if (candidate != null && !candidate.isSeparator() && candidate.isDefaultPrinter()) {
                return i;
            }
        }
        return -1;
    }

    private int firstSelectableIndex(List<LabelWorkflowService.PrinterOption> candidates) {
        for (int i = 0; i < candidates.size(); i++) {
            LabelWorkflowService.PrinterOption candidate = candidates.get(i);
            if (candidate != null && !candidate.isSeparator()) {
                return i;
            }
        }
        return -1;
    }
}
