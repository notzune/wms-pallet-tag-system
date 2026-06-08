package com.tbg.wms.cli.gui.sscc;

import com.tbg.wms.cli.gui.GuiExceptionMessageSupport;
import com.tbg.wms.cli.gui.GuiHelpSupport;
import com.tbg.wms.cli.gui.GuiHelpTopics;
import com.tbg.wms.cli.gui.GuiZplPreviewSupport;
import com.tbg.wms.cli.gui.TextFieldClipboardController;
import com.tbg.wms.cli.gui.ZplPreviewToolDialog;
import com.tbg.wms.core.sscc.SsccCsvSupport;
import com.tbg.wms.core.sscc.SsccLabelGroup;
import com.tbg.wms.core.sscc.SsccLabelPlanner;
import com.tbg.wms.core.sscc.SsccLabelRow;
import com.tbg.wms.core.sscc.SsccLabelTemplate;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.IOException;
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SSCC label import and preview dialog.
 */
public final class SsccLabelDialog extends JDialog {
    @Serial
    private static final long serialVersionUID = 1L;

    private static final Path DEFAULT_OUTPUT_DIR = Paths.get("out", "sscc-gui").toAbsolutePath();

    private final JFrame ownerFrame;
    private final SsccLabelTableModel rawTableModel = new SsccLabelTableModel();
    private final SsccGroupTableModel groupTableModel = new SsccGroupTableModel();
    private final JTable rawRowsTable = new JTable(rawTableModel);
    private final JTable groupRowsTable = new JTable(groupTableModel);
    private final JLabel statusLabel = new JLabel("Ready.");
    private final JLabel summaryLabel = new JLabel("No rows imported.");
    private final JLabel previewDetailsLabel = new JLabel("Select a grouped label to inspect the final ZPL.");
    private final JTextArea previewArea = new JTextArea(28, 52);
    private final JTextField csvPathField = new JTextField(40);
    private final JTextField outputDirField = new JTextField(DEFAULT_OUTPUT_DIR.toString(), 40);

    private final JTextField salesOrderField = new JTextField(12);
    private final JTextField purchaseOrderField = new JTextField(12);
    private final JTextField shipmentField = new JTextField(12);
    private final JTextField carrierCodeField = new JTextField(10);
    private final JTextField trailerIdField = new JTextField(10);
    private final JTextField destinationField = new JTextField(28);
    private final JTextField destinationAddressField = new JTextField(36);
    private final JTextField customerNameField = new JTextField(24);
    private final JTextField facilityField = new JTextField(12);
    private final JTextField itemField = new JTextField(18);
    private final JTextField level2ReferenceField = new JTextField(18);
    private final JTextField originallyShippedLpnField = new JTextField(18);
    private final JTextField sumOfShipCasesField = new JTextField(8);
    private final JTextField newReceivedLpnField = new JTextField(18);

