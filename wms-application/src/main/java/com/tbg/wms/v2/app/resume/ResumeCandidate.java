package com.tbg.wms.v2.app.resume;

public record ResumeCandidate(String checkpointId, String sourceId, int nextTaskIndex, int totalTasks, String lastError) {
    public ResumeCandidate {
        if (checkpointId == null || checkpointId.isBlank()) {
            throw new IllegalArgumentException("checkpointId is required");
        }
        if (sourceId == null || sourceId.isBlank()) {
            throw new IllegalArgumentException("sourceId is required");
        }
        if (nextTaskIndex < 0 || totalTasks < 0) {
            throw new IllegalArgumentException("indexes cannot be negative");
        }
        checkpointId = checkpointId.trim();
        sourceId = sourceId.trim();
    }
}
