package com.tbg.wms.cli.gui;

import java.util.Objects;

/**
 * Shared GUI-facing exception message extraction.
 *
 * <p>Dialogs and frames should present the deepest actionable message consistently instead of
 * each component rolling its own root-cause traversal.</p>
 */
public final class GuiExceptionMessageSupport {

    private GuiExceptionMessageSupport() {
        // Utility class.
    }

    public static String rootMessage(Throwable throwable) {
        Throwable cursor = Objects.requireNonNullElse(throwable, new RuntimeException("Unknown error"));
        while (cursor.getCause() != null) {
            cursor = cursor.getCause();
        }
        String message = cursor.getMessage();
        return (message == null || message.isBlank()) ? cursor.getClass().getSimpleName() : message;
    }
}
