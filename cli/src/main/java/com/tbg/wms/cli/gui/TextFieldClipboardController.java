/*
 * Copyright (c) 2026 Tropicana Brands Group
 */

package com.tbg.wms.cli.gui;

import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Handles terminal-like right-click copy/paste behavior for text components.
 *
 * <p>Behavior:
 * <ul>
 *   <li>Right-click with selected text: copy selection</li>
 *   <li>Right-click without selection: paste at click position</li>
 *   <li>Second right-click within cooldown window: ignored</li>
 * </ul>
 */
final class TextFieldClipboardController {

    private static final String RIGHT_CLICK_COOLDOWN_PROPERTY = "wms.tags.rightClickCooldownMs";
    private static final String RIGHT_CLICK_COOLDOWN_ENV = "RIGHT_CLICK_COOLDOWN_MS";
    private static final String LEGACY_RIGHT_CLICK_COOLDOWN_ENV = "WMS_TAGS_RIGHT_CLICK_COOLDOWN_MS";
    private static final long DEFAULT_RIGHT_CLICK_COOLDOWN_MS = 250L;

    private final long rightClickCooldownMs;
    private final Map<JTextComponent, Long> lastRightClickClipboardActionMs;

    TextFieldClipboardController() {
        this.rightClickCooldownMs = resolveRightClickCooldownMs();
        this.lastRightClickClipboardActionMs = new WeakHashMap<>();
    }

    /**
     * Installs clipboard behavior on provided text components.
     *
     * @param fields text components to bind
     */
    void install(JTextComponent... fields) {
        for (JTextComponent field : fields) {
            if (field == null) {
                continue;
            }
            field.setComponentPopupMenu(null);
            field.addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleRightClickClipboardAction(e, field);
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    handleRightClickClipboardAction(e, field);
                }
            });
        }
    }

    private void handleRightClickClipboardAction(MouseEvent event, JTextComponent field) {
        if (!(event.isPopupTrigger() || SwingUtilities.isRightMouseButton(event))) {
            return;
        }
        if (isWithinRightClickCooldown(field)) {
            event.consume();
            return;
        }

        String selection = field.getSelectedText();
        if (selection != null && !selection.isEmpty()) {
            field.copy();
            markRightClickAction(field);
            event.consume();
            return;
        }

        int position = field.viewToModel2D(event.getPoint());
        if (position >= 0) {
            field.setCaretPosition(position);
        }
        field.paste();
        markRightClickAction(field);
        event.consume();
    }

    private boolean isWithinRightClickCooldown(JTextComponent field) {
        Long last = lastRightClickClipboardActionMs.get(field);
        if (last == null || rightClickCooldownMs <= 0) {
            return false;
        }
        long elapsed = System.currentTimeMillis() - last;
        return elapsed >= 0 && elapsed < rightClickCooldownMs;
    }

    private void markRightClickAction(JTextComponent field) {
        lastRightClickClipboardActionMs.put(field, System.currentTimeMillis());
    }

    private long resolveRightClickCooldownMs() {
        String propertyValue = System.getProperty(RIGHT_CLICK_COOLDOWN_PROPERTY);
        Long parsed = tryParsePositiveLong(propertyValue);
        if (parsed != null) {
            return parsed;
        }
        String envValue = System.getenv(RIGHT_CLICK_COOLDOWN_ENV);
        parsed = tryParsePositiveLong(envValue);
        if (parsed != null) {
            return parsed;
        }
        envValue = System.getenv(LEGACY_RIGHT_CLICK_COOLDOWN_ENV);
        parsed = tryParsePositiveLong(envValue);
        return parsed == null ? DEFAULT_RIGHT_CLICK_COOLDOWN_MS : parsed;
    }

    private static Long tryParsePositiveLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed < 0 ? null : parsed;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
