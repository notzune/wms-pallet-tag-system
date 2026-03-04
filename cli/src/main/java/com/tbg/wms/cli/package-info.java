/**
 * Application entry layer for command-line and desktop workflows.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Expose process-level startup and argument dispatch.</li>
 *   <li>Route execution into CLI command handlers or GUI launch paths.</li>
 *   <li>Keep bootstrapping concerns separate from business services.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.cli.CliMain} - process bootstrap and Picocli startup.</li>
 *   <li>{@link com.tbg.wms.cli.commands.RootCommand} - top-level command registration and shared config access.</li>
 * </ul>
 *
 * @since 1.5.0
 */
package com.tbg.wms.cli;
