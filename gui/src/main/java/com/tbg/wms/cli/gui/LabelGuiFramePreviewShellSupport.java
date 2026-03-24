package com.tbg.wms.cli.gui;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.util.Objects;

/**
 * Shared UI-shell reset helpers for the main label GUI frame.
 */
final class LabelGuiFramePreviewShellSupport {

    void resetPreviewSurface(JPanel shipmentPreviewPanel, JTextArea shipmentArea, JTextArea mathArea) {
        Objects.requireNonNull(shipmentPreviewPanel, "shipmentPreviewPanel cannot be null");
        Objects.requireNonNull(shipmentArea, "shipmentArea cannot be null");
        Objects.requireNonNull(mathArea, "mathArea cannot be null");
        shipmentArea.setText("");
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
        mathArea.setText("");
    }

    void applyClearedState(JTextField shipmentField,
                           JPanel shipmentPreviewPanel,
                           JTextArea shipmentArea,
                           JTextArea mathArea,
                           JButton printButton,
                           boolean carrierMoveMode,
                           StatusSink statusSink) {
        Objects.requireNonNull(shipmentField, "shipmentField cannot be null");
        Objects.requireNonNull(printButton, "printButton cannot be null");
        Objects.requireNonNull(statusSink, "statusSink cannot be null");
        shipmentField.setText("");
        resetPreviewSurface(shipmentPreviewPanel, shipmentArea, mathArea);
        printButton.setEnabled(false);
        shipmentField.requestFocusInWindow();
        statusSink.setReady("Cleared. Enter the next " + (carrierMoveMode ? "Carrier Move ID." : "Shipment ID."));
    }

    void applyInputModeUi(JLabel inputLabel,
                          boolean carrierMoveMode,
                          JPanel shipmentPreviewPanel,
                          JTextArea shipmentArea,
                          JTextArea mathArea,
                          JButton printButton) {
        Objects.requireNonNull(inputLabel, "inputLabel cannot be null");
        Objects.requireNonNull(printButton, "printButton cannot be null");
        inputLabel.setText(carrierMoveMode ? "Carrier Move ID:" : "Shipment ID:");
        resetPreviewSurface(shipmentPreviewPanel, shipmentArea, mathArea);
        printButton.setEnabled(false);
    }

    interface StatusSink {
        void setReady(String message);
    }
}
