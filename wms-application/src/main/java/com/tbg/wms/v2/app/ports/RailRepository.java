package com.tbg.wms.v2.app.ports;

import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import java.util.List;
import java.util.Map;

public interface RailRepository {
    List<RailStopRecord> findStopsByTrainId(String trainId);

    Map<String, RailFamilyFootprint> findFootprintsByShortCode(List<String> shortCodes);
}
