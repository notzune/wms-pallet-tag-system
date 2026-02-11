/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WMS exception hierarchy.
 */
class WmsExceptionTest {

    @Test
    void testWmsConfigException() {
        String message = "Missing required env var";
        String hint = "Set ORACLE_PASSWORD environment variable";

        WmsConfigException ex = new WmsConfigException(message, hint);

        assertEquals(message, ex.getMessage());
        assertEquals(hint, ex.getRemediationHint());
        assertEquals(2, ex.getExitCode());
    }

    @Test
    void testWmsDbConnectivityException() {
        String message = "Connection refused";
        String hint = "Check DB host and port";

        WmsDbConnectivityException ex = new WmsDbConnectivityException(message, hint);

        assertEquals(message, ex.getMessage());
        assertEquals(hint, ex.getRemediationHint());
        assertEquals(3, ex.getExitCode());
    }

    @Test
    void testWmsExceptionWithCause() {
        String message = "Wrapped exception";
        Throwable cause = new RuntimeException("Underlying error");
        String hint = "Fix the underlying issue";

        WmsConfigException ex = new WmsConfigException(message, cause, hint);

        assertEquals(message, ex.getMessage());
        assertEquals(cause, ex.getCause());
        assertEquals(hint, ex.getRemediationHint());
        assertEquals(2, ex.getExitCode());
    }
}

