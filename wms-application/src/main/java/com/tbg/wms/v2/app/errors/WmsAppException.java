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

    public static WmsAppException printerNotFound(String printerId) {
        return new WmsAppException(
                "PRINTER_NOT_FOUND",
                "Printer not found or disabled: " + normalize(printerId, "UNKNOWN"),
                null
        );
    }

    public static WmsAppException printDispatchFailed(String printerId, Throwable cause) {
        return new WmsAppException(
                "PRINT_DISPATCH_FAILED",
                "Print dispatch failed for printer: " + normalize(printerId, "UNKNOWN"),
                cause
        );
    }

    public static WmsAppException validationFailed(String message) {
        return new WmsAppException("VALIDATION_FAILED", normalize(message, "Invalid request."), null);
    }

    public String code() {
        return code;
    }

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
