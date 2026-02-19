/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Database access layer for WMS Oracle database operations.
 *
 * Provides repository pattern implementation for querying WMS data with
 * prepared statements, connection pooling, and error handling.
 *
 * Key Components:
 * <ul>
 *   <li>{@link com.tbg.wms.db.DbConnectionPool} - HikariCP connection pool wrapper</li>
 *   <li>{@link com.tbg.wms.db.DbQueryRepository} - Repository interface for queries</li>
 *   <li>{@link com.tbg.wms.db.OracleDbQueryRepository} - Oracle implementation</li>
 * </ul>
 *
 * Architecture Pattern:
 * <pre>
 * DbQueryRepository (interface)
 *   └── OracleDbQueryRepository (implementation)
 *       ├── findShipmentWithLpnsAndLineItems()
 *       ├── shipmentExists()
 *       └── getStagingLocation()
 * </pre>
 *
 * Usage Example:
 * <pre>
 * // Create connection pool
 * DbConnectionPool pool = new DbConnectionPool(dataSource);
 *
 * // Create repository
 * DbQueryRepository repo = new OracleDbQueryRepository(dataSource);
 *
 * // Query WMS data
 * Shipment shipment = repo.findShipmentWithLpnsAndLineItems("SHIP123");
 *
 * // Check staging location for routing
 * String location = repo.getStagingLocation("SHIP123");
 * </pre>
 *
 * Query Execution Flow:
 * <pre>
 * 1. findShipmentWithLpnsAndLineItems("SHIP123")
 *    ├── Fetch shipment header from wms_shipments
 *    ├── Enumerate LPNs from wms_lpns
 *    └── For each LPN, fetch line items from wms_line_items
 *
 * 2. Data Normalization
 *    ├── Apply NormalizationService to all fields
 *    └── Convert to domain models (Shipment, Lpn, LineItem)
 *
 * 3. Error Handling
 *    ├── SQLException → WmsDbConnectivityException
 *    └── Include remediation hints for user guidance
 * </pre>
 *
 * Connection Pool Configuration:
 * <ul>
 *   <li>Driver: Oracle JDBC 11+ (ojdbc11)
 *   <li>Pool: HikariCP with configurable max connections
 *   <li>Timeouts: Configurable via DB_POOL_* env vars
 *   <li>Prepared Statements: Prevent SQL injection
 * </ul>
 *
 * Schema Placeholders (Update when WMS schema confirmed):
 * <ul>
 *   <li>wms_shipments - Shipment header data
 *   <li>wms_lpns - Pallet data with staging location
 *   <li>wms_line_items - SKU/quantity details per pallet
 * </ul>
 *
 * Error Handling:
 * <ul>
 *   <li>Connection refused -> Check host, port, or VPN</li>
 *   <li>Service not found -> Check service name or SID</li>
 *   <li>Auth failed -> Check credentials</li>
 *   <li>Query failed -> Check schema and table names</li>
 * </ul>
 *
 * @author Zeyad Rashed
 * @version 1.1
 * @since 1.0.0
 */
package com.tbg.wms.db;
