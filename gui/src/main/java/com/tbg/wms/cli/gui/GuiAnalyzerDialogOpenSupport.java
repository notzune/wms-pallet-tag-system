/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.9.1
 */
package com.tbg.wms.cli.gui;

/**
 * Pure policy helper for opening the developer-only analyzer dialog.
 */
final class GuiAnalyzerDialogOpenSupport {

    OpenPlan planOpen(boolean developerModeEnabled, boolean existingDialogDisplayable) {
        if (!developerModeEnabled) {
            return new OpenPlan(false, false, "Developer mode is required to open Analyzers.");
        }
        return new OpenPlan(true, !existingDialogDisplayable, null);
    }

    record OpenPlan(
            boolean shouldShowDialog,
            boolean shouldCreateDialog,
            String statusMessage
    ) {
    }
}
