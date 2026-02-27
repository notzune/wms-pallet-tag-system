/**
 * Typed exception hierarchy for configuration, database, validation, and print failures.
 *
 * <p>Exceptions in this package map operational errors to deterministic user-facing exit codes.</p>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.exception.WmsException} - base checked exception for WMS workflows.</li>
 *   <li>{@link com.tbg.wms.core.exception.WmsConfigException} - configuration loading/validation failures.</li>
 *   <li>{@link com.tbg.wms.core.exception.WmsDbConnectivityException} - database connectivity and query issues.</li>
 *   <li>{@link com.tbg.wms.core.exception.WmsPrintException} - printer routing and transport failures.</li>
 * </ul>
 *
 * @since 1.3.1
 */
package com.tbg.wms.core.exception;
