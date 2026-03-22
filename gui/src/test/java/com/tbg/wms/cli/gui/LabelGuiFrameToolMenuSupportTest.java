package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelGuiFrameToolMenuSupportTest {

    private final LabelGuiFrameToolMenuSupport support = new LabelGuiFrameToolMenuSupport();

    @Test
    void buildToolsMenu_shouldExposeExpectedMenuLabelsInOrder() {
        JPopupMenu menu = support.buildToolsMenu(new NoOpActions());
        List<String> labels = new ArrayList<>();
        for (var component : menu.getComponents()) {
            if (component instanceof JMenuItem item) {
                labels.add(item.getText());
            }
        }

        assertEquals(List.of(
                "Rail Labels...",
                "Queue Print...",
                "Barcode Generator...",
                "ZPL Preview...",
                "Resume Incomplete Job...",
                "Settings..."
        ), labels);
    }

    @Test
    void buildToolBar_shouldConfigureButtonAndAddItToToolBar() {
        JButton button = new JButton("Tools");

        JComponent toolBar = support.buildToolBar(button, new NoOpActions());

        assertTrue(toolBar instanceof JToolBar);
        assertEquals(button, ((JToolBar) toolBar).getComponent(0));
        assertEquals(javax.swing.SwingConstants.LEFT, button.getHorizontalTextPosition());
    }

    private static final class NoOpActions implements LabelGuiFrameToolMenuSupport.MenuActions {
        @Override
        public void openRailLabelsDialog() {
        }

        @Override
        public void openQueueDialog() {
        }

        @Override
        public void openBarcodeDialog() {
        }

        @Override
        public void openZplPreviewDialog() {
        }

        @Override
        public void openResumeDialog() {
        }

        @Override
        public void openSettingsDialog() {
        }
    }
}
