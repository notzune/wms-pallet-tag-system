package com.tbg.wms.v2.app.resume;

public record ResumePrintResult(int artifactsWritten, int tasksDispatched, int nextTaskIndex) {
    public ResumePrintResult {
        if (artifactsWritten < 0 || tasksDispatched < 0 || nextTaskIndex < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
    }
}
