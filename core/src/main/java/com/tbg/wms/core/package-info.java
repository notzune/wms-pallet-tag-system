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
 *   <li>{@link com.tbg.wms.core.exception} - Typed exception hierarchy with exit codes</li>
 *   <li>{@link com.tbg.wms.core.model} - Domain models and utility services</li>
 *   <li>{@link com.tbg.wms.core.label} - Label data mapping and enrichment</li>
 *   <li>{@link com.tbg.wms.core.sku} - SKU mapping and lookup utilities</li>
 *   <li>{@link com.tbg.wms.core.template} - ZPL template parsing and generation</li>
 *   <li>{@link com.tbg.wms.core.print} - Printer routing and network printing</li>
 *   <li>{@link com.tbg.wms.core.db} - Connection pooling and diagnostics</li>
 * </ul>
 *
 * Key Services:
 * <ul>
 *   <li>{@link com.tbg.wms.core.AppConfig} - Configuration loading from .env and env vars</li>
 *   <li>{@link com.tbg.wms.core.model.NormalizationService} - Data transformation</li>
 *   <li>{@link com.tbg.wms.core.model.SnapshotService} - JSON persistence</li>
 *   <li>{@link com.tbg.wms.core.model.PalletPlanningService} - Pallet planning summary</li>
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
 *   AppConfig - Configuration management
 *   exception/ - Error handling
 *   model/ - Domain objects and planning services
 *   label/ - Label data mapping
 *   sku/ - SKU lookup
 *   template/ - ZPL template engine
 *   print/ - Routing and printing
 *   db/ - Connection pool utilities
 * </pre>
 *
 * @author Zeyad Rashed
 * @version 1.1
 * @since 1.0.0
 */
package com.tbg.wms.core;
