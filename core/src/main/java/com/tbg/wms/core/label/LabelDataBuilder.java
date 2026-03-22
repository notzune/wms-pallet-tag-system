/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.label;

import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.location.LocationNumberMappingService;
import com.tbg.wms.core.model.*;
import com.tbg.wms.core.sku.SkuMappingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Builds label data by mapping rich domain objects to ZPL template field values.
 * <p>
 * This is the critical architectural bridge between the database-populated domain
 * models (Shipment, Lpn, LineItem) and the generic ZplTemplateEngine which requires
 * a {@code Map<String, String>} for field substitution.
 * <p>
 * Different label formats need different subsets of fields. The builder handles:
 * - Required fields (throw if missing)
 * - Optional fields (safe defaults like " " if not available)
 * - Type conversions (int->String, LocalDateTime->formatted date, etc.)
 * - SKU lookups (TBG PRTNUM -> Walmart item code via SkuMappingService)
 * - Composite field formatting (address combining, date formatting, etc.)
 * <p>
 * This design allows adding/removing label fields with just one line of code
 * in the builder, without changing domain models or template engine.
 */
public final class LabelDataBuilder {

    private static final Logger log = LoggerFactory.getLogger(LabelDataBuilder.class);

    private static final String SPACE_SAFE_DEFAULT = " ";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM.dd.yyyy");

    private final SkuMappingService skuMapping;
    private final SiteConfig siteConfig;
    private final Map<String, ShipmentSkuFootprint> footprintBySku;
    private final LocationNumberMappingService locationNumberMapping;
    private final LabelProductFieldSupport productFieldSupport;

    /**
     * Creates a new LabelDataBuilder.
     *
     * @param skuMapping service for Walmart SKU code lookups
     * @param siteConfig site-specific configuration (ship-from address, etc.)
     */
    public LabelDataBuilder(SkuMappingService skuMapping, SiteConfig siteConfig) {
        this(skuMapping, siteConfig, Collections.emptyMap(), loadLocationNumberMappingOrNull());
    }

    /**
     * Creates a new LabelDataBuilder with optional per-SKU footprint metadata.
     *
     * @param skuMapping     service for Walmart SKU code lookups
     * @param siteConfig     site-specific configuration (ship-from address, etc.)
     * @param footprintBySku shipment footprint map keyed by SKU
     */
    public LabelDataBuilder(SkuMappingService skuMapping,
                            SiteConfig siteConfig,
                            Map<String, ShipmentSkuFootprint> footprintBySku) {
        this(skuMapping, siteConfig, footprintBySku, loadLocationNumberMappingOrNull());
    }

