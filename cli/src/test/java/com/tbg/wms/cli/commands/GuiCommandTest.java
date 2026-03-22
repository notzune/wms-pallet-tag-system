/*
 * Copyright (c) 2026 Tropicana Brands Group
 */

package com.tbg.wms.cli.commands;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class GuiCommandTest {

    @Test
    void callDelegatesToLauncherAndReturnsItsExitCode() {
        AtomicBoolean invoked = new AtomicBoolean(false);
        GuiCommand command = new GuiCommand(() -> {
            invoked.set(true);
            return 0;
        });

        int exitCode = command.call();

        assertEquals(0, exitCode);
        assertTrue(invoked.get());
    }
}
