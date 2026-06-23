package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;

import java.nio.file.Path;
import java.util.List;

@FunctionalInterface
public interface RailPdfRenderer {
    Path render(List<RailCarCard> cards, Path outputPdf);
}
