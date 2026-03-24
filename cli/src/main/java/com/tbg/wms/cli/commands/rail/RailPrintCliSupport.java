/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.rail.RailCarCard;
import com.tbg.wms.core.rail.RailCardRenderer;
import com.tbg.wms.core.rail.RailWorkflowService;

import java.util.List;

/**
 * Encapsulates rail-print CLI validation and preview text formatting.
 */
final class RailPrintCliSupport {

    String validateOptions(boolean validateSystemDefaultPrint, boolean template, String trainId, boolean print) {
        if (validateSystemDefaultPrint && (template || (trainId != null && !trainId.isBlank()) || print)) {
            return "Error: --validate-system-default-print cannot be combined with train/template print options.";
        }
        if (!validateSystemDefaultPrint && !template && (trainId == null || trainId.isBlank())) {
            return "Error: --train is required unless --template is used.";
        }
        return null;
    }

    String buildPreviewText(RailWorkflowService.RailWorkflowResult result) {
        StringBuilder sb = new StringBuilder();
        List<RailCarCard> cards = result.getCards();
        sb.append(System.lineSeparator());
        sb.append("SEQ   VEHICLE      CAN   DOM   KEV").append(System.lineSeparator());
        for (RailCarCard card : cards) {
            sb.append(String.format("%-5s %-12s %4d %4d %4d%n",
                    card.getSequence(),
                    card.getVehicleId(),
                    card.getCanPallets(),
                    card.getDomPallets(),
                    card.getKevPallets()));
        }
        sb.append(System.lineSeparator());
        sb.append("Railcars: ").append(cards.size()).append(System.lineSeparator());
        sb.append("WMS rows: ").append(result.getRawRows().size()).append(System.lineSeparator());
        sb.append("Resolved footprints: ").append(result.getResolvedFootprints().size()).append(System.lineSeparator());
        sb.append("Unresolved short codes: ").append(result.getUnresolvedShortCodes().size()).append(System.lineSeparator());
        if (!result.getMissingItemsInCards().isEmpty()) {
            sb.append("Missing in card math: ")
                    .append(String.join(", ", result.getMissingItemsInCards()))
                    .append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        return sb.toString();
    }

    RailCardRenderer railRenderer(AppConfig config) {
        return new RailCardRenderer(
                (float) config.railLabelCenterGapInches(),
                (float) config.railLabelOffsetXInches(),
                (float) config.railLabelOffsetYInches()
        );
    }
}
