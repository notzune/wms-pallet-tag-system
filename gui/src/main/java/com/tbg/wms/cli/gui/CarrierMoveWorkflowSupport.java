package com.tbg.wms.cli.gui;

import com.tbg.wms.core.model.CarrierMoveStopRef;
import com.tbg.wms.db.DbQueryRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class CarrierMoveWorkflowSupport {
    private final CarrierMovePreparationSupport preparationSupport = new CarrierMovePreparationSupport();

    AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMoveJob(
            String carrierMoveId,
            DbQueryRepository repository,
            ShipmentResolver shipmentResolver
    ) throws Exception {
        Objects.requireNonNull(repository, "repository cannot be null");
        Objects.requireNonNull(shipmentResolver, "shipmentResolver cannot be null");
        if (carrierMoveId == null || carrierMoveId.isBlank()) {
            throw new IllegalArgumentException("Carrier Move ID is required.");
        }
        String cmid = carrierMoveId.trim();

        List<CarrierMoveStopRef> refs = repository.findCarrierMoveStops(cmid);
        if (refs.isEmpty()) {
            throw new IllegalArgumentException("Carrier Move not found or has no shipments: " + cmid);
        }

        List<AdvancedPrintWorkflowService.PreparedStopGroup> groups = buildPreparedStopGroups(
                repository,
                shipmentResolver,
                refs
        );
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Carrier Move has no printable shipments: " + cmid);
        }

        return new AdvancedPrintWorkflowService.PreparedCarrierMoveJob(cmid, groups);
    }

    private List<AdvancedPrintWorkflowService.PreparedStopGroup> buildPreparedStopGroups(
            DbQueryRepository repository,
            ShipmentResolver shipmentResolver,
            List<CarrierMoveStopRef> refs
    ) throws Exception {
        List<CarrierMovePreparationSupport.StopShipmentPlan> plans = preparationSupport.buildStopShipmentPlans(refs);
        List<AdvancedPrintWorkflowService.PreparedStopGroup> groups = new ArrayList<>(plans.size());
        int stopPosition = 1;
        for (CarrierMovePreparationSupport.StopShipmentPlan plan : plans) {
            List<LabelWorkflowService.PreparedJob> jobs = resolvePreparedJobsForStop(
                    repository,
                    shipmentResolver,
                    plan.shipmentIds()
            );
            if (!jobs.isEmpty()) {
                groups.add(new AdvancedPrintWorkflowService.PreparedStopGroup(plan.stopSequence(), stopPosition, jobs));
                stopPosition++;
            }
        }
        return groups;
    }

    private List<LabelWorkflowService.PreparedJob> resolvePreparedJobsForStop(
            DbQueryRepository repository,
            ShipmentResolver shipmentResolver,
            List<String> shipmentIds
    ) throws Exception {
        List<LabelWorkflowService.PreparedJob> jobs = new ArrayList<>(shipmentIds.size());
        for (String shipmentId : shipmentIds) {
            jobs.add(shipmentResolver.prepare(repository, shipmentId));
        }
        return jobs;
    }

    @FunctionalInterface
    interface ShipmentResolver {
        LabelWorkflowService.PreparedJob prepare(DbQueryRepository repository, String shipmentId) throws Exception;
    }
}
