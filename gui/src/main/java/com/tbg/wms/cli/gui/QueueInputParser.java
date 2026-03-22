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
import java.util.Objects;

/**
 * Parses queue dialog input into strongly-typed queue request items.
 */
final class QueueInputParser {
    private static final String SHIPMENT_ID_PREFIX = "800";

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
        Objects.requireNonNull(defaultType, "defaultType cannot be null");
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be > 0.");
        }
        List<AdvancedPrintWorkflowService.QueueRequestItem> requests = new ArrayList<>();
        String payload = text;
        if (payload == null) {
            payload = "";
        }
        int lineStart = 0;
        for (int i = 0; i < payload.length(); i++) {
            char ch = payload.charAt(i);
            if (ch == '\n' || ch == '\r' || ch == ';') {
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
        String line = rawLine.trim();
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
        } else {
            type = classifyUnprefixedId(line, defaultType);
        }
        if (!id.isBlank()) {
            if (requests.size() >= maxItems) {
                throw new IllegalArgumentException("Queue input exceeds max size of " + maxItems + " items.");
            }
            requests.add(new AdvancedPrintWorkflowService.QueueRequestItem(type, id));
        }
    }

    private static AdvancedPrintWorkflowService.QueueItemType classifyUnprefixedId(
            String id,
            AdvancedPrintWorkflowService.QueueItemType defaultType
    ) {
        if (looksLikeShipmentId(id)) {
            return AdvancedPrintWorkflowService.QueueItemType.SHIPMENT;
        }
        if (looksLikeCarrierMoveId(id)) {
            return AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE;
        }
        return defaultType;
    }

    private static boolean looksLikeShipmentId(String id) {
        if (id.length() < 4 || !id.startsWith(SHIPMENT_ID_PREFIX)) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static boolean looksLikeCarrierMoveId(String id) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < id.length(); i++) {
            char ch = id.charAt(i);
            if (Character.isLetter(ch)) {
                hasLetter = true;
            } else if (Character.isDigit(ch)) {
                hasDigit = true;
            } else {
                return false;
            }
        }
        return hasLetter && hasDigit;
    }
}
