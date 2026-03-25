package com.tbg.wms.cli.gui.analyzers.dashboard;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

@SuppressWarnings("serial")
public final class AnalyzerDashboardSectionPanel extends JPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel errorLabel = new JLabel();
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    public AnalyzerDashboardSectionPanel() {
        setLayout(new BorderLayout(0, 8));
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(titleLabel);
        header.add(errorLabel);
        errorLabel.setVisible(false);
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        add(header, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    void showSection(AnalyzerDashboardSectionSnapshot snapshot) {
        titleLabel.setText(snapshot.title());
        contentPanel.removeAll();
        if (snapshot.failed()) {
            errorLabel.setText(snapshot.errorText());
            errorLabel.setVisible(true);
        } else {
            errorLabel.setVisible(false);
            JComponent content = snapshot.content();
            if (content != null) {
                contentPanel.add(content, BorderLayout.CENTER);
            }
        }
    }

    String titleForTest() {
        return titleLabel.getText();
    }

    String errorTextForTest() {
        return errorLabel.getText();
    }
}
