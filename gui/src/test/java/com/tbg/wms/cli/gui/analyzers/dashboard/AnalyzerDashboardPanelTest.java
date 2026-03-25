package com.tbg.wms.cli.gui.analyzers.dashboard;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerDashboardPanelTest {

    @Test
    void dashboardPanel_shouldRenderSectionTitlesAndErrors() {
        AnalyzerDashboardSnapshot snapshot = new AnalyzerDashboardSnapshot(List.of(
                AnalyzerDashboardSectionSnapshot.success("Case Pick Summary", new JLabel("summary")),
                AnalyzerDashboardSectionSnapshot.failure("Unload and Load Activity", "ORA-00942")
        ));

        AnalyzerDashboardPanel panel = new AnalyzerDashboardPanel();
        panel.showSnapshot(snapshot);

        assertEquals(List.of("Case Pick Summary", "Unload and Load Activity"), panel.sectionTitlesForTest());
        assertTrue(panel.sectionErrorTextForTest("Unload and Load Activity").contains("ORA-00942"));
    }
}
