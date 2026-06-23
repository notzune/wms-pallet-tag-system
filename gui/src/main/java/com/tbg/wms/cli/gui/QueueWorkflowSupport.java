/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Queue parsing and aggregation policy shared by GUI print workflows.
 */
final class QueueWorkflowSupport {
    private final PreparationGateway preparationGateway;
    private final PrintGateway printGateway;

    QueueWorkflowSupport(PreparationGateway preparationGateway, PrintGateway printGateway) {
        this.preparationGateway = Objects.requireNonNull(preparationGateway, "preparationGateway cannot be null");
        this.printGateway = Objects.requireNonNull(printGateway, "printGateway cannot be null");
    }

    AdvancedPrintWorkflowService.PreparedQueueJob prepareQueue(
            List<AdvancedPrintWorkflowService.QueueRequestItem> requests,
            int maxQueueItems
    ) throws Exception {
        List<AdvancedPrintWorkflowService.QueueRequestItem> normalizedRequests =
                normalizeRequests(requests, maxQueueItems);
        List<AdvancedPrintWorkflowService.PreparedQueueItem> resolved =
                new ArrayList<>(normalizedRequests.size());
        for (AdvancedPrintWorkflowService.QueueRequestItem req : normalizedRequests) {
            if (req.getType() == AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE) {
                resolved.add(AdvancedPrintWorkflowService.PreparedQueueItem.forCarrier(
                        req.getId(),
                        preparationGateway.prepareCarrierMove(req.getId())
                ));
            } else {
                resolved.add(AdvancedPrintWorkflowService.PreparedQueueItem.forShipment(
                        req.getId(),
                        preparationGateway.prepareShipment(req.getId())
                ));
            }
        }
        return new AdvancedPrintWorkflowService.PreparedQueueJob(resolved);
    }

    List<AdvancedPrintWorkflowService.QueueRequestItem> normalizeRequests(
            List<AdvancedPrintWorkflowService.QueueRequestItem> requests,
            int maxQueueItems
    ) {
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty.");
        }
        if (requests.size() > maxQueueItems) {
            throw new IllegalArgumentException("Queue exceeds max size of " + maxQueueItems + " items.");
        }
        List<AdvancedPrintWorkflowService.QueueRequestItem> normalized = new ArrayList<>();
        for (AdvancedPrintWorkflowService.QueueRequestItem request : requests) {
            if (request == null || request.getId().isBlank()) {
                continue;
            }
            normalized.add(request);
        }
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Queue is empty after parsing.");
        }
        return List.copyOf(normalized);
    }

    AdvancedPrintWorkflowService.QueuePrintResult printQueue(
            AdvancedPrintWorkflowService.PreparedQueueJob queue,
            String printerId,
            boolean printToFile
    ) throws Exception {
        List<AdvancedPrintWorkflowService.PrintResult> results = new ArrayList<>(queue.getItems().size());
        for (AdvancedPrintWorkflowService.PreparedQueueItem item : queue.getItems()) {
            AdvancedPrintWorkflowService.PrintResult result =
                    item.getType() == AdvancedPrintWorkflowService.QueueItemType.CARRIER_MOVE
                            ? printGateway.printCarrierMove(item.getCarrierMoveJob(), printerId, null, printToFile)
                            : printGateway.printShipment(item.getShipmentJob(), printerId, null, printToFile);
            results.add(result);
        }
        return summarizeResults(results);
    }

    AdvancedPrintWorkflowService.QueuePrintResult summarizeResults(List<AdvancedPrintWorkflowService.PrintResult> results) {
        int labels = 0;
        int infoTags = 0;
        for (AdvancedPrintWorkflowService.PrintResult result : results) {
            labels += result.getLabelsPrinted();
            infoTags += result.getInfoTagsPrinted();
        }
        return new AdvancedPrintWorkflowService.QueuePrintResult(results, labels, infoTags);
    }

    interface PreparationGateway {
        LabelWorkflowService.PreparedJob prepareShipment(String shipmentId) throws Exception;

        AdvancedPrintWorkflowService.PreparedCarrierMoveJob prepareCarrierMove(String carrierMoveId) throws Exception;
    }

    interface PrintGateway {
        AdvancedPrintWorkflowService.PrintResult printShipment(
                LabelWorkflowService.PreparedJob shipmentJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) throws Exception;

        AdvancedPrintWorkflowService.PrintResult printCarrierMove(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierMoveJob,
                String printerId,
                Path outputDir,
                boolean printToFile
        ) throws Exception;
    }
}
