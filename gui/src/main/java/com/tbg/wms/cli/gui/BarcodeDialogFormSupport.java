/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;

import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import java.util.Objects;

/**
 * Form-state helpers for the barcode dialog.
 *
 * <p>This helper keeps widget-to-request mapping separate from dialog event wiring so barcode
 * field changes remain easier to test and safer to evolve.</p>
 */
final class BarcodeDialogFormSupport {

    void syncOutputState(JTextField outputDir, JComponent outputDirLabel, boolean printToFile) {
        Objects.requireNonNull(outputDir, "outputDir cannot be null");
        outputDir.setEnabled(printToFile);
        outputDir.setEditable(printToFile);
        if (outputDirLabel != null) {
            outputDirLabel.setEnabled(printToFile);
        }
    }

    BarcodeRequest buildRequest(String data,
                                JComboBox<Symbology> typeCombo,
                                JComboBox<Orientation> orientationCombo,
                                JSpinner labelWidth,
                                JSpinner labelHeight,
                                JSpinner originX,
                                JSpinner originY,
                                JSpinner moduleWidth,
                                JSpinner moduleRatio,
                                JSpinner barcodeHeight,
                                JCheckBox humanReadable,
                                JSpinner copies) {
        return new BarcodeRequest(
                data,
                (Symbology) typeCombo.getSelectedItem(),
                (Orientation) orientationCombo.getSelectedItem(),
                (int) labelWidth.getValue(),
                (int) labelHeight.getValue(),
                (int) originX.getValue(),
                (int) originY.getValue(),
                (int) moduleWidth.getValue(),
                (int) moduleRatio.getValue(),
                (int) barcodeHeight.getValue(),
                humanReadable.isSelected(),
                (int) copies.getValue()
        );
    }
}
