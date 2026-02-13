/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.exception;

/**
 * Exception thrown when printing operations fail.
 *
 * Indicates issues with network printing such as:
 * - Printer unreachable
 * - Connection timeout
 * - Printer offline
 * - Network errors during send
 *
 * @since 1.0.0
 */
public class WmsPrintException extends WmsException {

    /**
     * Creates a new print exception.
     *
     * @param message error description
     * @param remediation suggested remediation steps
     */
    public WmsPrintException(String message, String remediation) {
        super(message, 5, remediation);
    }

    /**
     * Creates a new print exception with cause.
     *
     * @param message error description
     * @param cause underlying exception
     * @param remediation suggested remediation steps
     */
    public WmsPrintException(String message, Throwable cause, String remediation) {
        super(message, cause, 5, remediation);
    }
}

