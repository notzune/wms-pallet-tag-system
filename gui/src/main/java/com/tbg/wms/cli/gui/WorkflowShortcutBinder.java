/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

/**
 * Binds shared keyboard shortcuts for high-frequency GUI workflow actions.
 *
 * <p>This helper keeps accelerator wiring consistent across dialogs and avoids repeating the same
 * input-map/action-map boilerplate in each workflow window.</p>
 */
public final class WorkflowShortcutBinder {

    private WorkflowShortcutBinder() {
    }

    /**
     * Binds {@code Ctrl+F} to click the provided action button when it is enabled.
     *
     * @param rootPane  target root pane receiving the keystroke binding
     * @param button    button to invoke
     * @param actionKey unique action-map key for this binding
     */
    public static void bindPreviewShortcut(JRootPane rootPane, AbstractButton button, String actionKey) {
        Objects.requireNonNull(rootPane, "rootPane cannot be null");
        Objects.requireNonNull(button, "button cannot be null");
        Objects.requireNonNull(actionKey, "actionKey cannot be null");

        InputMap inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = rootPane.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK), actionKey);
        actionMap.put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if (!button.isEnabled()) {
                    return;
                }
                button.doClick(0);
            }
        });
    }
}
