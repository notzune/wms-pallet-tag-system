/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DescriptionTextHeuristicsTest {

    @Test
    void isHumanReadableReturnsTrueWhenLettersPresent() {
        assertTrue(DescriptionTextHeuristics.isHumanReadable("ORANGE JUICE 12OZ"));
    }

    @Test
    void isHumanReadableReturnsFalseForBlankOrNumericOnlyValues() {
        assertFalse(DescriptionTextHeuristics.isHumanReadable(null));
        assertFalse(DescriptionTextHeuristics.isHumanReadable("  "));
        assertFalse(DescriptionTextHeuristics.isHumanReadable("123456"));
    }
}
