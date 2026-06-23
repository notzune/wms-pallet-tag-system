package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.ports.RailRepository;
import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import java.util.List;
import java.util.Map;

final class UnsupportedRailRepository implements RailRepository {
    @Override
    public List<RailStopRecord> findStopsByTrainId(String trainId) {
        throw new UnsupportedOperationException("Rail repository is not configured.");
    }

    @Override
    public Map<String, RailFamilyFootprint> findFootprintsByShortCode(List<String> shortCodes) {
        throw new UnsupportedOperationException("Rail repository is not configured.");
    }
}
