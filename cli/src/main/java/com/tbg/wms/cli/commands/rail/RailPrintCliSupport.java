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
        return buildPreviewText(
                List.of(result.getTrainId()),
                result.getCards(),
                result.getRawRows().size(),
                result.getResolvedFootprints().size(),
                result.getUnresolvedShortCodes(),
                result.getMissingItemsInCards()
        );
    }

    String buildPreviewText(RailWorkflowService.RailWorkflowBatchResult result) {
        return buildPreviewText(
                result.getTrainIds(),
                result.getCards(),
                result.getRawRows().size(),
                result.getResolvedFootprints().size(),
                result.getUnresolvedShortCodes(),
                result.getMissingItemsInCards()
        );
    }

    String fileNameToken(List<String> trainIds) {
        if (trainIds == null || trainIds.isEmpty()) {
            return "rail";
        }
        if (trainIds.size() == 1) {
            return trainIds.get(0);
        }
        return trainIds.get(0) + "-plus-" + (trainIds.size() - 1);
    }

    private String buildPreviewText(List<String> trainIds,
                                    List<RailCarCard> cards,
                                    int rawRowCount,
                                    int resolvedFootprintCount,
                                    java.util.Set<String> unresolvedShortCodes,
                                    java.util.Set<String> missingItemsInCards) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("Train IDs: ").append(String.join(", ", trainIds)).append(System.lineSeparator());
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
        sb.append("WMS rows: ").append(rawRowCount).append(System.lineSeparator());
        sb.append("Resolved footprints: ").append(resolvedFootprintCount).append(System.lineSeparator());
        sb.append("Unresolved short codes: ").append(unresolvedShortCodes.size()).append(System.lineSeparator());
        if (!missingItemsInCards.isEmpty()) {
            sb.append("Missing in card math: ")
                    .append(String.join(", ", missingItemsInCards))
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
