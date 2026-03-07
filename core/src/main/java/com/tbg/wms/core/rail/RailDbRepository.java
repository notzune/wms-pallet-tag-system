/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import java.util.List;
import java.util.Map;

/**
 * Rail-only database boundary for loading train rows and footprint candidates.
 */
public interface RailDbRepository {

    /**
     * Loads flattened rail stop rows for a train identifier.
     *
     * @param trainId train identifier entered by an operator
     * @return zero or more flattened rows
     */
    List<RailStopRecord> findRailStopsByTrainId(String trainId);

    /**
     * Loads WMS footprint candidates keyed by short code.
     *
     * @param shortCodes short codes extracted from rail rows
     * @return candidate map keyed by short code
     */
    Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes);
}
