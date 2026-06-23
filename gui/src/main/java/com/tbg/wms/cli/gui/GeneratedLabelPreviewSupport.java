package com.tbg.wms.cli.gui;

import com.tbg.wms.core.label.LabelSelectionRef;
import com.tbg.wms.core.model.Lpn;

import java.util.List;
import java.util.Objects;

final class GeneratedLabelPreviewSupport {

    private final DocumentBuilder documentBuilder;

    GeneratedLabelPreviewSupport(GuiZplPreviewSupport zplPreviewSupport) {
        this(new ZplPreviewDocumentBuilder(zplPreviewSupport));
    }

    GeneratedLabelPreviewSupport(DocumentBuilder documentBuilder) {
        this.documentBuilder = Objects.requireNonNull(documentBuilder, "documentBuilder cannot be null");
    }

    String title(boolean carrierMoveMode) {
        return carrierMoveMode ? "Carrier Move Label Preview" : "Shipment Label Preview";
    }

    List<GuiZplPreviewSupport.PreviewDocument> buildDocuments(Context context) {
        Objects.requireNonNull(context, "context cannot be null");
        return context.carrierMoveMode()
                ? documentBuilder.buildCarrierMoveDocuments(
                Objects.requireNonNull(context.preparedCarrierJob(), "preparedCarrierJob cannot be null"),
                context.selection().selectedCarrierLabels(),
                context.includeInfoTags()
        )
                : documentBuilder.buildShipmentDocuments(
                Objects.requireNonNull(context.preparedJob(), "preparedJob cannot be null"),
                context.selection().selectedShipmentLpns(),
                context.includeInfoTags()
        );
    }

    record Context(
            boolean carrierMoveMode,
            LabelWorkflowService.PreparedJob preparedJob,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
            PreviewSelectionSupport.SelectionSnapshot selection,
            boolean includeInfoTags
    ) {
        Context {
            Objects.requireNonNull(selection, "selection cannot be null");
        }
    }

    interface DocumentBuilder {
        List<GuiZplPreviewSupport.PreviewDocument> buildShipmentDocuments(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                boolean includeInfoTags
        );

        List<GuiZplPreviewSupport.PreviewDocument> buildCarrierMoveDocuments(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                boolean includeInfoTags
        );
    }

    private static final class ZplPreviewDocumentBuilder implements DocumentBuilder {
        private final GuiZplPreviewSupport zplPreviewSupport;

        private ZplPreviewDocumentBuilder(GuiZplPreviewSupport zplPreviewSupport) {
            this.zplPreviewSupport = Objects.requireNonNull(zplPreviewSupport, "zplPreviewSupport cannot be null");
        }

        @Override
        public List<GuiZplPreviewSupport.PreviewDocument> buildShipmentDocuments(
                LabelWorkflowService.PreparedJob preparedJob,
                List<Lpn> selectedLpns,
                boolean includeInfoTags
        ) {
            return zplPreviewSupport.buildShipmentDocuments(preparedJob, selectedLpns, includeInfoTags);
        }

        @Override
        public List<GuiZplPreviewSupport.PreviewDocument> buildCarrierMoveDocuments(
                AdvancedPrintWorkflowService.PreparedCarrierMoveJob preparedCarrierJob,
                List<LabelSelectionRef> selectedLabels,
                boolean includeInfoTags
        ) {
            return zplPreviewSupport.buildCarrierMoveDocuments(preparedCarrierJob, selectedLabels, includeInfoTags);
        }
    }
}
