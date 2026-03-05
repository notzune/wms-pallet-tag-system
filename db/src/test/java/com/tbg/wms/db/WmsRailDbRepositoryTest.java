/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.db;

import com.tbg.wms.core.rail.RailFootprintCandidate;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

final class WmsRailDbRepositoryTest {

    @Test
    void methodsRejectNullInputs() {
        DbQueryRepository delegate = mock(DbQueryRepository.class);
        WmsRailDbRepository repository = new WmsRailDbRepository(delegate);

        assertThrows(NullPointerException.class, () -> repository.findRailStopsByTrainId(null));
        assertThrows(NullPointerException.class, () -> repository.findRailFootprintsByShortCode(null));
    }

    @Test
    void methodsDelegateToDbRepository() {
        DbQueryRepository delegate = mock(DbQueryRepository.class);
        WmsRailDbRepository repository = new WmsRailDbRepository(delegate);

        repository.findRailStopsByTrainId("JC03032026");
        repository.findRailFootprintsByShortCode(List.of("01830"));

        verify(delegate).findRailStopsByTrainId("JC03032026");
        verify(delegate).findRailFootprintsByShortCode(List.of("01830"));
    }
}

