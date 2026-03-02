package com.tbg.wms.cli.gui;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses queue dialog input into strongly-typed queue request items.
 */
final class QueueInputParser {

    private QueueInputParser() {
    }

    static List<AdvancedPrintWorkflowService.QueueRequestItem> parse(
            String text,
            AdvancedPrintWorkflowService.QueueItemType defaultType,
            int maxItems) {
        List<AdvancedPrintWorkflowService.QueueRequestItem> requests = new ArrayList<>();
        String[] lines = (text == null ? "" : text).split("\\r?\\n");
        for (String raw : lines) {
            String line = raw == null ? "" : raw.trim();
            if (line.isEmpty()) {
                continue;
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
        if (requests.isEmpty()) {
            throw new IllegalArgumentException("Queue input is empty.");
        }
        return requests;
    }
}
