package com.tbg.wms.cli.gui.analyzers.dashboard;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("serial")
public final class AnalyzerDashboardPanel extends JScrollPane {

    private final JPanel sectionsPanel = new JPanel();
    private final List<AnalyzerDashboardSectionPanel> sectionPanels = new ArrayList<>();

    public AnalyzerDashboardPanel() {
        sectionsPanel.setLayout(new BoxLayout(sectionsPanel, BoxLayout.Y_AXIS));
        setViewportView(sectionsPanel);
    }

    public void showSnapshot(AnalyzerDashboardSnapshot snapshot) {
        sectionsPanel.removeAll();
        sectionPanels.clear();
        for (AnalyzerDashboardSectionSnapshot section : snapshot.sections()) {
            AnalyzerDashboardSectionPanel panel = new AnalyzerDashboardSectionPanel();
            panel.showSection(section);
            sectionPanels.add(panel);
            sectionsPanel.add(panel);
        }
        sectionsPanel.revalidate();
        sectionsPanel.repaint();
    }

    List<String> sectionTitlesForTest() {
        return sectionPanels.stream().map(AnalyzerDashboardSectionPanel::titleForTest).toList();
    }

    String sectionErrorTextForTest(String title) {
        return sectionPanels.stream()
                .filter(panel -> panel.titleForTest().equals(title))
                .map(AnalyzerDashboardSectionPanel::errorTextForTest)
                .findFirst()
                .orElse("");
    }
}
