package com.tbg.wms.core.sscc;

/**
 * SSCC and PO barcode formatting helpers.
 */
public final class SsccBarcodeSupport {
    private SsccBarcodeSupport() {
    }

    public static String formatSsccHumanReadable(String sscc) {
        String digits = digitsOnly(sscc);
        if (digits.length() == 20 && digits.startsWith("00")) {
            return "(00) " + digits.substring(2);
        }
        if (digits.length() == 18) {
            return "(00) " + digits;
        }
        return trim(sscc);
    }

    public static String formatSsccCode128Data(String sscc) {
        String digits = digitsOnly(sscc);
        if (digits.length() == 18) {
            digits = "00" + digits;
        }
        if (digits.length() != 20 || !digits.startsWith("00")) {
            throw new IllegalArgumentException("SSCC must be 18 digits or 20 digits including AI 00.");
        }
        return ">;>8" + digits;
    }

    public static String escapeZplText(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("^", " ").replace("~", " ").replace("\\", "/").trim();
        return escaped.replace("\r\n", "\\&").replace("\n", "\\&").replace("\r", "\\&");
    }

    private static String digitsOnly(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
