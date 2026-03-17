package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * GUI editor for non-secret runtime configuration files stored next to the application.
 */
final class AdvancedSettingsDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JComboBox<EditableConfigFile> fileCombo = new JComboBox<>();
    private final JTextArea editorArea = new JTextArea(28, 90);
    private final JLabel pathLabel = new JLabel(" ");
    private final transient Runnable onSave;
    private final transient java.util.function.Consumer<String> showError;
    private final transient java.util.function.Consumer<JTextComponent[]> installClipboardBehavior;

    private transient EditableConfigFile currentFile;
    private boolean dirty;

    AdvancedSettingsDialog(
            Frame owner,
            AppConfig config,
            Runnable onSave,
            java.util.function.Consumer<String> showError,
            java.util.function.Consumer<JTextComponent[]> installClipboardBehavior
    ) {
        super(owner, "Advanced Settings", true);
        this.onSave = Objects.requireNonNull(onSave, "onSave cannot be null");
        this.showError = Objects.requireNonNull(showError, "showError cannot be null");
        this.installClipboardBehavior = Objects.requireNonNull(installClipboardBehavior, "installClipboardBehavior cannot be null");

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        List<EditableConfigFile> editableFiles = buildEditableFiles(config);
        DefaultComboBoxModel<EditableConfigFile> model = new DefaultComboBoxModel<>();
        for (EditableConfigFile editableFile : editableFiles) {
            model.addElement(editableFile);
        }
        fileCombo.setModel(model);

        JPanel top = new JPanel(new BorderLayout(8, 8));
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        selector.add(new JLabel("Config file:"));
        selector.add(fileCombo);
        JButton reloadButton = new JButton("Reload");
        JButton openFolderButton = new JButton("Open Config Folder");
        selector.add(reloadButton);
        selector.add(openFolderButton);
        top.add(selector, BorderLayout.NORTH);
        top.add(new JLabel("Advanced settings edit runtime YAML/CSV/ZPL config files only. Env values remain root-only."), BorderLayout.CENTER);
        top.add(pathLabel, BorderLayout.SOUTH);

        editorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        installClipboardBehavior.accept(new JTextComponent[]{editorArea});

        JButton saveButton = new JButton("Save");
        JButton closeButton = new JButton("Close");
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(saveButton);
        buttons.add(closeButton);

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(editorArea), BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);

        fileCombo.addActionListener(e -> selectFile((EditableConfigFile) fileCombo.getSelectedItem()));
        reloadButton.addActionListener(e -> reloadCurrentFile());
        openFolderButton.addActionListener(e -> openConfigFolder());
        saveButton.addActionListener(e -> saveCurrentFile());
        closeButton.addActionListener(e -> {
            if (confirmDiscardIfDirty()) {
                dispose();
            }
        });
        editorArea.getDocument().addDocumentListener(SimpleDocumentListener.of(() -> dirty = true));

        if (model.getSize() > 0) {
            fileCombo.setSelectedIndex(0);
            selectFile((EditableConfigFile) model.getSelectedItem());
        }

        pack();
        setLocationRelativeTo(owner);
    }

    private List<EditableConfigFile> buildEditableFiles(AppConfig config) {
        String siteCode = config.activeSiteCode();
        Path configBaseDir = RuntimePathResolver.resolveWorkingDirOrJarSiblingDir(AdvancedSettingsDialog.class, "config");
        List<EditableConfigFile> files = new ArrayList<>();
        files.add(new EditableConfigFile("Printers", configBaseDir.resolve(siteCode).resolve("printers.yaml")));
        files.add(new EditableConfigFile("Printer Routing", configBaseDir.resolve(siteCode).resolve("printer-routing.yaml")));
        files.add(new EditableConfigFile("SKU Matrix", configBaseDir.resolve("walmart-sku-matrix.csv")));
        files.add(new EditableConfigFile("Location Matrix", configBaseDir.resolve("walm_loc_num_matrix.csv")));
        files.add(new EditableConfigFile("Label Template", configBaseDir.resolve("templates").resolve("walmart-canada-label.zpl")));
        return files;
    }

    private void selectFile(EditableConfigFile selected) {
        if (selected == null) {
            return;
        }
        if (!confirmDiscardIfDirty()) {
            if (currentFile != null) {
                fileCombo.setSelectedItem(currentFile);
            }
            return;
        }
        currentFile = selected;
        reloadCurrentFile();
    }

    private void reloadCurrentFile() {
        if (currentFile == null) {
            return;
        }
        try {
            if (!Files.exists(currentFile.path())) {
                Files.createDirectories(currentFile.path().getParent());
                Files.createFile(currentFile.path());
            }
            editorArea.setText(Files.readString(currentFile.path(), StandardCharsets.UTF_8));
            editorArea.setCaretPosition(0);
            pathLabel.setText(currentFile.path().toAbsolutePath().toString());
            dirty = false;
        } catch (IOException ex) {
            showError.accept("Failed to load config file: " + ex.getMessage());
        }
    }

    private void saveCurrentFile() {
        if (currentFile == null) {
            return;
        }
        try {
            Files.createDirectories(currentFile.path().getParent());
            Files.writeString(currentFile.path(), editorArea.getText(), StandardCharsets.UTF_8);
            dirty = false;
            onSave.run();
            JOptionPane.showMessageDialog(this,
                    "Saved " + currentFile.label() + ".",
                    "Advanced Settings",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            showError.accept("Failed to save config file: " + ex.getMessage());
        }
    }

    private void openConfigFolder() {
        if (currentFile == null) {
            return;
        }
        try {
            Desktop.getDesktop().open(currentFile.path().getParent().toFile());
        } catch (Exception ex) {
            showError.accept("Failed to open config folder: " + ex.getMessage());
        }
    }

    private boolean confirmDiscardIfDirty() {
        if (!dirty) {
            return true;
        }
        int choice = JOptionPane.showConfirmDialog(
                this,
                "Discard unsaved changes?",
                "Advanced Settings",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }

    private record EditableConfigFile(String label, Path path) {
        @Override
        public String toString() {
            return label;
        }
    }
}
