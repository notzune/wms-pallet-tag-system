package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreviewSelectionPanelSupportTest {

    private final PreviewSelectionPanelSupport support = new PreviewSelectionPanelSupport();

    @Test
    void buildPanel_shouldPopulateCheckboxesAndControls() {
        JPanel panel = new JPanel();
        JPanel content = new JPanel();
        JButton toggle = new JButton();
        JToggleButton collapse = new JToggleButton();
        JCheckBox infoTags = new JCheckBox();
        JLabel status = new JLabel();
        AtomicInteger actions = new AtomicInteger();
        List<PreviewSelectionSupport.LabelOption> options = List.of(
                new PreviewSelectionSupport.LabelOption("01. LPN-1", new Lpn("LPN-1", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of()), null),
                new PreviewSelectionSupport.LabelOption("02. LPN-2", new Lpn("LPN-2", "S1", null, 0, 0, 0.0, null, null, null, null, null, List.of()), null)
        );

        PreviewSelectionPanelSupport.PanelBuildResult result = support.buildPanel(
                panel, content, toggle, collapse, infoTags, status, options, actions::incrementAndGet
        );

        assertEquals(2, result.checkboxes().size());
        assertEquals(2, result.options().size());
        assertEquals(2, content.getComponentCount());
        assertEquals(2, panel.getComponentCount());
        result.checkboxes().get(0).doClick();
        assertEquals(1, actions.get());
    }

    @Test
    void resetPanel_shouldRestoreDefaultTextsAndFlags() {
        JPanel panel = new JPanel();
        panel.add(new JLabel("x"));
        JPanel content = new JPanel();
        content.add(new JLabel("y"));
        JLabel status = new JLabel("busy");
        JButton toggle = new JButton("Select All");
        JToggleButton collapse = new JToggleButton("expanded", true);
        JCheckBox infoTags = new JCheckBox("Info", false);

        support.resetPanel(panel, content, status, toggle, collapse, infoTags);

        assertEquals(0, panel.getComponentCount());
        assertEquals(0, content.getComponentCount());
        assertEquals(" ", status.getText());
        assertEquals("Deselect All", toggle.getText());
        assertEquals("Label Selection [collapsed]", collapse.getText());
        assertTrue(infoTags.isSelected());
    }
}
