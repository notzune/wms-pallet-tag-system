/**
 * Release metadata, asset matching, and version comparison helpers for update workflows.
 *
 * <p>The package defines the trusted release-feed boundary used by the GUI updater while keeping
 * update transport/integrity mechanics separate from Swing presentation code. It also owns the
 * small amount of release-tag normalization needed for stable and prerelease GitHub releases.</p>
 */
package com.tbg.wms.core.update;
