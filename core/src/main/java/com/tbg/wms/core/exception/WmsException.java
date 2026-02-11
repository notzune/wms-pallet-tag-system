/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.exception;

/**
 * Base exception for WMS system errors.
 *
 * <p>All WMS exceptions inherit from this to allow uniform error handling
 * and mapping to CLI exit codes.</p>
 */
public abstract class WmsException extends RuntimeException {

    private final int exitCode;
    private final String remediationHint;

    /**
     * Creates a new WMS exception.
     *
     * @param message the error message
     * @param exitCode the recommended CLI exit code (2 for user error, 3+ for system error)
     * @param remediationHint an actionable hint for resolving the error
     */
    protected WmsException(String message, int exitCode, String remediationHint) {
        super(message);
        this.exitCode = exitCode;
        this.remediationHint = remediationHint;
    }

    /**
     * Creates a new WMS exception with cause.
     *
     * @param message the error message
     * @param cause the underlying exception
     * @param exitCode the recommended CLI exit code
     * @param remediationHint an actionable hint for resolving the error
     */
    protected WmsException(String message, Throwable cause, int exitCode, String remediationHint) {
        super(message, cause);
        this.exitCode = exitCode;
        this.remediationHint = remediationHint;
    }

    /**
     * Returns the recommended CLI exit code.
     *
     * @return exit code (2, 3, 4, 5, 6, or 10)
     */
    public int getExitCode() {
        return exitCode;
    }

    /**
     * Returns an actionable hint for resolving the error.
     *
     * @return remediation hint or null if not applicable
     */
    public String getRemediationHint() {
        return remediationHint;
    }
}

