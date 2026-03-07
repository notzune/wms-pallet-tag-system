/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.db;

import com.tbg.wms.core.rail.RailDbRepository;
import com.tbg.wms.core.rail.RailFootprintCandidate;
import com.tbg.wms.core.rail.RailStopRecord;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter that exposes rail-only queries through the core rail repository boundary.
 */
public final class WmsRailDbRepository implements RailDbRepository {
    private final DbQueryRepository delegate;

    public WmsRailDbRepository(DbQueryRepository delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    }

    @Override
    public List<RailStopRecord> findRailStopsByTrainId(String trainId) {
        Objects.requireNonNull(trainId, "trainId cannot be null");
        return delegate.findRailStopsByTrainId(trainId);
    }

    @Override
    public Map<String, List<RailFootprintCandidate>> findRailFootprintsByShortCode(List<String> shortCodes) {
        Objects.requireNonNull(shortCodes, "shortCodes cannot be null");
        return delegate.findRailFootprintsByShortCode(shortCodes);
    }
}
