/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Small frame-shell helpers for printer combo models and selection restoration.
 *
 * <p>This helper exists so combo-box selection preservation stays consistent when printer models
 * are rebuilt after preview mode or routing changes.</p>
 */
final class FramePrinterSelectionSupport {

    List<LabelWorkflowService.PrinterOption> comboItems(ComboBoxModel<LabelWorkflowService.PrinterOption> model) {
        Objects.requireNonNull(model, "model cannot be null");
        List<LabelWorkflowService.PrinterOption> items = new ArrayList<>(model.getSize());
        for (int i = 0; i < model.getSize(); i++) {
            items.add(model.getElementAt(i));
        }
        return items;
    }

    void restoreSelection(JComboBox<LabelWorkflowService.PrinterOption> comboBox,
                          LabelWorkflowService.PrinterOption previousSelection,
                          GuiPrinterSelectionSupport printerSelectionSupport) {
        Objects.requireNonNull(comboBox, "comboBox cannot be null");
        Objects.requireNonNull(printerSelectionSupport, "printerSelectionSupport cannot be null");
        int selectionIndex = printerSelectionSupport.resolveSelectionIndex(
                previousSelection,
                comboItems(comboBox.getModel())
        );
        if (selectionIndex >= 0) {
            comboBox.setSelectedIndex(selectionIndex);
        }
    }
}
