/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * Resolves deterministic footprint records from potentially ambiguous WMS candidates.
 *
 * <p><strong>Why this helper exists:</strong> WMS can return multiple footprint
 * candidates for a short code. The policy for accepting/rejecting ambiguity is
 * domain logic and should remain isolated from orchestration in
 * {@link RailWorkflowService}.</p>
 *
 * <p><strong>Why it is necessary:</strong> the rail workflow must avoid silently
 * mixing conflicting family/cases-per-pallet data, because that would corrupt
 * CAN/DOM pallet totals. Centralizing this consistency gate makes behavior explicit,
 * testable, and reusable by both CLI and GUI flows.</p>
 *
 * <p>A short code is considered resolvable when all candidates agree on normalized
 * family bucket ({@code CAN}, {@code DOM}, or {@code KEV}). Cases-per-pallet is
 * resolved deterministically by taking the maximum value across candidates.</p>
 */
public final class RailFootprintResolver {
    private final RailFamilyClassifier familyClassifier = new RailFamilyClassifier();

    /**
     * Resolves one footprint row per short code when candidates are internally consistent.
     *
     * @param candidatesByShortCode candidate rows from WMS keyed by short code
     * @return resolved short-code footprint map
     */
    public Map<String, RailFamilyFootprint> resolve(Map<String, List<RailFootprintCandidate>> candidatesByShortCode) {
        Objects.requireNonNull(candidatesByShortCode, "candidatesByShortCode cannot be null");
        Map<String, RailFamilyFootprint> resolved = new LinkedHashMap<>();
        for (Map.Entry<String, List<RailFootprintCandidate>> entry : candidatesByShortCode.entrySet()) {
            List<RailFootprintCandidate> rows = entry.getValue();
            if (rows == null || rows.isEmpty()) {
                continue;
            }
            RailFamilyFootprint footprint = resolveOne(entry.getKey(), rows);
            if (footprint == null) {
                continue;
            }
            resolved.put(entry.getKey(), footprint);
        }
        return Collections.unmodifiableMap(resolved);
    }

    private RailFamilyFootprint resolveOne(String shortCode, List<RailFootprintCandidate> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }

        String selectedFamily = null;
        int selectedCasesPerPallet = 0;

        for (RailFootprintCandidate current : rows) {
            if (current == null || !current.isValid()) {
                continue;
            }
            String currentFamily = familyClassifier.classify(current.getFamilyCode()).name();
            if (selectedFamily == null) {
                selectedFamily = currentFamily;
            } else if (!selectedFamily.equals(currentFamily)) {
                return null;
            }

            if (current.getCasesPerPallet() > selectedCasesPerPallet) {
                selectedCasesPerPallet = current.getCasesPerPallet();
            }
        }

        if (selectedFamily == null || selectedCasesPerPallet <= 0) {
            return null;
        }
        return new RailFamilyFootprint(shortCode, selectedFamily, selectedCasesPerPallet);
    }

}
