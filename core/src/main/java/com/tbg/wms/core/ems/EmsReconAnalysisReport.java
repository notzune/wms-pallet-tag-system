package com.tbg.wms.core.ems;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Final structured output for an EMS reconciliation analysis run.
 */
public final class EmsReconAnalysisReport {
    private final LocalDateTime generatedAt;
    private final String sourceFile;
    private final int totalFindings;
    private final List<EmsReconPassSummary> passes;
    private final List<EmsReconFinding> findings;
    private final List<String> notes;
    private final EndpointProbeResult qaProbe;

    public EmsReconAnalysisReport(LocalDateTime generatedAt,
                                  String sourceFile,
                                  int totalFindings,
                                  List<EmsReconPassSummary> passes,
                                  List<EmsReconFinding> findings,
                                  List<String> notes,
                                  EndpointProbeResult qaProbe) {
        this.generatedAt = Objects.requireNonNull(generatedAt, "generatedAt cannot be null");
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile cannot be null");
        this.totalFindings = totalFindings;
        this.passes = List.copyOf(Objects.requireNonNull(passes, "passes cannot be null"));
        this.findings = List.copyOf(Objects.requireNonNull(findings, "findings cannot be null"));
        this.notes = List.copyOf(Objects.requireNonNull(notes, "notes cannot be null"));
        this.qaProbe = qaProbe;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public int getTotalFindings() {
        return totalFindings;
    }

    public List<EmsReconPassSummary> getPasses() {
        return passes;
    }

    public List<EmsReconFinding> getFindings() {
        return findings;
    }

    public List<String> getNotes() {
        return notes;
    }

    public EndpointProbeResult getQaProbe() {
        return qaProbe;
    }
}
