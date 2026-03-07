/**
 * Printer routing and raw network print transport services.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Resolve a print target from workflow context and routing configuration.</li>
 *   <li>Send print payloads over network transport with retry/error semantics.</li>
 *   <li>Isolate printer concerns from label-data construction and command parsing.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.print.PrinterRoutingService} - routing resolver from runtime context to printer target.</li>
 *   <li>{@link com.tbg.wms.core.print.PrinterConfig} - configured printer endpoint definition.</li>
 *   <li>{@link com.tbg.wms.core.print.RoutingRule} - conditional routing rule model.</li>
 *   <li>{@link com.tbg.wms.core.print.NetworkPrintService} - TCP 9100 transport for raw ZPL payloads.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.core.print;
