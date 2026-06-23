package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.print.PrintTask;

import java.nio.file.Path;

/**
 * Stores generated print artifacts before live dispatch.
 */
public interface ArtifactStore {
    Path write(PrintTask task);
}
