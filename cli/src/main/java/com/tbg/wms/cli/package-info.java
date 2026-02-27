/**
 * Application entry layer for command-line and desktop workflows.
 *
 * <p>This package exposes the runtime entry point ({@link com.tbg.wms.cli.CliMain})
 * and delegates feature behavior to command and GUI packages.</p>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.cli.CliMain} - application bootstrap and Picocli startup.</li>
 *   <li>{@link com.tbg.wms.cli.commands.RootCommand} - top-level command registration and shared config access.</li>
 * </ul>
 *
 * @since 1.3.1
 */
package com.tbg.wms.cli;
