package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelDataBuilder;
import com.tbg.wms.core.label.LabelType;
import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.template.ZplTemplateEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class PalletLabelPrintTaskSupport {
    private static final int MAX_LABELS_PER_JOB = 10_000;
    private static final int MAX_ARTIFACT_SLUG_LENGTH = 64;

    List<AdvancedPrintWorkflowService.PrintTask> buildPalletLabelTasks(PrintTaskPlanner.ShipmentPrintBatch batch) {
        Objects.requireNonNull(batch, "batch cannot be null");
        LabelWorkflowService.PreparedJob job = batch.getShipmentJob();
        List<Lpn> lpnsToPrint = batch.getLpnsToPrint();
        int labelCount = lpnsToPrint.size();
        if (labelCount > MAX_LABELS_PER_JOB) {
            throw new IllegalArgumentException("Label count exceeds max limit: " + MAX_LABELS_PER_JOB);
        }

        LabelDataBuilder builder = new LabelDataBuilder(job.getSkuMapping(), job.getSiteConfig(), job.getFootprintBySku());
        Shipment shipmentForLabels = LabelingSupport.buildShipmentForLabeling(job.getShipment(), lpnsToPrint);
        List<AdvancedPrintWorkflowService.PrintTask> tasks = new ArrayList<>(labelCount);
        String safeShipmentId = ArtifactNameSupport.safeSlug(job.getShipmentId(), "shipment", MAX_ARTIFACT_SLUG_LENGTH);
        String stopSuffix = batch.getStopPosition() == null ? "" : (" stop " + batch.getStopPosition());
        for (int i = 0; i < labelCount; i++) {
            Lpn lpn = lpnsToPrint.get(i);
            Map<String, String> data = new LinkedHashMap<>(builder.build(shipmentForLabels, lpn, i, LabelType.WALMART_CANADA_GRID));
            if (batch.getStopSequence() != null) {
                data.put("stopSequence", String.valueOf(batch.getStopSequence()));
            }
            if (job.isUsingVirtualLabels()) {
                data.put("palletSeq", String.valueOf(i + 1));
                data.put("palletTotal", String.valueOf(labelCount));
            }
            String safeLpnId = ArtifactNameSupport.safeSlug(lpn.getLpnId(), "lpn", MAX_ARTIFACT_SLUG_LENGTH);
            tasks.add(new AdvancedPrintWorkflowService.PrintTask(
                    AdvancedPrintWorkflowService.TaskKind.PALLET_LABEL,
                    String.format("%s_%s_%d_of_%d.zpl", safeShipmentId, safeLpnId, i + 1, labelCount),
                    ZplTemplateEngine.generate(job.getTemplate(), data),
                    job.getShipmentId() + ":" + lpn.getLpnId() + stopSuffix
            ));
        }
        return tasks;
    }
}
