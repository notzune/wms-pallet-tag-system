/*
 * Copyright © 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.exception;

/**
 * Exception thrown for configuration errors.
 *
 * <p>Exit code: 2</p>
 * <p>Examples:</p>
 * <ul>
 *   <li>Missing required environment variable</li>
 *   <li>Invalid YAML configuration format</li>
 *   <li>Invalid printer ID in routing config</li>
 *   <li>Invalid configuration value (e.g., negative pool size)</li>
 * </ul>
 */
public final class WmsConfigException extends WmsException {

    /**
     * Creates a new configuration exception.
     *
     * @param message the error message
     * @param remediationHint actionable hint (e.g., "Set ORACLE_PASSWORD environment variable")
     */
    public WmsConfigException(String message, String remediationHint) {
        super(message, 2, remediationHint);
    }

    /**
     * Creates a new configuration exception with cause.
     *
     * @param message the error message
     * @param cause the underlying exception
     * @param remediationHint actionable hint
     */
    public WmsConfigException(String message, Throwable cause, String remediationHint) {
        super(message, cause, 2, remediationHint);
    }
}

