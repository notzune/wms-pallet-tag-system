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
    void mainWindowHelpUsesPlainLanguageExamplesAndMouseCopyPasteWording() {
        String helpText = GuiHelpSupport.formatHelpText(GuiHelpTopics.mainWindow());

        assertTrue(helpText.contains("For example:"));
        assertTrue(helpText.contains("left-click and drag"));
        assertTrue(helpText.contains("right-click"));
        assertFalse(helpText.toLowerCase().contains("terminal-like"));
    }

    @Test
    void railHelpExplainsMultipleTrainInputWithConcreteExamples() {
        String helpText = GuiHelpSupport.formatHelpText(GuiHelpTopics.railLabels());

        assertTrue(helpText.contains("302, 303, 304"));
        assertTrue(helpText.contains("302/303"));
        assertTrue(helpText.contains("302:303;304"));
        assertTrue(helpText.contains("same PDF"));
        assertTrue(helpText.contains("uncheck"));
        assertNotNull(helpText);
    }
}
