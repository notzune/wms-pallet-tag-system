package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Adapts GUI print execution callbacks to the advanced print workflow service.
 */
final class GuiAdvancedPrintRunner implements GuiPrintExecutionSupport.PrintRunner {
    private final Gateway gateway;

    GuiAdvancedPrintRunner(AdvancedPrintWorkflowService advancedService) {
        this(new Gateway() {
            @Override
            public AdvancedPrintWorkflowService.PrintResult printShipmentJob(
                    LabelWorkflowService.PreparedJob preparedJob,
                    List<Lpn> selectedLpns,
                    String printerId,
                    Path outputDir,
                    boolean printToFile,
                    boolean includeInfoTags
            ) throws Exception {
                return advancedService.printShipmentJob(
                        preparedJob,
                        selectedLpns,
                        printerId,
                        outputDir,
                        printToFile,
                        includeInfoTags
                );
            }

            @Override
            public AdvancedPrintWorkflowService.PrintResult printCarrierMoveJob(
                    AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                    List<LabelSelectionRef> selectedLabels,
                    String printerId,
                    Path outputDir,
                    boolean printToFile,
                    boolean includeInfoTags
            ) throws Exception {
                return advancedService.printCarrierMoveJob(
                        preparedCarrierJob,
                        selectedLabels,
                        printerId,
                        outputDir,
                        printToFile,
                        includeInfoTags
                );
            }
        });
    }

    GuiAdvancedPrintRunner(Gateway gateway) {
        this.gateway = Objects.requireNonNull(gateway, "gateway cannot be null");
    }

    @Override
    public AdvancedPrintWorkflowService.PrintResult printShipment(
            LabelWorkflowService.PreparedJob preparedJob,
            List<Lpn> selectedLpns,
            String printerId,
            Path outputDir,
            boolean printToFile,
            boolean includeInfoTags
    ) throws Exception {
        return gateway.printShipmentJob(
                preparedJob,
                selectedLpns,
                printerId,
                outputDir,
                printToFile,
                includeInfoTags
        );
    }

    @Override
    public AdvancedPrintWorkflowService.PrintResult printCarrierMove(
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            List<LabelSelectionRef> selectedLabels,
            String printerId,
            Path outputDir,
            boolean printToFile,
            boolean includeInfoTags
    ) throws Exception {
        return gateway.printCarrierMoveJob(
                preparedCarrierJob,
                selectedLabels,
                printerId,
                outputDir,
                printToFile,
                includeInfoTags
        );
    }

    interface Gateway {
        AdvancedPrintWorkflowService.PrintResult printShipmentJob(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) throws Exception;

        AdvancedPrintWorkflowService.PrintResult printCarrierMoveJob(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                String printerId,
                Path outputDir,
                boolean printToFile,
                boolean includeInfoTags
        ) throws Exception;
    }
}
