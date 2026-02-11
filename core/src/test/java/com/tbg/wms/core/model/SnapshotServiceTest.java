/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for SnapshotService snapshot capture and loading.
 *
 * Tests verify JSON serialization, file creation, metadata inclusion,
 * and deserialization for replay scenarios.
 */
class SnapshotServiceTest {

    @TempDir
    Path tempDir;

    private Shipment testShipment;

    @BeforeEach
    void setUp() {
        // Create a test shipment with LPNs and line items
        LineItem item1 = new LineItem("1", "SKU001", "Product A", 10, 25.5, "LB");
        LineItem item2 = new LineItem("2", "SKU002", "Product B", 20, 50.0, "LB");

        Lpn lpn1 = new Lpn("LPN001", "SHIP123", "123456789012", 5, 30, 75.5, "ROSSI", List.of(item1));
        Lpn lpn2 = new Lpn("LPN002", "SHIP123", "123456789013", 4, 20, 50.0, "ROSSI", List.of(item2));

        testShipment = new Shipment(
                "SHIP123", "ORD456", "Acme Corp", "123 Main St",
                "Springfield", "IL", "62701", "UPS", "GND",
                LocalDateTime.of(2026, 2, 10, 10, 30, 0),
                List.of(lpn1, lpn2)
        );
    }

    @Test
    void testCaptureSnapshotCreatesFile() throws IOException {
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", tempDir.toString());

        assertTrue(Files.exists(snapshotFile), "Snapshot file should exist");
        assertTrue(snapshotFile.toString().endsWith(".json"), "File should have .json extension");
        assertTrue(Files.size(snapshotFile) > 0, "Snapshot file should not be empty");
    }

    @Test
    void testCaptureSnapshotFileNameContainsIdentifiers() throws IOException {
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123abc", "SHIP123", tempDir.toString());

        String fileName = snapshotFile.getFileName().toString();
        assertTrue(fileName.startsWith("snapshot_"), "Filename should start with snapshot_");
        assertTrue(fileName.contains("SHIP123"), "Filename should contain shipment ID");
        assertTrue(fileName.contains("job123"), "Filename should contain job ID prefix");
        assertTrue(fileName.endsWith(".json"), "Filename should end with .json");
    }

    @Test
    void testCaptureSnapshotRequiresShipment() {
        assertThrows(NullPointerException.class,
                () -> SnapshotService.captureSnapshot(null, "job123", "SHIP123", tempDir.toString()),
                "Should require non-null shipment");
    }

    @Test
    void testCaptureSnapshotRequiresJobId() {
        assertThrows(NullPointerException.class,
                () -> SnapshotService.captureSnapshot(testShipment, null, "SHIP123", tempDir.toString()),
                "Should require non-null jobId");
    }

    @Test
    void testCaptureSnapshotRequiresInputKey() {
        assertThrows(NullPointerException.class,
                () -> SnapshotService.captureSnapshot(testShipment, "job123", null, tempDir.toString()),
                "Should require non-null inputKey");
    }

    @Test
    void testCaptureSnapshotRequiresOutputDirectory() {
        assertThrows(NullPointerException.class,
                () -> SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", null),
                "Should require non-null outputDirectory");
    }

    @Test
    void testCaptureSnapshotValidatesDirectoryExists() {
        String nonExistentDir = tempDir.resolve("nonexistent").toString();

        assertThrows(IllegalArgumentException.class,
                () -> SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", nonExistentDir),
                "Should reject non-existent directory");
    }

    @Test
    void testCaptureSnapshotValidatesDirectoryIsDirectory() throws IOException {
        // Create a file instead of a directory
        Path filePath = tempDir.resolve("file.txt");
        Files.createFile(filePath);

        assertThrows(IllegalArgumentException.class,
                () -> SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", filePath.toString()),
                "Should reject file path instead of directory");
    }

    @Test
    void testReadSnapshotRequiresFile() {
        assertThrows(NullPointerException.class,
                () -> SnapshotService.readSnapshot(null),
                "Should require non-null snapshot file");
    }

    @Test
    void testReadSnapshotValidatesFileExists() {
        Path nonExistentFile = tempDir.resolve("nonexistent.json");

        assertThrows(IllegalArgumentException.class,
                () -> SnapshotService.readSnapshot(nonExistentFile),
                "Should reject non-existent file");
    }

