package com.tbg.wms.cli.gui;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.util.Objects;

/**
 * Builds the tools toolbar and popup menu for the main GUI frame.
 */
final class LabelGuiFrameToolMenuSupport {

    JComponent buildToolBar(JButton toolsButton, MenuActions actions) {
        Objects.requireNonNull(toolsButton, "toolsButton cannot be null");
        Objects.requireNonNull(actions, "actions cannot be null");
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        toolsButton.setFocusable(false);
        toolsButton.setHorizontalTextPosition(SwingConstants.LEFT);
        toolBar.add(toolsButton);

        JPopupMenu toolsMenu = buildToolsMenu(actions);
        toolsButton.addActionListener(e -> toolsMenu.show(toolsButton, 0, toolsButton.getHeight()));
        return toolBar;
    }

    JPopupMenu buildToolsMenu(MenuActions actions) {
        Objects.requireNonNull(actions, "actions cannot be null");
        JPopupMenu toolsMenu = new JPopupMenu();
        addMenuItem(toolsMenu, "Rail Labels...", actions::openRailLabelsDialog);
        addMenuItem(toolsMenu, "Queue Print...", actions::openQueueDialog);
        addMenuItem(toolsMenu, "Barcode Generator...", actions::openBarcodeDialog);
        addMenuItem(toolsMenu, "ZPL Preview...", actions::openZplPreviewDialog);
        toolsMenu.addSeparator();
        addMenuItem(toolsMenu, "Resume Incomplete Job...", actions::openResumeDialog);
        addMenuItem(toolsMenu, "Settings...", actions::openSettingsDialog);
        return toolsMenu;
    }

    private void addMenuItem(JPopupMenu menu, String label, Runnable action) {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    interface MenuActions {
        void openRailLabelsDialog();

        void openQueueDialog();

        void openBarcodeDialog();

        void openZplPreviewDialog();

        void openResumeDialog();

        void openSettingsDialog();
    }
}
