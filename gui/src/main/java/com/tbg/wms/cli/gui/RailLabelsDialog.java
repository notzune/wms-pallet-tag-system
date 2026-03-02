/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.1
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/**
 * Rail labels workflow dialog.
 */
public final class RailLabelsDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JTextField trainIdField = new JTextField(18);
    private final JTextField footprintCsvField = new JTextField(48);
    private final JTextField templateDocxField = new JTextField(48);
    private final JTextField outputDirField = new JTextField(48);
    private final JCheckBox csvOverrideCheck = new JCheckBox("Use CSV override", true);
    private final JTextArea mergePreviewArea = new JTextArea();
    private final JTextArea diagnosticsArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JButton previewButton = new JButton("Load Preview");
    private final JButton generateButton = new JButton("Generate Artifacts");

    private final transient RailWorkflowService railService;
    private transient RailWorkflowService.PreparedRailJob preparedJob;

    public RailLabelsDialog(JFrame owner, AppConfig config) {
        super(owner, "Rail Labels Workflow", true);
        this.railService = new RailWorkflowService(Objects.requireNonNull(config, "config cannot be null"));
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1200, 800);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        footprintCsvField.setText(railService.defaultFootprintCsvPath());
        templateDocxField.setText(Paths.get("out", "Print.docx").toAbsolutePath().toString());
        outputDirField.setText(Paths.get("out", "rail-gui").toAbsolutePath().toString());

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);
        wireActions();
        generateButton.setEnabled(false);
    }

    private static String rootMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage() == null ? current.toString() : current.getMessage();
    }

    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Train ID:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 0.2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(trainIdField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(previewButton, gbc);
        gbc.gridx = 3;
        panel.add(generateButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        panel.add(new JLabel("Footprint CSV:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(footprintCsvField, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(buildBrowseButton(footprintCsvField, false), gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        panel.add(csvOverrideCheck, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        panel.add(new JLabel("Template DOCX:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(templateDocxField, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(buildBrowseButton(templateDocxField, false), gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        panel.add(new JLabel("Output Directory:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(outputDirField, gbc);
        gbc.gridx = 3;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(buildBrowseButton(outputDirField, true), gbc);

        return panel;
    }

    private JSplitPane buildCenterPanel() {
        mergePreviewArea.setEditable(false);
        diagnosticsArea.setEditable(false);
        Font mono = new Font(Font.MONOSPACED, Font.PLAIN, 12);
        mergePreviewArea.setFont(mono);
        diagnosticsArea.setFont(mono);
        JScrollPane mergeScroll = new JScrollPane(mergePreviewArea);
        JScrollPane diagScroll = new JScrollPane(diagnosticsArea);
        mergeScroll.setBorder(BorderFactory.createTitledBorder("Merge Preview"));
        diagScroll.setBorder(BorderFactory.createTitledBorder("Diagnostics"));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mergeScroll, diagScroll);
        split.setDividerLocation(420);
        return split;
    }

    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.add(statusLabel);
        panel.add(left, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        JButton close = new JButton("Close");
        close.addActionListener(e -> dispose());
        right.add(close);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JButton buildBrowseButton(JTextField field, boolean directoriesOnly) {
        JButton button = new JButton("Browse...");
        button.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(directoriesOnly
                    ? JFileChooser.DIRECTORIES_ONLY
                    : JFileChooser.FILES_ONLY);
            if (!field.getText().isBlank()) {
                chooser.setSelectedFile(Paths.get(field.getText()).toFile());
            }
            int result = chooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                field.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        });
        return button;
    }

    private void wireActions() {
        previewButton.addActionListener(e -> loadPreview());
        generateButton.addActionListener(e -> generateArtifacts());
    }

    private void loadPreview() {
        String trainId = trainIdField.getText().trim();
        if (trainId.isEmpty()) {
            showError("Train ID is required.");
            return;
        }

        setBusy("Loading WMS rail rows and footprints...");
        generateButton.setEnabled(false);
        preparedJob = null;

        SwingWorker<RailWorkflowService.PreparedRailJob, Void> worker = new SwingWorker<>() {
            @Override
            protected RailWorkflowService.PreparedRailJob doInBackground() throws Exception {
                Path csvPath = footprintCsvField.getText().trim().isEmpty() ? null : Paths.get(footprintCsvField.getText().trim());
                return railService.prepareRailJob(trainId, csvPath, csvOverrideCheck.isSelected());
            }

            @Override
            protected void done() {
                try {
                    preparedJob = get();
                    mergePreviewArea.setText(railService.buildMergePreviewText(preparedJob, 400));
                    diagnosticsArea.setText(railService.buildDiagnosticsText(preparedJob));
                    generateButton.setEnabled(true);
                    setReady("Preview ready.");
                } catch (Exception ex) {
                    setReady("Preview failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void generateArtifacts() {
        if (preparedJob == null) {
            showError("Load Preview first.");
            return;
        }
        setBusy("Generating rail artifacts...");
        previewButton.setEnabled(false);
        generateButton.setEnabled(false);

        SwingWorker<RailWorkflowService.GenerationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected RailWorkflowService.GenerationResult doInBackground() throws Exception {
                Path outputDir = outputDirField.getText().trim().isEmpty() ? null : Paths.get(outputDirField.getText().trim());
                Path templateDocx = templateDocxField.getText().trim().isEmpty() ? null : Paths.get(templateDocxField.getText().trim());
                return railService.generateArtifacts(preparedJob, outputDir, templateDocx);
            }

            @Override
            protected void done() {
                previewButton.setEnabled(true);
                generateButton.setEnabled(true);
                try {
                    RailWorkflowService.GenerationResult result = get();
                    StringBuilder message = new StringBuilder();
                    message.append("Artifacts generated in:\n").append(result.getOutputDirectory()).append("\n\n")
                            .append("CSV: ").append(result.getMergeCsvPath()).append("\n")
                            .append("Summary: ").append(result.getSummaryPath()).append("\n");
                    RailArtifactService.WordArtifactResult word = result.getWordArtifacts();
                    if (word.getMergedDocx() != null) {
                        message.append("DOCX: ").append(word.getMergedDocx()).append("\n");
                    }
                    if (word.getMergedPdf() != null) {
                        message.append("PDF: ").append(word.getMergedPdf()).append("\n");
                    }
                    if (word.getMergedPrn() != null) {
                        message.append("PRN: ").append(word.getMergedPrn()).append("\n");
                    }
                    if (!word.getWarnings().isEmpty()) {
                        message.append("\nWarnings:\n");
                        for (String warning : word.getWarnings()) {
                            message.append(" - ").append(warning).append('\n');
                        }
                    }
                    diagnosticsArea.append("\n\nGeneration Result\n-----------------\n" + message + '\n');
                    setReady("Artifacts generated.");
                } catch (Exception ex) {
                    setReady("Generation failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void setBusy(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(false);
    }

    private void setReady(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Rail Labels", JOptionPane.ERROR_MESSAGE);
    }
}
