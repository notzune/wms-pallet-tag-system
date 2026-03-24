package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GuiExceptionMessageSupportTest {

    @Test
    void rootMessageShouldReturnDeepestNonBlankMessage() {
        RuntimeException throwable = new RuntimeException("top",
                new IllegalStateException("middle",
                        new IllegalArgumentException("deepest")));

        assertEquals("deepest", GuiExceptionMessageSupport.rootMessage(throwable));
    }

    @Test
    void rootMessageShouldFallbackToTypeWhenMessageBlank() {
        RuntimeException throwable = new RuntimeException("top", new IllegalStateException(" "));

        assertEquals("IllegalStateException", GuiExceptionMessageSupport.rootMessage(throwable));
    }
}
