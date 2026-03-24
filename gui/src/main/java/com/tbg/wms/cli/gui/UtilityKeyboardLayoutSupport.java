/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Defines the button layout for the utility keyboard palette.
 *
 * <p>The included actions are based on common keyboard-wedge/operator keys called out in
 * scanner guidance such as function-key mapping plus Enter/Tab style actions.</p>
 */
final class UtilityKeyboardLayoutSupport {

    List<List<ButtonSpec>> sections() {
        return List.of(
                List.of(
                        key("F1", KeyEvent.VK_F1), key("F2", KeyEvent.VK_F2), key("F3", KeyEvent.VK_F3),
                        key("F4", KeyEvent.VK_F4), key("F5", KeyEvent.VK_F5), key("F6", KeyEvent.VK_F6),
                        key("F7", KeyEvent.VK_F7), key("F8", KeyEvent.VK_F8), key("F9", KeyEvent.VK_F9),
                        key("F10", KeyEvent.VK_F10), key("F11", KeyEvent.VK_F11), key("F12", KeyEvent.VK_F12)
                ),
                List.of(
                        key("Tab", KeyEvent.VK_TAB),
                        key("Enter", KeyEvent.VK_ENTER),
                        key("Return", KeyEvent.VK_ENTER),
                        key("Esc", KeyEvent.VK_ESCAPE),
                        key("Space", KeyEvent.VK_SPACE),
                        key("Bksp", KeyEvent.VK_BACK_SPACE),
                        key("Delete", KeyEvent.VK_DELETE),
                        clear("Clear")
                ),
                List.of(
                        key("Home", KeyEvent.VK_HOME),
                        key("End", KeyEvent.VK_END),
                        key("PgUp", KeyEvent.VK_PAGE_UP),
                        key("PgDn", KeyEvent.VK_PAGE_DOWN),
                        key("Ins", KeyEvent.VK_INSERT),
                        key("Left", KeyEvent.VK_LEFT),
                        key("Up", KeyEvent.VK_UP),
                        key("Down", KeyEvent.VK_DOWN),
                        key("Right", KeyEvent.VK_RIGHT)
                ),
                List.of(
                        combo("Select All", KeyEvent.VK_CONTROL, KeyEvent.VK_A),
                        combo("Copy", KeyEvent.VK_CONTROL, KeyEvent.VK_C),
                        combo("Paste", KeyEvent.VK_CONTROL, KeyEvent.VK_V),
                        combo("Cut", KeyEvent.VK_CONTROL, KeyEvent.VK_X),
                        combo("Undo", KeyEvent.VK_CONTROL, KeyEvent.VK_Z),
                        combo("Redo", KeyEvent.VK_CONTROL, KeyEvent.VK_Y)
                )
        );
    }

    private static ButtonSpec key(String label, int keyCode) {
        return new ButtonSpec(label, ActionType.KEY, new int[]{keyCode});
    }

    private static ButtonSpec combo(String label, int... keyCodes) {
        return new ButtonSpec(label, ActionType.COMBO, keyCodes);
    }

    private static ButtonSpec clear(String label) {
        return new ButtonSpec(label, ActionType.CLEAR, new int[0]);
    }

    enum ActionType {
        KEY,
        COMBO,
        CLEAR
    }

    static final class ButtonSpec {
        private final String label;
        private final ActionType actionType;
        private final int[] keyCodes;

        ButtonSpec(String label, ActionType actionType, int[] keyCodes) {
            this.label = label;
            this.actionType = actionType;
            this.keyCodes = keyCodes;
        }

        String label() {
            return label;
        }

        ActionType actionType() {
            return actionType;
        }

        int[] keyCodes() {
            return keyCodes;
        }
    }
}
