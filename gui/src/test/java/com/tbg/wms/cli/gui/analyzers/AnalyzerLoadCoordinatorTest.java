package com.tbg.wms.cli.gui.analyzers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerLoadCoordinatorTest {

    @Test
    void beginLoad_shouldInvalidateEarlierTokens() {
        AnalyzerLoadCoordinator coordinator = new AnalyzerLoadCoordinator();

        long first = coordinator.beginLoad();
        long second = coordinator.beginLoad();

        assertFalse(coordinator.isCurrent(first));
        assertTrue(coordinator.isCurrent(second));
    }
}
