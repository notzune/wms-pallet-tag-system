package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;

import java.nio.file.Path;
import java.util.List;

/**
 * Defines the contract for rail pdf renderer behavior in WMS 2.0 workflows.
 */
@FunctionalInterface
public interface RailPdfRenderer {
    /**
     * Renders the requested output.
     *
     * @param cards the cards.
     * @param outputPdf the output pdf.
     * @return the rendered output path.
     */
    Path render(List<RailCarCard> cards, Path outputPdf);
}
