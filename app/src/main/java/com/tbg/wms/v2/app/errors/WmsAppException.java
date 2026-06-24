package com.tbg.wms.v2.app.errors;

/**
 * Typed application-layer failure intended for operator-facing workflows.
 */
public final class WmsAppException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final String code;
    private final String operatorMessage;

    private WmsAppException(String code, String operatorMessage, Throwable cause) {
        super(operatorMessage, cause);
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("code is required");
        }
        if (operatorMessage == null || operatorMessage.isBlank()) {
            throw new IllegalArgumentException("operatorMessage is required");
        }
        this.code = code;
        this.operatorMessage = operatorMessage;
    }

    /**
     * Creates an exception for a missing or disabled printer.
     *
     * @return printer-not-found exception
     */
    public static WmsAppException printerNotFound(String printerId) {
        return new WmsAppException(
                "PRINTER_NOT_FOUND",
                "Printer not found or disabled: " + normalize(printerId, "UNKNOWN"),
                null
        );
    }

    /**
     * Creates an exception for a failed printer dispatch.
     *
     * @param printerId printer identifier that failed
     * @param cause underlying dispatch failure
     * @return print-dispatch-failed exception
     */
    public static WmsAppException printDispatchFailed(String printerId, Throwable cause) {
        return new WmsAppException(
                "PRINT_DISPATCH_FAILED",
                "Print dispatch failed for printer: " + normalize(printerId, "UNKNOWN"),
                cause
        );
    }

    /**
     * Creates an exception for an invalid operator request.
     *
     * @param message validation message to show the operator
     * @return validation-failed exception
     */
    public static WmsAppException validationFailed(String message) {
        return new WmsAppException("VALIDATION_FAILED", normalize(message, "Invalid request."), null);
    }

    /**
     * Returns the stable application error code.
     *
     * @return application error code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the message intended for operator-facing output.
     *
     * @return operator-facing error message
     */
    public String operatorMessage() {
        return operatorMessage;
    }

    private static String normalize(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
