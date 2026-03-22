package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LabelGuiFramePreviewShellSupportTest {

    private final LabelGuiFramePreviewShellSupport support = new LabelGuiFramePreviewShellSupport();

    @Test
    void applyClearedState_shouldResetSurfaceAndSetCarrierMoveMessage() {
        JTextField shipmentField = new JTextField("OLD");
        JTextArea shipmentArea = new JTextArea("preview");
        JTextArea mathArea = new JTextArea("math");
        JPanel previewPanel = new JPanel();
        previewPanel.add(new JLabel("old"));
        JButton printButton = new JButton();
        printButton.setEnabled(true);
        StringBuilder ready = new StringBuilder();

        support.applyClearedState(
                shipmentField,
                previewPanel,
                shipmentArea,
                mathArea,
                printButton,
                true,
                ready::append
        );

        assertEquals("", shipmentField.getText());
        assertEquals("", shipmentArea.getText());
        assertEquals("", mathArea.getText());
        assertEquals(1, previewPanel.getComponentCount());
        assertEquals(shipmentArea, previewPanel.getComponent(0));
        assertFalse(printButton.isEnabled());
        assertEquals("Cleared. Enter the next Carrier Move ID.", ready.toString());
    }

    @Test
    void applyInputModeUi_shouldUpdateLabelAndDisablePrint() {
        JLabel inputLabel = new JLabel();
        JTextArea shipmentArea = new JTextArea("preview");
        JTextArea mathArea = new JTextArea("math");
        JPanel previewPanel = new JPanel();
        previewPanel.add(new JLabel("old"));
        JButton printButton = new JButton();
        printButton.setEnabled(true);

        support.applyInputModeUi(inputLabel, false, previewPanel, shipmentArea, mathArea, printButton);

        assertEquals("Shipment ID:", inputLabel.getText());
        assertEquals("", shipmentArea.getText());
        assertEquals("", mathArea.getText());
        assertEquals(1, previewPanel.getComponentCount());
        assertEquals(shipmentArea, previewPanel.getComponent(0));
        assertFalse(printButton.isEnabled());
    }
}
