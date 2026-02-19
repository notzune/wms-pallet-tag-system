/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Network printing and printer routing services.
 *
 * <p>This package provides printer management functionality:</p>
 * <ul>
 *   <li>{@link com.tbg.wms.core.print.PrinterConfig} - Printer configuration with network endpoint</li>
 *   <li>{@link com.tbg.wms.core.print.RoutingRule} - Rule-based printer selection</li>
 *   <li>{@link com.tbg.wms.core.print.PrinterRoutingService} - YAML-driven routing engine</li>
 *   <li>{@link com.tbg.wms.core.print.NetworkPrintService} - TCP/IP printing via port 9100</li>
 * </ul>
 *
 * <h2>Printer Routing Example</h2>
 * <pre>
 * // Load routing configuration
 * PrinterRoutingService routing = PrinterRoutingService.load("TBG3002", Paths.get("config"));
 *
 * // Select printer based on staging location
 * Map&lt;String, String&gt; context = Map.of("stagingLocation", "ROSSI");
 * PrinterConfig printer = routing.selectPrinter(context);
 *
 * // Print ZPL content
 * NetworkPrintService printService = new NetworkPrintService();
 * printService.print(printer, zplContent, "LPN123");
 * </pre>
 *
 * <h2>Configuration Files</h2>
 * <p>Routing requires two YAML files per site in {@code config/{siteCode}/}:</p>
 * <ul>
 *   <li>{@code printers.yaml} - Printer inventory with IDs, IPs, and tags</li>
 *   <li>{@code printer-routing.yaml} - Routing rules and default printer</li>
 * </ul>
 *
 * @since 1.0.0
 */
package com.tbg.wms.core.print;

