package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

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
}
