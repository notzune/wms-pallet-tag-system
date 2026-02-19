/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

/**
 * Swing-based GUI for shipment preview and label printing.
 */
public final class LabelGuiFrame extends JFrame {

    private final JTextField shipmentField = new JTextField(16);
    private final JComboBox<LabelWorkflowService.PrinterOption> printerCombo = new JComboBox<>();
    private final JButton previewButton = new JButton("Preview");
    private final JButton clearButton = new JButton("Clear");
    private final JButton printButton = new JButton("Confirm Print");
    private final JTextArea shipmentArea = new JTextArea();
    private final JTextArea mathArea = new JTextArea();
    private final JLabel statusLabel = new JLabel("Ready.");

    private final LabelWorkflowService service = new LabelWorkflowService(new AppConfig());
    private LabelWorkflowService.PreparedJob preparedJob;

    public LabelGuiFrame() {
        super("WMS Pallet Tag System");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(8, 8));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        wireActions();
        loadPrintersAsync();
        printButton.setEnabled(false);
    }

    private JComponent buildTopPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(new JLabel("Shipment ID:"), gbc);

        gbc.gridx = 1;
        panel.add(shipmentField, gbc);

        gbc.gridx = 2;
        panel.add(new JLabel("Printer:"), gbc);

        gbc.gridx = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(printerCombo, gbc);

        gbc.gridx = 4;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(previewButton, gbc);

        gbc.gridx = 5;
        panel.add(clearButton, gbc);

        gbc.gridx = 6;
        panel.add(printButton, gbc);

        return panel;
    }

    private JComponent buildCenterPanel() {
        shipmentArea.setEditable(false);
        shipmentArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        mathArea.setEditable(false);
        mathArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(shipmentArea),
                new JScrollPane(mathArea));
        split.setDividerLocation(340);
        return split;
    }

    private JComponent buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 6, 8));
        panel.add(statusLabel, BorderLayout.CENTER);
        return panel;
    }

    private void wireActions() {
        previewButton.addActionListener(e -> previewJob());
        clearButton.addActionListener(e -> clearForm());
        printButton.addActionListener(e -> confirmAndPrint());
    }

    private void loadPrintersAsync() {
        setBusy("Loading printers...");
        SwingWorker<List<LabelWorkflowService.PrinterOption>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<LabelWorkflowService.PrinterOption> doInBackground() throws Exception {
                return service.loadPrinters();
            }

            @Override
            protected void done() {
                try {
                    List<LabelWorkflowService.PrinterOption> printers = get();
                    DefaultComboBoxModel<LabelWorkflowService.PrinterOption> model = new DefaultComboBoxModel<>();
                    for (LabelWorkflowService.PrinterOption printer : printers) {
                        model.addElement(printer);
                    }
                    printerCombo.setModel(model);
                    if (model.getSize() > 0) {
                        printerCombo.setSelectedIndex(0);
                        setReady("Printers loaded.");
                    } else {
                        setReady("No enabled printers found in routing config.");
                    }
                } catch (Exception ex) {
                    setReady("Failed to load printers.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void previewJob() {
        String shipmentId = shipmentField.getText().trim();
        if (shipmentId.isEmpty()) {
            showError("Enter a shipment ID.");
            return;
        }

        setBusy("Preparing preview...");
        printButton.setEnabled(false);
        preparedJob = null;

        SwingWorker<LabelWorkflowService.PreparedJob, Void> worker = new SwingWorker<>() {
            @Override
            protected LabelWorkflowService.PreparedJob doInBackground() throws Exception {
                return service.prepareJob(shipmentId);
            }

            @Override
            protected void done() {
                try {
                    preparedJob = get();
                    renderPreview(preparedJob);
                    printButton.setEnabled(true);
                    setReady("Preview ready. Verify details, then click Confirm Print.");
                } catch (Exception ex) {
                    setReady("Preview failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void renderPreview(LabelWorkflowService.PreparedJob job) {
        StringBuilder summary = new StringBuilder();
        summary.append("Shipment: ").append(job.getShipment().getShipmentId()).append('\n');
        summary.append("Order: ").append(value(job.getShipment().getOrderId())).append('\n');
        summary.append("Ship To: ").append(value(job.getShipment().getShipToName())).append('\n');
        summary.append("Address: ").append(value(job.getShipment().getShipToAddress1())).append(", ")
                .append(value(job.getShipment().getShipToCity())).append(", ")
                .append(value(job.getShipment().getShipToState())).append(" ")
                .append(value(job.getShipment().getShipToZip())).append('\n');
        summary.append("PO: ").append(value(job.getShipment().getCustomerPo())).append('\n');
        summary.append("Location No: ").append(value(job.getShipment().getLocationNumber())).append('\n');
        summary.append("Carrier Move: ").append(value(job.getShipment().getCarrierCode())).append(" ")
                .append(value(job.getShipment().getCarrierMoveId())).append('\n');
        summary.append("Staging Location: ").append(value(job.getStagingLocation())).append('\n');
        summary.append('\n');
        summary.append("Label Plan:\n");
        summary.append(" - Actual LPNs: ").append(job.getShipment().getLpnCount()).append('\n');
        summary.append(" - Labels To Generate: ").append(job.getLpnsForLabels().size()).append('\n');
        summary.append(" - Virtual Labels Used: ").append(job.isUsingVirtualLabels() ? "YES" : "NO").append('\n');
        summary.append(" - Total Units: ").append(job.getPlanResult().getTotalUnits()).append('\n');
        summary.append(" - Estimated Pallets (Footprint): ").append(job.getPlanResult().getEstimatedPallets()).append('\n');
        summary.append(" - Full Pallets (Footprint): ").append(job.getPlanResult().getFullPallets()).append('\n');
        summary.append(" - Partial Pallets (Footprint): ").append(job.getPlanResult().getPartialPallets()).append('\n');
        summary.append(" - Missing Footprint SKUs: ")
                .append(job.getPlanResult().getSkusMissingFootprint().isEmpty()
                        ? "None"
                        : String.join(", ", job.getPlanResult().getSkusMissingFootprint()))
                .append('\n');
        shipmentArea.setText(summary.toString());

        StringBuilder math = new StringBuilder();
        math.append("Pallet Math (Full vs Partial)\n");
        math.append(String.format("%-20s %-10s %-14s %-8s %-10s %-10s %s%n",
                "SKU", "Units", "Units/Pallet", "Full", "Partial", "TotalPal", "Description"));
        math.append("----------------------------------------------------------------------------------------------------\n");
        for (LabelWorkflowService.SkuMathRow row : job.getSkuMathRows()) {
            math.append(String.format("%-20s %-10d %-14s %-8d %-10d %-10d %s%n",
                    value(row.getSku()),
                    row.getUnits(),
                    row.getUnitsPerPallet() == null ? "-" : row.getUnitsPerPallet().toString(),
                    row.getFullPallets(),
                    row.getPartialUnits(),
                    row.getEstimatedPallets(),
                    value(row.getDescription())));
        }
        mathArea.setText(math.toString());
    }

    private void confirmAndPrint() {
        LabelWorkflowService.PreparedJob job = preparedJob;
        if (job == null) {
            showError("Run Preview first.");
            return;
        }

        LabelWorkflowService.PrinterOption selected = (LabelWorkflowService.PrinterOption) printerCombo.getSelectedItem();
        if (selected == null) {
            showError("Select a printer.");
            return;
        }

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Print " + job.getLpnsForLabels().size() + " labels to " + selected + "?",
                "Confirm Print",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy("Printing...");
        printButton.setEnabled(false);
        previewButton.setEnabled(false);
        clearButton.setEnabled(false);

        SwingWorker<LabelWorkflowService.PrintResult, Void> worker = new SwingWorker<>() {
            @Override
            protected LabelWorkflowService.PrintResult doInBackground() throws Exception {
                Path outDir = Paths.get("out", "gui-" + job.getShipmentId() + "-" +
                        DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(LocalDateTime.now()));
                return service.print(job, selected.getId(), outDir);
            }

            @Override
            protected void done() {
                previewButton.setEnabled(true);
                printButton.setEnabled(true);
                clearButton.setEnabled(true);
                try {
                    LabelWorkflowService.PrintResult result = get();
                    setReady("Printed " + result.getLabelsPrinted() + " labels to " + result.getPrinterId() +
                            " (" + result.getPrinterEndpoint() + ")");
                    JOptionPane.showMessageDialog(
                            LabelGuiFrame.this,
                            "Printed " + result.getLabelsPrinted() + " labels.\nOutput: " + result.getOutputDirectory(),
                            "Print Complete",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                } catch (Exception ex) {
                    setReady("Print failed.");
                    showError(rootMessage(ex));
                }
            }
        };
        worker.execute();
    }

    private void setBusy(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(false);
        clearButton.setEnabled(false);
    }

    private void setReady(String message) {
        statusLabel.setText(message);
        previewButton.setEnabled(true);
        clearButton.setEnabled(true);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private void clearForm() {
        shipmentField.setText("");
        shipmentArea.setText("");
        mathArea.setText("");
        preparedJob = null;
        printButton.setEnabled(false);
        shipmentField.requestFocusInWindow();
        setReady("Cleared. Enter the next shipment ID.");
    }

    private String rootMessage(Throwable throwable) {
        Throwable cursor = Objects.requireNonNullElse(throwable, new RuntimeException("Unknown error"));
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return (message == null || message.isBlank()) ? cursor.getClass().getSimpleName() : message;
    }

    private String value(String v) {
        return (v == null || v.isBlank()) ? "-" : v;
    }
}
