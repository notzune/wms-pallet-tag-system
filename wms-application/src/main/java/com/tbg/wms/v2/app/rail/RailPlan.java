package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;

import java.util.List;
import java.util.Locale;

public record RailPlan(String trainId, List<RailCarCard> cards, List<String> missingFootprintItems) {
    public RailPlan {
        trainId = trainId == null ? "" : trainId.trim().toUpperCase(Locale.ROOT);
        cards = List.copyOf(cards == null ? List.of() : cards);
        missingFootprintItems = List.copyOf(missingFootprintItems == null ? List.of() : missingFootprintItems);
    }
}
