/**
 * Immutable WMS domain models and supporting planning/normalization utilities.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Represent canonical shipment/pallet/line-item structures.</li>
 *   <li>Host deterministic planning and normalization helpers used across modules.</li>
 *   <li>Keep model semantics stable for preview, rendering, and print paths.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.model.Shipment} - shipment header and address/route metadata.</li>
 *   <li>{@link com.tbg.wms.core.model.Lpn} - pallet/license-plate aggregate with line items.</li>
 *   <li>{@link com.tbg.wms.core.model.LineItem} - SKU-level shipment or pallet quantity record.</li>
 *   <li>{@link com.tbg.wms.core.model.ShipmentSkuFootprint} - SKU units and units-per-pallet lookup row.</li>
 *   <li>{@link com.tbg.wms.core.model.PalletPlanningService} - full/partial pallet planning math.</li>
 *   <li>{@link com.tbg.wms.core.model.SnapshotService} - typed snapshot serialization for debug/replay workflows.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core.model;
