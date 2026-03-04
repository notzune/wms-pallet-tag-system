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
 *   <li>{@link com.tbg.wms.cli.gui.LabelWorkflowService} - shipment-level preview/print preparation service.</li>
 *   <li>{@link com.tbg.wms.cli.gui.AdvancedPrintWorkflowService} - carrier-move, queue, and resume workflows.</li>
 *   <li>{@link com.tbg.wms.cli.gui.BarcodeDialogFactory} - barcode dialog UI factory and action wiring.</li>
 *   <li>{@link com.tbg.wms.cli.gui.TextFieldClipboardController} - terminal-like right-click clipboard behavior.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.cli.gui;
