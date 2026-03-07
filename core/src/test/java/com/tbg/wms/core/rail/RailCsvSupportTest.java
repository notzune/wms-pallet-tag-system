/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.2
 */
package com.tbg.wms.core.rail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RailCsvSupportTest {

    @Test
    void normalizeHeaderRemovesPunctuationAndUppercases() {
        assertEquals("ITEMNBR1", RailCsvSupport.normalizeHeader(" item_nbr-1 "));
    }

    @Test
    void parseCsvLineHandlesQuotedCommasAndEscapedQuotes() {
        List<String> values = RailCsvSupport.parseCsvLine("A,\"B,C\",\"He said \"\"Hi\"\"\",D");
        assertEquals(List.of("A", "B,C", "He said \"Hi\"", "D"), values);
    }

    @Test
    void parseCsvLineHandlesNullAndEmptyInput() {
        assertEquals(List.of(""), RailCsvSupport.parseCsvLine(null));
        assertEquals(List.of(""), RailCsvSupport.parseCsvLine(""));
    }
}
