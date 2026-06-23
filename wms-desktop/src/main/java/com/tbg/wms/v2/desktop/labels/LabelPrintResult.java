package com.tbg.wms.v2.desktop.labels;

import java.nio.file.Path;

public record LabelPrintResult(int artifactsWritten, int tasksDispatched, Path outputDir) {
    public LabelPrintResult {
        if (artifactsWritten < 0 || tasksDispatched < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
    }
}
