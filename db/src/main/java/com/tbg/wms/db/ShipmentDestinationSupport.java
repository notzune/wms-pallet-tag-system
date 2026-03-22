package com.tbg.wms.db;

import com.tbg.wms.core.model.NormalizationService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolves shipment destination identifiers from available WMS fields.
 */
final class ShipmentDestinationSupport {
    private static final Pattern DC_NUMBER_PATTERN = Pattern.compile("(?i)\\bDC\\s*#?\\s*(\\d{3,6})\\b");

    String resolveLocationNumber(String destNum, String vcDestId, String shipToName, String adrHostExtId) {
        String destination = NormalizationService.normalizeString(destNum);
        if (!destination.isBlank()) {
            return destination;
        }
        String vcDestination = NormalizationService.normalizeString(vcDestId);
        if (!vcDestination.isBlank()) {
            return vcDestination;
        }
        String dcFromName = extractDcNumber(shipToName);
        if (dcFromName != null) {
            return dcFromName;
        }
        String addressHost = NormalizationService.normalizeString(adrHostExtId);
        return addressHost.isBlank() ? null : addressHost;
    }

    String extractDcNumber(String shipToName) {
        if (shipToName == null || shipToName.isBlank()) {
            return null;
        }
        Matcher matcher = DC_NUMBER_PATTERN.matcher(shipToName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
