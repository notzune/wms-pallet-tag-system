/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QueueWorkflowSupportTest {
    private final QueueWorkflowSupport support = new QueueWorkflowSupport();

    @Test
    void normalizeRequests_shouldDropBlankEntriesAndRejectEmptyQueues() {
        List<AdvancedPrintWorkflowService.QueueRequestItem> normalized = support.normalizeRequests(
                Arrays.asList(
                        null,
                        new AdvancedPrintWorkflowService.QueueRequestItem(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, " "),
                        new AdvancedPrintWorkflowService.QueueRequestItem(AdvancedPrintWorkflowService.QueueItemType.SHIPMENT, "SHIP123")
                ),
                5
        );

        assertEquals(1, normalized.size());
        assertEquals("SHIP123", normalized.get(0).getId());
        assertThrows(IllegalArgumentException.class, () -> support.normalizeRequests(List.of(), 5));
    }

    @Test
    void summarizeResults_shouldAggregateLabelsAndInfoTags() {
        AdvancedPrintWorkflowService.QueuePrintResult result = support.summarizeResults(List.of(
                new AdvancedPrintWorkflowService.PrintResult(2, 1, Path.of("out", "a"), "P1", "10.0.0.1", false),
                new AdvancedPrintWorkflowService.PrintResult(3, 2, Path.of("out", "b"), "P1", "10.0.0.1", false)
        ));

        assertEquals(5, result.getTotalLabelsPrinted());
        assertEquals(3, result.getTotalInfoTagsPrinted());
        assertEquals(2, result.getItemResults().size());
    }
}
