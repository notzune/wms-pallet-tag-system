/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class RailTrainInputParserTest {

    private final RailTrainInputParser parser = new RailTrainInputParser();

    @Test
    void parse_shouldAcceptConfiguredDelimitersAndPreserveOrder() {
        assertEquals(
                List.of("JC04152026", "JC05012026", "JC06012026"),
                parser.parse("jc04152026, JC05012026 / jc06012026")
        );
        assertEquals(
                List.of("JC04152026", "JC05012026", "JC06012026"),
                parser.parse("JC04152026: JC05012026; JC06012026")
        );
    }

    @Test
    void parse_shouldDeduplicateByFirstOccurrence() {
        assertEquals(
                List.of("JC04152026", "JC05012026"),
                parser.parse("JC04152026; jc05012026 / jc04152026")
        );
    }

    @Test
    void parse_shouldRejectInputWithoutTrainIds() {
        IllegalArgumentException nullEx = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(null)
        );
        IllegalArgumentException blankEx = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse(" , / ; : ")
        );

        assertEquals("At least one train ID is required.", nullEx.getMessage());
        assertEquals("At least one train ID is required.", blankEx.getMessage());
    }
}
