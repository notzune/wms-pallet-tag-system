package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import java.util.List;
import java.util.Map;

/**
 * Defines the contract for rail repository behavior in WMS 2.0 workflows.
 */
public interface RailRepository {
    /**
     * Finds rail stop records for a train.
     *
     * @param trainId the train id.
     * @return the matching value, when present.
     */
    List<RailStopRecord> findStopsByTrainId(String trainId);

    /**
     * Finds rail item footprints by short code.
     *
     * @param shortCodes the short codes.
     * @return the matching value, when present.
     */
    Map<String, RailFamilyFootprint> findFootprintsByShortCode(List<String> shortCodes);
}
