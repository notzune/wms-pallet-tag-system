/**
 * Typed exception hierarchy for configuration, database, validation, and print failures.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Provide semantically meaningful failure categories.</li>
 *   <li>Map operational failures to deterministic user-facing outcomes.</li>
 *   <li>Preserve cause chains and remediation context for diagnostics.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.exception.WmsException} - base checked exception for WMS workflows.</li>
 *   <li>{@link com.tbg.wms.core.exception.WmsConfigException} - configuration loading/validation failures.</li>
 *   <li>{@link com.tbg.wms.core.exception.WmsDbConnectivityException} - database connectivity and query issues.</li>
 *   <li>{@link com.tbg.wms.core.exception.WmsPrintException} - printer routing and transport failures.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core.exception;
