/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Core module for the WMS Pallet Tag System.
 *
 * Provides foundational services, configuration, exception handling, and domain models
 * used across all WMS system components.
 *
 * Subpackages:
 * <ul>
 *   <li>{@link com.tbg.wms.core.exception} - Typed exception hierarchy with exit codes
 *   <li>{@link com.tbg.wms.core.model} - Domain models and utility services
 * </ul>
 *
 * Key Services:
 * <ul>
 *   <li>{@link com.tbg.wms.core.AppConfig} - Configuration loading from .env and env vars
 *   <li>{@link com.tbg.wms.core.model.NormalizationService} - Data transformation
 *   <li>{@link com.tbg.wms.core.model.SnapshotService} - JSON persistence
 *   <li>{@link com.tbg.wms.core.db.DbConnectionPool} - Connection pool management
 *   <li>{@link com.tbg.wms.core.db.DbHealthService} - Connectivity validation
 * </ul>
 *
 * Module Dependencies:
 * <ul>
 *   <li>SLF4J API - Structured logging
 *   <li>Logback Classic - Logging implementation
 *   <li>Jackson Databind - JSON serialization
 *   <li>HikariCP - Connection pooling
 *   <li>dotenv-java - .env file support
 *   <li>JUnit Jupiter - Testing framework
 *   <li>Mockito - Test mocking
 * </ul>
 *
 * Architecture:
 * <pre>
 * core/
 *   ├── AppConfig - Configuration management
 *   ├── exception/ - Error handling
 *   │   ├── WmsException (base)
 *   │   ├── WmsConfigException
 *   │   └── WmsDbConnectivityException
 *   ├── model/ - Domain objects
 *   │   ├── Shipment, Lpn, LineItem
 *   │   ├── NormalizationService
 *   │   └── SnapshotService
 *   ├── db/ - Database access
 *   │   ├── DbConnectionPool
 *   │   ├── DbHealthService
 *   │   ├── DataSourceFactory
 *   │   └── (legacy components)
 *   └── resources/
 *       └── logback.xml
 * </pre>
 *
 * @author Zeyad Rashed
 * @version 1.0
 * @since 1.0.0
 */
package com.tbg.wms.core;

