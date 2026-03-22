/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.barcode.BarcodeZplBuilder;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.util.List;
import java.util.Objects;

/**
 * Builds preview-ready ZPL document sets from barcode, shipment, and carrier workflows.
 *
 * <p>This helper reuses the same task-planning paths used for real print execution so previewed
 * ZPL matches the exact documents that would be printed or written to file.</p>
 */
final class GuiZplPreviewSupport {

    List<PreviewDocument> buildBarcodeDocuments(BarcodeRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        String data = request.getData() == null || request.getData().isBlank() ? "barcode" : request.getData().trim();
        return List.of(new PreviewDocument("barcode-" + data + ".zpl", BarcodeZplBuilder.build(request)));
    }

    List<PreviewDocument> buildShipmentDocuments(
            LabelWorkflowService.PreparedJob preparedJob,
            List<Lpn> selectedLpns,
            boolean includeInfoTags
    ) {
        Objects.requireNonNull(preparedJob, "preparedJob cannot be null");
        List<AdvancedPrintWorkflowService.PrintTask> tasks = PrintTaskPlanner.buildShipmentTasks(
                PrintTaskPlanner.ShipmentPrintBatch.forShipment(preparedJob, selectedLpns, includeInfoTags)
        );
        return toPreviewDocuments(tasks);
    }

    List<PreviewDocument> buildCarrierMoveDocuments(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            List<LabelSelectionRef> selectedLabels,
            boolean includeInfoTags
    ) {
        Objects.requireNonNull(preparedCarrierJob, "preparedCarrierJob cannot be null");
        Objects.requireNonNull(selectedLabels, "selectedLabels cannot be null");
        return toPreviewDocuments(PrintTaskPlanner.buildCarrierMoveTasks(preparedCarrierJob, selectedLabels, includeInfoTags));
    }

    private List<PreviewDocument> toPreviewDocuments(List<AdvancedPrintWorkflowService.PrintTask> tasks) {
        Objects.requireNonNull(tasks, "tasks cannot be null");
        return tasks.stream()
                .map(task -> new PreviewDocument(task.fileName, task.zpl))
                .toList();
    }

    record PreviewDocument(String name, String zpl) {
        PreviewDocument {
            name = Objects.requireNonNullElse(name, "preview.zpl");
            zpl = Objects.requireNonNullElse(zpl, "");
        }
    }
}
