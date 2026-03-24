/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core;

/**
 * Resolves printer and rail-print runtime configuration values.
 */
final class PrintRuntimeConfigSupport {

    private final ConfigValueSupport valueSupport;

    PrintRuntimeConfigSupport(ConfigValueSupport valueSupport) {
        this.valueSupport = valueSupport;
    }

    String printerRoutingFile() {
        return valueSupport.get("PRINTER_ROUTING_FILE", "config/printer-routing.yaml");
    }

    String defaultPrinterId() {
        return valueSupport.get("PRINTER_DEFAULT_ID", "DISPATCH");
    }

    String railDefaultPrinterIdOrNull() {
        return optionalTrimmedRaw("RAIL_DEFAULT_PRINTER_ID");
    }

    double railLabelCenterGapInches() {
        return valueSupport.parseDouble("RAIL_LABEL_CENTER_GAP_IN", "0.125");
    }

    double railLabelOffsetXInches() {
        return valueSupport.parseDouble("RAIL_LABEL_OFFSET_X_IN", "0.02");
    }

    double railLabelOffsetYInches() {
        return valueSupport.parseDouble("RAIL_LABEL_OFFSET_Y_IN", "0.02");
    }

    String forcedPrinterIdOrNull() {
        return optionalTrimmedRaw("PRINTER_FORCE_ID");
    }

    private String optionalTrimmedRaw(String key) {
        String value = valueSupport.raw(key);
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
