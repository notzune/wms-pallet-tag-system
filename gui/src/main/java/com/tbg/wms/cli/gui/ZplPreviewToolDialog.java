package com.tbg.wms.cli.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Live ZPL preview tool using a real rendered image.
 */
final class ZplPreviewToolDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final JTextArea zplTextArea = new JTextArea(28, 56);
    private final JLabel previewLabel = new JLabel("Paste ZPL or open a .zpl file to preview.", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JComboBox<Integer> dpmmCombo = new JComboBox<>(new Integer[]{6, 8, 12, 24});
    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(4.0d, 0.1d, 20.0d, 0.1d));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(6.0d, 0.1d, 20.0d, 0.1d));
    private final JSpinner indexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
    private final JCheckBox liveCheck = new JCheckBox("Render live", true);
    private final Timer debounceTimer;
    private final ZplPreviewRenderService renderService = new ZplPreviewRenderService();
    private SwingWorker<BufferedImage, Void> currentWorker;
    private int renderGeneration;

    ZplPreviewToolDialog(JFrame owner) {
        super(owner, "ZPL Preview Tool", Dialog.ModalityType.MODELESS);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));

        dpmmCombo.setSelectedItem(8);
        new TextFieldClipboardController().install(zplTextArea);
        zplTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        JButton openButton = new JButton("Open ZPL...");
        JButton renderButton = new JButton("Render Now");
        JButton clearButton = new JButton("Clear");
        topBar.add(openButton);
        topBar.add(renderButton);
        topBar.add(clearButton);
        topBar.add(new JLabel("DPMM"));
        topBar.add(dpmmCombo);
        topBar.add(new JLabel("Width (in)"));
        topBar.add(widthSpinner);
        topBar.add(new JLabel("Height (in)"));
        topBar.add(heightSpinner);
        topBar.add(new JLabel("Label #"));
        topBar.add(indexSpinner);
        topBar.add(liveCheck);

        JScrollPane textScroll = new JScrollPane(zplTextArea);
        JScrollPane previewScroll = new JScrollPane(previewLabel);
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, textScroll, previewScroll);
        splitPane.setResizeWeight(0.5d);

        JPanel footer = new JPanel(new BorderLayout());
        footer.add(statusLabel, BorderLayout.CENTER);
        footer.add(new JLabel("Rendered via Labelary preview API. Network access required."), BorderLayout.EAST);

        add(topBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        debounceTimer = new Timer(350, e -> renderNow());
        debounceTimer.setRepeats(false);

        openButton.addActionListener(e -> openZplFile());
        renderButton.addActionListener(e -> renderNow());
        clearButton.addActionListener(e -> {
            zplTextArea.setText("");
            previewLabel.setIcon(null);
            previewLabel.setText("Paste ZPL or open a .zpl file to preview.");
            statusLabel.setText("Cleared");
        });

        DocumentListener changeListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                queueRender();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                queueRender();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                queueRender();
            }
        };
        zplTextArea.getDocument().addDocumentListener(changeListener);
        dpmmCombo.addActionListener(e -> queueRender());
        widthSpinner.addChangeListener(e -> queueRender());
        heightSpinner.addChangeListener(e -> queueRender());
        indexSpinner.addChangeListener(e -> queueRender());
        liveCheck.addActionListener(e -> queueRender());

        pack();
        setSize(1200, 760);
        setLocationRelativeTo(owner);
    }

    private void queueRender() {
        if (!liveCheck.isSelected()) {
            return;
        }
        debounceTimer.restart();
    }

    private void openZplFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Open ZPL File");
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        Path selected = chooser.getSelectedFile().toPath();
        try {
            zplTextArea.setText(Files.readString(selected));
            statusLabel.setText("Loaded " + selected.toAbsolutePath());
        } catch (Exception ex) {
            statusLabel.setText("Failed to load file");
            JOptionPane.showMessageDialog(this,
                    "Failed to open ZPL file: " + GuiExceptionMessageSupport.rootMessage(ex),
                    "ZPL Preview",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void renderNow() {
        String zpl = zplTextArea.getText();
        if (zpl == null || zpl.isBlank()) {
            previewLabel.setIcon(null);
            previewLabel.setText("Paste ZPL or open a .zpl file to preview.");
            statusLabel.setText("Waiting for ZPL input");
            return;
        }

        if (currentWorker != null && !currentWorker.isDone()) {
            currentWorker.cancel(true);
        }

        int generation = ++renderGeneration;
        int dpmm = (Integer) dpmmCombo.getSelectedItem();
        double width = ((Number) widthSpinner.getValue()).doubleValue();
        double height = ((Number) heightSpinner.getValue()).doubleValue();
        int index = ((Number) indexSpinner.getValue()).intValue();
        statusLabel.setText("Rendering preview...");

        currentWorker = new SwingWorker<>() {
            @Override
            protected BufferedImage doInBackground() throws Exception {
                return renderService.render(zpl, dpmm, width, height, index);
            }

            @Override
            protected void done() {
                if (generation != renderGeneration || isCancelled()) {
                    return;
                }
                try {
                    BufferedImage image = get();
                    previewLabel.setText("");
                    previewLabel.setIcon(new ImageIcon(image));
                    statusLabel.setText(String.format("Rendered %dx%d preview at %ddpmm", image.getWidth(), image.getHeight(), dpmm));
                } catch (Exception ex) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("Preview unavailable");
                    statusLabel.setText(GuiExceptionMessageSupport.rootMessage(ex));
                }
            }
        };
        currentWorker.execute();
    }
}
