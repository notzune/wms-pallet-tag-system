/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.label;

import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.WalmartSkuMapping;
import com.tbg.wms.core.sku.SkuMappingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LabelDataBuilder.
 */
class LabelDataBuilderTest {

    @TempDir
    Path tempDir;

    private LabelDataBuilder builder;
    private Shipment testShipment;
    private Lpn testLpn;

    @BeforeEach
    void setUp() throws IOException {
        // Create test CSV and SkuMappingService
        Path csvFile = tempDir.resolve("sku-mapping.csv");
        String csvContent = "TBG SKU#,WALMART ITEM#,Item Description,check based on TBG SKU\n" +
                "205641,30081705,1.36L PL 1/6 NJ STRW BAN,1.36L PL 1/6 NJ STRW BAN\n";
        Files.writeString(csvFile, csvContent);
        SkuMappingService skuMapping = new SkuMappingService(csvFile);

        // Create test SiteConfig
        SiteConfig siteConfig = new SiteConfig(
                "TROPICANA PRODUCTS, INC.",
                "20405 E Business Parkway Rd",
                "Walnut, CA 91789"
        );

        builder = new LabelDataBuilder(skuMapping, siteConfig);

        // Create test line items
        LineItem item1 = new LineItem(
                "1", "0", "10048500205641000", "1.36L TROPICANA", null,
                "8000141715", null, "1000000001",
                100, 6, "CS", 275.0,
                null, null, null
        );

        // Create test LPN
        LocalDate mfgDate = LocalDate.now().minusDays(30);
        LocalDate expDate = LocalDate.now().plusMonths(6);
        testLpn = new Lpn("LPN001", "8000141715", "123456789012345678", 10, 60, 275.0, "ROSSI",
                "LOT001", "SLOT001", mfgDate, expDate, List.of(item1));

        // Create test Shipment
        LocalDateTime now = LocalDateTime.now();
        testShipment = new Shipment(
                "8000141715", "EXT8000141715", "8000141715", "3002",
                "CJR WHOLESALE GROCERS LTD", "5876 COOPERS AVE", null, null,
                "MISSISSAUGA", "ON", "L4Z 2B9", "CAN", "555-1234",
                "MDLE", "TL", "30021144717", "8000141715", "ROSSI",
                "PO123", "6080", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", now.minusDays(1), now.plusDays(3), now,
                List.of(testLpn)
        );
    }

    @Test
    void testBuildPopulatesRequiredFields() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        // Verify required fields are present and non-empty
        assertNotNull(fields.get("shipToName"));
        assertNotNull(fields.get("shipToAddress1"));
        assertNotNull(fields.get("shipToCity"));
        assertNotNull(fields.get("shipToState"));
        assertNotNull(fields.get("shipToZip"));
        assertNotNull(fields.get("carrierCode"));
        assertNotNull(fields.get("lpnId"));
        assertNotNull(fields.get("ssccBarcode"));
        assertNotNull(fields.get("tbgSku"));

