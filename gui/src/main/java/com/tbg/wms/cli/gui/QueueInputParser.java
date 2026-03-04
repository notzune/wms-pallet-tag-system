/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.0
 */
package com.tbg.wms.cli.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses queue dialog input into strongly-typed queue request items.
 */
final class QueueInputParser {

    private QueueInputParser() {
    }

    /**
     * Parses queue text into typed queue request items.
     *
     * @param text        free-form queue lines
     * @param defaultType default type applied to unprefixed lines
     * @param maxItems    hard limit for accepted queue items
     * @return parsed queue request list
     */
    static List<AdvancedPrintWorkflowService.QueueRequestItem> parse(
            String text,
            AdvancedPrintWorkflowService.QueueItemType defaultType,
            int maxItems) {
        List<AdvancedPrintWorkflowService.QueueRequestItem> requests = new ArrayList<>();
        String payload = text == null ? "" : text;
        int lineStart = 0;
        for (int i = 0; i < payload.length(); i++) {
            char ch = payload.charAt(i);
            if (ch == '\n' || ch == '\r') {
                appendLine(payload.substring(lineStart, i), defaultType, maxItems, requests);
                if (ch == '\r' && i + 1 < payload.length() && payload.charAt(i + 1) == '\n') {
                    i++;
                }
                lineStart = i + 1;
            }
        }
        appendLine(payload.substring(lineStart), defaultType, maxItems, requests);

        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Queue input is empty.");
        }
        return requests;
    }

    private static void appendLine(String rawLine,
                                   AdvancedPrintWorkflowService.QueueItemType defaultType,
                                   int maxItems,
                                   List<AdvancedPrintWorkflowService.QueueRequestItem> requests) {
        String line = rawLine == null ? "" : rawLine.trim();
        if (line.isEmpty()) {
            return;
        }
        AdvancedPrintWorkflowService.QueueItemType type = defaultType;
        String id = line;
        if (line.length() > 2 && line.charAt(1) == ':') {
            char prefix = Character.toUpperCase(line.charAt(0));
            if (prefix == 'C') {
                type = AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE;
                id = line.substring(2).trim();
            } else if (prefix == 'S') {
                type = AdvancedPrintWorkflowService.QueueItemType.SHIPMENT;
                id = line.substring(2).trim();
            }
        }
        if (!id.isBlank()) {
            if (requests.size() >= maxItems) {
                throw new IllegalArgumentException("Queue input exceeds max size of " + maxItems + " items.");
            }
            requests.add(new AdvancedPrintWorkflowService.QueueRequestItem(type, id));
        }
    }
}
