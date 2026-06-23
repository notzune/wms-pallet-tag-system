package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FramePrinterSelectionSupportTest {

    private final FramePrinterSelectionSupport support = new FramePrinterSelectionSupport();
    private final GuiPrinterSelectionSupport printerSelectionSupport = new GuiPrinterSelectionSupport();

    @Test
    void comboItems_shouldReturnOrderedModelEntries() {
        JComboBox<LabelWorkflowService.PrinterOption> comboBox = new JComboBox<>();
        comboBox.addItem(new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1"));
        comboBox.addItem(new LabelWorkflowService.PrinterOption("P2", "Printer 2", "10.0.0.2"));

        List<LabelWorkflowService.PrinterOption> items = support.comboItems(comboBox.getModel());

        assertEquals(List.of("P1", "P2"), items.stream().map(LabelWorkflowService.PrinterOption::getId).toList());
    }

    @Test
    void restoreSelection_shouldApplyResolvedPrinterIndex() {
        JComboBox<LabelWorkflowService.PrinterOption> comboBox = new JComboBox<>();
        comboBox.addItem(new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1"));
        comboBox.addItem(new LabelWorkflowService.PrinterOption("P2", "Printer 2", "10.0.0.2"));

        support.restoreSelection(
                comboBox,
                new LabelWorkflowService.PrinterOption("P2", "Printer 2", "10.0.0.2"),
                printerSelectionSupport
        );

        assertEquals("P2", ((LabelWorkflowService.PrinterOption) comboBox.getSelectedItem()).getId());
    }

    @Test
    void planLoadedPrinters_shouldResolveSelectionAndStatus() {
        DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
        model.addElement(new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1"));
        model.addElement(new LabelWorkflowService.PrinterOption(
                "P2",
                "Printer 2",
                "10.0.0.2",
                Collections.emptyList(),
                false,
                true
        ));

        FramePrinterSelectionSupport.LoadedPrinterPlan plan =
                support.planLoadedPrinters(model, 2, printerSelectionSupport);

        assertEquals(1, plan.selectionIndex());
        assertEquals("Printers loaded.", plan.statusMessage());
    }

    @Test
    void resolveSelectionAction_shouldRestoreLastValidSelectionForSeparator() {
        LabelWorkflowService.PrinterOption previous =
                new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1");

        FramePrinterSelectionSupport.SelectionAction action = support.resolveSelectionAction(
                GuiPrinterTargetSupport.buildPrinterSectionSeparator(),
                previous
        );

        assertEquals(FramePrinterSelectionSupport.SelectionAction.RESTORE_LAST_VALID, action);
    }

    @Test
    void resolveSelectionAction_shouldRestoreDefaultForSeparatorWithoutLastValidSelection() {
        FramePrinterSelectionSupport.SelectionAction action = support.resolveSelectionAction(
                GuiPrinterTargetSupport.buildPrinterSectionSeparator(),
                null
        );

        assertEquals(FramePrinterSelectionSupport.SelectionAction.RESTORE_DEFAULT, action);
    }

    @Test
    void resolveSelectionAction_shouldAcceptSelectablePrinter() {
        FramePrinterSelectionSupport.SelectionAction action = support.resolveSelectionAction(
                new LabelWorkflowService.PrinterOption("P1", "Printer 1", "10.0.0.1"),
                null
        );

        assertEquals(FramePrinterSelectionSupport.SelectionAction.ACCEPT_SELECTED, action);
    }
}
