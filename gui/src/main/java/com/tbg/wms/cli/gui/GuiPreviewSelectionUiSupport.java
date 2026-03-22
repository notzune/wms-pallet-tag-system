/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

/**
 * Pure helper for label-selection UI text and button-enable policy.
 */
final class GuiPreviewSelectionUiSupport {
    String selectionStatusText(int selected, int total, int infoTags, int totalDocuments) {
        return "Selected " + selected + " of " + total + " labels | Info Tags " + infoTags
                + " | Total Documents " + totalDocuments;
    }

    String selectionToggleText(int selected, int total) {
        return selected == total ? "Deselect All" : "Select All";
    }

    String collapseButtonText(boolean expanded) {
        return expanded ? "Label Selection [expanded]" : "Label Selection [collapsed]";
    }

    boolean shouldEnablePrint(
            boolean carrierMoveMode,
            boolean hasPreparedCarrierJob,
            boolean hasPreparedShipmentJob,
            int selectedCarrierLabels,
            int selectedShipmentLabels
    ) {
        if (carrierMoveMode && hasPreparedCarrierJob) {
            return selectedCarrierLabels > 0;
        }
        return !carrierMoveMode && hasPreparedShipmentJob && selectedShipmentLabels > 0;
    }
}
