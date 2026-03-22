/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
/**
 * Swing GUI workflows and orchestration for preview, queue, resume, and print actions.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Provide operator-facing desktop interactions and workflow controls.</li>
 *   <li>Coordinate preview/confirmation/print execution while delegating core logic to shared services.</li>
 *   <li>Keep Swing state management separate from data-access and domain planning layers.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.cli.gui.LabelGuiFrame} - main desktop window and workflow orchestrator.</li>
 *   <li>{@link com.tbg.wms.cli.gui.MainSettingsDialog} - primary runtime settings and maintenance dialog.</li>
 *   <li>{@link com.tbg.wms.cli.gui.LabelWorkflowService} - shipment-level preview/print preparation service.</li>
 *   <li>{@link com.tbg.wms.cli.gui.AdvancedPrintWorkflowService} - carrier-move, queue, and resume workflows.</li>
 *   <li>{@link com.tbg.wms.cli.gui.PrintTaskPlanner} - task planning and info-tag counting extracted from workflow orchestration.</li>
 *   <li>{@link com.tbg.wms.cli.gui.BarcodeDialogFactory} - barcode dialog UI factory and action wiring.</li>
 *   <li>{@link com.tbg.wms.cli.gui.ArtifactNameSupport} - shared artifact filename slugging for generated ZPL output.</li>
 *   <li>{@link com.tbg.wms.cli.gui.TextFieldClipboardController} - terminal-like right-click clipboard behavior.</li>
 *   <li>Update/install maintenance flows are kept in dedicated helpers so the main frame does not own
 *       network/download/process-launch details directly.</li>
 *   <li>Workflow caches are scoped per site and use concurrent maps for safe GUI/background-thread access.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.cli.gui;
