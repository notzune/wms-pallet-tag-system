package com.tbg.wms.core.rail;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailCardRendererTest {

    @Test
    void renderPdfCreatesLetterDocumentWithMultipleCards() throws Exception {
        List<RailCarCard> cards = List.of(
                new RailCarCard("0124", "142", "CAR1", "L1",
                        List.of(new RailStopRecord.ItemQuantity("01830", 120)),
                        2, 1, 0, List.of("DOM:100"), List.of()),
                new RailCarCard("0124", "143", "CAR2", "L2",
                        List.of(new RailStopRecord.ItemQuantity("01831", 300)),
                        0, 6, 0, List.of("CAN:60", "DOM:40"), List.of())
        );

        Path output = Files.createTempFile("rail-cards-test", ".pdf");
        new RailCardRenderer().renderPdf(cards, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
        try (PDDocument doc = PDDocument.load(output.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    void renderPdfPaginatesAfterTenCards() throws Exception {
        Path output = Files.createTempFile("rail-cards-pagination-test", ".pdf");
        new RailCardRenderer().renderPdf(cards(11), output);

        try (PDDocument doc = PDDocument.load(output.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
        }
    }

    @Test
    void renderPdfUsesCompactMainDestinationCountText() throws Exception {
        Path output = Files.createTempFile("rail-cards-main-count-test", ".pdf");
        RailCarCard card = new RailCarCard("JC04152026", "302", "TPIX3127", "0415 FP 8000582489",
                List.of(new RailStopRecord.ItemQuantity("20554", 7600)),
                0, 76, 0, List.of("DOM:100"), List.of());

        new RailCardRenderer().renderPdf(List.of(card), output);

        try (PDDocument doc = PDDocument.load(output.toFile())) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("DOM:76"));
        }
    }

    @Test
    void renderAlignmentTemplateCreatesOneLetterPage() throws Exception {
        Path output = Files.createTempFile("rail-alignment-template-test", ".pdf");
        new RailCardRenderer().renderAlignmentTemplate(output);

        try (PDDocument doc = PDDocument.load(output.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    private static List<RailCarCard> cards(int count) {
        List<RailCarCard> cards = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            cards.add(new RailCarCard("TRAIN", String.valueOf(i + 1), "CAR" + i, "L" + i,
                    List.of(new RailStopRecord.ItemQuantity("01830", 120)),
                    2, 1, 0, List.of("DOM:100"), List.of()));
        }
        return cards;
    }
}
