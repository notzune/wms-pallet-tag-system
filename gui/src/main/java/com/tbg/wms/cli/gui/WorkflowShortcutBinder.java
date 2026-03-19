/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Objects;

public final class WorkflowShortcutBinder {

    private WorkflowShortcutBinder() {
    }

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
