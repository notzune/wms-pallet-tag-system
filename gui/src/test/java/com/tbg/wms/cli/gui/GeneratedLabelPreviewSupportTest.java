package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class GeneratedLabelPreviewSupportTest {

    private final RecordingDocumentBuilder documentBuilder = new RecordingDocumentBuilder();
    private final GeneratedLabelPreviewSupport support = new GeneratedLabelPreviewSupport(documentBuilder);

    @Test
    void title_shouldReflectPreviewMode() {
        assertEquals("Carrier Move Label Preview", support.title(true));
        assertEquals("Shipment Label Preview", support.title(false));
    }

    @Test
    void buildDocuments_shouldDelegateShipmentSelections() {
        Lpn selected = new Lpn("LPN-1", "SHIP1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob job = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(selected));
        PreviewSelectionSupport.SelectionSnapshot selection =
                new PreviewSelectionSupport.SelectionSnapshot(List.of(), List.of(selected), List.of(), 1);

        List<GuiZplPreviewSupport.PreviewDocument> documents = support.buildDocuments(
                new GeneratedLabelPreviewSupport.Context(false, job, null, selection, true)
        );

        assertEquals(List.of(new GuiZplPreviewSupport.PreviewDocument("shipment.zpl", "^XA^XZ")), documents);
        assertSame(job, documentBuilder.shipmentJob);
        assertEquals(List.of(selected), documentBuilder.selectedLpns);
        assertEquals(true, documentBuilder.includeInfoTags);
    }

    @Test
    void buildDocuments_shouldDelegateCarrierMoveSelections() {
        Lpn selected = new Lpn("LPN-1", "SHIP1", null, 0, 0, 0.0, null, null, null, null, null, List.of());
        LabelWorkflowService.PreparedJob shipment = PreviewSelectionTestData.shipmentJob("SHIP1", List.of(selected));
        AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob = PreviewSelectionTestData.carrierMoveJob(
                "CMID1",
                List.of(PreviewSelectionTestData.stopGroup(10, 1, List.of(shipment)))
        );
        LabelSelectionRef selectedLabel = LabelSelectionRef.forCarrierMove(1, "SHIP1", "LPN-1", 1);
        PreviewSelectionSupport.SelectionSnapshot selection =
                new PreviewSelectionSupport.SelectionSnapshot(List.of(), List.of(), List.of(selectedLabel), 2);

        List<GuiZplPreviewSupport.PreviewDocument> documents = support.buildDocuments(
                new GeneratedLabelPreviewSupport.Context(true, null, carrierJob, selection, false)
        );

        assertEquals(List.of(new GuiZplPreviewSupport.PreviewDocument("carrier.zpl", "^XA^XZ")), documents);
        assertSame(carrierJob, documentBuilder.carrierJob);
        assertEquals(List.of(selectedLabel), documentBuilder.selectedLabels);
        assertEquals(false, documentBuilder.includeInfoTags);
    }

    private static final class RecordingDocumentBuilder implements GeneratedLabelPreviewSupport.DocumentBuilder {
        private LabelWorkflowService.PreparedJob shipmentJob;
        private AdvancedPrintWorkflowService.PreparedCarrierMoveJob carrierJob;
        private List<Lpn> selectedLpns = List.of();
        private List<LabelSelectionRef> selectedLabels = List.of();
        private boolean includeInfoTags;

        public List<GuiZplPreviewSupport.PreviewDocument> buildShipmentDocuments(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                boolean includeInfoTags
        ) {
            this.shipmentJob = preparedJob;
            this.selectedLpns = selectedLpns;
            this.includeInfoTags = includeInfoTags;
            return List.of(new GuiZplPreviewSupport.PreviewDocument("shipment.zpl", "^XA^XZ"));
        }

        public List<GuiZplPreviewSupport.PreviewDocument> buildCarrierMoveDocuments(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                boolean includeInfoTags
        ) {
            this.carrierJob = preparedCarrierJob;
            this.selectedLabels = selectedLabels;
            this.includeInfoTags = includeInfoTags;
            return List.of(new GuiZplPreviewSupport.PreviewDocument("carrier.zpl", "^XA^XZ"));
        }
    }
}
