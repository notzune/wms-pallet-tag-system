package com.tbg.wms.v2.app.barcode;

/**
 * Carries barcode label data across WMS 2.0 module boundaries.
 *
 * @param artifactName the artifact name.
 * @param zpl the zpl.
 */
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
