package com.tbg.wms.cli.gui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.Serial;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Primary operator/admin settings dialog for non-secret runtime preferences and maintenance actions.
 */
final class MainSettingsDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient Consumer<String> showError;
    private final transient Consumer<JTextComponent[]> installClipboardBehavior;
    private final transient BiConsumer<Path, Integer> onSave;
    private final transient IntConsumer onCleanup;
    private final transient Runnable onOpenUpdateManager;
    private final transient Runnable onUninstall;
    private final transient Runnable onAdvancedSettings;

    MainSettingsDialog(
            Frame owner,
            Path defaultOutputDir,
            int retentionDays,
            String updateStatus,
            Consumer<String> showError,
            Consumer<JTextComponent[]> installClipboardBehavior,
            BiConsumer<Path, Integer> onSave,
            IntConsumer onCleanup,
            Runnable onOpenUpdateManager,
            Runnable onUninstall,
            Runnable onAdvancedSettings
    ) {
        super(owner, "Settings", true);
        this.showError = Objects.requireNonNull(showError, "showError cannot be null");
        this.installClipboardBehavior = Objects.requireNonNull(installClipboardBehavior, "installClipboardBehavior cannot be null");
        this.onSave = Objects.requireNonNull(onSave, "onSave cannot be null");
        this.onCleanup = Objects.requireNonNull(onCleanup, "onCleanup cannot be null");
        this.onOpenUpdateManager = Objects.requireNonNull(onOpenUpdateManager, "onOpenUpdateManager cannot be null");
        this.onUninstall = Objects.requireNonNull(onUninstall, "onUninstall cannot be null");
        this.onAdvancedSettings = Objects.requireNonNull(onAdvancedSettings, "onAdvancedSettings cannot be null");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField outputDirField = new JTextField(defaultOutputDir.toString(), 40);
        JTextField retentionDaysField = new JTextField(String.valueOf(retentionDays), 6);
        JLabel updateStatusLabel = new JLabel(updateStatus);
        installClipboardBehavior.accept(new JTextComponent[]{outputDirField, retentionDaysField});

        JButton browseButton = new JButton("Browse...");
        JButton cleanupNowButton = new JButton("Clean Old Output Now");
        JButton checkUpdatesButton = new JButton("Update Manager...");
        JButton uninstallButton = new JButton("Uninstall / Clean Install Prep...");
        JButton advancedSettingsButton = new JButton("Advanced Settings...");

        addFormRow(content, gbc, 0, "Default print-to-file output dir:", outputDirField);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        content.add(browseButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.0;
        content.add(new JLabel("Out cleanup policy:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        JPanel retentionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        retentionPanel.add(new JLabel("Delete generated files/folders older than"));
        retentionPanel.add(retentionDaysField);
        retentionPanel.add(new JLabel("day(s) from out/."));
        content.add(retentionPanel, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        content.add(cleanupNowButton, gbc);

        addFormRow(content, gbc, 2, "Updates:", updateStatusLabel);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        content.add(checkUpdatesButton, gbc);

        addFormRow(content, gbc, 3, "Install maintenance:", new JLabel("Launch uninstall or clean-install prep for packaged installs."));
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        content.add(uninstallButton, gbc);

        addFormRow(content, gbc, 4, "Config editing:", new JLabel("Advanced Settings edits runtime config files only. Env secrets remain outside the GUI."));
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        content.add(advancedSettingsButton, gbc);

        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");
        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonRow.add(saveButton);
        buttonRow.add(cancelButton);

        browseButton.addActionListener(e -> openOutputDirectoryChooser(outputDirField));
        cleanupNowButton.addActionListener(e -> {
            Integer parsed = parseRetentionDays(retentionDaysField.getText());
            if (parsed == null) {
                showError.accept("Out cleanup retention must be a positive whole number of days.");
                return;
            }
            onCleanup.accept(parsed);
        });
        checkUpdatesButton.addActionListener(e -> onOpenUpdateManager.run());
        uninstallButton.addActionListener(e -> onUninstall.run());
        advancedSettingsButton.addActionListener(e -> onAdvancedSettings.run());
        saveButton.addActionListener(e -> saveSettings(outputDirField, retentionDaysField));
        cancelButton.addActionListener(e -> dispose());

        add(content, BorderLayout.CENTER);
        add(buttonRow, BorderLayout.SOUTH);
        pack();
        setLocationRelativeTo(owner);
    }

    private static void addFormRow(JPanel form, GridBagConstraints gbc, int row, String label, JComponent field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.0;
        form.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        form.add(field, gbc);
    }

    private void openOutputDirectoryChooser(JTextField outputDirField) {
        JFileChooser chooser = new JFileChooser(outputDirField.getText());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select default print-to-file output directory");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().toPath().toString());
        }
    }

    private void saveSettings(JTextField outputDirField, JTextField retentionDaysField) {
        String raw = outputDirField.getText();
        if (raw == null || raw.isBlank()) {
            showError.accept("Default output directory is required.");
            return;
        }
        Integer parsedRetention = parseRetentionDays(retentionDaysField.getText());
        if (parsedRetention == null) {
            showError.accept("Out cleanup retention must be a positive whole number of days.");
            return;
        }

        Path configuredPath;
        try {
            configuredPath = Paths.get(raw.trim()).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            showError.accept("Invalid output directory path.");
            return;
        }

        onSave.accept(configuredPath, parsedRetention);
        dispose();
    }

    private Integer parseRetentionDays(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(raw.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @FunctionalInterface
    interface IntConsumer {
        void accept(int value);
    }
}
