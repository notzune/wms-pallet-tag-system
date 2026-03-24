/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Queue parsing and aggregation policy shared by GUI print workflows.
 */
final class QueueWorkflowSupport {
    List<AdvancedPrintWorkflowService.QueueRequestItem> normalizeRequests(
            List<AdvancedPrintWorkflowService.QueueRequestItem> requests,
            int maxQueueItems
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty.");
        }
        if (requests.size() > maxQueueItems) {
            throw new IllegalArgumentException("Queue exceeds max size of " + maxQueueItems + " items.");
        }
        List<AdvancedPrintWorkflowService.QueueRequestItem> normalized = new ArrayList<>();
        for (AdvancedPrintWorkflowService.QueueRequestItem request : requests) {
            if (request == null || request.getId().isBlank()) {
                continue;
            }
            normalized.add(request);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty after parsing.");
        }
        return List.copyOf(normalized);
    }

    AdvancedPrintWorkflowService.QueuePrintResult summarizeResults(List<AdvancedPrintWorkflowService.PrintResult> results) {
        int labels = 0;
        int infoTags = 0;
        for (AdvancedPrintWorkflowService.PrintResult result : results) {
            labels += result.getLabelsPrinted();
            infoTags += result.getInfoTagsPrinted();
        }
        return new AdvancedPrintWorkflowService.QueuePrintResult(results, labels, infoTags);
    }
}
