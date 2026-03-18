/**
 * Database-adjacent core abstractions that remain independent from Oracle-specific query code.
 *
 * <p>These types define connection-factory and health-check contracts used by higher layers while
 * keeping vendor-specific SQL adapters isolated under the {@code db} module.</p>
 */
package com.tbg.wms.core.db;
