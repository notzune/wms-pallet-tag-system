/**
 * Swing GUI workflows and orchestration for preview, queue, resume, and print actions.
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
 * @since 1.3.2
 */
package com.tbg.wms.cli.gui;
