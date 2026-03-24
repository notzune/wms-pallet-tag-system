package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BarcodeDialogFormSupportTest {

    private final BarcodeDialogFormSupport support = new BarcodeDialogFormSupport();

    @Test
    void syncOutputState_shouldToggleFieldAndLabelTogether() {
        JTextField outputDir = new JTextField();
        JLabel label = new JLabel();

        support.syncOutputState(outputDir, label, false);
        assertFalse(outputDir.isEnabled());
        assertFalse(outputDir.isEditable());
        assertFalse(label.isEnabled());

        support.syncOutputState(outputDir, label, true);
        assertTrue(outputDir.isEnabled());
        assertTrue(outputDir.isEditable());
        assertTrue(label.isEnabled());
    }

    @Test
    void buildRequest_shouldReflectFormSelections() {
        JComboBox<Symbology> typeCombo = new JComboBox<>(Symbology.values());
        typeCombo.setSelectedItem(Symbology.GS1_128);
        JComboBox<Orientation> orientationCombo = new JComboBox<>(Orientation.values());
        orientationCombo.setSelectedItem(Orientation.LANDSCAPE);
        JSpinner labelWidth = new JSpinner(new SpinnerNumberModel(812, 1, 10000, 1));
        JSpinner labelHeight = new JSpinner(new SpinnerNumberModel(1218, 1, 20000, 1));
        JSpinner originX = new JSpinner(new SpinnerNumberModel(50, 0, 10000, 1));
        JSpinner originY = new JSpinner(new SpinnerNumberModel(70, 0, 10000, 1));
        JSpinner moduleWidth = new JSpinner(new SpinnerNumberModel(4, 1, 20, 1));
        JSpinner moduleRatio = new JSpinner(new SpinnerNumberModel(3, 1, 10, 1));
        JSpinner barcodeHeight = new JSpinner(new SpinnerNumberModel(200, 1, 2000, 1));
        JCheckBox humanReadable = new JCheckBox();
        humanReadable.setSelected(false);
        JSpinner copies = new JSpinner(new SpinnerNumberModel(3, 1, 100, 1));

        var request = support.buildRequest(
                "DATA",
                typeCombo,
                orientationCombo,
                labelWidth,
                labelHeight,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                humanReadable,
                copies
        );

        assertEquals("DATA", request.getData());
        assertEquals(Symbology.GS1_128, request.getSymbology());
        assertEquals(Orientation.LANDSCAPE, request.getOrientation());
        assertEquals(50, request.getOriginX());
        assertEquals(70, request.getOriginY());
        assertEquals(3, request.getCopies());
        assertFalse(request.isHumanReadable());
    }
}
