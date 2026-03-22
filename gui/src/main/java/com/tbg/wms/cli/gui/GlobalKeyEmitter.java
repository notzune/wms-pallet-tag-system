package com.tbg.wms.cli.gui;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.util.Objects;

/**
 * Emits system-wide key presses and common editing combos through {@link Robot}.
 *
 * <p>The robot is created lazily so normal GUI construction and unit tests do not require
 * an immediately available desktop session.</p>
 */
final class GlobalKeyEmitter {

    private Robot robot;

    synchronized void pressKey(int keyCode) throws AWTException {
        Robot activeRobot = robot();
        activeRobot.keyPress(keyCode);
        activeRobot.keyRelease(keyCode);
    }

    synchronized void pressCombo(int... keyCodes) throws AWTException {
        Objects.requireNonNull(keyCodes, "keyCodes cannot be null");
        Robot activeRobot = robot();
        for (int keyCode : keyCodes) {
            activeRobot.keyPress(keyCode);
        }
        for (int i = keyCodes.length - 1; i >= 0; i--) {
            activeRobot.keyRelease(keyCodes[i]);
        }
    }

    synchronized void clearFocusedField() throws AWTException {
        pressCombo(java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_A);
        pressKey(java.awt.event.KeyEvent.VK_DELETE);
    }

    synchronized void pasteText(String text) throws Exception {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Object previousContents = clipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
                ? clipboard.getData(DataFlavor.stringFlavor)
                : null;
        clipboard.setContents(new StringSelection(text), null);
        try {
            pressCombo(java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.VK_V);
        } finally {
            if (previousContents instanceof String previousText) {
                clipboard.setContents(new StringSelection(previousText), null);
            }
        }
    }

    private Robot robot() throws AWTException {
        if (robot == null) {
            robot = new Robot();
            robot.setAutoDelay(20);
        }
        return robot;
    }
}
