/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Renders railcar cards directly to a deterministic letter-sized PDF.
 */
public final class RailCardRenderer {
    private static final PDRectangle PAGE_SIZE = PDRectangle.LETTER;
    private static final int GRID_COLS = 2;
    private static final int GRID_ROWS = 5;
    private static final float POINTS_PER_INCH = 72f;
    private static final float LABEL_WIDTH = 4.0f * POINTS_PER_INCH;
    private static final float LABEL_HEIGHT = 2.0f * POINTS_PER_INCH;
    private static final float GRID_GAP_Y = 0f;
    private static final float HEADER_FONT_SIZE = 8.5f;
    private static final float BODY_FONT_SIZE = 6.3f;
    private static final float LINE_HEIGHT = 8.0f;
    private static final int ITEMS_PER_CARD = 5;
    private final float gridGapX;
    private final float pageMarginX;
    private final float pageMarginY;

    public RailCardRenderer() {
        this(0.125f, 0f, 0f);
    }

    /**
     * Creates a rail card renderer with calibration controls.
     *
     * @param centerGapInches gap between left/right columns in inches
     * @param offsetXInches   positive moves grid right
     * @param offsetYInches   positive moves grid down
     */
    public RailCardRenderer(float centerGapInches, float offsetXInches, float offsetYInches) {
        this.gridGapX = centerGapInches * POINTS_PER_INCH;
        this.pageMarginX =
                ((PAGE_SIZE.getWidth() - ((GRID_COLS * LABEL_WIDTH) + ((GRID_COLS - 1) * gridGapX))) / 2.0f)
                        + (offsetXInches * POINTS_PER_INCH);
        this.pageMarginY =
                ((PAGE_SIZE.getHeight() - ((GRID_ROWS * LABEL_HEIGHT) + ((GRID_ROWS - 1) * GRID_GAP_Y))) / 2.0f)
                        + (offsetYInches * POINTS_PER_INCH);
    }

