/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Live ZPL preview tool using a real rendered image.
 *
 * <p>The dialog intentionally separates live-edit debounce/throttle behavior from remote render
 * transport so the preview remains operator-friendly without overrunning the external render API.</p>
 */
final class ZplPreviewToolDialog extends JDialog {
    private static final long serialVersionUID = 1L;
    private static final int LIVE_RENDER_DEBOUNCE_MS = 350;
    private static final int MIN_RENDER_INTERVAL_MS = 1000;

    private final JTextArea zplTextArea = new JTextArea(28, 56);
    private final JLabel previewLabel = new JLabel("Paste ZPL or open a .zpl file to preview.", SwingConstants.CENTER);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JComboBox<Integer> dpmmCombo = new JComboBox<>(new Integer[]{6, 8, 12, 24});
    private final JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(4.0d, 0.1d, 20.0d, 0.1d));
    private final JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(6.0d, 0.1d, 20.0d, 0.1d));
    private final JSpinner indexSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 50, 1));
    private final JCheckBox liveCheck = new JCheckBox("Render live", true);
    private final Timer debounceTimer;
    private final Timer throttleTimer;
    private final ZplPreviewRenderService renderService = new ZplPreviewRenderService();
    private SwingWorker<BufferedImage, Void> currentWorker;
    private int renderGeneration;
    private long lastRenderStartedAtMs;
    private boolean pendingLiveRender;

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

        debounceTimer = new Timer(LIVE_RENDER_DEBOUNCE_MS, e -> scheduleRender(false));
        debounceTimer.setRepeats(false);
        throttleTimer = new Timer(MIN_RENDER_INTERVAL_MS, null);
        throttleTimer.setRepeats(false);
        throttleTimer.addActionListener(e -> {
            throttleTimer.stop();
            if (pendingLiveRender) {
                pendingLiveRender = false;
                renderNow();
            }
        });

        openButton.addActionListener(e -> openZplFile());
        renderButton.addActionListener(e -> scheduleRender(true));
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
        installRenderKeyBinding();
        dpmmCombo.addActionListener(e -> queueRender());
        widthSpinner.addChangeListener(e -> queueRender());
        heightSpinner.addChangeListener(e -> queueRender());
        indexSpinner.addChangeListener(e -> queueRender());
        liveCheck.addActionListener(e -> queueRender());

        pack();
        setSize(1200, 760);
        setLocationRelativeTo(owner);
    }

    private void installRenderKeyBinding() {
        InputMap inputMap = zplTextArea.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap actionMap = zplTextArea.getActionMap();

        KeyStroke enterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        KeyStroke shiftEnterKey = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        Object defaultEnterAction = inputMap.get(enterKey);
        Object defaultShiftEnterAction = inputMap.get(shiftEnterKey);

        inputMap.put(enterKey, "render-preview-now");
        if (defaultEnterAction != null) {
            inputMap.put(shiftEnterKey, defaultEnterAction);
        }

        actionMap.put("render-preview-now", new AbstractAction() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                scheduleRender(true);
            }
        });

        if (defaultShiftEnterAction != null) {
            actionMap.put("insert-newline-shift", actionMap.get(defaultShiftEnterAction));
            inputMap.put(shiftEnterKey, "insert-newline-shift");
        }
    }

    private void queueRender() {
        if (!liveCheck.isSelected()) {
            return;
        }
        pendingLiveRender = true;
        statusLabel.setText("Preview changed. Waiting to render...");
        debounceTimer.restart();
    }

    private void scheduleRender(boolean forceImmediate) {
        if (forceImmediate) {
            pendingLiveRender = false;
            debounceTimer.stop();
            throttleTimer.stop();
            renderNow();
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - lastRenderStartedAtMs;
        if (elapsed >= MIN_RENDER_INTERVAL_MS && (currentWorker == null || currentWorker.isDone())) {
            pendingLiveRender = false;
            renderNow();
            return;
        }

        pendingLiveRender = true;
        int delay = (int) Math.max(1L, MIN_RENDER_INTERVAL_MS - Math.max(0L, elapsed));
        throttleTimer.setInitialDelay(delay);
        throttleTimer.restart();
        statusLabel.setText("Preview changed. Render queued...");
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
            pendingLiveRender = false;
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
        lastRenderStartedAtMs = System.currentTimeMillis();
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
                    if (pendingLiveRender) {
                        scheduleRender(false);
                    }
                } catch (Exception ex) {
                    previewLabel.setIcon(null);
                    previewLabel.setText("Preview unavailable");
                    statusLabel.setText(GuiExceptionMessageSupport.rootMessage(ex));
                    if (pendingLiveRender) {
                        scheduleRender(false);
                    }
                }
            }
        };
        currentWorker.execute();
    }
}
