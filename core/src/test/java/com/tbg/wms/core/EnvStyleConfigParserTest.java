/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

final class EnvStyleConfigParserTest {

    @Test
    void parseLinesHandlesCommentsExportAndQuotedValues() {
        Map<String, String> values = EnvStyleConfigParser.parseLines(List.of(
                "# comment",
                "export ACTIVE_SITE=TBG3002",
                "ORACLE_PASSWORD=\"secret\"",
                "SITE_NAME='Jersey City'"
        ));

        assertEquals("TBG3002", values.get("ACTIVE_SITE"));
        assertEquals("secret", values.get("ORACLE_PASSWORD"));
        assertEquals("Jersey City", values.get("SITE_NAME"));
    }

    @Test
    void parseLinesSkipsMalformedEntries() {
        Map<String, String> values = EnvStyleConfigParser.parseLines(List.of(
                "",
                "   ",
                "=value",
                "KEY_ONLY",
                "VALID_KEY=value"
        ));

        assertEquals("value", values.get("VALID_KEY"));
        assertNull(values.get("KEY_ONLY"));
    }

    @Test
    void parseLinesStripsUnquotedInlineComments() {
        Map<String, String> values = EnvStyleConfigParser.parseLines(List.of(
                "A=1 # comment",
                "B=plain#comment",
                "C= 2    # trailing",
                "D=\"x#y\" # comment",
                "E='left#right' # comment"
        ));

        assertEquals("1", values.get("A"));
        assertEquals("plain", values.get("B"));
        assertEquals("2", values.get("C"));
        assertEquals("x#y", values.get("D"));
        assertEquals("left#right", values.get("E"));
    }
}
