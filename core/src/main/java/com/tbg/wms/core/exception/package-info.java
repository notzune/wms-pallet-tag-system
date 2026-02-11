/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Typed exception hierarchy for WMS system error handling.
 *
 * This package provides a structured exception hierarchy enabling precise
 * error classification and user-friendly remediation guidance.
 *
 * Exception Hierarchy:
 * <pre>
 * Exception
 *   └── RuntimeException
 *       └── {@link com.tbg.wms.core.exception.WmsException}
 *           ├── {@link com.tbg.wms.core.exception.WmsConfigException}
 *           └── {@link com.tbg.wms.core.exception.WmsDbConnectivityException}
 * </pre>
 *
 * Key Features:
 * <ul>
 *   <li>Exit code mapping for CLI error reporting (0, 2, 3, 10)
 *   <li>Remediation hints for user guidance (e.g., "Check DB_HOST and VPN")
 *   <li>Root cause exception chaining for debugging
 *   <li>Message templates with context information
 * </ul>
 *
 * Usage Examples:
 * <pre>
 * // Configuration error (exit code 2)
 * throw new WmsConfigException("Missing DB_HOST", "Set DB_HOST env var or .env file");
 *
 * // Database connectivity error (exit code 3)
 * throw new WmsDbConnectivityException(
 *     "Connection refused",
 *     cause,
 *     "Verify DB_HOST, port, and VPN connectivity"
 * );
 *
 * // Catch and handle by exit code
 * try {
 *     // WMS operation
 * } catch (WmsException e) {
 *     System.exit(e.getExitCode());
 * }
 * </pre>
 *
 * Exit Codes:
 * <ul>
 *   <li>0 - Success
 *   <li>2 - Configuration error (use WmsConfigException)
 *   <li>3 - Database connectivity error (use WmsDbConnectivityException)
 *   <li>10 - Unexpected internal error (use WmsException)
 * </ul>
 *
 * @author Zeyad Rashed
 * @version 1.0
 * @since 1.0.0
 */
package com.tbg.wms.core.exception;

