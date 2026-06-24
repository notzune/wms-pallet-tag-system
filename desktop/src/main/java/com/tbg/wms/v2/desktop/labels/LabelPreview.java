package com.tbg.wms.v2.desktop.labels;

import java.util.List;

/**
 * Carries label preview data across WMS 2.0 module boundaries.
 *
 * @param workflowName the workflow name.
 * @param sourceId the source id.
 * @param taskCount the task count.
 * @param artifactNames the artifact names.
 */
public record LabelPreview(String workflowName, String sourceId, int taskCount, List<String> artifactNames) {
    public LabelPreview {
        workflowName = require(workflowName, "workflowName");
        sourceId = require(sourceId, "sourceId");
        if (taskCount < 0) {
            throw new IllegalArgumentException("taskCount cannot be negative");
        }
        artifactNames = List.copyOf(artifactNames == null ? List.of() : artifactNames);
    }

    private static String require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.trim();
    }
}
