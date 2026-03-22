package com.tbg.wms.cli.gui;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.Objects;

/**
 * Encapsulates queue-print and resume-job dialog orchestration for the main GUI.
 *
 * <p>This keeps {@link LabelGuiFrame} focused on the primary shipment/carrier workflow while
 * isolating auxiliary operator-recovery flows behind a smaller dependency surface.</p>
 */
final class QueueResumeDialogSupport {
    private final Dependencies dependencies;
    private final int maxQueueItems;

    QueueResumeDialogSupport(Dependencies dependencies, int maxQueueItems) {
        this.dependencies = Objects.requireNonNull(dependencies, "dependencies cannot be null");
        if (maxQueueItems <= 0) {
            throw new IllegalArgumentException("maxQueueItems must be positive");
        }
        this.maxQueueItems = maxQueueItems;
    }

    void openQueueDialog() {
        JDialog dialog = new JDialog(dependencies.ownerFrame(), "Queue Print", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.setLayout(new BorderLayout(8, 8));

        JTextArea inputArea = new JTextArea(12, 72);
        inputArea.setLineWrap(false);
        dependencies.installClipboardBehavior(inputArea);

        JComboBox<String> defaultType = new JComboBox<>(new String[]{"Carrier Move ID", "Shipment ID"});
        JLabel hint = new JLabel("Use prefixes for mixed queue: C:<cmid> or S:<shipment>. One item per line.");

        JTextArea previewArea = new JTextArea(14, 72);
        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel top = new JPanel(new BorderLayout(4, 4));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel("Default Type:"));
        controls.add(defaultType);
        top.add(controls, BorderLayout.NORTH);
        top.add(new JScrollPane(inputArea), BorderLayout.CENTER);
        top.add(hint, BorderLayout.SOUTH);

        JButton previewBtn = new JButton("Preview Queue");
        JButton printBtn = new JButton("Print Queue");
        JButton closeBtn = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(previewBtn);
        buttons.add(printBtn);
        buttons.add(closeBtn);

        final AdvancedPrintWorkflowService.PreparedQueueJob[] prepared = new AdvancedPrintWorkflowService.PreparedQueueJob[1];

        previewBtn.addActionListener(e -> {
            try {
                List<AdvancedPrintWorkflowService.QueueRequestItem> requests = QueueInputParser.parse(
                        inputArea.getText(),
                        defaultType.getSelectedIndex() == 0
                                ? AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE
                                : AdvancedPrintWorkflowService.QueueItemType.SHIPMENT,
                        maxQueueItems);
                prepared[0] = dependencies.workflow().prepareQueue(requests);
                previewArea.setText(dependencies.previewFormatter().buildQueuePreview(prepared[0]));
            } catch (Exception ex) {
                dependencies.showError(dependencies.rootMessage(ex));
            }
        });

        printBtn.addActionListener(e -> {
            if (prepared[0] == null) {
                dependencies.showError("Preview queue first.");
                return;
            }
            LabelWorkflowService.PrinterOption selected = dependencies.selectedPrinterOption();
            boolean printToFile = dependencies.isPrintToFileSelected(selected);
            String printerId = printToFile ? null : (selected == null ? null : selected.getId());
            try {
                AdvancedPrintWorkflowService.QueuePrintResult result =
                        dependencies.workflow().printQueue(prepared[0], printerId, printToFile);
                JOptionPane.showMessageDialog(dialog,
                        "Queue complete.\nLabels: " + result.getTotalLabelsPrinted() +
                                "\nInfo Tags: " + result.getTotalInfoTagsPrinted(),
                        "Queue Complete",
                        JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
            } catch (Exception ex) {
                dependencies.showError("Queue print failed: " + dependencies.rootMessage(ex));
            }
        });

        closeBtn.addActionListener(e -> dialog.dispose());

        dialog.add(top, BorderLayout.NORTH);
        dialog.add(new JScrollPane(previewArea), BorderLayout.CENTER);
        dialog.add(buttons, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setLocationRelativeTo(dependencies.ownerFrame());
        dialog.setVisible(true);
    }

    void openResumeDialog() {
        try {
            List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = dependencies.workflow().listIncompleteJobs();
            if (candidates.isEmpty()) {
                JOptionPane.showMessageDialog(
                        dependencies.ownerFrame(),
                        "No incomplete jobs found.",
                        "Resume Job",
                        JOptionPane.INFORMATION_MESSAGE);
                return;
            }

            String[] options = buildResumeOptions(candidates);
            String selected = (String) JOptionPane.showInputDialog(
                    dependencies.ownerFrame(),
                    "Select a job to resume (safe mode reprints last successful label/tag):",
                    "Resume Incomplete Job",
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]
            );
            if (selected == null) {
                return;
            }
            int index = findSelectedOptionIndex(options, selected);
            if (index < 0) {
                return;
            }
            AdvancedPrintWorkflowService.PrintResult result =
                    dependencies.workflow().resumeJob(candidates.get(index).checkpointId());
            JOptionPane.showMessageDialog(dependencies.ownerFrame(),
                    "Resume complete.\nLabels: " + result.getLabelsPrinted() +
                            "\nInfo Tags: " + result.getInfoTagsPrinted() +
                            "\nOutput: " + result.getOutputDirectory(),
                    "Resume Complete",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            dependencies.showError("Resume failed: " + dependencies.rootMessage(ex));
        }
    }

    void autoResumeIfFound() {
        SwingUtilities.invokeLater(() -> {
            try {
                List<AdvancedPrintWorkflowService.ResumeCandidate> candidates = dependencies.workflow().listIncompleteJobs();
                if (candidates.isEmpty()) {
                    return;
                }
                AdvancedPrintWorkflowService.ResumeCandidate latest = candidates.get(0);
                int choice = JOptionPane.showConfirmDialog(
                        dependencies.ownerFrame(),
                        buildAutoResumePrompt(latest),
                        "Incomplete Job Found",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE
                );
                if (choice == JOptionPane.YES_OPTION) {
                    AdvancedPrintWorkflowService.PrintResult result =
                            dependencies.workflow().resumeJob(latest.checkpointId());
                    dependencies.setReady("Resumed job. Printed " + result.getLabelsPrinted() + " labels and " +
                            result.getInfoTagsPrinted() + " info tags.");
                }
            } catch (Exception ignored) {
                // Startup should continue even if resume scan fails.
            }
        });
    }

    static String[] buildResumeOptions(List<AdvancedPrintWorkflowService.ResumeCandidate> candidates) {
        String[] options = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            AdvancedPrintWorkflowService.ResumeCandidate candidate = candidates.get(i);
            options[i] = candidate.mode() + " " + candidate.sourceId() + " | progress " +
                    candidate.nextTaskIndex() + "/" + candidate.totalTasks();
        }
        return options;
    }

    static int findSelectedOptionIndex(String[] options, String selected) {
        if (selected == null) {
            return -1;
        }
        for (int i = 0; i < options.length; i++) {
            if (Objects.equals(options[i], selected)) {
                return i;
            }
        }
        return -1;
    }

    static String buildAutoResumePrompt(AdvancedPrintWorkflowService.ResumeCandidate latest) {
        return "Found incomplete job (" + latest.mode() + " " + latest.sourceId() + ", " +
                latest.nextTaskIndex() + "/" + latest.totalTasks() + ").\nResume now?";
    }

    interface Dependencies {
        JFrame ownerFrame();

        AdvancedPrintWorkflowService workflow();

        LabelPreviewFormatter previewFormatter();

        LabelWorkflowService.PrinterOption selectedPrinterOption();

        boolean isPrintToFileSelected(LabelWorkflowService.PrinterOption selected);

        void installClipboardBehavior(JTextComponent... fields);

        void showError(String message);

        String rootMessage(Throwable throwable);

        void setReady(String message);
    }
}