    public SsccLabelDialog(JFrame owner) {
        super(owner, "SSCC Labels", true);
        this.ownerFrame = Objects.requireNonNull(owner, "owner cannot be null");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setSize(1500, 900);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        new TextFieldClipboardController().install(
                csvPathField,
                outputDirField,
                salesOrderField,
                purchaseOrderField,
                shipmentField,
                carrierCodeField,
                trailerIdField,
                destinationField,
                destinationAddressField,
                customerNameField,
                facilityField,
                itemField,
                level2ReferenceField,
                originallyShippedLpnField,
                sumOfShipCasesField,
                newReceivedLpnField,
                previewArea
        );

        add(buildHeaderPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        refreshGroupsAndPreview(true);
    }

    private JComponent buildHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 0, 8));

        JPanel controls = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 4, 4, 4);
        gbc.anchor = GridBagConstraints.WEST;

        JButton browseCsvButton = new JButton("Browse...");
        JButton importButton = new JButton("Import CSV");
        JButton previewButton = new JButton("Preview Selected");
        JButton visualPreviewButton = new JButton("Open Visual Preview");
        JButton exportSelectedButton = new JButton("Export Selected");
        JButton exportAllButton = new JButton("Export All");
        JButton browseOutputButton = new JButton("Browse...");

        gbc.gridx = 0;
        gbc.gridy = 0;
        controls.add(new JLabel("CSV File:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controls.add(csvPathField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        controls.add(browseCsvButton, gbc);
        gbc.gridx = 3;
        controls.add(importButton, gbc);
        gbc.gridx = 4;
        controls.add(previewButton, gbc);
        gbc.gridx = 5;
        controls.add(visualPreviewButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        controls.add(new JLabel("Output Dir:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        controls.add(outputDirField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        controls.add(browseOutputButton, gbc);
        gbc.gridx = 3;
        controls.add(exportSelectedButton, gbc);
        gbc.gridx = 4;
        controls.add(exportAllButton, gbc);

        JPanel helpPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        helpPanel.add(GuiHelpSupport.createHelpButton(this, "SSCC Labels", GuiHelpTopics.ssccLabels()));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.add(summaryLabel, BorderLayout.WEST);
        topRow.add(helpPanel, BorderLayout.EAST);

        panel.add(controls, BorderLayout.CENTER);
        panel.add(topRow, BorderLayout.SOUTH);

        browseCsvButton.addActionListener(e -> browseCsv());
        browseOutputButton.addActionListener(e -> browseOutputDirectory());
        importButton.addActionListener(e -> importCsv());
        previewButton.addActionListener(e -> updatePreviewFromSelection(true));
        visualPreviewButton.addActionListener(e -> openVisualPreview());
        exportSelectedButton.addActionListener(e -> exportSelectedZpl());
        exportAllButton.addActionListener(e -> exportAllZpl());
        return panel;
    }

    private JComponent buildCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.HORIZONTAL_SPLIT,
                buildDataTabs(),
                buildPreviewPanel()
        );
        splitPane.setResizeWeight(0.62d);
        splitPane.setContinuousLayout(true);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildDataTabs() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Grouped Labels", buildGroupedLabelsPanel());
        tabs.addTab("Imported Rows", buildRawRowsPanel());
        tabs.addTab("Manual Entry", buildManualEntryPanel());
        return tabs;
    }

    private JComponent buildGroupedLabelsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        groupRowsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupRowsTable.setFillsViewportHeight(true);
        groupRowsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        groupRowsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updatePreviewFromSelection(false);
            }
        });

        configureGroupColumns();

        JScrollPane tableScroll = new JScrollPane(groupRowsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Final Labels"));
        panel.add(tableScroll, BorderLayout.CENTER);
        return panel;
    }

    private void configureGroupColumns() {
        int[] widths = {110, 150, 92, 120, 130, 70, 88, 86, 82, 115, 320};
        for (int i = 0; i < widths.length; i++) {
            TableColumn column = groupRowsTable.getColumnModel().getColumn(i);
            column.setPreferredWidth(widths[i]);
        }
    }

    private JComponent buildRawRowsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        rawRowsTable.setAutoCreateRowSorter(true);
        rawRowsTable.setFillsViewportHeight(true);
        rawRowsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        JScrollPane tableScroll = new JScrollPane(rawRowsTable);
        tableScroll.setBorder(BorderFactory.createTitledBorder("Imported Workbook Rows"));
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton removeButton = new JButton("Remove Selected");
        JButton clearButton = new JButton("Clear Rows");
        removeButton.addActionListener(e -> removeSelectedRows());
        clearButton.addActionListener(e -> clearRows());
        buttonRow.add(removeButton);
        buttonRow.add(clearButton);
        panel.add(buttonRow, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildManualEntryPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 4, 3, 4);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;

        int row = 0;
        row = addFieldRow(formPanel, gbc, row, "Sales Order #", salesOrderField);
        row = addFieldRow(formPanel, gbc, row, "Purchase Order #", purchaseOrderField);
        row = addFieldRow(formPanel, gbc, row, "Shipment #", shipmentField);
        row = addFieldRow(formPanel, gbc, row, "Carrier Code", carrierCodeField);
        row = addFieldRow(formPanel, gbc, row, "Trailer ID", trailerIdField);
        row = addFieldRow(formPanel, gbc, row, "Destination", destinationField);
        row = addFieldRow(formPanel, gbc, row, "Destination Address", destinationAddressField);
        row = addFieldRow(formPanel, gbc, row, "Customer Name", customerNameField);
        row = addFieldRow(formPanel, gbc, row, "Facility", facilityField);
        row = addFieldRow(formPanel, gbc, row, "Item #", itemField);
        row = addFieldRow(formPanel, gbc, row, "Level 2 Reference #", level2ReferenceField);
        row = addFieldRow(formPanel, gbc, row, "Originally Shipped LPN", originallyShippedLpnField);
        row = addFieldRow(formPanel, gbc, row, "Sum of Ship Cases", sumOfShipCasesField);
        addFieldRow(formPanel, gbc, row, "New Received LPN", newReceivedLpnField);

        JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        JButton addRowButton = new JButton("Add Row");
        JButton clearFieldsButton = new JButton("Clear Fields");
        addRowButton.addActionListener(e -> addManualRow());
        clearFieldsButton.addActionListener(e -> clearEntryFields());
        buttonRow.add(addRowButton);
        buttonRow.add(clearFieldsButton);

        panel.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        panel.add(buttonRow, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 8));

        previewArea.setEditable(false);
        previewArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        previewArea.setLineWrap(false);
        previewArea.setWrapStyleWord(false);
        previewArea.setTabSize(2);

        JPanel header = new JPanel(new BorderLayout());
        header.add(previewDetailsLabel, BorderLayout.CENTER);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setBorder(BorderFactory.createTitledBorder("Selected Label ZPL"));

        panel.add(header, BorderLayout.NORTH);
        panel.add(previewScroll, BorderLayout.CENTER);
        return panel;
    }

    private JComponent buildBottomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 8, 8, 8));
        panel.add(statusLabel, BorderLayout.WEST);

        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> dispose());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        right.add(closeButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private int addFieldRow(JPanel panel, GridBagConstraints gbc, int row, String label, JTextField field) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.weightx = 0.0;
        panel.add(new JLabel(label + ":"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
        return row + 1;
    }

    private void browseCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("CSV files", "csv"));
        if (!csvPathField.getText().isBlank()) {
            chooser.setSelectedFile(Paths.get(csvPathField.getText()).toFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            csvPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseOutputDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (!outputDirField.getText().isBlank()) {
            chooser.setSelectedFile(Paths.get(outputDirField.getText()).toFile());
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void importCsv() {
        Path csv = resolvePath(csvPathField.getText());
        if (csv == null) {
            showError("Choose a CSV file first.");
            return;
        }
        try {
            List<SsccLabelRow> rows = SsccCsvSupport.parse(csv);
            rawTableModel.clear();
            rawTableModel.addRows(rows);
            refreshGroupsAndPreview(true);
            setStatus("Imported " + rows.size() + " raw rows and built " + groupTableModel.getRowCount() + " labels.");
        } catch (Exception ex) {
            showError("Failed to import CSV: " + rootMessage(ex));
        }
    }

    private void addManualRow() {
        try {
            rawTableModel.addRow(createRowFromFields());
            refreshGroupsAndPreview(true);
            setStatus("Added row. Total raw rows: " + rawTableModel.getRowCount());
            clearEntryFields();
        } catch (IllegalArgumentException ex) {
            showError(ex.getMessage());
        }
    }

    private SsccLabelRow createRowFromFields() {
        return new SsccLabelRow(
                required(salesOrderField, "Sales Order #"),
                required(purchaseOrderField, "Purchase Order #"),
                required(shipmentField, "Shipment #"),
                required(carrierCodeField, "Carrier Code"),
                required(trailerIdField, "Trailer ID"),
                required(destinationField, "Destination"),
                required(destinationAddressField, "Destination Address"),
                required(customerNameField, "Customer Name"),
                required(facilityField, "Facility"),
                required(itemField, "Item #"),
                required(level2ReferenceField, "Level 2 Reference #"),
                required(originallyShippedLpnField, "Originally Shipped LPN"),
                parseDouble(sumOfShipCasesField.getText(), "Sum of Ship Cases"),
                required(newReceivedLpnField, "New Received LPN")
        );
    }

    private String required(JTextField field, String label) {
        String value = field.getText() == null ? "" : field.getText().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value;
    }

    private double parseDouble(String value, String label) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0d;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(label + " must be numeric.");
        }
    }

    private void updatePreviewFromSelection(boolean forceSelection) {
        List<SsccLabelGroup> groups = groupTableModel.groups();
        if (groups.isEmpty()) {
            previewDetailsLabel.setText("No grouped labels available yet.");
            previewArea.setText("");
            return;
        }

        if (forceSelection && groupRowsTable.getSelectedRow() < 0) {
            groupRowsTable.setRowSelectionInterval(0, 0);
        }

        int selected = groupRowsTable.getSelectedRow();
        if (selected < 0) {
            selected = 0;
            groupRowsTable.setRowSelectionInterval(0, 0);
        }

        int modelIndex = groupRowsTable.convertRowIndexToModel(selected);
        SsccLabelGroup group = groupTableModel.groupAt(modelIndex);
        String zpl = new SsccLabelTemplate().render(group, modelIndex + 1, groups.size());
        previewDetailsLabel.setText(buildPreviewDetails(group, modelIndex + 1, groups.size()));
        previewArea.setText(zpl);
        previewArea.setCaretPosition(0);
    }

    private String buildPreviewDetails(SsccLabelGroup group, int labelNumber, int totalLabels) {
        return "Label " + labelNumber + " of " + totalLabels
                + " | SO " + group.salesOrder()
                + " | LPN " + group.newReceivedLpn()
                + " | " + group.bannerText();
    }

    private void openVisualPreview() {
        List<SsccLabelGroup> groups = groupTableModel.groups();
        if (groups.isEmpty()) {
            showError("Import or add at least one row first.");
            return;
        }

        int selected = groupRowsTable.getSelectedRow();
        if (selected < 0) {
            selected = 0;
        }
        int modelIndex = groupRowsTable.convertRowIndexToModel(selected);
        SsccLabelGroup group = groupTableModel.groupAt(modelIndex);
        String zpl = new SsccLabelTemplate().render(group, modelIndex + 1, groups.size());
        List<GuiZplPreviewSupport.PreviewDocument> documents = List.of(
                new GuiZplPreviewSupport.PreviewDocument(
                        safeFileName(group.salesOrder() + "_" + group.newReceivedLpn() + ".zpl"),
                        zpl
                )
        );
        ZplPreviewToolDialog.openWithDocuments(ownerFrame, "SSCC Label Preview", documents);
    }

    private void exportSelectedZpl() {
        List<SsccLabelGroup> groups = groupTableModel.groups();
        if (groups.isEmpty()) {
            showError("Import or add at least one row first.");
            return;
        }

        int selected = groupRowsTable.getSelectedRow();
        if (selected < 0) {
            showError("Select a grouped label first.");
            return;
        }

        Path outputDir = resolvePath(outputDirField.getText());
        if (outputDir == null) {
            showError("Choose an output directory first.");
            return;
        }

        try {
            Files.createDirectories(outputDir);
            int modelIndex = groupRowsTable.convertRowIndexToModel(selected);
            SsccLabelGroup group = groupTableModel.groupAt(modelIndex);
            Path out = outputDir.resolve(safeFileName(group.salesOrder() + "_" + group.newReceivedLpn() + ".zpl"));
            Files.writeString(out, new SsccLabelTemplate().render(group, modelIndex + 1, groups.size()));
            setStatus("Wrote 1 ZPL file to " + out.getParent());
        } catch (IOException ex) {
            showError("Failed to export ZPL: " + rootMessage(ex));
        }
    }

    private void exportAllZpl() {
        List<SsccLabelGroup> groups = groupTableModel.groups();
        if (groups.isEmpty()) {
            showError("Import or add at least one row first.");
            return;
        }

        Path outputDir = resolvePath(outputDirField.getText());
        if (outputDir == null) {
            showError("Choose an output directory first.");
            return;
        }

        try {
            exportGroups(groups, outputDir);
        } catch (IOException ex) {
            showError("Failed to export ZPL: " + rootMessage(ex));
        }
    }

    private void exportGroups(List<SsccLabelGroup> groups, Path outputDir) throws IOException {
        Files.createDirectories(outputDir);
        SsccLabelTemplate template = new SsccLabelTemplate();
        for (int i = 0; i < groups.size(); i++) {
            SsccLabelGroup group = groups.get(i);
            String fileName = safeFileName(group.salesOrder() + "_" + group.newReceivedLpn() + ".zpl");
            Path out = outputDir.resolve(fileName);
            Files.writeString(out, template.render(group, i + 1, groups.size()));
        }
        setStatus("Wrote " + groups.size() + " ZPL files to " + outputDir);
    }

    private void refreshGroupsAndPreview(boolean selectFirstGroup) {
        List<SsccLabelGroup> groups = new SsccLabelPlanner().groupRows(rawTableModel.rows());
        groupTableModel.setGroups(groups);
        updateSummary(groups);
        if (groups.isEmpty()) {
            previewDetailsLabel.setText("No grouped labels available yet.");
            previewArea.setText("");
            return;
        }
        if (selectFirstGroup || groupRowsTable.getSelectedRow() < 0) {
            groupRowsTable.setRowSelectionInterval(0, 0);
        }
        updatePreviewFromSelection(false);
    }

    private void updateSummary(List<SsccLabelGroup> groups) {
        long mixedCount = groups.stream().filter(SsccLabelGroup::mixedSku).count();
        double totalCases = groups.stream().mapToDouble(SsccLabelGroup::sumOfShipCases).sum();
        summaryLabel.setText(
                "Raw rows: " + rawTableModel.getRowCount()
                        + " | Labels: " + groups.size()
                        + " | Mixed SKU: " + mixedCount
                        + " | Cases: " + Math.round(totalCases)
        );
    }

    private void removeSelectedRows() {
        int[] selectedRows = rawRowsTable.getSelectedRows();
        if (selectedRows.length == 0) {
            return;
        }
        int[] modelRows = new int[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            modelRows[i] = rawRowsTable.convertRowIndexToModel(selectedRows[i]);
        }
        rawTableModel.removeRows(modelRows);
        refreshGroupsAndPreview(true);
        setStatus("Removed selected rows. Remaining raw rows: " + rawTableModel.getRowCount());
    }

    private void clearRows() {
        rawTableModel.clear();
        refreshGroupsAndPreview(false);
        setStatus("Cleared staged rows.");
    }

    private void clearEntryFields() {
        for (JTextField field : List.of(
                salesOrderField,
                purchaseOrderField,
                shipmentField,
                carrierCodeField,
                trailerIdField,
                destinationField,
                destinationAddressField,
                customerNameField,
                facilityField,
                itemField,
                level2ReferenceField,
                originallyShippedLpnField,
                sumOfShipCasesField,
                newReceivedLpnField
        )) {
            field.setText("");
        }
    }

    private void setStatus(String message) {
        statusLabel.setText(message);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "SSCC Labels", JOptionPane.ERROR_MESSAGE);
    }

    private String rootMessage(Throwable throwable) {
        return GuiExceptionMessageSupport.rootMessage(throwable);
    }

    private Path resolvePath(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return Paths.get(text.trim());
    }

    private String safeFileName(String value) {
        return value.replaceAll("[^A-Za-z0-9._-]", "_");
    }
}
