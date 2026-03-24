package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.PalletPlanningService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;

import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

final class PreviewSelectionTestData {
    private PreviewSelectionTestData() {
    }

    static LabelWorkflowService.PreparedJob shipmentJob(String shipmentId, List<Lpn> lpns) {
        try {
            Constructor<LabelWorkflowService.PreparedJob> ctor = LabelWorkflowService.PreparedJob.class.getDeclaredConstructor(
                    String.class,
                    Shipment.class,
                    PrinterRoutingService.class,
                    SiteConfig.class,
                    SkuMappingService.class,
                    LabelTemplate.class,
                    Map.class,
                    PalletPlanningService.PlanResult.class,
                    List.class,
                    List.class,
                    boolean.class,
                    String.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(
                    shipmentId,
                    shipment(shipmentId, lpns),
                    null,
                    null,
                    null,
                    null,
                    Map.<String, ShipmentSkuFootprint>of(),
                    planResult(lpns.size()),
                    lpns,
                    List.of(),
                    false,
                    "STAGE"
            );
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PreparedJob test fixture.", ex);
        }
    }

    static AdvancedPrintWorkflowService.PreparedStopGroup stopGroup(
            Integer stopSequence,
            int stopPosition,
            List<LabelWorkflowService.PreparedJob> jobs
    ) {
        try {
            Constructor<AdvancedPrintWorkflowService.PreparedStopGroup> ctor =
                    AdvancedPrintWorkflowService.PreparedStopGroup.class.getDeclaredConstructor(Integer.class, int.class, List.class);
            ctor.setAccessible(true);
            return ctor.newInstance(stopSequence, stopPosition, jobs);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PreparedStopGroup test fixture.", ex);
        }
    }

    static AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob(
            String carrierMoveId,
            List<AdvancedPrintWorkflowService.PreparedStopGroup> stopGroups
    ) {
        try {
            Constructor<AdvancedPrintWorkflowService.PreparedCarrierMoveJob> ctor =
                    AdvancedPrintWorkflowService.PreparedCarrierMoveJob.class.getDeclaredConstructor(String.class, List.class);
            ctor.setAccessible(true);
            return ctor.newInstance(carrierMoveId, stopGroups);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PreparedCarrierMoveJob test fixture.", ex);
        }
    }

    private static Shipment shipment(String shipmentId, List<Lpn> lpns) {
        return new Shipment(
                shipmentId,
                shipmentId + "-EXT",
                shipmentId + "-ORDER",
                "3002",
                "Ship To",
                "123 Any St",
                null,
                null,
                "City",
                "ST",
                "12345",
                "USA",
                null,
                "CARRIER",
                "TL",
                null,
                null,
                null,
                null,
                "6080",
                null,
                null,
                1,
                "CMID",
                null,
                null,
                "R",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                lpns
        );
    }

    private static PalletPlanningService.PlanResult planResult(int labelCount) {
        try {
            Constructor<PalletPlanningService.PlanResult> ctor = PalletPlanningService.PlanResult.class.getDeclaredConstructor(
                    int.class,
                    int.class,
                    int.class,
                    int.class,
                    List.class
            );
            ctor.setAccessible(true);
            return ctor.newInstance(labelCount, labelCount, 0, labelCount, List.of());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to create PlanResult test fixture.", ex);
        }
    }
}