    LabelDataBuilder(SkuMappingService skuMapping,
                     SiteConfig siteConfig,
                     Map<String, ShipmentSkuFootprint> footprintBySku,
                     LocationNumberMappingService locationNumberMapping) {
        this.skuMapping = Objects.requireNonNull(skuMapping, "skuMapping cannot be null");
        this.siteConfig = Objects.requireNonNull(siteConfig, "siteConfig cannot be null");
        this.footprintBySku = footprintBySku == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(footprintBySku));
        this.locationNumberMapping = locationNumberMapping;
        this.productFieldSupport = new LabelProductFieldSupport(this.skuMapping, this.footprintBySku);
    }

    private static LocationNumberMappingService loadLocationNumberMappingOrNull() {
        try {
            java.nio.file.Path matrix = LabelingSupport.resolveLocationMatrixCsv();
            if (matrix == null) {
                return null;
            }
            return new LocationNumberMappingService(matrix);
        } catch (Exception e) {
            log.warn("Location number matrix could not be loaded; sold-to to DC mapping is disabled: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Builds label data for a specific pallet within a shipment.
     * <p>
     * Populates the exact fields needed for the given label type. Required fields
     * (ship-to name, SSCC, etc.) throw IllegalArgumentException if missing. Optional
     * fields fall back to safe defaults.
     *
     * @param shipment    the parent shipment
     * @param lpn         the specific pallet (LPN) to label
     * @param palletIndex 0-based index (will be converted to 1-based "1 of N" format)
     * @param labelType   the label format type (WALMART_CANADA, etc.)
     * @return {@code Map<String, String>} ready for ZplTemplateEngine
     * @throws IllegalArgumentException if required fields are missing
     */
    public Map<String, String> build(Shipment shipment, Lpn lpn, int palletIndex, LabelType labelType) {
        Objects.requireNonNull(shipment, "shipment cannot be null");
        Objects.requireNonNull(lpn, "lpn cannot be null");
        Objects.requireNonNull(labelType, "labelType cannot be null");
        validatePalletIndex(shipment, palletIndex);

        log.debug("Building label data for shipment {} pallet {}/{}",
                shipment.getShipmentId(), palletIndex + 1, shipment.getLpnCount());

        Map<String, String> fields = new LinkedHashMap<>();

        // Ship From (static per site)
        fields.put("shipFromName", require(siteConfig.getShipFromName(), "shipFromName"));
        fields.put("shipFromAddress", require(siteConfig.getShipFromAddress(), "shipFromAddress"));
        fields.put("shipFromCityStateZip", require(siteConfig.getShipFromCityStateZip(), "shipFromCityStateZip"));

        // Ship To (required)
        fields.put("shipToName", require(shipment.getShipToName(), "shipToName"));
        fields.put("shipToAddress1", require(shipment.getShipToAddress1(), "shipToAddress1"));
        fields.put("shipToAddress2", orDefault(shipment.getShipToAddress2(), SPACE_SAFE_DEFAULT));
        fields.put("shipToAddress3", orDefault(shipment.getShipToAddress3(), SPACE_SAFE_DEFAULT));
        fields.put("shipToCity", require(shipment.getShipToCity(), "shipToCity"));
        fields.put("shipToState", require(shipment.getShipToState(), "shipToState"));
        fields.put("shipToZip", require(shipment.getShipToZip(), "shipToZip"));
        fields.put("shipToCountry", orDefault(shipment.getShipToCountry(), SPACE_SAFE_DEFAULT));
        fields.put("shipToPhone", orDefault(shipment.getShipToPhone(), SPACE_SAFE_DEFAULT));

        // Carrier and shipping info
        fields.put("carrierCode", require(shipment.getCarrierCode(), "carrierCode"));
        fields.put("carrierMoveId", orDefault(shipment.getCarrierMoveId(), SPACE_SAFE_DEFAULT));
        fields.put("serviceLevel", orDefault(shipment.getServiceLevel(), SPACE_SAFE_DEFAULT));
        fields.put("documentNumber", orDefault(shipment.getDocumentNumber(), SPACE_SAFE_DEFAULT));
        fields.put("trackingNumber", orDefault(shipment.getTrackingNumber(), SPACE_SAFE_DEFAULT));

        // Order-level fields
        fields.put("customerPo", orDefault(shipment.getCustomerPo(), SPACE_SAFE_DEFAULT));
        fields.put("locationNumber", orDefault(resolveLocationNumber(shipment), SPACE_SAFE_DEFAULT));
        fields.put("departmentNumber", orDefault(shipment.getDepartmentNumber(), SPACE_SAFE_DEFAULT));

        // Carrier move / stop details
        fields.put("proNumber", orDefault(shipment.getProNumber(), SPACE_SAFE_DEFAULT));
        fields.put("bolNumber", orDefault(shipment.getBolNumber(), SPACE_SAFE_DEFAULT));
        fields.put("stopSequence", orDefault(str(shipment.getStopSequence()), SPACE_SAFE_DEFAULT));

        // Dates
        fields.put("shipDate", orDefault(fmtDate(shipment.getShipDate()), SPACE_SAFE_DEFAULT));
        fields.put("deliveryDate", orDefault(fmtDate(shipment.getDeliveryDate()), SPACE_SAFE_DEFAULT));

        // LPN / barcode data (required)
        fields.put("lpnId", require(lpn.getLpnId(), "lpnId"));
        fields.put("ssccBarcode", require(lpn.getSscc(), "ssccBarcode"));
        fields.put("palletSeq", String.valueOf(palletIndex + 1));
        fields.put("palletTotal", String.valueOf(shipment.getLpnCount()));

        // Pallet weight and dimensions
        fields.put("weight", orDefault(String.valueOf(lpn.getWeight()), SPACE_SAFE_DEFAULT));

        // Lot tracking (optional)
        fields.put("warehouseLot", orDefault(lpn.getWarehouseLot(), SPACE_SAFE_DEFAULT));
        fields.put("customerLot", orDefault(lpn.getCustomerLot(), SPACE_SAFE_DEFAULT));
        fields.put("manufactureDate", orDefault(fmtDate(lpn.getManufactureDate()), SPACE_SAFE_DEFAULT));
        fields.put("bestByDate", orDefault(fmtDate(lpn.getBestByDate()), SPACE_SAFE_DEFAULT));

        // Product/line item data (representative item on this pallet)
        productFieldSupport.populateFields(fields, shipment, lpn);

        // Staging location (for diagnostics / printer routing)
        fields.put("stagingLocation", orDefault(lpn.getStagingLocation(), SPACE_SAFE_DEFAULT));

        return Collections.unmodifiableMap(fields);
    }

    private void validatePalletIndex(Shipment shipment, int palletIndex) {
        if (palletIndex < 0) {
            throw new IllegalArgumentException("palletIndex must be >= 0");
        }
        int lpnCount = shipment.getLpnCount();
        if (lpnCount <= 0) {
            throw new IllegalArgumentException("shipment must contain at least one pallet");
        }
        if (palletIndex >= lpnCount) {
            throw new IllegalArgumentException(
                    "palletIndex " + palletIndex + " is out of range for shipment pallet count " + lpnCount
            );
        }
    }

    /**
     * Require a non-empty value or throw.
     *
     * @param value     the value to check
     * @param fieldName name of field (for error message)
     * @return the value if non-empty
     * @throws IllegalArgumentException if value is null or empty
     */
    private String require(String value, String fieldName) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Required field missing: " + fieldName);
        }
        return value.trim();
    }

    /**
     * Return value if non-empty, otherwise default.
     *
     * @param value        the value to check
     * @param defaultValue fallback if value is null/empty
     * @return value or default
     */
    private String orDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }

    /**
     * Format a LocalDateTime to MM.dd.yyyy string.
     *
     * @param datetime the date/time to format
     * @return formatted string, or null if input is null
     */
    private String fmtDate(LocalDateTime datetime) {
        if (datetime == null) {
            return null;
        }
        return datetime.format(DATE_FORMAT);
    }

    /**
     * Format a LocalDate to MM.dd.yyyy string.
     *
     * @param date the date to format
     * @return formatted string, or null if input is null
     */
    private String fmtDate(java.time.LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.format(DATE_FORMAT);
    }

    /**
     * Safe null-to-string conversion.
     *
     * @param obj the object to convert
     * @return string representation or null
     */
    private String str(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private String resolveLocationNumber(Shipment shipment) {
        String raw = shipment.getLocationNumber();
        if (locationNumberMapping == null) {
            return raw;
        }
        return locationNumberMapping.resolveDcLocation(raw);
    }
}
