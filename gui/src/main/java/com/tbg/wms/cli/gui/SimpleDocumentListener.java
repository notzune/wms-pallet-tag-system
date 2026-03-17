package com.tbg.wms.cli.gui;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.Objects;

/**
 * Small adapter to reduce boilerplate around Swing document change callbacks.
 */
@FunctionalInterface
interface SimpleDocumentListener extends DocumentListener {
    void onChange();

    @Override
    default void insertUpdate(DocumentEvent e) {
        onChange();
    }

    @Override
    default void removeUpdate(DocumentEvent e) {
        onChange();
    }

    @Override
    default void changedUpdate(DocumentEvent e) {
        onChange();
    }

    static SimpleDocumentListener of(Runnable action) {
        Objects.requireNonNull(action, "action cannot be null");
        return action::run;
    }
}
