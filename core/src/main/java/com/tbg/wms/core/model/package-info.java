/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Core domain models and utilities for the WMS Pallet Tag System.
 *
 * This package contains the fundamental data structures and utility services
 * for representing and manipulating WMS shipment data.
 *
 * Key Components:
 * <ul>
 *   <li>{@link com.tbg.wms.core.model.Shipment} - Top-level shipment container
 *   <li>{@link com.tbg.wms.core.model.Lpn} - License plate number (pallet)
 *   <li>{@link com.tbg.wms.core.model.LineItem} - SKU and quantity details
 *   <li>{@link com.tbg.wms.core.model.NormalizationService} - Data transformation utility
 *   <li>{@link com.tbg.wms.core.model.SnapshotService} - JSON snapshot persistence
 * </ul>
 *
 * Usage:
 * <pre>
 * // Create a shipment from WMS data
 * LineItem item = new LineItem("1", "SKU123", "Product", 10, 25.5, "LB");
 * Lpn lpn = new Lpn("LPN001", "SHIP123", "123456789012", 5, 50, 127.5, "ROSSI", List.of(item));
 * Shipment shipment = new Shipment("SHIP123", "ORD456", ..., List.of(lpn));
 *
 * // Normalize and validate data
 * String normalizedSku = NormalizationService.normalizeSku(rawSku);
 *
 * // Capture snapshot for replay
 * Path snapshotFile = SnapshotService.captureSnapshot(shipment, jobId, inputKey, outDir);
 * Shipment loaded = SnapshotService.readSnapshot(snapshotFile);
 * </pre>
 *
 * Design Principles:
 * <ul>
 *   <li>Immutable domain models prevent accidental mutation
 *   <li>Stateless services for reusable transformations
 *   <li>Type-safe error handling with checked exceptions
 *   <li>JSON serialization for snapshot capture and replay
 * </ul>
 *
 * @author Zeyad Rashed
 * @version 1.0
 * @since 1.0.0
 */
package com.tbg.wms.core.model;

