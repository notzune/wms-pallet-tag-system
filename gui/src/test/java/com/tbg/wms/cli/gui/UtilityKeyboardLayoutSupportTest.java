package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UtilityKeyboardLayoutSupportTest {

    @Test
    void layoutIncludesFunctionActionAndNavigationKeys() {
        UtilityKeyboardLayoutSupport support = new UtilityKeyboardLayoutSupport();
        List<String> labels = support.sections().stream()
                .flatMap(List::stream)
                .map(UtilityKeyboardLayoutSupport.ButtonSpec::label)
                .toList();

        assertTrue(labels.containsAll(List.of(
                "F1", "F12", "Tab", "Enter", "Return", "Clear",
                "Home", "End", "PgUp", "PgDn", "Left", "Right",
                "Select All", "Copy", "Paste", "Undo", "Redo"
        )));
    }
}
