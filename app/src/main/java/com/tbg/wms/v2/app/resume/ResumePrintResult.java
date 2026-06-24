package com.tbg.wms.v2.app.resume;

/**
 * Carries resume print result data across WMS 2.0 module boundaries.
 *
 * @param artifactsWritten the artifacts written.
 * @param tasksDispatched the tasks dispatched.
 * @param nextTaskIndex the next task index.
 */
public record ResumePrintResult(int artifactsWritten, int tasksDispatched, int nextTaskIndex) {
    public ResumePrintResult {
        if (artifactsWritten < 0 || tasksDispatched < 0 || nextTaskIndex < 0) {
            throw new IllegalArgumentException("counts cannot be negative");
        }
    }
}
