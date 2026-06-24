package com.tbg.wms.v2.desktop.labels;

import java.nio.file.Path;

/**
 * Carries label print result data across WMS 2.0 module boundaries.
 *
 * @param artifactsWritten the artifacts written.
 * @param tasksDispatched the tasks dispatched.
 * @param outputDir the output dir.
 */
public record LabelPrintResult(int artifactsWritten, int tasksDispatched, Path outputDir) {
    public LabelPrintResult {
        if (artifactsWritten < 0 || tasksDispatched < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
    }
}