    @Test
    void testCaptureAndReadSnapshotRoundTrip() throws IOException {
        // Capture snapshot
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", tempDir.toString());

        // Read snapshot back
        Shipment loadedShipment = SnapshotService.readSnapshot(snapshotFile);

        // Verify round-trip preserves data
        assertEquals(testShipment.getShipmentId(), loadedShipment.getShipmentId());
        assertEquals(testShipment.getOrderId(), loadedShipment.getOrderId());
        assertEquals(testShipment.getShipToName(), loadedShipment.getShipToName());
        assertEquals(testShipment.getLpnCount(), loadedShipment.getLpnCount());
    }

    @Test
    void testSnapshotPreservesShipmentDetails() throws IOException {
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", tempDir.toString());
        Shipment loaded = SnapshotService.readSnapshot(snapshotFile);

        assertEquals("SHIP123", loaded.getShipmentId());
        assertEquals("ORD456", loaded.getOrderId());
        assertEquals("Acme Corp", loaded.getShipToName());
        assertEquals("123 Main St", loaded.getShipToAddress());
        assertEquals("Springfield", loaded.getShipToCity());
        assertEquals("IL", loaded.getShipToState());
        assertEquals("62701", loaded.getShipToZip());
        assertEquals("UPS", loaded.getCarrierCode());
        assertEquals("GND", loaded.getServiceCode());
    }

    @Test
    void testSnapshotPreservesLpnDetails() throws IOException {
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", tempDir.toString());
        Shipment loaded = SnapshotService.readSnapshot(snapshotFile);

        assertEquals(2, loaded.getLpnCount());

        Lpn lpn1 = loaded.getLpns().get(0);
        assertEquals("LPN001", lpn1.getLpnId());
        assertEquals("123456789012", lpn1.getSscc());
        assertEquals(5, lpn1.getCaseCount());
        assertEquals(30, lpn1.getUnitCount());
        assertEquals(75.5, lpn1.getWeight());
        assertEquals("ROSSI", lpn1.getStagingLocation());
    }

    @Test
    void testSnapshotPreservesLineItemDetails() throws IOException {
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", tempDir.toString());
        Shipment loaded = SnapshotService.readSnapshot(snapshotFile);

        Lpn lpn1 = loaded.getLpns().get(0);
        assertEquals(1, lpn1.getLineItems().size());

        LineItem item = lpn1.getLineItems().get(0);
        assertEquals("1", item.getLineNumber());
        assertEquals("SKU001", item.getSku());
        assertEquals("Product A", item.getDescription());
        assertEquals(10, item.getQuantity());
        assertEquals(25.5, item.getWeight());
        assertEquals("LB", item.getUom());
    }

    @Test
    void testMultipleSnapshotsWithDifferentShipments() throws IOException {
        // Create second shipment
        Lpn lpn3 = new Lpn("LPN003", "SHIP999", "999999999999", 1, 5, 10.0, "OFFICE", List.of());
        Shipment secondShipment = new Shipment(
                "SHIP999", "ORD789", "BigCo", "456 Oak Ave",
                "Shelbyville", "KY", "40065", "FEDEX", "OVR",
                LocalDateTime.now(),
                List.of(lpn3)
        );

        // Capture both
        Path file1 = SnapshotService.captureSnapshot(testShipment, "job1", "SHIP123", tempDir.toString());
        Path file2 = SnapshotService.captureSnapshot(secondShipment, "job2", "SHIP999", tempDir.toString());

        // Verify both exist and are different
        assertTrue(Files.exists(file1));
        assertTrue(Files.exists(file2));
        assertNotEquals(file1, file2);

        // Verify content
        Shipment loaded1 = SnapshotService.readSnapshot(file1);
        Shipment loaded2 = SnapshotService.readSnapshot(file2);

        assertEquals("SHIP123", loaded1.getShipmentId());
        assertEquals("SHIP999", loaded2.getShipmentId());
    }

    @Test
    void testSnapshotJsonIsReadable() throws IOException {
        Path snapshotFile = SnapshotService.captureSnapshot(testShipment, "job123", "SHIP123", tempDir.toString());

        String content = Files.readString(snapshotFile);
        assertTrue(content.contains("\"metadata\""), "Snapshot should contain metadata");
        assertTrue(content.contains("\"shipment\""), "Snapshot should contain shipment");
        assertTrue(content.contains("\"jobId\""), "Metadata should contain jobId");
        assertTrue(content.contains("\"schemaVersion\""), "Metadata should contain schemaVersion");
    }
}

