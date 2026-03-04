package com.tbg.wms.core.rail;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailCardRendererTest {

    @Test
    void renderPdfCreatesLetterDocumentWithMultipleCards() throws Exception {
        List<RailCarCard> cards = List.of(
                new RailCarCard("0124", "142", "CAR1", "L1",
                        List.of(new RailStopRecord.ItemQuantity("01830", 120)),
                        2, 1, List.of("DOM:100"), List.of()),
                new RailCarCard("0124", "143", "CAR2", "L2",
                        List.of(new RailStopRecord.ItemQuantity("01831", 300)),
                        0, 6, List.of("CAN:60", "DOM:40"), List.of())
        );

        Path output = Files.createTempFile("rail-cards-test", ".pdf");
        new RailCardRenderer().renderPdf(cards, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
        try (PDDocument doc = PDDocument.load(output.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }
}
