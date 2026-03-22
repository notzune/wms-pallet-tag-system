/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Builds and resets the preview label-selection panel shell.
 */
final class PreviewSelectionPanelSupport {

    PanelBuildResult buildPanel(
            JPanel labelSelectionPanel,
            JPanel labelSelectionContentPanel,
            JButton labelSelectionToggleButton,
            JToggleButton labelSelectionCollapseButton,
            JCheckBox includeInfoTagsCheckBox,
            JLabel labelSelectionStatusLabel,
            List<PreviewSelectionSupport.LabelOption> options,
            Runnable selectionChangedAction
    ) {
        Objects.requireNonNull(labelSelectionPanel, "labelSelectionPanel cannot be null");
        Objects.requireNonNull(labelSelectionContentPanel, "labelSelectionContentPanel cannot be null");
        Objects.requireNonNull(labelSelectionToggleButton, "labelSelectionToggleButton cannot be null");
        Objects.requireNonNull(labelSelectionCollapseButton, "labelSelectionCollapseButton cannot be null");
        Objects.requireNonNull(includeInfoTagsCheckBox, "includeInfoTagsCheckBox cannot be null");
        Objects.requireNonNull(labelSelectionStatusLabel, "labelSelectionStatusLabel cannot be null");
        Objects.requireNonNull(options, "options cannot be null");
        Objects.requireNonNull(selectionChangedAction, "selectionChangedAction cannot be null");

        List<JCheckBox> checkboxes = new ArrayList<>(options.size());
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        controls.add(labelSelectionToggleButton);
        controls.add(includeInfoTagsCheckBox);
        controls.add(labelSelectionStatusLabel);

        JPanel checkboxGrid = new JPanel(new GridLayout(0, 3, 8, 4));
        for (PreviewSelectionSupport.LabelOption option : options) {
            JCheckBox checkbox = new JCheckBox(option.labelText(), true);
            checkbox.addActionListener(e -> selectionChangedAction.run());
            checkboxes.add(checkbox);
            checkboxGrid.add(checkbox);
        }

        labelSelectionPanel.removeAll();
        labelSelectionPanel.setLayout(new BorderLayout(0, 6));
        labelSelectionPanel.setBorder(BorderFactory.createEtchedBorder());
        labelSelectionCollapseButton.setFocusPainted(false);
        labelSelectionCollapseButton.setHorizontalAlignment(SwingConstants.LEFT);
        labelSelectionPanel.add(labelSelectionCollapseButton, BorderLayout.NORTH);
        labelSelectionContentPanel.removeAll();
        labelSelectionContentPanel.add(controls, BorderLayout.NORTH);
        labelSelectionContentPanel.add(checkboxGrid, BorderLayout.CENTER);
        labelSelectionPanel.add(labelSelectionContentPanel, BorderLayout.CENTER);
        labelSelectionCollapseButton.setSelected(false);

        return new PanelBuildResult(List.copyOf(options), List.copyOf(checkboxes));
    }

    void resetPanel(
            JPanel labelSelectionPanel,
            JPanel labelSelectionContentPanel,
            JLabel labelSelectionStatusLabel,
            JButton labelSelectionToggleButton,
            JToggleButton labelSelectionCollapseButton,
            JCheckBox includeInfoTagsCheckBox
    ) {
        Objects.requireNonNull(labelSelectionPanel, "labelSelectionPanel cannot be null");
        Objects.requireNonNull(labelSelectionContentPanel, "labelSelectionContentPanel cannot be null");
        Objects.requireNonNull(labelSelectionStatusLabel, "labelSelectionStatusLabel cannot be null");
        Objects.requireNonNull(labelSelectionToggleButton, "labelSelectionToggleButton cannot be null");
        Objects.requireNonNull(labelSelectionCollapseButton, "labelSelectionCollapseButton cannot be null");
        Objects.requireNonNull(includeInfoTagsCheckBox, "includeInfoTagsCheckBox cannot be null");

        labelSelectionPanel.removeAll();
        labelSelectionContentPanel.removeAll();
        labelSelectionStatusLabel.setText(" ");
        labelSelectionToggleButton.setText("Deselect All");
        labelSelectionCollapseButton.setSelected(false);
        labelSelectionCollapseButton.setText("Label Selection [collapsed]");
        includeInfoTagsCheckBox.setSelected(true);
    }

    record PanelBuildResult(
            List<PreviewSelectionSupport.LabelOption> options,
            List<JCheckBox> checkboxes
    ) {
    }
}
