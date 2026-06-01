/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.rail;

import org.apache.pdfbox.pdmodel.common.PDRectangle;

/**
 * Physical sheet layout for rail labels.
 *
 * <p>The rail label stock is a letter-sized 8.5 inch by 11 inch portrait sheet
 * with two columns and five rows of 4.0 inch by 2.0 inch labels.</p>
 *
 * <p>Approved physical measurements:</p>
 *
 * <pre>
 * Page width       = 8.5"
 * Page height      = 11.0"
 * Label width      = 4.0"
 * Label height     = 2.0"
 * Left margin      = 5/32" = 0.15625"
 * Right margin     = 5/32" = 0.15625"
 * Top margin       = 0.5"
 * Bottom margin    = 0.5"
 * Center gap       = 3/16" = 0.1875"
 *
 * Horizontal check = (4.0 * 2) + 0.1875 + (0.15625 * 2) = 8.5
 * Vertical check   = (2.0 * 5) + (0.5 * 2) = 11.0
 * </pre>
 *
 * <p>PDFBox uses points, so all rendering coordinates are derived from inches at runtime:</p>
 *
 * <pre>
 * points = inches * 72
 * </pre>
 *
 * <p>For 300 DPI print-preview comparisons:</p>
 *
 * <pre>
 * Page:              8.5 * 300 = 2550 px, 11 * 300 = 3300 px
 * Label:             4.0 * 300 = 1200 px, 2.0 * 300 = 600 px
 * Left/right margin: 0.15625 * 300 = 46.875 px, rounded 47 px
 * Center gap:        0.1875 * 300 = 56.25 px, rounded 56 px
 * Top/bottom margin: 0.5 * 300 = 150 px
 * </pre>
 *
 * <p>This class deliberately owns only media geometry. It does not know about rail-card data,
 * fonts, PDF content streams, print routing, or Swing UI state.</p>
 */
public final class RailLabelSheetLayout {
    private static final float POINTS_PER_INCH = 72f;
    private static final float PAGE_WIDTH_INCHES = 8.5f;
    private static final float PAGE_HEIGHT_INCHES = 11.0f;
    private static final float LABEL_WIDTH_INCHES = 4.0f;
    private static final float LABEL_HEIGHT_INCHES = 2.0f;
    private static final float LEFT_MARGIN_INCHES = 0.15625f;
    private static final float TOP_MARGIN_INCHES = 0.5f;
    private static final float CENTER_GAP_INCHES = 0.1875f;
    private static final int COLUMNS = 2;
    private static final int ROWS = 5;

    private final float pageWidthPoints;
    private final float pageHeightPoints;
    private final float labelWidthPoints;
    private final float labelHeightPoints;
    private final float leftMarginPoints;
    private final float topMarginPoints;
    private final float horizontalGapPoints;

    private RailLabelSheetLayout(float pageWidthPoints,
                                 float pageHeightPoints,
                                 float labelWidthPoints,
                                 float labelHeightPoints,
                                 float leftMarginPoints,
                                 float topMarginPoints,
                                 float horizontalGapPoints) {
        this.pageWidthPoints = pageWidthPoints;
        this.pageHeightPoints = pageHeightPoints;
        this.labelWidthPoints = labelWidthPoints;
        this.labelHeightPoints = labelHeightPoints;
        this.leftMarginPoints = leftMarginPoints;
        this.topMarginPoints = topMarginPoints;
        this.horizontalGapPoints = horizontalGapPoints;
    }

    public static RailLabelSheetLayout defaultLayout() {
        return new RailLabelSheetLayout(
                inchesToPoints(PAGE_WIDTH_INCHES),
                inchesToPoints(PAGE_HEIGHT_INCHES),
                inchesToPoints(LABEL_WIDTH_INCHES),
                inchesToPoints(LABEL_HEIGHT_INCHES),
                inchesToPoints(LEFT_MARGIN_INCHES),
                inchesToPoints(TOP_MARGIN_INCHES),
                inchesToPoints(CENTER_GAP_INCHES)
        );
    }

    static RailLabelSheetLayout calibrated(float centerGapInches, float offsetXInches, float offsetYInches) {
        float labelWidth = inchesToPoints(LABEL_WIDTH_INCHES);
        float gap = inchesToPoints(centerGapInches);
        float pageWidth = inchesToPoints(PAGE_WIDTH_INCHES);
        float centeredLeftMargin = (pageWidth - ((COLUMNS * labelWidth) + ((COLUMNS - 1) * gap))) / 2.0f;
        return new RailLabelSheetLayout(
                pageWidth,
                inchesToPoints(PAGE_HEIGHT_INCHES),
                labelWidth,
                inchesToPoints(LABEL_HEIGHT_INCHES),
                centeredLeftMargin + inchesToPoints(offsetXInches),
                inchesToPoints(TOP_MARGIN_INCHES) + inchesToPoints(offsetYInches),
                gap
        );
    }

    public PDRectangle pageSize() {
        return new PDRectangle(pageWidthPoints, pageHeightPoints);
    }

    public float pageWidthPoints() {
        return pageWidthPoints;
    }

    public float pageHeightPoints() {
        return pageHeightPoints;
    }

    public float labelWidthPoints() {
        return labelWidthPoints;
    }

    public float labelHeightPoints() {
        return labelHeightPoints;
    }

    public int slotsPerPage() {
        return COLUMNS * ROWS;
    }

    public LabelSlot slot(int index) {
        if (index < 0 || index >= slotsPerPage()) {
            throw new IllegalArgumentException("Label slot index out of range: " + index);
        }
        int row = index / COLUMNS;
        int column = index % COLUMNS;
        float left = leftMarginPoints + (column * (labelWidthPoints + horizontalGapPoints));
        float top = pageHeightPoints - topMarginPoints - (row * labelHeightPoints);
        float bottom = top - labelHeightPoints;
        return new LabelSlot(left, top, bottom, labelWidthPoints, labelHeightPoints);
    }

    static float inchesToPoints(float inches) {
        return inches * POINTS_PER_INCH;
    }

    public record LabelSlot(float left, float top, float bottom, float width, float height) {
    }
}
