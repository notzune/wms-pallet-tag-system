package com.tbg.wms.v2.domain.barcode;

public enum BarcodePreset {
    BREAK_START("BRKSTART", "BREAK START", "break-start"),
    BREAK_STOP("BRKSTOP", "BREAK STOP", "break-stop");

    private static final int DEFAULT_LABEL_WIDTH_DOTS = 812;
    private static final int DEFAULT_LABEL_HEIGHT_DOTS = 1218;
    private static final int DEFAULT_ORIGIN_X = 40;
    private static final int DEFAULT_ORIGIN_Y = 40;
    private static final int DEFAULT_MODULE_WIDTH = 2;
    private static final int DEFAULT_MODULE_RATIO = 3;
    private static final int DEFAULT_BARCODE_HEIGHT = 120;

    private final String data;
    private final String caption;
    private final String fileSlug;

    BarcodePreset(String data, String caption, String fileSlug) {
        this.data = data;
        this.caption = caption;
        this.fileSlug = fileSlug;
    }

    public BarcodeRequest toRequest() {
        return new BarcodeRequest(
                data,
                BarcodeSymbology.CODE128,
                BarcodeRequest.Orientation.PORTRAIT,
                DEFAULT_LABEL_WIDTH_DOTS,
                DEFAULT_LABEL_HEIGHT_DOTS,
                DEFAULT_ORIGIN_X,
                DEFAULT_ORIGIN_Y,
                DEFAULT_MODULE_WIDTH,
                DEFAULT_MODULE_RATIO,
                DEFAULT_BARCODE_HEIGHT,
                false,
                1,
                caption,
                false
        );
    }

    public String fileSlug() {
        return fileSlug;
    }
}
