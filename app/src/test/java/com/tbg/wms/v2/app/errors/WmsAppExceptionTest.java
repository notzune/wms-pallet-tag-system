package com.tbg.wms.v2.app.errors;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class WmsAppExceptionTest {
    @Test
    void appException_exposesCodeAndOperatorMessage() {
        WmsAppException ex = WmsAppException.printerNotFound("P1");

        assertEquals("PRINTER_NOT_FOUND", ex.code());
        assertEquals("Printer not found or disabled: P1", ex.operatorMessage());
        assertEquals("Printer not found or disabled: P1", ex.getMessage());
    }

    @Test
    void appException_preservesCause() {
        RuntimeException cause = new RuntimeException("socket closed");

        WmsAppException ex = WmsAppException.printDispatchFailed("P1", cause);

        assertEquals("PRINT_DISPATCH_FAILED", ex.code());
        assertEquals("Print dispatch failed for printer: P1", ex.operatorMessage());
        assertSame(cause, ex.getCause());
    }
}
