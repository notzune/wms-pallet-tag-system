package com.tbg.wms.v2.domain.print;

/**
 * One planned printable artifact in a WMS 2.0 workflow.
 */
public record PrintTask(Kind kind, String artifactName, String subject) {
    public PrintTask {
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if (artifactName == null || artifactName.isBlank()) {
            throw new IllegalArgumentException("artifactName is required");
        }
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("subject is required");
        }
        artifactName = artifactName.trim();
        subject = subject.trim();
    }

    public enum Kind {
        PALLET_LABEL,
        SHIPMENT_INFO_TAG,
        STOP_INFO_TAG,
        FINAL_INFO_TAG
    }
}
