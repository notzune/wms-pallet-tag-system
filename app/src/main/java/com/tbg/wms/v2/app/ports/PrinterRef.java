package com.tbg.wms.v2.app.ports;

import java.util.List;

/**
 * Application-level printer reference without transport implementation details.
 */
public record PrinterRef(String id, String displayName, List<String> capabilities) {
    public PrinterRef {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id is required");
        }
        displayName = displayName == null || displayName.isBlank() ? id.trim() : displayName.trim();
        id = id.trim();
        capabilities = capabilities == null ? List.of() : List.copyOf(capabilities);
    }
}
