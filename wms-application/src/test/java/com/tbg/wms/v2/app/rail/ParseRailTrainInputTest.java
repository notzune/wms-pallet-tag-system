package com.tbg.wms.v2.app.rail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ParseRailTrainInputTest {
    private final ParseRailTrainInput parser = new ParseRailTrainInput();

    @Test
    void parse_acceptsConfiguredDelimitersUppercasesAndDeduplicates() {
        assertEquals(
                List.of("JC04152026", "JC05012026", "JC06012026"),
                parser.parse("jc04152026, JC05012026 / jc06012026")
        );
        assertEquals(
                List.of("JC04152026", "JC05012026", "JC06012026"),
                parser.parse("JC04152026: JC05012026; jc04152026\njc06012026")
        );
    }

    @Test
    void parse_rejectsInputWithoutTrainIds() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> parser.parse(" , / ; : "));

        assertEquals("At least one train ID is required.", ex.getMessage());
    }
}
