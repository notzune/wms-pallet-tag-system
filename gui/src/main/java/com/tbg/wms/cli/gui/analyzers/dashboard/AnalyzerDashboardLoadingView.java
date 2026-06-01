package com.tbg.wms.cli.gui.analyzers.dashboard;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

@SuppressWarnings("serial")
public final class AnalyzerDashboardLoadingView extends JPanel {

    private final JLabel messageLabel = new JLabel("Loading live data...");

    public AnalyzerDashboardLoadingView() {
        setLayout(new BorderLayout());
        add(messageLabel, BorderLayout.NORTH);
    }

    String messageForTest() {
        return messageLabel.getText();
    }
}
