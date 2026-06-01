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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Renders railcar cards directly to a deterministic letter-sized PDF.
 */
public final class RailCardRenderer {
    private static final PDRectangle PAGE_SIZE = PDRectangle.LETTER;
    private static final float BORDER_WIDTH = 0.5f;
    private static final float TEXT_INSET = 7f;
    private static final float FOOTER_Y_OFFSET = 8f;
    private static final int ITEMS_PER_CARD = 5;
    private final RailLabelSheetLayout layout;

    public RailCardRenderer() {
        this.layout = RailLabelSheetLayout.defaultLayout();
    }

    /**
     * Creates a rail card renderer with calibration controls.
     *
     * @param centerGapInches gap between left/right columns in inches
     * @param offsetXInches   positive moves grid right
     * @param offsetYInches   positive moves grid down
     */
    public RailCardRenderer(float centerGapInches, float offsetXInches, float offsetYInches) {
        this.layout = RailLabelSheetLayout.calibrated(centerGapInches, offsetXInches, offsetYInches);
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
                        for (int slot = 0; slot < layout.slotsPerPage() && index < cards.size(); slot++, index++) {
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
                for (int slot = 0; slot < layout.slotsPerPage(); slot++) {
                    drawAlignmentSlot(content, slot);
                }
            }
            document.save(outputPdf.toFile());
            return outputPdf;
        }
    }

    private void drawAlignmentSlot(PDPageContentStream content, int slot) throws IOException {
        RailLabelSheetLayout.LabelSlot rect = layout.slot(slot);
        float left = rect.left();
        float top = rect.top();
        float bottom = rect.bottom();

        content.addRect(left, bottom, rect.width(), rect.height());
        content.stroke();

        float inset = 10f;
        content.addRect(left + inset, bottom + inset, rect.width() - (2 * inset), rect.height() - (2 * inset));
        content.stroke();

        float cx = left + (rect.width() / 2f);
        float cy = bottom + (rect.height() / 2f);
        content.moveTo(cx - 9f, cy);
        content.lineTo(cx + 9f, cy);
        content.moveTo(cx, cy - 9f);
        content.lineTo(cx, cy + 9f);
        content.stroke();

        int row = (slot / 2) + 1;
        int col = (slot % 2) + 1;
        String label = "POS " + row + "-" + col + " (slot " + (slot + 1) + ")";
        writeText(content, PDType1Font.HELVETICA_BOLD, 9f, left + 8f, top - 14f, label);
        writeText(content, PDType1Font.HELVETICA, 7f, left + 8f, top - 26f,
                "4.00in x 2.00in rail label");
    }

    private void drawCard(PDPageContentStream content, RailCarCard card, int slot) throws IOException {
        RailLabelSheetLayout.LabelSlot rect = layout.slot(slot);
        float left = rect.left();
        float top = rect.top();
        float bottom = rect.bottom();
        float right = left + rect.width();

        content.setLineWidth(BORDER_WIDTH);
        content.addRect(left, bottom, rect.width(), rect.height());
        content.stroke();

        float textLeft = left + TEXT_INSET;
        float textRight = right - TEXT_INSET;
        float centerX = left + (rect.width() / 2f);

        writeUnderlinedText(content, PDType1Font.HELVETICA_BOLD_OBLIQUE, RailLabelTypography.SEQUENCE_SIZE,
                textLeft, top - 22f, safe(card.getSequence()));
        writeRightAlignedUnderlinedText(content, PDType1Font.HELVETICA_BOLD_OBLIQUE, RailLabelTypography.VEHICLE_SIZE,
                textRight, top - 27f, safe(card.getVehicleId()));

        float supportY = top - 39f;
        if (!card.getRouteHeader().isBlank()) {
            writeText(content, PDType1Font.HELVETICA_OBLIQUE, RailLabelTypography.ROUTE_HEADER_SIZE,
                    textLeft, supportY, card.getRouteHeader());
            supportY -= 10f;
        }

        float itemFontSize = itemFontSize(card.getItemLines().size());
        for (int i = 0; i < card.getItemLines().size(); i++) {
            RailStopRecord.ItemQuantity item = card.getItemLines().get(i);
            String line = safe(item.getItemNumber()) + " " + item.getCases();
            writeText(content, PDType1Font.HELVETICA, itemFontSize, textLeft, supportY, line);
            supportY -= itemFontSize + 2f;
        }

        List<DestinationCount> counts = destinationCounts(card);
        if (!counts.isEmpty()) {
            writeCenteredText(content, PDType1Font.HELVETICA_BOLD, RailLabelTypography.PRIMARY_DESTINATION_SIZE,
                    centerX, bottom + 63f, counts.get(0).display());
        }
        if (counts.size() > 1) {
            writeCenteredUnderlinedText(content, PDType1Font.HELVETICA_BOLD_OBLIQUE,
                    RailLabelTypography.SECONDARY_DESTINATION_SIZE, centerX, bottom + 40f, counts.get(1).display());
        }

        if (!card.getMissingFootprintItems().isEmpty()) {
            writeText(content, PDType1Font.HELVETICA_OBLIQUE, RailLabelTypography.MIN_ITEM_SIZE,
                    textLeft, bottom + 22f, "MISSING: " + card.getMissingFootprintItems().size());
        }

        drawFooterField(content, textLeft, bottom + FOOTER_Y_OFFSET, "PASS:");
        drawFooterField(content, left + 102f, bottom + FOOTER_Y_OFFSET, "FUEL:");
        drawFooterField(content, left + 198f, bottom + FOOTER_Y_OFFSET, "BH:");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private float itemFontSize(int itemRows) {
        if (itemRows <= ITEMS_PER_CARD) {
            return RailLabelTypography.ITEM_SIZE;
        }
        float shrink = (itemRows - ITEMS_PER_CARD) * 0.4f;
        return Math.max(RailLabelTypography.MIN_ITEM_SIZE, RailLabelTypography.ITEM_SIZE - shrink);
    }

    private List<DestinationCount> destinationCounts(RailCarCard card) {
        List<DestinationCount> counts = new ArrayList<>(3);
        if (card.getCanPallets() > 0) {
            counts.add(new DestinationCount("CAN", card.getCanPallets()));
        }
        if (card.getDomPallets() > 0) {
            counts.add(new DestinationCount("DOM", card.getDomPallets()));
        }
        if (card.getKevPallets() > 0) {
            counts.add(new DestinationCount("KEV", card.getKevPallets()));
        }
        return counts;
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

    private void writeUnderlinedText(PDPageContentStream content,
                                     PDType1Font font,
                                     float fontSize,
                                     float x,
                                     float y,
                                     String text) throws IOException {
        writeText(content, font, fontSize, x, y, text);
        underline(content, font, fontSize, x, y, text);
    }

    private void writeRightAlignedUnderlinedText(PDPageContentStream content,
                                                PDType1Font font,
                                                float fontSize,
                                                float right,
                                                float y,
                                                String text) throws IOException {
        float width = textWidth(font, fontSize, text);
        writeUnderlinedText(content, font, fontSize, right - width, y, text);
    }

    private void writeCenteredText(PDPageContentStream content,
                                   PDType1Font font,
                                   float fontSize,
                                   float centerX,
                                   float y,
                                   String text) throws IOException {
        float width = textWidth(font, fontSize, text);
        writeText(content, font, fontSize, centerX - (width / 2f), y, text);
    }

    private void writeCenteredUnderlinedText(PDPageContentStream content,
                                             PDType1Font font,
                                             float fontSize,
                                             float centerX,
                                             float y,
                                             String text) throws IOException {
        float width = textWidth(font, fontSize, text);
        float x = centerX - (width / 2f);
        writeUnderlinedText(content, font, fontSize, x, y, text);
    }

    private void drawFooterField(PDPageContentStream content, float x, float y, String label) throws IOException {
        writeText(content, PDType1Font.HELVETICA_BOLD, RailLabelTypography.FOOTER_SIZE, x, y, label);
        float labelWidth = textWidth(PDType1Font.HELVETICA_BOLD, RailLabelTypography.FOOTER_SIZE, label);
        float underlineStart = x + labelWidth + 2f;
        content.moveTo(underlineStart, y - 1f);
        content.lineTo(underlineStart + 36f, y - 1f);
        content.stroke();
    }

    private void underline(PDPageContentStream content,
                           PDType1Font font,
                           float fontSize,
                           float x,
                           float y,
                           String text) throws IOException {
        content.moveTo(x, y - 2f);
        content.lineTo(x + textWidth(font, fontSize, text) + 2f, y - 2f);
        content.stroke();
    }

    private float textWidth(PDType1Font font, float fontSize, String text) throws IOException {
        String safeText = text == null ? "" : text;
        return font.getStringWidth(safeText) / 1000f * fontSize;
    }

    private record DestinationCount(String label, int count) {
        private String display() {
            return label + ":" + count;
        }
    }
}
