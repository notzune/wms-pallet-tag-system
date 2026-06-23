package com.tbg.wms.v2.desktop.labels;

import java.nio.file.Path;

public interface LabelPrintWorkflow {
    LabelPreview previewShipment(String shipmentId);

    LabelPreview previewCarrierMove(String carrierMoveId);

    LabelPrintResult printToFile(LabelPreview preview, Path outputDir);
}
