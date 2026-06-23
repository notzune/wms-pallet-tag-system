/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import java.util.List;
import java.util.Objects;

final class AdvancedPrintResumeSupport {
    private final PrintCheckpointSupport checkpointSupport;
    private final AdvancedPrintResultSupport resultSupport;

    AdvancedPrintResumeSupport(PrintCheckpointSupport checkpointSupport, AdvancedPrintResultSupport resultSupport) {
        this.checkpointSupport = Objects.requireNonNull(checkpointSupport, "checkpointSupport cannot be null");
        this.resultSupport = Objects.requireNonNull(resultSupport, "resultSupport cannot be null");
    }

    List<AdvancedPrintWorkflowService.ResumeCandidate> listIncompleteJobs() throws Exception {
        return checkpointSupport.listIncompleteJobs();
    }

    AdvancedPrintWorkflowService.PrintResult resumeJob(String checkpointId) throws Exception {
        return resultSupport.toResult(checkpointSupport.resumeJob(checkpointId));
    }
}
