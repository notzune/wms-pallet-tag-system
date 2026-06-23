package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RenderRailPdf {
    private final RailPdfRenderer renderer;

    public RenderRailPdf(RailPdfRenderer renderer) {
        if (renderer == null) {
            throw new IllegalArgumentException("Rail PDF renderer is required.");
        }
        this.renderer = renderer;
    }

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
