package com.tbg.wms.cli.gui.analyzers;

import com.tbg.wms.cli.gui.analyzers.unpicked.UnpickedPartialsAnalyzerDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzerRegistryTest {

    @Test
    void registry_shouldReturnUnpickedPartialsAsDefaultAnalyzer() {
        AnalyzerRegistry registry = new AnalyzerRegistry(List.of(
                new UnpickedPartialsAnalyzerDefinition()
        ));

        assertEquals("unpicked-partials", registry.defaultAnalyzer().id());
        assertEquals(1, registry.definitions().size());
    }
}
