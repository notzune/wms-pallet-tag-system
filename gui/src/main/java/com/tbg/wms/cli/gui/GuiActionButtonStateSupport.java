package com.tbg.wms.cli.gui;

import javax.swing.AbstractButton;

final class GuiActionButtonStateSupport {

    void applyBusy(AbstractButton preview, AbstractButton clear, AbstractButton showLabels, AbstractButton print) {
        preview.setEnabled(false);
        clear.setEnabled(false);
        showLabels.setEnabled(false);
        print.setEnabled(false);
    }

    void restorePrimaryActions(AbstractButton preview, AbstractButton clear, AbstractButton showLabels, AbstractButton print) {
        preview.setEnabled(true);
        clear.setEnabled(true);
        showLabels.setEnabled(false);
        print.setEnabled(false);
    }

    void restorePrintActions(AbstractButton preview, AbstractButton clear, AbstractButton showLabels, AbstractButton print) {
        preview.setEnabled(true);
        clear.setEnabled(true);
        showLabels.setEnabled(false);
        print.setEnabled(true);
    }
}
