/**
 * Printer routing and raw network print transport services.
 *
 * <p>This package resolves target printers from routing rules and sends ZPL payloads over TCP.</p>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.print.PrinterRoutingService} - routing resolver from runtime context to printer target.</li>
 *   <li>{@link com.tbg.wms.core.print.PrinterConfig} - configured printer endpoint definition.</li>
 *   <li>{@link com.tbg.wms.core.print.RoutingRule} - conditional routing rule model.</li>
 *   <li>{@link com.tbg.wms.core.print.NetworkPrintService} - TCP 9100 transport for raw ZPL payloads.</li>
 * </ul>
 *
 * @since 1.3.1
 */
package com.tbg.wms.core.print;
