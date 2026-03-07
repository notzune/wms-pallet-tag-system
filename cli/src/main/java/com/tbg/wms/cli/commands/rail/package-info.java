/**
 * Rail-specific CLI commands and helpers.
 *
 * <p><strong>Package Responsibility</strong></p>
 * <ul>
 *   <li>Host rail-oriented command entrypoints distinct from general shipment commands.</li>
 *   <li>Preserve separate operator paths for CSV/offline workflows and live WMS workflows.</li>
 * </ul>
 *
 * <p><strong>Key Types</strong></p>
 * <ul>
 *   <li>{@link com.tbg.wms.cli.commands.rail.RailHelperCommand} - CSV-driven rail merge planning/export command.</li>
 *   <li>{@link com.tbg.wms.cli.commands.rail.RailPrintCommand} - WMS-first rail preview, render, and optional print command.</li>
 * </ul>
 *
 * @since 1.5.2
 */
package com.tbg.wms.cli.commands.rail;
