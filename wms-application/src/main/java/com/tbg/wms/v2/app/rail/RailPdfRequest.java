package com.tbg.wms.v2.app.rail;

import java.nio.file.Path;
import java.util.List;

public record RailPdfRequest(RailPlan plan, List<String> selectedSequences, Path outputPdf) {
    public RailPdfRequest {
        if (plan == null) {
            throw new IllegalArgumentException("Rail plan is required.");
        }
        selectedSequences = List.copyOf(selectedSequences == null ? List.of() : selectedSequences);
        if (outputPdf == null) {
            throw new IllegalArgumentException("Output PDF path is required.");
        }
    }
}
