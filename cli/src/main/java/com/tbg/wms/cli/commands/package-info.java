/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

/**
 * Picocli command implementations for CLI operations.
 *
 * This package contains all executable commands providing functionality for
 * configuration display, database testing, label generation, and printing.
 *
 * Implemented Commands:
 * <ul>
 *   <li>{@link com.tbg.wms.cli.commands.RootCommand} - Root command with subcommands</li>
 *   <li>{@link com.tbg.wms.cli.commands.ShowConfigCommand} - Display effective config</li>
 *   <li>{@link com.tbg.wms.cli.commands.DbTestCommand} - Test DB connectivity</li>
 *   <li>{@link com.tbg.wms.cli.commands.RunCommand} - Generate and print labels</li>
 *   <li>{@link com.tbg.wms.cli.commands.GuiCommand} - Launch GUI workflow</li>
 * </ul>
 *
 * Command Pattern:
 * <pre>
 * @Command(name = "command-name", description = "...")
 * class CommandClass implements Runnable {
 *     @Option(names = {"-f", "--flag"}, description = "...")
 *     String flag;
 *
 *     @Override
 *     public void run() {
 *         // Implementation
 *     }
 * }
 * </pre>
 *
 * Exit Code Convention:
 * <ul>
 *   <li>0 - Success</li>
 *   <li>2 - Configuration or user input error</li>
 *   <li>3 - Database connectivity error</li>
 *   <li>4 - Database query error</li>
 *   <li>5 - Validation error</li>
 *   <li>6 - Print or network error</li>
 *   <li>10 - Unexpected error</li>
 * </ul>
 *
 * Logging Pattern:
 * <pre>
 * // Always include context for traceability
 * MDC.put("jobId", jobId);
 * MDC.put("site", site);
 * MDC.put("env", environment);
 *
 * log.info("Starting operation");
 * try {
 *     // Command logic
 *     log.info("Success");
 * } catch (Exception e) {
 *     log.error("Failed: {}", e.getMessage());
 *     // Handle error and set exit code
 * }
 * </pre>
 *
 * Common Command Features:
 * <ul>
 *   <li>Required option validation (null checks)
 *   <li>Structured logging with MDC correlation
 *   <li>Exception handling with typed exceptions
 *   <li>User-friendly error messages
 *   <li>Output format selection (text/json/both)
 * </ul>
 *
 * Usage by Developers:
 * <ul>
 *   <li>Extend existing command classes for new subcommands
 *   <li>Use @Command, @Option, @Parameters annotations
 *   <li>Implement Runnable or Callable for execution
 *   <li>Register new commands in RootCommand
 * </ul>
 *
 * @author Zeyad Rashed
 * @version 1.1
 * @since 1.0.0
 */
package com.tbg.wms.cli.commands;
