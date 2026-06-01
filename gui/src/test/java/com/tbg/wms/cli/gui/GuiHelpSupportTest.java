package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiHelpSupportTest {

    @Test
    void formatHelpTextIncludesSectionsAndShortcuts() {
        String helpText = GuiHelpSupport.formatHelpText(List.of(
                new GuiHelpSupport.HelpSection("Shortcuts", List.of("Ctrl+F loads the preview.", "Space toggles selected rows.")),
                new GuiHelpSupport.HelpSection("Workflow", List.of("Select the rows that should print."))
        ));

        assertTrue(helpText.contains("Shortcuts"));
        assertTrue(helpText.contains("- Ctrl+F loads the preview."));
        assertTrue(helpText.contains("- Space toggles selected rows."));
        assertTrue(helpText.contains("Workflow"));
    }

    @Test
    void createHelpButtonUsesConsistentLabelTooltipAndAction() {
        JButton button = GuiHelpSupport.createHelpButton(null, "Rail Labels", List.of(
                new GuiHelpSupport.HelpSection("Rows", List.of("Ctrl-click selects additional rows."))
        ));

        assertEquals("Help", button.getText());
        assertEquals("Show help for Rail Labels", button.getToolTipText());
        assertFalse(button.isFocusable());
        ActionListener[] listeners = button.getActionListeners();
        assertEquals(1, listeners.length);
    }

    @Test
    void mainWindowHelpPrioritizesOperatorWorkflowAndShortcuts() {
        String helpText = GuiHelpSupport.formatHelpText(GuiHelpTopics.mainWindow());

        assertTrue(helpText.contains("Ctrl+F"));
        assertTrue(helpText.contains("Ctrl+C"));
        assertTrue(helpText.contains("Ctrl+V"));
        assertTrue(helpText.contains("Ctrl+A"));
        assertTrue(helpText.contains("selected labels"));
        assertFalse(helpText.contains("left-click and drag"));
        assertFalse(helpText.toLowerCase().contains("right-click"));
        assertFalse(helpText.toLowerCase().contains("terminal-like"));
    }

    @Test
    void railHelpHighlightsSelectionShortcutsAndCombinedPdfBehavior() {
        String helpText = GuiHelpSupport.formatHelpText(GuiHelpTopics.railLabels());

        assertTrue(helpText.contains("Ctrl+F"));
        assertTrue(helpText.contains("Space"));
        assertTrue(helpText.contains("Ctrl-click"));
        assertTrue(helpText.contains("Shift-click"));
        assertTrue(helpText.contains("JC05262026"));
        assertTrue(helpText.contains("combined PDF"));
        assertFalse(helpText.toLowerCase().contains("type one full train code"));
        assertNotNull(helpText);
    }

    @Test
    void allHelpTopicsUseConciseOperatorTone() {
        List<List<GuiHelpSupport.HelpSection>> topics = List.of(
                GuiHelpTopics.mainWindow(),
                GuiHelpTopics.railLabels(),
                GuiHelpTopics.barcodeGenerator(),
                GuiHelpTopics.barcodeAdvancedSettings(),
                GuiHelpTopics.zplPreview(),
                GuiHelpTopics.queuePrint(),
                GuiHelpTopics.settings(),
                GuiHelpTopics.advancedSettings(),
                GuiHelpTopics.updates(),
                GuiHelpTopics.analyzers()
        );

        for (List<GuiHelpSupport.HelpSection> topic : topics) {
            String helpText = GuiHelpSupport.formatHelpText(topic).toLowerCase();
            assertFalse(helpText.contains("type or paste"));
            assertFalse(helpText.contains("click load"));
            assertFalse(helpText.contains("click generate"));
            assertFalse(helpText.contains("left-click"));
            assertFalse(helpText.contains("right-click"));
            assertFalse(helpText.contains("when those options are available"));
        }
    }
}
