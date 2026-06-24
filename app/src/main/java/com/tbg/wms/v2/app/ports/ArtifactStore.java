package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.print.PrintTask;

import java.nio.file.Path;

/**
 * Stores generated print artifacts before live dispatch.
 */
public interface ArtifactStore {
    /**
     * Writes one print artifact.
     *
     * @param task the task.
     * @return the written artifact path.
     */
    Path write(PrintTask task);
}
