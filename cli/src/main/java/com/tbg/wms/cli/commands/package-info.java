/**
 * Picocli command implementations for CLI operations.
 *
 * <p>Commands in this package perform configuration inspection, database diagnostics,
 * label generation, and GUI startup using consistent exit-code semantics.</p>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.cli.commands.RootCommand} - root Picocli command and common wiring.</li>
 *   <li>{@link com.tbg.wms.cli.commands.RunCommand} - shipment/carrier-move label generation and printing.</li>
 *   <li>{@link com.tbg.wms.cli.commands.GuiCommand} - desktop GUI launcher command.</li>
 *   <li>{@link com.tbg.wms.cli.commands.DbTestCommand} - database connectivity diagnostics.</li>
 *   <li>{@link com.tbg.wms.cli.commands.ShowConfigCommand} - effective configuration inspection.</li>
 *   <li>{@link com.tbg.wms.cli.commands.BarcodeCommand} - standalone barcode label generation/printing.</li>
 *   <li>{@link com.tbg.wms.cli.commands.VersionCommand} - application version output.</li>
 * </ul>
 *
 * @since 1.3.1
 */
package com.tbg.wms.cli.commands;
