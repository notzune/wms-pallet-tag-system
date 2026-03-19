/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkflowShortcutBinderTest {

    @Test
    void testCtrlFInvokesButtonClickWhenEnabled() {
        JRootPane rootPane = new JRootPane();
        JButton button = new JButton("Preview");
        AtomicInteger clicks = new AtomicInteger();
        button.addActionListener(event -> clicks.incrementAndGet());

        WorkflowShortcutBinder.bindPreviewShortcut(rootPane, button, "preview");

        Action action = rootPane.getActionMap().get("preview");
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(rootPane, ActionEvent.ACTION_PERFORMED, "preview"));

        assertEquals(1, clicks.get());
    }

    @Test
    void testCtrlFNoOpsWhenButtonDisabled() {
        JRootPane rootPane = new JRootPane();
        JButton button = new JButton("Preview");
        AtomicInteger clicks = new AtomicInteger();
        button.addActionListener(event -> clicks.incrementAndGet());
        button.setEnabled(false);

        WorkflowShortcutBinder.bindPreviewShortcut(rootPane, button, "preview");

        Action action = rootPane.getActionMap().get("preview");
        assertNotNull(action);
        action.actionPerformed(new ActionEvent(rootPane, ActionEvent.ACTION_PERFORMED, "preview"));

        assertEquals(0, clicks.get());
    }
}
