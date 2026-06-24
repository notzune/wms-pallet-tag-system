package com.tbg.wms.v2.desktop.labels;

import java.nio.file.Path;

/**
 * Defines the contract for label print workflow behavior in WMS 2.0 workflows.
 */
public interface LabelPrintWorkflow {
    /**
     * Previews shipment.
     *
     * @param shipmentId the shipment id.
     * @return the preview.
     */
    LabelPreview previewShipment(String shipmentId);

    /**
     * Previews carrier move.
     *
     * @param carrierMoveId the carrier move id.
     * @return the preview.
     */
    LabelPreview previewCarrierMove(String carrierMoveId);

    /**
     * Prints to file.
     *
     * @param preview the preview.
     * @param outputDir the output dir.
     * @return print-to-file result
     */
    LabelPrintResult printToFile(LabelPreview preview, Path outputDir);
}
