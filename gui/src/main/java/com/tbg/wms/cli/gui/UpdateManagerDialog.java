package com.tbg.wms.cli.gui;

import com.tbg.wms.core.update.UpdateActionService;

import javax.swing.*;
import java.awt.*;
import java.io.Serial;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Modal dialog for release visibility, experimental settings, and manual target selection.
 */
final class UpdateManagerDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient SnapshotLoader snapshotLoader;
    private final transient Consumer<UpdateActionService.InstallTarget> installAction;
    private final transient Consumer<String> openReleaseAction;

    private final JCheckBox experimentalCheckBox = new JCheckBox("Enable experimental / prerelease updates");
    private final JLabel currentVersionValue = new JLabel();
    private final JLabel latestStableValue = new JLabel();
    private final JLabel latestExperimentalValue = new JLabel();
    private final JLabel updatesBehindValue = new JLabel();
    private final JLabel statusValue = new JLabel();
    private final JComboBox<UpdateActionService.InstallTarget> targetCombo = new JComboBox<>();
    private final JButton refreshButton = new JButton("Refresh");
    private final JButton installButton = new JButton("Install Selected");
    private final JButton openReleaseButton = new JButton("Open Release Page");
    private final JLabel dialogStatusLabel = new JLabel(" ");

    private transient UpdateManagerSnapshot snapshot;

    UpdateManagerDialog(
            Frame owner,
            UpdateManagerSnapshot initialSnapshot,
            SnapshotLoader snapshotLoader,
            Consumer<UpdateActionService.InstallTarget> installAction,
            Consumer<String> openReleaseAction
    ) {
        super(owner, "Update Manager", true);
        this.snapshot = Objects.requireNonNull(initialSnapshot, "initialSnapshot cannot be null");
        this.snapshotLoader = Objects.requireNonNull(snapshotLoader, "snapshotLoader cannot be null");
        this.installAction = Objects.requireNonNull(installAction, "installAction cannot be null");
        this.openReleaseAction = Objects.requireNonNull(openReleaseAction, "openReleaseAction cannot be null");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        addRow(content, gbc, 0, "Current version:", currentVersionValue);
        addRow(content, gbc, 1, "Latest stable:", latestStableValue);
        addRow(content, gbc, 2, "Latest experimental:", latestExperimentalValue);
        addRow(content, gbc, 3, "Stable releases behind:", updatesBehindValue);
        addRow(content, gbc, 4, "Status:", statusValue);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        content.add(experimentalCheckBox, gbc);

        gbc.gridy = 6;
        gbc.gridwidth = 1;
        content.add(new JLabel("Install target:"), gbc);
        gbc.gridx = 1;
        content.add(targetCombo, gbc);

        targetCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(
                    JList<?> list,
                    Object value,
                    int index,
                    boolean isSelected,
                    boolean cellHasFocus
            ) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof UpdateActionService.InstallTarget target) {
                    String suffix = target.prerelease() ? " experimental" : " stable";
                    String current = Objects.equals(target.version(), snapshot.currentVersion()) ? " (current)" : "";
                    setText(target.version() + " - " + suffix + current);
                }
                return this;
            }
        });

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Close");
        buttons.add(refreshButton);
        buttons.add(openReleaseButton);
        buttons.add(installButton);
        buttons.add(closeButton);

        add(content, BorderLayout.CENTER);
        add(dialogStatusLabel, BorderLayout.NORTH);
        add(buttons, BorderLayout.SOUTH);

        experimentalCheckBox.addActionListener(e -> reloadSnapshot());
        refreshButton.addActionListener(e -> reloadSnapshot());
        openReleaseButton.addActionListener(e -> openSelectedRelease());
        installButton.addActionListener(e -> installSelectedTarget());
        closeButton.addActionListener(e -> dispose());

        applySnapshot(snapshot);
        pack();
        setLocationRelativeTo(owner);
        if (snapshot.catalog() == null) {
            reloadSnapshot();
        }
    }

    private static void addRow(JPanel panel, GridBagConstraints gbc, int row, String label, JComponent value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(value, gbc);
    }

    private void reloadSnapshot() {
        setBusy("Refreshing update catalog...");
        boolean experimentalEnabled = experimentalCheckBox.isSelected();
        SwingWorker<UpdateManagerSnapshot, Void> worker = new SwingWorker<>() {
            @Override
            protected UpdateManagerSnapshot doInBackground() throws Exception {
                return snapshotLoader.load(experimentalEnabled);
            }

            @Override
            protected void done() {
                try {
                    applySnapshot(get());
                    dialogStatusLabel.setText("Update catalog refreshed.");
                } catch (Exception ex) {
                    dialogStatusLabel.setText("Update check failed.");
                    JOptionPane.showMessageDialog(
                            UpdateManagerDialog.this,
                            "Update check failed: " + rootMessage(ex),
                            "Updates",
                            JOptionPane.ERROR_MESSAGE
                    );
                    applySnapshot(snapshot);
                } finally {
                    setBusy(null);
                }
            }
        };
        worker.execute();
    }

    private void applySnapshot(UpdateManagerSnapshot newSnapshot) {
        snapshot = Objects.requireNonNull(newSnapshot, "newSnapshot cannot be null");
        currentVersionValue.setText(blankOrValue(snapshot.currentVersion(), "unknown"));
        latestStableValue.setText(blankOrValue(snapshot.latestStableVersion(), "unavailable"));
        latestExperimentalValue.setText(snapshot.latestExperimentalVersion().isBlank()
                ? "none published"
                : snapshot.latestExperimentalVersion() + (snapshot.experimentalUpdatesEnabled() ? "" : " (opt-in disabled)"));
        updatesBehindValue.setText(String.valueOf(snapshot.stableUpdatesBehind()));
        statusValue.setText(snapshot.summary());
        experimentalCheckBox.setSelected(snapshot.experimentalUpdatesEnabled());
        DefaultComboBoxModel<UpdateActionService.InstallTarget> model = new DefaultComboBoxModel<>();
        for (UpdateActionService.InstallTarget target : snapshot.installTargets()) {
            model.addElement(target);
        }
        targetCombo.setModel(model);
        if (model.getSize() > 0) {
            targetCombo.setSelectedIndex(0);
        }
        installButton.setEnabled(model.getSize() > 0);
        openReleaseButton.setEnabled(model.getSize() > 0 || !snapshot.latestStableReleaseUrl().isBlank());
    }

    private void openSelectedRelease() {
        UpdateActionService.InstallTarget selectedTarget =
                (UpdateActionService.InstallTarget) targetCombo.getSelectedItem();
        String releaseUrl = selectedTarget == null ? snapshot.latestStableReleaseUrl() : selectedTarget.releaseUrl();
        if (releaseUrl == null || releaseUrl.isBlank()) {
            JOptionPane.showMessageDialog(this, "Release page URL is unavailable.", "Updates", JOptionPane.WARNING_MESSAGE);
            return;
        }
        openReleaseAction.accept(releaseUrl);
    }

    private void installSelectedTarget() {
        UpdateActionService.InstallTarget selectedTarget =
                (UpdateActionService.InstallTarget) targetCombo.getSelectedItem();
        if (selectedTarget == null) {
            JOptionPane.showMessageDialog(this, "Select a target version first.", "Updates", JOptionPane.WARNING_MESSAGE);
            return;
        }
        installAction.accept(selectedTarget);
    }

    private void setBusy(String message) {
        boolean busy = message != null;
        refreshButton.setEnabled(!busy);
        installButton.setEnabled(!busy && targetCombo.getItemCount() > 0);
        openReleaseButton.setEnabled(!busy);
        targetCombo.setEnabled(!busy);
        experimentalCheckBox.setEnabled(!busy);
        dialogStatusLabel.setText(message == null ? " " : message);
    }

    private static String blankOrValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String rootMessage(Throwable throwable) {
        Throwable cursor = throwable;
        while (cursor != null && cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        return cursor == null || cursor.getMessage() == null || cursor.getMessage().isBlank()
                ? "Unknown error"
                : cursor.getMessage();
    }

    @FunctionalInterface
    interface SnapshotLoader {
        UpdateManagerSnapshot load(boolean experimentalEnabled) throws Exception;
    }
}
