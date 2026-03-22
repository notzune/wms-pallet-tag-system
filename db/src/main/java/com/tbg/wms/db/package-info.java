/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
/**
 * Oracle WMS data-access layer.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Provide read-only Oracle WMS query adapters.</li>
 *   <li>Encapsulate connection-pool lifecycle and connectivity diagnostics.</li>
 *   <li>Isolate SQL/schema probing concerns from core planning services and UI layers.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.db.DbQueryRepository} - read-only query contract used by CLI/GUI workflows.</li>
 *   <li>{@link com.tbg.wms.db.OracleDbQueryRepository} - Oracle-backed implementation of query operations.</li>
 *   <li>{@link com.tbg.wms.db.DbConnectionPool} - HikariCP lifecycle and read-only datasource setup.</li>
 *   <li>{@link com.tbg.wms.db.DbConnectivityDiagnostics} - connection diagnostics and health checks.</li>
 *   <li>Rail family normalization honors explicit WMS override flags (for example, UC_PARS_FLG=1 implies CAN).</li>
 * </ul>
 *
 * <p><strong>Internal Helper Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.db.DescriptionTextHeuristics} - centralized text-quality policy so description
 *       filtering stays consistent across all lookup branches.</li>
 *   <li>{@link com.tbg.wms.db.PrtmstDescriptionColumnResolver} - cached PRTMST description-column discovery,
 *       separated from repository SQL orchestration for SRP and predictable lookup performance.</li>
 *   <li>Shipment line-items are loaded in one shipment-scoped query and grouped by LPN to avoid N+1 database probes.</li>
 *   <li>Shipment LPN rows are coalesced after the inventory-detail join so mixed-lot pallets cannot create duplicate labels.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.db;
