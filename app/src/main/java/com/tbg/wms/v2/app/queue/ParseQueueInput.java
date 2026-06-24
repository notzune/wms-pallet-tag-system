package com.tbg.wms.v2.app.queue;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Parses free-form operator queue input into typed queue requests.
 */
public final class ParseQueueInput {
    private static final String SHIPMENT_ID_PREFIX = "800";

    /**
     * Parses user-supplied text into normalized workflow input.
     *
     * @param text the text.
     * @param defaultType the default type.
     * @param maxItems the max items.
     * @return the parsed queue items.
     */
    public List<QueueRequestItem> parse(String text, QueueItemType defaultType, int maxItems) {
        Objects.requireNonNull(defaultType, "defaultType cannot be null");
        if (maxItems <= 0) {
            throw new IllegalArgumentException("maxItems must be > 0.");
        }

        List<QueueRequestItem> requests = new ArrayList<>();
        String payload = text == null ? "" : text;
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
        return List.copyOf(requests);
    }

    private static void appendLine(
            String rawLine,
            QueueItemType defaultType,
            int maxItems,
            List<QueueRequestItem> requests
    ) {
        String line = rawLine.trim();
        if (line.isEmpty()) {
            return;
        }

        QueueItemType type = defaultType;
        String id = line;
        if (line.length() > 2 && line.charAt(1) == ':') {
            char prefix = Character.toUpperCase(line.charAt(0));
            if (prefix == 'C') {
                type = QueueItemType.CARRIER_MOVE;
                id = line.substring(2).trim();
            } else if (prefix == 'S') {
                type = QueueItemType.SHIPMENT;
                id = line.substring(2).trim();
            }
        } else {
            type = classifyUnprefixedId(line, defaultType);
        }

        if (!id.isBlank()) {
            if (requests.size() >= maxItems) {
                throw new IllegalArgumentException("Queue input exceeds max size of " + maxItems + " items.");
            }
            requests.add(new QueueRequestItem(type, id));
        }
    }

    private static QueueItemType classifyUnprefixedId(String id, QueueItemType defaultType) {
        if (looksLikeShipmentId(id)) {
            return QueueItemType.SHIPMENT;
        }
        if (looksLikeNumericId(id)) {
            return QueueItemType.CARRIER_MOVE;
        }
        return defaultType;
    }

    private static boolean looksLikeShipmentId(String id) {
        return id.length() >= 4 && id.startsWith(SHIPMENT_ID_PREFIX) && looksLikeNumericId(id);
    }

    private static boolean looksLikeNumericId(String id) {
        if (id.isBlank()) {
            return false;
        }
        for (int i = 0; i < id.length(); i++) {
            if (!Character.isDigit(id.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
