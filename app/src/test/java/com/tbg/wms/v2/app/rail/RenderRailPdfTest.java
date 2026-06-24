package com.tbg.wms.v2.app.rail;

import com.tbg.wms.v2.domain.rail.RailCarCard;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RenderRailPdfTest {
    @Test
    void renderRailPdf_delegatesToRendererWithSelectedCards() {
        RecordingRenderer renderer = new RecordingRenderer();
        RenderRailPdf renderRailPdf = new RenderRailPdf(renderer);
        RailPlan plan = new RailPlan(
                "TRAIN1",
                List.of(card("1"), card("2")),
                List.of()
        );

        Path output = renderRailPdf.render(new RailPdfRequest(plan, List.of("2"), Path.of("out", "rail.pdf")));

        assertEquals(Path.of("out", "rail.pdf"), output);
        assertEquals(List.of("2"), renderer.renderedCards.stream().map(RailCarCard::sequence).toList());
    }

    private static RailCarCard card(String sequence) {
        return new RailCarCard(
                "TRAIN1",
                sequence,
                "CAR" + sequence,
                "LOAD" + sequence,
                "TRAIN1 BR LOAD" + sequence,
                List.of(new RailItemQuantity("ITEM", 10)),
                0,
                1,
                0,
                List.of("DOM:100"),
                List.of()
        );
    }

    private static final class RecordingRenderer implements RailPdfRenderer {
        private List<RailCarCard> renderedCards = List.of();

        @Override
        public Path render(List<RailCarCard> cards, Path outputPdf) {
            renderedCards = List.copyOf(cards);
            return outputPdf;
        }
    }
}
