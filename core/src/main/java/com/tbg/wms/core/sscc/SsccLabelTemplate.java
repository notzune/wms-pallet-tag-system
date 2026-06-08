package com.tbg.wms.core.sscc;

import java.util.List;
import java.util.Objects;

/**
 * Renders the SSCC pallet label as ZPL.
 */
public final class SsccLabelTemplate {
    private static final String SHIP_FROM_NAME = "Tropicana Manufacturing Company Inc.";
    private static final String SHIP_FROM_STREET = "4 Owens Rd.";
    private static final String SHIP_FROM_CITY = "Brockport, NY 14420";

    public String render(SsccLabelGroup label, int palletNumber, int palletTotal) {
        Objects.requireNonNull(label, "label cannot be null");

        String ssccData = SsccBarcodeSupport.formatSsccCode128Data(label.newReceivedLpn());
        String ssccText = SsccBarcodeSupport.formatSsccHumanReadable(label.newReceivedLpn());
        String poData = SsccBarcodeSupport.escapeZplText(label.purchaseOrder());
        String[] destinationLines = splitDestinationAddress(label.destinationAddress());
        String itemText = label.itemLineText();
        String skuBanner = label.bannerText();
        String cartonText = label.cartonText();

        StringBuilder zpl = new StringBuilder(1024);
        zpl.append("^XA\n");
        zpl.append("^CI28\n");
        zpl.append("^PW812\n");
        zpl.append("^LL1218\n");
        zpl.append("^LH0,0\n");
        zpl.append("^FO40,35^GB732,1120,3^FS\n");
        zpl.append("^FO40,270^GB732,0,3^FS\n");
        zpl.append("^FO406,35^GB0,235,3^FS\n");
        zpl.append("^FO40,450^GB732,0,3^FS\n");
        zpl.append("^FO406,270^GB0,180,3^FS\n");
        zpl.append("^FO40,520^GB732,0,3^FS\n");
        zpl.append("^FO406,450^GB0,70,3^FS\n");
        zpl.append("^FO40,650^GB732,0,3^FS\n");

        appendTextBlock(zpl, 60, 55, 26, 24, 320, 1, "SHIP FROM:");
        appendTextBlock(zpl, 60, 90, 24, 22, 320, 4,
                SHIP_FROM_NAME + "\n" + SHIP_FROM_STREET + "\n" + SHIP_FROM_CITY);

        appendTextBlock(zpl, 430, 55, 26, 24, 300, 1, "SHIP TO:");
        appendTextBlock(zpl, 430, 90, 24, 22, 300, 5,
                label.destination() + "\n" + destinationLines[0] + "\n" + destinationLines[1]);

        appendTextBlock(zpl, 60, 290, 24, 22, 300, 4,
                "CARRIER: " + label.carrierCode()
                        + "\nSHIPMENT #: " + label.shipment()
                        + "\nSO #: " + label.salesOrder()
                        + "\nTRAILER: " + label.trailerId());

        appendTextBlock(zpl, 430, 290, 24, 22, 300, 1, "PO(s):");
        zpl.append("^FO500,330^BCN,80,Y,N,N^FD").append(poData).append("^FS\n");

        appendTextBlock(zpl, 60, 473, 26, 24, 300, 1, "Pallet " + palletNumber + " of " + palletTotal);
        appendTextBlock(zpl, 430, 473, 26, 24, 300, 1,
                cartonText.isEmpty() ? "Cartons on Pallet:" : "Cartons on Pallet: " + cartonText);

        appendTextBlock(zpl, 60, 555, 54, 50, 680, 1, skuBanner);
        appendTextBlock(zpl, 60, 672, 30, 28, 680, 1, "Pallet SSCC");
        zpl.append("^FO95,735^BCN,190,N,N,N^FD").append(ssccData).append("^FS\n");
        appendTextBlock(zpl, 95, 940, 34, 30, 620, 1, ssccText);

        appendTextBlock(zpl, 60, 1015, 22, 20, 675, 3, "Items: " + itemText + "\nLot/Date: " + join(label.level2ReferenceCodes()));
        zpl.append("^XZ\n");
        return zpl.toString();
    }

    private static String join(List<String> values) {
        return values == null || values.isEmpty() ? "" : String.join(", ", values);
    }

    private static String[] splitDestinationAddress(String address) {
        String safe = address == null ? "" : address.trim();
        String[] parts = safe.split(",", 2);
        if (parts.length >= 2) {
            return new String[]{parts[0].trim(), parts[1].trim()};
        }
        return new String[]{safe, ""};
    }

    private static void appendTextBlock(StringBuilder zpl, int x, int y, int height, int width, int blockWidth, int lines, String text) {
        zpl.append("^FO").append(x).append(',').append(y)
                .append("^A0N,").append(height).append(',').append(width)
                .append("^FB").append(blockWidth).append(',').append(lines).append(",4,L,0^FD")
                .append(SsccBarcodeSupport.escapeZplText(text))
                .append("^FS\n");
    }
}
