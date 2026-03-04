/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.*;

/**
 * Resolves deterministic footprint records from potentially ambiguous WMS candidates.
 *
 * <p>A short code is considered resolvable only when all candidates agree on
 * family code and cases-per-pallet.</p>
 */
public final class RailFootprintResolver {

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
            RailFootprintCandidate first = rows.get(0);
            if (!isConsistent(rows, first)) {
                continue;
            }
            resolved.put(entry.getKey(),
                    new RailFamilyFootprint(entry.getKey(), first.getFamilyCode(), first.getCasesPerPallet()));
        }
        return Collections.unmodifiableMap(resolved);
    }

    private boolean isConsistent(List<RailFootprintCandidate> rows, RailFootprintCandidate seed) {
        for (int i = 1; i < rows.size(); i++) {
            RailFootprintCandidate current = rows.get(i);
            if (!seed.getFamilyCode().equals(current.getFamilyCode())
                    || seed.getCasesPerPallet() != current.getCasesPerPallet()) {
                return false;
            }
        }
        return true;
    }
}
