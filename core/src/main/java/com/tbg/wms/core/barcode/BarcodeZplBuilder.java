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

    /**
     * Builds a ZPL document containing a single barcode label.
     *
     * Orientation defaults to portrait; landscape rotates the field while keeping
     * the printer in portrait mode for consistent behavior.
     *
     * @param request barcode request
     * @return ZPL content
     */
    public static String build(BarcodeRequest request) {
        Objects.requireNonNull(request, "request cannot be null");
        boolean landscape = request.getOrientation() == Orientation.LANDSCAPE;

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
                .append(request.getOriginX())
                .append(',')
                .append(request.getOriginY())
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

    private BarcodeZplBuilder() {
    }
}
