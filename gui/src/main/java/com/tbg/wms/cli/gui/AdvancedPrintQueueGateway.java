/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import java.nio.file.Path;
import java.util.Objects;

final class AdvancedPrintQueueGateway implements QueueWorkflowSupport.PreparationGateway, QueueWorkflowSupport.PrintGateway {
    private final ShipmentPreparer shipmentPreparer;
    private final CarrierMovePreparer carrierMovePreparer;
    private final ShipmentPrinter shipmentPrinter;
    private final CarrierMovePrinter carrierMovePrinter;

    AdvancedPrintQueueGateway(
            ShipmentPreparer shipmentPreparer,
            CarrierMovePreparer carrierMovePreparer,
            ShipmentPrinter shipmentPrinter,
            CarrierMovePrinter carrierMovePrinter
    ) {
        this.shipmentPreparer = Objects.requireNonNull(shipmentPreparer, "shipmentPreparer cannot be null");
        this.carrierMovePreparer = Objects.requireNonNull(carrierMovePreparer, "carrierMovePreparer cannot be null");
        this.shipmentPrinter = Objects.requireNonNull(shipmentPrinter, "shipmentPrinter cannot be null");
        this.carrierMovePrinter = Objects.requireNonNull(carrierMovePrinter, "carrierMovePrinter cannot be null");
    }

    @Override
    public LabelWorkflowService.PreparedJob prepareShipment(String shipmentId) throws Exception {
        return shipmentPreparer.prepareShipment(shipmentId);
    }

    @Override
    public AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMove(String carrierMoveId) throws Exception {
        return carrierMovePreparer.prepareCarrierMove(carrierMoveId);
    }

    @Override
    public AdvancedPrintWorkflowService.PrintResult printShipment(
            LabelWorkflowService.PreparedJob shipmentJob,
            String printerId,
            Path outputDir,
            boolean printToFile
    ) throws Exception {
        return shipmentPrinter.printShipment(shipmentJob, printerId, outputDir, printToFile);
    }

    @Override
    public AdvancedPrintWorkflowService.PrintResult printCarrierMove(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob,
            String printerId,
            Path outputDir,
            boolean printToFile
    ) throws Exception {
        return carrierMovePrinter.printCarrierMove(carrierMoveJob, printerId, outputDir, printToFile);
    }

    interface ShipmentPreparer {
        LabelWorkflowService.PreparedJob prepareShipment(String shipmentId) throws Exception;
    }

    interface CarrierMovePreparer {
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMove(String carrierMoveId) throws Exception;
    }

    interface ShipmentPrinter {
        AdvancedPrintWorkflowService.PrintResult printShipment(
                LabelWorkflowService.PreparedJob shipmentJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) throws Exception;
    }

    interface CarrierMovePrinter {
        AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) throws Exception;
    }
}
