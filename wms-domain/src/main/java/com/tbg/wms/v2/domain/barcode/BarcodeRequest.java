package com.tbg.wms.v2.domain.barcode;

import java.util.Objects;

public record BarcodeRequest(
        String data,
        BarcodeSymbology symbology,
        Orientation orientation,
        int labelWidthDots,
        int labelHeightDots,
        int originX,
        int originY,
        int moduleWidth,
        int moduleRatio,
        int barcodeHeight,
        boolean humanReadable,
        int copies,
        String captionText,
        boolean hexEncoded
) {
    public BarcodeRequest {
        data = hexEncoded ? requireNonBlankRaw(data, "data") : requireNonBlank(data, "data");
        symbology = Objects.requireNonNull(symbology, "symbology cannot be null");
        orientation = Objects.requireNonNull(orientation, "orientation cannot be null");
        labelWidthDots = requirePositive(labelWidthDots, "labelWidthDots");
        labelHeightDots = requirePositive(labelHeightDots, "labelHeightDots");
        originX = requireNonNegative(originX, "originX");
        originY = requireNonNegative(originY, "originY");
        moduleWidth = requirePositive(moduleWidth, "moduleWidth");
        moduleRatio = requirePositive(moduleRatio, "moduleRatio");
        barcodeHeight = requirePositive(barcodeHeight, "barcodeHeight");
        copies = requirePositive(copies, "copies");
    }

    public enum Orientation {
        PORTRAIT,
        LANDSCAPE
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

    private static String requireNonBlankRaw(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " cannot be blank");
        }
        return value;
    }
}
