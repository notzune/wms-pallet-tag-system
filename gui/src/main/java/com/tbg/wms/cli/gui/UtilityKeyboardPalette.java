package com.tbg.wms.cli.gui;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

/**
 * Toggleable non-modal utility keyboard that emits system-wide keys.
 */
final class UtilityKeyboardPalette {

    private final GlobalKeyEmitter keyEmitter = new GlobalKeyEmitter();
    private final UtilityKeyboardLayoutSupport layoutSupport = new UtilityKeyboardLayoutSupport();
    private final MessageSink messageSink;
    private JDialog dialog;

    UtilityKeyboardPalette(MessageSink messageSink) {
        this.messageSink = Objects.requireNonNull(messageSink, "messageSink cannot be null");
    }

    void toggle(JFrame owner) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dispose();
            return;
        }
        dialog = buildDialog(owner);
        dialog.setVisible(true);
    }

    private JDialog buildDialog(JFrame owner) {
        JDialog palette = new JDialog(owner, "Utility Keyboard", Dialog.ModalityType.MODELESS);
        palette.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        palette.setAlwaysOnTop(true);
        palette.setFocusableWindowState(false);
        palette.setAutoRequestFocus(false);
        palette.setLayout(new BorderLayout(8, 8));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        for (java.util.List<UtilityKeyboardLayoutSupport.ButtonSpec> section : layoutSupport.sections()) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
            for (UtilityKeyboardLayoutSupport.ButtonSpec spec : section) {
                row.add(buildButton(spec));
            }
            content.add(row);
        }

        JLabel note = new JLabel("System-wide key sender. Keep the target field focused, then click a button here.");
        JPanel footer = new JPanel(new BorderLayout());
        footer.add(note, BorderLayout.CENTER);

        palette.add(content, BorderLayout.CENTER);
        palette.add(footer, BorderLayout.SOUTH);
        palette.pack();
        palette.setLocationRelativeTo(owner);
        return palette;
    }

    private JButton buildButton(UtilityKeyboardLayoutSupport.ButtonSpec spec) {
        JButton button = new JButton(spec.label());
        button.setFocusable(false);
        button.addActionListener(e -> trigger(spec));
        return button;
    }

    private void trigger(UtilityKeyboardLayoutSupport.ButtonSpec spec) {
        try {
            switch (spec.actionType()) {
                case KEY -> keyEmitter.pressKey(spec.keyCodes()[0]);
                case COMBO -> keyEmitter.pressCombo(spec.keyCodes());
                case CLEAR -> keyEmitter.clearFocusedField();
                default -> throw new IllegalStateException("Unsupported keyboard action: " + spec.actionType());
            }
        } catch (Exception ex) {
            messageSink.showError("Utility keyboard failed: " + GuiExceptionMessageSupport.rootMessage(ex));
        }
    }

    interface MessageSink {
        void showError(String message);
    }
}
