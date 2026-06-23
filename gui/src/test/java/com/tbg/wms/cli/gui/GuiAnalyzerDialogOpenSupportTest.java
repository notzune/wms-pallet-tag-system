package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuiAnalyzerDialogOpenSupportTest {

    private final GuiAnalyzerDialogOpenSupport support = new GuiAnalyzerDialogOpenSupport();

    @Test
    void planOpen_shouldBlockWhenDeveloperModeIsDisabled() {
        GuiAnalyzerDialogOpenSupport.OpenPlan plan = support.planOpen(false, false);

        assertFalse(plan.shouldShowDialog());
        assertFalse(plan.shouldCreateDialog());
        assertEquals("Developer mode is required to open Analyzers.", plan.statusMessage());
    }

    @Test
    void planOpen_shouldCreateAndShowDialogWhenDeveloperModeIsEnabledWithoutReusableDialog() {
        GuiAnalyzerDialogOpenSupport.OpenPlan plan = support.planOpen(true, false);

        assertTrue(plan.shouldShowDialog());
        assertTrue(plan.shouldCreateDialog());
        assertEquals(null, plan.statusMessage());
    }

    @Test
    void planOpen_shouldReuseDisplayableDialogWhenDeveloperModeIsEnabled() {
        GuiAnalyzerDialogOpenSupport.OpenPlan plan = support.planOpen(true, true);

        assertTrue(plan.shouldShowDialog());
        assertFalse(plan.shouldCreateDialog());
        assertEquals(null, plan.statusMessage());
    }
}