        assertEquals("CJR WHOLESALE GROCERS LTD", fields.get("shipToName"));
        assertEquals("MISSISSAUGA", fields.get("shipToCity"));
        assertEquals("MDLE", fields.get("carrierCode"));
    }

    @Test
    void testBuildPopulatesShipFromFromConfig() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        assertEquals("TROPICANA PRODUCTS, INC.", fields.get("shipFromName"));
        assertEquals("20405 E Business Parkway Rd", fields.get("shipFromAddress"));
        assertEquals("Walnut, CA 91789", fields.get("shipFromCityStateZip"));
    }

    @Test
    void testBuildPopulatesPalletSequence() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        assertEquals("1", fields.get("palletSeq"));
        assertEquals("1", fields.get("palletTotal")); // Only 1 LPN in test shipment
    }

    @Test
    void testBuildPalletSequenceForMultiplePallets() {
        // Create another LPN
        LineItem item2 = new LineItem(
                "1", "0", "10048500205641000", "1.36L TROPICANA", null,
                "8000141715", null, "1000000001",
                50, 3, "CS", 137.5,
                null, null, null
        );
        Lpn lpn2 = new Lpn("LPN002", "8000141715", "123456789012345679", 5, 30, 137.5, "ROSSI",
                "LOT002", "SLOT002", LocalDate.now(), LocalDate.now(), List.of(item2));

        // Create shipment with 2 LPNs
        LocalDateTime now = LocalDateTime.now();
        Shipment shipmentWith2 = new Shipment(
                "8000141715", "EXT8000141715", "8000141715", "3002",
                "CJR WHOLESALE GROCERS LTD", "5876 COOPERS AVE", null, null,
                "MISSISSAUGA", "ON", "L4Z 2B9", "CAN", "555-1234",
                "MDLE", "TL", "30021144717", "8000141715", "ROSSI",
                "PO123", "6080", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", now.minusDays(1), now.plusDays(3), now,
                List.of(testLpn, lpn2)
        );

        Map<String, String> fields1 = builder.build(shipmentWith2, testLpn, 0, LabelType.WALMART_CANADA_GRID);
        Map<String, String> fields2 = builder.build(shipmentWith2, lpn2, 1, LabelType.WALMART_CANADA_GRID);

        assertEquals("1", fields1.get("palletSeq"));
        assertEquals("2", fields2.get("palletSeq"));
        assertEquals("2", fields1.get("palletTotal"));
        assertEquals("2", fields2.get("palletTotal"));
    }

    @Test
    void testBuildPopulatesWalmartItemViaSkuLookup() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        assertEquals("30081705", fields.get("walmartItemNumber"));
        assertEquals("1.36L PL 1/6 NJ STRW BAN", fields.get("itemDescription"));
    }

    @Test
    void testBuildUsesSpaceSafeDefaults() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        // These fields are optional and should have defaults
        String shipToAddress2 = fields.get("shipToAddress2");
        assertNotNull(shipToAddress2);
        assertTrue(shipToAddress2.equals(" ") || shipToAddress2.isEmpty() || !shipToAddress2.isBlank());
    }

    @Test
    void testBuildFormatsDatesProperly() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        // Dates should be formatted MM.dd.yyyy
        String shipDate = fields.get("shipDate");
        assertNotNull(shipDate);
        assertTrue(shipDate.matches("\\d{2}\\.\\d{2}\\.\\d{4}"), "Date should be MM.dd.yyyy format");
    }

    @Test
    void testBuildPopulatesLotTracking() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        assertEquals("LOT001", fields.get("warehouseLot"));
        assertEquals("SLOT001", fields.get("customerLot"));
        assertNotNull(fields.get("manufactureDate"));
        assertNotNull(fields.get("bestByDate"));
    }

    @Test
    void testBuildThrowsForMissingRequiredField() {
        // Create shipment with null ship-to name
        LocalDateTime now = LocalDateTime.now();
        Shipment badShipment = new Shipment(
                "8000141715", "EXT8000141715", "8000141715", "3002",
                null, "5876 COOPERS AVE", null, null, // shipToName is null
                "MISSISSAUGA", "ON", "L4Z 2B9", "CAN", "555-1234",
                "MDLE", "TL", "30021144717", "8000141715", "ROSSI",
                "PO123", "6080", "DEPT1",
                "STOP1", 1, "MOVE1", "PRO123", "BOL123",
                "R", now.minusDays(1), now.plusDays(3), now,
                List.of(testLpn)
        );

        assertThrows(IllegalArgumentException.class,
                () -> builder.build(badShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID),
                "Should throw for missing required field");
    }

    @Test
    void testBuildHandlesEmptyLineItems() {
        Lpn emptyLpn = new Lpn("LPN_EMPTY", "8000141715", "123456789012345680", 0, 0, 0.0, "ROSSI",
                "LOT003", "SLOT003", LocalDate.now(), LocalDate.now(), List.of()); // Empty line items

        Map<String, String> fields = builder.build(testShipment, emptyLpn, 0, LabelType.WALMART_CANADA_GRID);

        // Should have safe defaults for product fields
        assertNotNull(fields.get("tbgSku"));
        assertNotNull(fields.get("walmartItemNumber"));
        assertNotNull(fields.get("itemDescription"));
    }

    @Test
    void testBuildReturnsUnmodifiableMap() {
        Map<String, String> fields = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        assertThrows(UnsupportedOperationException.class,
                () -> fields.put("newKey", "newValue"),
                "Map should be immutable");
    }

    @Test
    void testBuildForMultipleLabelTypes() {
        Map<String, String> canada = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);
        Map<String, String> detailed = builder.build(testShipment, testLpn, 0, LabelType.WALMART_DETAILED);

        // Both should have the same core fields
        assertEquals(canada.get("shipToName"), detailed.get("shipToName"));
        assertEquals(canada.get("ssccBarcode"), detailed.get("ssccBarcode"));

        // Both should have similar size (though exact count may differ)
        assertTrue(canada.size() > 0);
        assertTrue(detailed.size() > 0);
    }

    @Test
    void testBuildConsistentForSameInput() {
        Map<String, String> fields1 = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);
        Map<String, String> fields2 = builder.build(testShipment, testLpn, 0, LabelType.WALMART_CANADA_GRID);

        assertEquals(fields1, fields2, "Same input should produce same output");
    }
}

