/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.exception;

/**
 * Exception thrown when database connectivity fails.
 *
 * <p>Exit code: 3</p>
 * <p>Examples:</p>
 * <ul>
 *   <li>Host unreachable (DNS resolution failure)</li>
 *   <li>Connection timeout (port not open)</li>
 *   <li>Authentication failure (invalid credentials)</li>
 *   <li>Service/SID not found</li>
 * </ul>
 */
public final class WmsDbConnectivityException extends WmsException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new connectivity exception.
     *
     * @param message the error message
     * @param remediationHint actionable hint (e.g., "Check DB_HOST and VPN connectivity")
     */
    public WmsDbConnectivityException(String message, String remediationHint) {
        super(message, 3, remediationHint);
    }

    /**
     * Creates a new connectivity exception with cause.
     *
     * @param message the error message
     * @param cause the underlying exception
     * @param remediationHint actionable hint
     */
    public WmsDbConnectivityException(String message, Throwable cause, String remediationHint) {
        super(message, cause, 3, remediationHint);
    }
}

