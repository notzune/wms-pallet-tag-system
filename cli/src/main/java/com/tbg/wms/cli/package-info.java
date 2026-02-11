/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Command-line interface for the WMS Pallet Tag System.
 *
 * Provides a comprehensive CLI for label generation, printing, and system diagnostics
 * using Picocli framework for command organization and argument parsing.
 *
 * Main Entry Point:
 * <ul>
 *   <li>{@link com.tbg.wms.cli.CliMain} - Application entry point
 * </ul>
 *
 * Command Hierarchy:
 * <pre>
 * RootCommand
 *   ├── ShowConfigCommand - Display effective configuration
 *   ├── DbTestCommand - Validate database connectivity
 *   ├── RunCommand (planned) - Generate and print labels
 *   ├── TemplateCommand (planned) - Generate blank templates
 *   ├── ManualCommand (planned) - Manual label entry mode
 *   └── ValidationCommand (planned) - Validate snapshots
 * </pre>
 *
 * Command Descriptions:
 * <ul>
 *   <li><b>config</b> - Display effective configuration with secrets redacted
 *   <li><b>db-test</b> - Test Oracle WMS database connectivity and report diagnostics
 *   <li><b>run</b> - Generate labels from shipment data with optional printing
 *   <li><b>template</b> - Generate blank label templates for calibration
 *   <li><b>print-template</b> - Print blank templates to a specific printer
 *   <li><b>manual</b> - Manually enter label data without database
 * </ul>
 *
 * Exit Codes:
 * <ul>
 *   <li>0 - Success
 *   <li>2 - Configuration error
 *   <li>3 - Database connectivity error
 *   <li>4 - Database query/data error
 *   <li>5 - Data validation error
 *   <li>6 - Printer/network error
 *   <li>10 - Unexpected internal error
 * </ul>
 *
 * Usage Examples:
 * <pre>
 * # Display configuration (secrets redacted)
 * java -jar cli-*.jar config
 *
 * # Test database connectivity
 * java -jar cli-*.jar db-test
 *
 * # Generate labels (future)
 * java -jar cli-*.jar run SHIP123 --env QA --dry-run --out out/
 *
 * # Generate blank templates (future)
 * java -jar cli-*.jar template --count 10 --out out/templates
 *
 * # Manual label entry (future)
 * java -jar cli-*.jar manual --field lpn=LPN001 --field sku=SKU123 --out out/manual
 * </pre>
 *
 * Architecture:
 * <pre>
 * CLI Module
 *   ├── CliMain - Picocli application bootstrap
 *   ├── RootCommand - Root command with subcommands
 *   ├── commands/
 *   │   ├── ShowConfigCommand - Config display
 *   │   ├── DbTestCommand - DB connectivity
 *   │   ├── RunCommand (planned) - Label generation
 *   │   ├── TemplateCommand (planned) - Template generation
 *   │   ├── ManualCommand (planned) - Manual entry
 *   │   └── ValidationCommand (planned) - Validation
 *   └── resources/
 *       └── logback.xml (inherited from core)
 * </pre>
 *
 * Configuration Loading:
 * <ul>
 *   <li>CLI flags (highest priority)
 *   <li>Environment variables
 *   <li>.env file (local only, ignored by git)
 *   <li>Default values in code
 * </ul>
 *
 * Output Modes:
 * <ul>
 *   <li><b>--format json</b> - Machine-readable JSON output
 *   <li><b>--format text</b> - Human-readable text output
 *   <li><b>--format both</b> - Both JSON and text
 * </ul>
 *
 * Common Flags:
 * <ul>
 *   <li><b>--env QA|PROD</b> - WMS environment
 *   <li><b>--site SITE_CODE</b> - Site code (e.g., TBG3002)
 *   <li><b>--dry-run</b> - Execute without printing/writing
 *   <li><b>--out DIR</b> - Output directory for artifacts
 *   <li><b>--verbose</b> - Increase console logging
 * </ul>
 *
 * @author Zeyad Rashed
 * @version 1.0
 * @since 1.0.0
 */
package com.tbg.wms.cli;

