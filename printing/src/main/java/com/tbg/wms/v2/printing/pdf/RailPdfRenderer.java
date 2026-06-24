package com.tbg.wms.v2.printing.pdf;

import com.tbg.wms.v2.domain.rail.RailCarCard;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.StringJoiner;

/**
 * Provides rail pdf renderer behavior for WMS 2.0 workflows.
 */
public final class RailPdfRenderer implements com.tbg.wms.v2.app.rail.RailPdfRenderer {
    /**
     * Renders the requested output artifact.
     *
     * @param cards the cards.
     * @param outputPdf the output pdf.
     * @return the rendered artifact path.
     */
    @Override
    public Path render(List<RailCarCard> cards, Path outputPdf) {
        if (outputPdf.getParent() != null) {
            try {
                Files.createDirectories(outputPdf.getParent());
            } catch (IOException ex) {
                throw new IllegalStateException("Unable to create rail PDF output directory: " + outputPdf.getParent(), ex);
            }
        }
        try {
            Files.writeString(outputPdf, pdfDocument(cards), StandardCharsets.UTF_8);
            return outputPdf;
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to write rail PDF: " + outputPdf, ex);
        }
    }

    private static String pdfDocument(List<RailCarCard> cards) {
        String content = escapePdfText(cardText(cards == null ? List.of() : cards));
        String stream = "BT /F1 11 Tf 36 756 Td (" + content + ") Tj ET";

        StringBuilder pdf = new StringBuilder();
        pdf.append("%PDF-1.4\n");
        int obj1 = pdf.length();
        pdf.append("1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n");
        int obj2 = pdf.length();
        pdf.append("2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n");
        int obj3 = pdf.length();
        pdf.append("3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >> endobj\n");
        int obj4 = pdf.length();
        pdf.append("4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n");
        int obj5 = pdf.length();
        pdf.append("5 0 obj << /Length ").append(stream.length()).append(" >> stream\n")
                .append(stream).append("\nendstream endobj\n");
        int xref = pdf.length();
        pdf.append("xref\n0 6\n");
        pdf.append("0000000000 65535 f \n");
        appendOffset(pdf, obj1);
        appendOffset(pdf, obj2);
        appendOffset(pdf, obj3);
        appendOffset(pdf, obj4);
        appendOffset(pdf, obj5);
        pdf.append("trailer << /Size 6 /Root 1 0 R >>\nstartxref\n")
                .append(xref)
                .append("\n%%EOF\n");
        return pdf.toString();
    }

    private static String cardText(List<RailCarCard> cards) {
        StringJoiner joiner = new StringJoiner(" | ");
        for (RailCarCard card : cards) {
            StringJoiner items = new StringJoiner(", ");
            for (RailItemQuantity item : card.itemLines()) {
                items.add(item.itemNumber() + " " + item.cases());
            }
            joiner.add(card.trainId() + " " + card.sequence() + " " + card.vehicleId()
                    + " loads " + card.loadNumbers()
                    + " pallets C" + card.canPallets() + " D" + card.domPallets() + " K" + card.kevPallets()
                    + " items " + items);
        }
        return joiner.toString();
    }

    private static String escapePdfText(String value) {
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private static void appendOffset(StringBuilder pdf, int offset) {
        pdf.append(String.format("%010d 00000 n \n", offset));
    }
}
