package com.tbg.wms.v2.printing.zpl;

import com.tbg.wms.v2.app.barcode.GenerateBarcodeLabel;
import com.tbg.wms.v2.domain.barcode.BarcodeRequest;
import com.tbg.wms.v2.domain.barcode.BarcodeSymbology;

import java.util.Objects;

/**
 * Renders standalone barcode labels as Zebra Programming Language.
 */
public final class BarcodeZplRenderer implements GenerateBarcodeLabel.Renderer {
    private static final int ESTIMATED_CODE128_START_STOP_MODULES = 35;
    private static final int ESTIMATED_CODE128_MODULES_PER_CHAR = 11;
    private static final int ESTIMATED_CODE128_QUIET_ZONE_MODULES = 20;
    private static final int HUMAN_READABLE_TEXT_HEIGHT_DOTS = 36;
    private static final int HUMAN_READABLE_TEXT_GAP_DOTS = 12;
    private static final int DATA_MATRIX_MODULE_DOTS = 12;
    private static final int CAPTION_TEXT_HEIGHT_DOTS = 36;
    private static final int CAPTION_TEXT_GAP_DOTS = 12;
    private static final int CENTER_UPWARD_BIAS_DOTS = 36;

    @Override
    public String render(BarcodeRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        boolean landscape = request.orientation() == BarcodeRequest.Orientation.LANDSCAPE;
        Placement placement = computePlacement(request, landscape);

        StringBuilder zpl = new StringBuilder(256);
        zpl.append("^XA\n");
        zpl.append("^PON\n");
        zpl.append("^PW").append(request.labelWidthDots()).append('\n');
        zpl.append("^LL").append(request.labelHeightDots()).append('\n');
        zpl.append(landscape ? "^FWR\n" : "^FWN\n");
        if (request.captionText() != null && !request.captionText().isBlank()) {
            zpl.append("^FO").append(request.originX()).append(',').append(request.originY()).append('\n');
            zpl.append("^A0N,36,36\n");
            zpl.append("^FD").append(escapeZpl(request.captionText())).append("^FS\n");
        }
        if (request.symbology() == BarcodeSymbology.DATA_MATRIX) {
            int captionOffset = request.captionText() != null && !request.captionText().isBlank()
                    ? CAPTION_TEXT_HEIGHT_DOTS + CAPTION_TEXT_GAP_DOTS
                    : 0;
            appendSymbol(zpl, request, request.originX(), request.originY() + captionOffset, landscape);
        } else {
            appendSymbol(zpl, request, placement.originX(), placement.originY(), landscape);
        }
        if (request.copies() > 1) {
            zpl.append("^PQ").append(request.copies()).append('\n');
        }
        zpl.append("^XZ\n");
        return zpl.toString();
    }

    private static Placement computePlacement(BarcodeRequest request, boolean landscape) {
        int estimatedBarcodeWidth = estimateBarcodeWidthDots(request);
        int textHeight = 0;
        if (request.captionText() != null && !request.captionText().isBlank()) {
            textHeight += CAPTION_TEXT_HEIGHT_DOTS + CAPTION_TEXT_GAP_DOTS;
        }
        if (request.humanReadable()) {
            textHeight += HUMAN_READABLE_TEXT_HEIGHT_DOTS + HUMAN_READABLE_TEXT_GAP_DOTS;
        }
        int blockWidth = estimatedBarcodeWidth;
        int blockHeight = request.barcodeHeight() + textHeight;
        if (landscape) {
            int unrotatedWidth = blockWidth;
            blockWidth = blockHeight;
            blockHeight = unrotatedWidth;
        }
        int centeredX = centerWithinSafeArea(request.labelWidthDots(), request.originX(), blockWidth, 0);
        int centeredY = centerWithinSafeArea(
                request.labelHeightDots(),
                request.originY(),
                blockHeight,
                CENTER_UPWARD_BIAS_DOTS
        );
        return new Placement(centeredX, centeredY);
    }

    private static int estimateBarcodeWidthDots(BarcodeRequest request) {
        int moduleCount = ESTIMATED_CODE128_START_STOP_MODULES
                + ESTIMATED_CODE128_QUIET_ZONE_MODULES
                + (request.data().length() * ESTIMATED_CODE128_MODULES_PER_CHAR);
        return moduleCount * request.moduleWidth();
    }

    private static int centerWithinSafeArea(int labelSize, int safeMargin, int blockSize, int leadingBias) {
        int safeAreaSize = Math.max(0, labelSize - (safeMargin * 2));
        int centeredOffset = Math.max(0, (safeAreaSize - blockSize) / 2);
        int biasedOrigin = safeMargin + centeredOffset - leadingBias;
        int maxOrigin = Math.max(safeMargin, labelSize - safeMargin - blockSize);
        return Math.max(safeMargin, Math.min(biasedOrigin, maxOrigin));
    }

    private static void appendSymbol(StringBuilder zpl, BarcodeRequest request, int x, int y, boolean landscape) {
        if (request.symbology() == BarcodeSymbology.DATA_MATRIX) {
            zpl.append("^FO").append(x).append(',').append(y).append('\n');
            zpl.append("^BX").append(landscape ? "R" : "N").append(',').append(DATA_MATRIX_MODULE_DOTS).append(",200\n");
            appendFieldData(zpl, request);
            return;
        }
        zpl.append("^BY")
                .append(request.moduleWidth())
                .append(',')
                .append(request.moduleRatio())
                .append(',')
                .append(request.barcodeHeight())
                .append('\n');
        zpl.append("^FO").append(x).append(',').append(y).append('\n');
        zpl.append("^BC")
                .append(landscape ? "R" : "N")
                .append(',')
                .append(request.barcodeHeight())
                .append(',')
                .append(request.humanReadable() ? "Y" : "N")
                .append(",N,N\n");
        appendFieldData(zpl, request);
    }

    private static void appendFieldData(StringBuilder zpl, BarcodeRequest request) {
        if (request.hexEncoded()) {
            zpl.append("^FH");
        }
        zpl.append("^FD");
        if (request.symbology() == BarcodeSymbology.GS1_128) {
            zpl.append(">;");
        }
        zpl.append(request.hexEncoded() ? toZplHexFieldData(request.data()) : escapeZpl(request.data()));
        zpl.append("^FS\n");
    }

    private static String escapeZpl(String value) {
        return value
                .replace("~", "~~")
                .replace("^", "~~^")
                .replace("{", "{{")
                .replace("}", "}}");
    }

    private static String toZplHexFieldData(String value) {
        StringBuilder encoded = new StringBuilder(value.length() * 4);
        for (int i = 0; i < value.length(); i++) {
            int ch = value.charAt(i);
            encoded.append('_');
            encoded.append(Character.forDigit((ch >> 4) & 0xF, 16));
            encoded.append(Character.forDigit(ch & 0xF, 16));
        }
        return encoded.toString().toUpperCase();
    }

    private record Placement(int originX, int originY) {
    }
}
