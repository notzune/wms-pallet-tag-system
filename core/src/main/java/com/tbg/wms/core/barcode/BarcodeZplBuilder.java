/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.1.0
 */

package com.tbg.wms.core.barcode;

import java.util.Objects;

/**
 * Utility for generating standalone ZPL barcode labels.
 */
public final class BarcodeZplBuilder {

    private static final int ESTIMATED_CODE128_START_STOP_MODULES = 35;
    private static final int ESTIMATED_CODE128_MODULES_PER_CHAR = 11;
    private static final int ESTIMATED_CODE128_QUIET_ZONE_MODULES = 20;
    private static final int HUMAN_READABLE_TEXT_HEIGHT_DOTS = 36;
    private static final int HUMAN_READABLE_TEXT_GAP_DOTS = 12;
    private static final int CENTER_UPWARD_BIAS_DOTS = 36;

    private BarcodeZplBuilder() {
    }

    /**
     * Builds a ZPL document containing a single barcode label.
     * <p>
     * Orientation defaults to portrait; landscape rotates the field while keeping
     * the printer in portrait mode for consistent behavior.
     *
     * @param request barcode request
     * @return ZPL content
     */
    public static String build(BarcodeRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        boolean landscape = request.getOrientation() == Orientation.LANDSCAPE;
        Placement placement = computePlacement(request, landscape);

        StringBuilder zpl = new StringBuilder(256);
        zpl.append("^XA\n");
        zpl.append("^PON\n");
        zpl.append("^PW").append(request.getLabelWidthDots()).append('\n');
        zpl.append("^LL").append(request.getLabelHeightDots()).append('\n');
        zpl.append(landscape ? "^FWR\n" : "^FWN\n");
        zpl.append("^BY")
                .append(request.getModuleWidth())
                .append(',')
                .append(request.getModuleRatio())
                .append(',')
                .append(request.getBarcodeHeight())
                .append('\n');
        zpl.append("^FO")
                .append(placement.originX())
                .append(',')
                .append(placement.originY())
                .append('\n');
        zpl.append("^BC")
                .append(landscape ? "R" : "N")
                .append(',')
                .append(request.getBarcodeHeight())
                .append(',')
                .append(request.isHumanReadable() ? "Y" : "N")
                .append(",N,N\n");
        zpl.append("^FD");
        if (request.getSymbology() == Symbology.GS1_128) {
            zpl.append(">;");
        }
        zpl.append(escapeZpl(request.getData()));
        zpl.append("^FS\n");
        if (request.getCopies() > 1) {
            zpl.append("^PQ").append(request.getCopies()).append('\n');
        }
        zpl.append("^XZ\n");
        return zpl.toString();
    }

    private static Placement computePlacement(BarcodeRequest request, boolean landscape) {
        int estimatedBarcodeWidth = estimateBarcodeWidthDots(request);
        int textHeight = request.isHumanReadable() ? HUMAN_READABLE_TEXT_HEIGHT_DOTS + HUMAN_READABLE_TEXT_GAP_DOTS : 0;
        int blockWidth = estimatedBarcodeWidth;
        int blockHeight = request.getBarcodeHeight() + textHeight;
        if (landscape) {
            int rotatedWidth = blockHeight;
            int rotatedHeight = blockWidth;
            blockWidth = rotatedWidth;
            blockHeight = rotatedHeight;
        }

        int centeredX = centerWithinSafeArea(
                request.getLabelWidthDots(),
                request.getOriginX(),
                blockWidth,
                0
        );
        int centeredY = centerWithinSafeArea(
                request.getLabelHeightDots(),
                request.getOriginY(),
                blockHeight,
                CENTER_UPWARD_BIAS_DOTS
        );
        return new Placement(centeredX, centeredY);
    }

    private static int estimateBarcodeWidthDots(BarcodeRequest request) {
        int charCount = request.getData().length();
        int moduleCount = ESTIMATED_CODE128_START_STOP_MODULES
                + ESTIMATED_CODE128_QUIET_ZONE_MODULES
                + (charCount * ESTIMATED_CODE128_MODULES_PER_CHAR);
        return moduleCount * request.getModuleWidth();
    }

    private static int centerWithinSafeArea(int labelSize, int safeMargin, int blockSize, int leadingBias) {
        int safeAreaSize = Math.max(0, labelSize - (safeMargin * 2));
        int centeredOffset = Math.max(0, (safeAreaSize - blockSize) / 2);
        int biasedOrigin = safeMargin + centeredOffset - leadingBias;
        int maxOrigin = Math.max(safeMargin, labelSize - safeMargin - blockSize);
        return Math.max(safeMargin, Math.min(biasedOrigin, maxOrigin));
    }

    private static String escapeZpl(String value) {
        return value
                .replace("~", "~~")
                .replace("^", "~~^")
                .replace("{", "{{")
                .replace("}", "}}");
    }

    private static int requirePositive(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be greater than 0");
        }
        return value;
    }

    private static int requireNonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be >= 0");
        }
        return value;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value.trim();
    }

    /**
     * Supported barcode symbologies for standalone labels.
     */
    public enum Symbology {
        CODE128,
        GS1_128
    }

    /**
     * Field orientation for barcode rendering.
     */
    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
    }

    /**
     * Request payload for barcode label generation.
     */
    public static final class BarcodeRequest {
        private final String data;
        private final Symbology symbology;
        private final Orientation orientation;
        private final int labelWidthDots;
        private final int labelHeightDots;
        private final int originX;
        private final int originY;
        private final int moduleWidth;
        private final int moduleRatio;
        private final int barcodeHeight;
        private final boolean humanReadable;
        private final int copies;

        public BarcodeRequest(String data,
                              Symbology symbology,
                              Orientation orientation,
                              int labelWidthDots,
                              int labelHeightDots,
                              int originX,
                              int originY,
                              int moduleWidth,
                              int moduleRatio,
                              int barcodeHeight,
                              boolean humanReadable,
                              int copies) {
            this.data = requireNonBlank(data, "data");
            this.symbology = Objects.requireNonNull(symbology, "symbology cannot be null");
            this.orientation = Objects.requireNonNull(orientation, "orientation cannot be null");
            this.labelWidthDots = requirePositive(labelWidthDots, "labelWidthDots");
            this.labelHeightDots = requirePositive(labelHeightDots, "labelHeightDots");
            this.originX = requireNonNegative(originX, "originX");
            this.originY = requireNonNegative(originY, "originY");
            this.moduleWidth = requirePositive(moduleWidth, "moduleWidth");
            this.moduleRatio = requirePositive(moduleRatio, "moduleRatio");
            this.barcodeHeight = requirePositive(barcodeHeight, "barcodeHeight");
            this.humanReadable = humanReadable;
            this.copies = requirePositive(copies, "copies");
        }

        public String getData() {
            return data;
        }

        public Symbology getSymbology() {
            return symbology;
        }

        public Orientation getOrientation() {
            return orientation;
        }

        public int getLabelWidthDots() {
            return labelWidthDots;
        }

        public int getLabelHeightDots() {
            return labelHeightDots;
        }

        public int getOriginX() {
            return originX;
        }

        public int getOriginY() {
            return originY;
        }

        public int getModuleWidth() {
            return moduleWidth;
        }

        public int getModuleRatio() {
            return moduleRatio;
        }

        public int getBarcodeHeight() {
            return barcodeHeight;
        }

        public boolean isHumanReadable() {
            return humanReadable;
        }

        public int getCopies() {
            return copies;
        }
    }

    private record Placement(int originX, int originY) {
    }
}
