package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides render rail pdf behavior for WMS 2.0 workflows.
 */
public final class RenderRailPdf {
    private final RailPdfRenderer renderer;

    /**
     * Creates a rail PDF rendering use case.
     *
     * @param renderer the renderer.
     */
    public RenderRailPdf(RailPdfRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("Rail PDF renderer is required.");
        }
        this.renderer = renderer;
    }

    /**
     * Renders the requested output artifact.
     *
     * @param request the request.
     * @return the rendered artifact path.
     */
    public Path render(RailPdfRequest request) {
        Set<String> selectedSequences = new HashSet<>(request.selectedSequences());
        List<RailCarCard> selectedCards = selectedSequences.isEmpty()
                ? request.plan().cards()
                : request.plan().cards().stream()
                .filter(card -> selectedSequences.contains(card.sequence()))
                .toList();
        return renderer.render(selectedCards, request.outputPdf());
    }
}
