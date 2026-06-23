package com.tbg.wms.v2.app.barcode;

public record BarcodeLabel(String artifactName, String zpl) {
    public BarcodeLabel {
        if (artifactName == null || artifactName.isBlank()) {
            throw new IllegalArgumentException("artifactName is required");
        }
        if (zpl == null || zpl.isBlank()) {
            throw new IllegalArgumentException("zpl is required");
        }
        artifactName = artifactName.trim();
    }
}