    /**
     * Renders cards into a PDF file.
     *
     * @param cards     card rows to render
     * @param outputPdf output PDF path
     * @return saved PDF path
     */
    public Path renderPdf(List<RailCarCard> cards, Path outputPdf) throws IOException {
        Objects.requireNonNull(cards, "cards cannot be null");
        Objects.requireNonNull(outputPdf, "outputPdf cannot be null");
        Path parent = outputPdf.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {
            if (cards.isEmpty()) {
                document.addPage(new PDPage(PAGE_SIZE));
            } else {
                int index = 0;
                while (index < cards.size()) {
                    PDPage page = new PDPage(PAGE_SIZE);
                    document.addPage(page);
                    try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                        for (int slot = 0; slot < GRID_COLS * GRID_ROWS && index < cards.size(); slot++, index++) {
                            drawCard(content, cards.get(index), slot);
                        }
                    }
                }
            }
            document.save(outputPdf.toFile());
            return outputPdf;
        }
    }

    /**
     * Renders a 10-position alignment template for physical label-stock calibration.
     *
     * @param outputPdf output PDF path
     * @return saved PDF path
     */
    public Path renderAlignmentTemplate(Path outputPdf) throws IOException {
        Objects.requireNonNull(outputPdf, "outputPdf cannot be null");
        Path parent = outputPdf.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PAGE_SIZE);
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                for (int slot = 0; slot < GRID_COLS * GRID_ROWS; slot++) {
                    drawAlignmentSlot(content, slot);
                }
            }
            document.save(outputPdf.toFile());
            return outputPdf;
        }
    }

    private void drawAlignmentSlot(PDPageContentStream content, int slot) throws IOException {
        Rect rect = slotRect(slot);
        float left = rect.left;
        float top = rect.top;
        float bottom = rect.bottom;

        content.addRect(left, bottom, LABEL_WIDTH, LABEL_HEIGHT);
        content.stroke();

        float inset = 10f;
        content.addRect(left + inset, bottom + inset, LABEL_WIDTH - (2 * inset), LABEL_HEIGHT - (2 * inset));
        content.stroke();

        float cx = left + (LABEL_WIDTH / 2f);
        float cy = bottom + (LABEL_HEIGHT / 2f);
        content.moveTo(cx - 9f, cy);
        content.lineTo(cx + 9f, cy);
        content.moveTo(cx, cy - 9f);
        content.lineTo(cx, cy + 9f);
        content.stroke();

        int row = (slot / GRID_COLS) + 1;
        int col = (slot % GRID_COLS) + 1;
        String label = "POS " + row + "-" + col + " (slot " + (slot + 1) + ")";
        writeText(content, PDType1Font.HELVETICA_BOLD, 9f, left + 8f, top - 14f, label);
        writeText(content, PDType1Font.HELVETICA, 7f, left + 8f, top - 26f,
                "4.00in x 2.00in | center gap=" + (gridGapX / POINTS_PER_INCH) + "in");
    }

    private void drawCard(PDPageContentStream content, RailCarCard card, int slot) throws IOException {
        Rect rect = slotRect(slot);
        float left = rect.left;
        float top = rect.top;
        float bottom = rect.bottom;

        content.addRect(left, bottom, LABEL_WIDTH, LABEL_HEIGHT);
        content.stroke();

        float textX = left + 7f;
        float y = top - 11f;

        writeText(content, PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE,
                textX, y, safe(card.getSequence()) + "      " + safe(card.getVehicleId()));
        y -= LINE_HEIGHT;

        if (!card.getLoadNumbers().isBlank()) {
            writeText(content, PDType1Font.HELVETICA, BODY_FONT_SIZE, textX, y, "LOAD: " + card.getLoadNumbers());
            y -= LINE_HEIGHT;
        }
        writeText(content, PDType1Font.COURIER_BOLD, BODY_FONT_SIZE, textX, y, "ITEM LIST");
        y -= LINE_HEIGHT;

        int itemCount = Math.min(ITEMS_PER_CARD, card.getItemLines().size());
        for (int i = 0; i < itemCount; i++) {
            RailStopRecord.ItemQuantity item = card.getItemLines().get(i);
            String line = String.format("%-10s %6d", safe(item.getItemNumber()), item.getCases());
            writeText(content, PDType1Font.COURIER, BODY_FONT_SIZE, textX, y, line);
            y -= LINE_HEIGHT;
        }

        if (card.getItemLines().size() > ITEMS_PER_CARD) {
            int remaining = card.getItemLines().size() - ITEMS_PER_CARD;
            writeText(content, PDType1Font.HELVETICA_OBLIQUE, BODY_FONT_SIZE, textX, y,
                    "... " + remaining + " more item(s)");
            y -= LINE_HEIGHT;
        }

        y -= 2f;
        writeText(content, PDType1Font.HELVETICA_BOLD, BODY_FONT_SIZE, textX, y, "CAN: " + card.getCanPallets());
        y -= LINE_HEIGHT;
        writeText(content, PDType1Font.HELVETICA_BOLD, BODY_FONT_SIZE, textX, y, "DOM: " + card.getDomPallets());
        y -= LINE_HEIGHT;
        writeText(content, PDType1Font.HELVETICA_BOLD, BODY_FONT_SIZE, textX, y, "KEV: " + card.getKevPallets());
        y -= LINE_HEIGHT;

        if (!card.getTopFamilies().isEmpty()) {
            writeText(content, PDType1Font.HELVETICA, BODY_FONT_SIZE, textX, y,
                    "TOP: " + String.join(" ", card.getTopFamilies()));
            y -= LINE_HEIGHT;
        }

        if (!card.getMissingFootprintItems().isEmpty()) {
            writeText(content, PDType1Font.HELVETICA_OBLIQUE, BODY_FONT_SIZE, textX, y,
                    "MISSING: " + card.getMissingFootprintItems().size());
            y -= LINE_HEIGHT;
        }

        writeText(content, PDType1Font.HELVETICA, BODY_FONT_SIZE, textX, bottom + 8f,
                "PASS: ______   FUEL: ______   BH: ______");
    }

    private Rect slotRect(int slot) {
        int row = slot / GRID_COLS;
        int col = slot % GRID_COLS;
        float left = pageMarginX + (col * (LABEL_WIDTH + gridGapX));
        float top = PAGE_SIZE.getHeight() - pageMarginY - (row * (LABEL_HEIGHT + GRID_GAP_Y));
        float bottom = top - LABEL_HEIGHT;
        return new Rect(left, top, bottom);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void writeText(PDPageContentStream content,
                           PDType1Font font,
                           float fontSize,
                           float x,
                           float y,
                           String text) throws IOException {
        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text == null ? "" : text);
        content.endText();
    }

    private static final class Rect {
        private final float left;
        private final float top;
        private final float bottom;

        private Rect(float left, float top, float bottom) {
            this.left = left;
            this.top = top;
            this.bottom = bottom;
        }
    }
}
