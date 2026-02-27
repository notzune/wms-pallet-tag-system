/**
 * Oracle WMS data-access layer.
 *
 * <p>Provides read-only repository interfaces and implementations for shipment, stop, and footprint queries.</p>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.db.DbQueryRepository} - read-only query contract used by CLI/GUI workflows.</li>
 *   <li>{@link com.tbg.wms.db.OracleDbQueryRepository} - Oracle-backed implementation of query operations.</li>
 *   <li>{@link com.tbg.wms.db.DbConnectionPool} - HikariCP lifecycle and read-only datasource setup.</li>
 *   <li>{@link com.tbg.wms.db.DbConnectivityDiagnostics} - connection diagnostics and health checks.</li>
 * </ul>
 *
 * @since 1.3.1
 */
package com.tbg.wms.db;
