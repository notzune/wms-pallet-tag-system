package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.ports.RailRepository;
import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import java.util.List;
import java.util.Map;

/**
 * Provides unsupported rail repository behavior for WMS 2.0 workflows.
 */
final class UnsupportedRailRepository implements RailRepository {
    /**
     * Finds stops by train id.
     *
     * @param trainId the train id.
     * @return matching stops if the repository were configured
     */
    @Override
    public List<RailStopRecord> findStopsByTrainId(String trainId) {
        throw new UnsupportedOperationException("Rail repository is not configured.");
    }

    /**
     * Finds footprints by short code.
     *
     * @param shortCodes the short codes.
     * @return matching footprints if the repository were configured
     */
    @Override
    public Map<String, RailFamilyFootprint> findFootprintsByShortCode(List<String> shortCodes) {
        throw new UnsupportedOperationException("Rail repository is not configured.");
    }
}
