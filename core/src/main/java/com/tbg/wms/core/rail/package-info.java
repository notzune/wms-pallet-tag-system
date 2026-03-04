/**
 * Rail-office helper models and services for deterministic label math.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Implement rail planning logic independent of CLI/GUI transport layers.</li>
 *   <li>Preserve deterministic CAN/DOM pallet calculations per railcar.</li>
 *   <li>Centralize rail rendering/export primitives used by multiple workflows.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.rail.RailStopRecord} - one rail load row with item/quantity pairs.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailFamilyFootprint} - item-to-family footprint lookup row.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailLabelPlanner} - computes family percentages and top callouts.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailAggregationService} - groups flattened rows by railcar.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailPalletCalculator} - computes per-railcar CAN/DOM pallets.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailFootprintResolver} - resolves deterministic short-code footprints and
 *       rejects ambiguous WMS candidates so pallet math is not computed from conflicting footprint data.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailCardRenderer} - renders direct letter-size card PDFs.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailWorkflowService} - orchestrates the full rail planning workflow.</li>
 *   <li>{@link com.tbg.wms.core.rail.RailTrainDetailExporter} - writes merge-ready `_TrainDetail.csv` output.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core.rail;
