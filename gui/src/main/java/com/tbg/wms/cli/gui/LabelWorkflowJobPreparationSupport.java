package com.tbg.wms.cli.gui;

import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.db.DbQueryRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Loads shipment-backed planning inputs used by the GUI workflow service.
 */
final class LabelWorkflowJobPreparationSupport {

    LoadedShipmentData loadShipmentData(
            DbQueryRepository queryRepo,
            String shipmentId,
            LabelWorkflowPlanningSupport planningSupport
    ) {
        Objects.requireNonNull(queryRepo, "queryRepo cannot be null");
        Objects.requireNonNull(planningSupport, "planningSupport cannot be null");
        String normalizedShipmentId = shipmentId == null ? "" : shipmentId.trim();
        if (normalizedShipmentId.isEmpty()) {
            throw new IllegalArgumentException("Shipment ID is required.");
        }

        if (!queryRepo.shipmentExists(normalizedShipmentId)) {
            throw new IllegalArgumentException("Shipment not found: " + normalizedShipmentId);
        }

        Shipment shipment = queryRepo.findShipmentWithLpnsAndLineItems(normalizedShipmentId);
        if (shipment == null) {
            throw new IllegalStateException("Could not retrieve shipment data.");
        }

        List<ShipmentSkuFootprint> footprintRows = queryRepo.findShipmentSkuFootprints(normalizedShipmentId);
        Map<String, ShipmentSkuFootprint> footprintBySku = LabelingSupport.buildFootprintMap(footprintRows);
        PalletPlanningService.PlanResult planResult = new PalletPlanningService().plan(footprintRows);
        List<Lpn> lpnsForLabels = planningSupport.resolveLpnsForLabeling(shipment, footprintRows);
        boolean usingVirtualLabels = shipment.getLpnCount() == 0 && !lpnsForLabels.isEmpty();
        String stagingLocation = queryRepo.getStagingLocation(normalizedShipmentId);
        return new LoadedShipmentData(
                normalizedShipmentId,
                shipment,
                footprintRows,
                footprintBySku,
                planResult,
                lpnsForLabels,
                usingVirtualLabels,
                stagingLocation
        );
    }

    record LoadedShipmentData(
            String shipmentId,
            Shipment shipment,
            List<ShipmentSkuFootprint> footprintRows,
            Map<String, ShipmentSkuFootprint> footprintBySku,
            PalletPlanningService.PlanResult planResult,
            List<Lpn> lpnsForLabels,
            boolean usingVirtualLabels,
            String stagingLocation
    ) {
    }
}
