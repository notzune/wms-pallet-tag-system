/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Command-line interface and GUI entry points for the WMS Pallet Tag System.
 *
 * Provides Picocli commands for configuration inspection, database diagnostics,
 * shipment label generation, and the desktop workflow.
 *
 * Main Entry Point:
 * <ul>
 *   <li>{@link com.tbg.wms.cli.CliMain} - Application entry point</li>
 * </ul>
 *
 * Implemented Commands:
 * <ul>
 *   <li><b>config</b> - Display effective configuration with secrets redacted</li>
 *   <li><b>db-test</b> - Test Oracle WMS database connectivity</li>
 *   <li><b>run</b> - Generate labels from shipment data with optional printing</li>
 *   <li><b>gui</b> - Desktop workflow with preview and confirm-print</li>
 * </ul>
 *
 * Exit Codes:
 * <ul>
 *   <li>0 - Success</li>
 *   <li>2 - Configuration error</li>
 *   <li>3 - Database connectivity error</li>
 *   <li>4 - Database query or data error</li>
 *   <li>5 - Data validation error</li>
 *   <li>6 - Printer or network error</li>
 *   <li>10 - Unexpected internal error</li>
 * </ul>
 *
 * Usage Examples:
 * <pre>
 * java -jar cli-*.jar config
 * java -jar cli-*.jar db-test
 * java -jar cli-*.jar run --shipment-id SHIP123 --dry-run --output-dir out/
 * java -jar cli-*.jar gui
 * </pre>
 *
 * @author Zeyad Rashed
 * @version 1.1
 * @since 1.0.0
 */
package com.tbg.wms.cli;
