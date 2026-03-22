/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;

import java.util.List;
import java.util.Objects;

/**
 * Builds preview summary and math text for the GUI shell.
 */
final class GuiPreviewDisplaySupport {

    private final LabelPreviewFormatter previewFormatter;
    private final int maxPreviewStops;
    private final int maxPreviewShipmentsPerStop;
    private final int maxPreviewSkuRowsPerShipment;

    GuiPreviewDisplaySupport(
            LabelPreviewFormatter previewFormatter,
            int maxPreviewStops,
            int maxPreviewShipmentsPerStop,
            int maxPreviewSkuRowsPerShipment
    ) {
        this.previewFormatter = Objects.requireNonNull(previewFormatter, "previewFormatter cannot be null");
        this.maxPreviewStops = maxPreviewStops;
        this.maxPreviewShipmentsPerStop = maxPreviewShipmentsPerStop;
        this.maxPreviewSkuRowsPerShipment = maxPreviewSkuRowsPerShipment;
    }

    PreviewText buildShipmentPreview(
            LabelWorkflowService.PreparedJob preparedJob,
            PreviewSelectionSupport.SelectionSnapshot selection
    ) {
        Objects.requireNonNull(preparedJob, "preparedJob cannot be null");
        Objects.requireNonNull(selection, "selection cannot be null");
        return new PreviewText(
                previewFormatter.buildShipmentSummaryText(
                        preparedJob,
                        selection.selectedShipmentLpns(),
                        selection.infoTagCount()
                ),
                previewFormatter.buildShipmentMathText(
                        preparedJob,
                        maxPreviewSkuRowsPerShipment,
                        selection.selectedLabelCount(),
                        selection.infoTagCount()
                )
        );
    }

    PreviewText buildCarrierMovePreview(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            List<LabelSelectionRef> selectedLabels,
            int infoTagCount
    ) {
        Objects.requireNonNull(preparedCarrierJob, "preparedCarrierJob cannot be null");
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");

        StringBuilder summary = new StringBuilder(previewFormatter.buildCarrierMoveSummary(
                preparedCarrierJob,
                selectedLabels.size(),
                infoTagCount
        ));
        int shownStops = Math.min(preparedCarrierJob.getStopGroups().size(), maxPreviewStops);
        if (preparedCarrierJob.getStopGroups().size() > shownStops) {
            summary.append("Preview Notice: Showing first ")
                    .append(maxPreviewStops)
                    .append(" stops of ")
                    .append(preparedCarrierJob.getStopGroups().size())
                    .append(".\n");
        }

        return new PreviewText(
                summary.toString(),
                previewFormatter.buildCarrierMoveMathText(
                        preparedCarrierJob,
                        maxPreviewStops,
                        maxPreviewShipmentsPerStop,
                        selectedLabels.size(),
                        infoTagCount
                )
        );
    }

    record PreviewText(
            String summaryText,
            String mathText
    ) {
    }
}
