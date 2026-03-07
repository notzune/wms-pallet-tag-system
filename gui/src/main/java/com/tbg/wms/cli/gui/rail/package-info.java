/**
 * Rail-specific GUI workflow dialogs and services.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Provide rail-focused GUI operator flows separate from shipment/carrier GUI flows.</li>
 *   <li>Bridge live WMS rail planning data into preview, diagnostics, and render actions.</li>
 *   <li>Keep rail-specific UI concerns isolated from reusable rail core math components.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.cli.gui.rail.RailLabelsDialog} - rail labels workflow dialog and operator controls.</li>
 *   <li>{@link com.tbg.wms.cli.gui.rail.RailWorkflowService} - WMS-first rail planning and diagnostics orchestration.</li>
 *   <li>{@link com.tbg.wms.cli.gui.rail.RailArtifactService} - DOCX/PDF/PRN artifact automation support.</li>
 *   <li>Artifact generation validates merge CSV readability and ensures output directories exist before automation.</li>
 * </ul>
 *
 * @since 1.5.2
 */
package com.tbg.wms.cli.gui.rail;
