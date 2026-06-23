package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiActionButtonStateSupportTest {

    private final GuiActionButtonStateSupport support = new GuiActionButtonStateSupport();

    @Test
    void applyBusy_shouldDisablePrimaryAndPrintActions() {
        JButton preview = new JButton();
        JButton clear = new JButton();
        JButton showLabels = new JButton();
        JButton print = new JButton();

        support.applyBusy(preview, clear, showLabels, print);

        assertFalse(preview.isEnabled());
        assertFalse(clear.isEnabled());
        assertFalse(showLabels.isEnabled());
        assertFalse(print.isEnabled());
    }

    @Test
    void restorePrimaryActions_shouldEnablePreviewAndClearOnly() {
        JButton preview = disabledButton();
        JButton clear = disabledButton();
        JButton showLabels = disabledButton();
        JButton print = disabledButton();

        support.restorePrimaryActions(preview, clear, showLabels, print);

        assertTrue(preview.isEnabled());
        assertTrue(clear.isEnabled());
        assertFalse(showLabels.isEnabled());
        assertFalse(print.isEnabled());
    }

    @Test
    void restorePrintActions_shouldEnablePreviewPrintAndClearOnly() {
        JButton preview = disabledButton();
        JButton clear = disabledButton();
        JButton showLabels = disabledButton();
        JButton print = disabledButton();

        support.restorePrintActions(preview, clear, showLabels, print);

        assertTrue(preview.isEnabled());
        assertTrue(clear.isEnabled());
        assertFalse(showLabels.isEnabled());
        assertTrue(print.isEnabled());
    }

    private static JButton disabledButton() {
        JButton button = new JButton();
        button.setEnabled(false);
        return button;
    }
}
