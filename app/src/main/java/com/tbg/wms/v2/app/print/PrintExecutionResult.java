package com.tbg.wms.v2.app.print;

import java.nio.file.Path;
import java.util.List;

/**
 * Summary returned after a prepared print plan is executed.
 */
public record PrintExecutionResult(int artifactsWritten, int tasksDispatched, List<Path> artifactPaths) {
    public PrintExecutionResult {
        if (artifactsWritten < 0) {
            throw new IllegalArgumentException("artifactsWritten cannot be negative");
        }
        if (tasksDispatched < 0) {
            throw new IllegalArgumentException("tasksDispatched cannot be negative");
        }
        if (artifactPaths == null) {
            throw new IllegalArgumentException("artifactPaths are required");
        }
        artifactPaths = List.copyOf(artifactPaths);
    }
}
