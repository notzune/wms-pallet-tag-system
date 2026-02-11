/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Service for capturing snapshots of normalized WMS data to JSON files.
 *
 * Snapshots preserve the exact state of normalized data at a point in time,
 * enabling replay operations and debugging without database access.
 *
 * Each snapshot includes metadata (jobId, timestamp, input key, schema version)
 * and the complete normalized shipment data.
 */
public final class SnapshotService {

    private static final Logger log = LoggerFactory.getLogger(SnapshotService.class);

    private static final String SCHEMA_VERSION = "1.0";
    private static final DateTimeFormatter FILENAME_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final ObjectMapper objectMapper = createObjectMapper();

    /**
     * Creates and configures an ObjectMapper for JSON serialization.
     *
     * @return configured ObjectMapper with Java 8 date/time support
     */
    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Captures a snapshot of a shipment to a JSON file.
     *
     * The snapshot includes metadata and the complete normalized shipment data.
     * Files are created with deterministic names based on timestamp and shipment ID
     * for easy identification and replay.
     *
     * @param shipment the normalized shipment to capture
     * @param jobId unique job identifier for traceability
     * @param inputKey the input key (shipment/order/load ID)
     * @param outputDirectory directory where snapshot file should be created
     * @return Path to the created snapshot file
     * @throws IllegalArgumentException if parameters are invalid or directory doesn't exist
     * @throws IOException if snapshot file cannot be written
     */
    public static Path captureSnapshot(Shipment shipment, String jobId, String inputKey, String outputDirectory) throws IOException {
        Objects.requireNonNull(shipment, "shipment cannot be null");
        Objects.requireNonNull(jobId, "jobId cannot be null");
        Objects.requireNonNull(inputKey, "inputKey cannot be null");
        Objects.requireNonNull(outputDirectory, "outputDirectory cannot be null");

        Path outputPath = Paths.get(outputDirectory);
        if (!Files.exists(outputPath)) {
            throw new IllegalArgumentException("Output directory does not exist: " + outputDirectory);
        }
        if (!Files.isDirectory(outputPath)) {
            throw new IllegalArgumentException("Output path is not a directory: " + outputDirectory);
        }

        LocalDateTime now = LocalDateTime.now();
        String timestamp = now.format(FILENAME_DATE_FORMAT);
        String filename = String.format("snapshot_%s_%s_%s.json",
                timestamp, shipment.getShipmentId(), jobId.substring(0, Math.min(8, jobId.length())));

        Path snapshotFile = outputPath.resolve(filename);

        Map<String, Object> snapshot = buildSnapshot(shipment, jobId, inputKey, now);

        try {
            objectMapper.writeValue(snapshotFile.toFile(), snapshot);
            log.info("Snapshot captured: {} ({} LPNs)", snapshotFile.getFileName(), shipment.getLpnCount());
            return snapshotFile;
        } catch (IOException e) {
            log.error("Failed to write snapshot to {}: {}", snapshotFile, e.getMessage());
            throw new IOException("Failed to write snapshot: " + e.getMessage(), e);
        }
    }

    /**
     * Builds the snapshot structure with metadata and shipment data.
     *
     * @param shipment the shipment to include
     * @param jobId the job identifier
     * @param inputKey the input key
     * @param timestamp the capture timestamp
     * @return snapshot map structure
     */
    private static Map<String, Object> buildSnapshot(Shipment shipment, String jobId, String inputKey, LocalDateTime timestamp) {
        Map<String, Object> snapshot = new LinkedHashMap<>();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("schemaVersion", SCHEMA_VERSION);
        metadata.put("jobId", jobId);
        metadata.put("capturedAt", timestamp);
        metadata.put("inputKey", inputKey);
        metadata.put("shipmentId", shipment.getShipmentId());

        snapshot.put("metadata", metadata);
        snapshot.put("shipment", shipment);

        return snapshot;
    }

    /**
     * Reads and deserializes a snapshot from a JSON file.
     *
     * Used for replay operations to reconstruct the exact state without
     * database access.
     *
     * @param snapshotFile path to the snapshot JSON file
     * @return the deserialized Shipment
     * @throws IOException if snapshot file cannot be read or parsed
     * @throws IllegalArgumentException if snapshot file doesn't exist
     */
    public static Shipment readSnapshot(Path snapshotFile) throws IOException {
        Objects.requireNonNull(snapshotFile, "snapshotFile cannot be null");

        if (!Files.exists(snapshotFile)) {
            throw new IllegalArgumentException("Snapshot file does not exist: " + snapshotFile);
        }

        try {
            Map<String, Object> snapshot = objectMapper.readValue(snapshotFile.toFile(), Map.class);

            // Validate schema version
            Map<String, Object> metadata = (Map<String, Object>) snapshot.get("metadata");
            String version = (String) metadata.get("schemaVersion");
            if (!SCHEMA_VERSION.equals(version)) {
                log.warn("Snapshot schema version mismatch: expected {}, got {}", SCHEMA_VERSION, version);
            }

            // Extract and deserialize shipment
            Map<String, Object> shipmentData = (Map<String, Object>) snapshot.get("shipment");
            Shipment shipment = objectMapper.convertValue(shipmentData, Shipment.class);

            log.info("Snapshot loaded: {} ({} LPNs)", snapshotFile.getFileName(), shipment.getLpnCount());
            return shipment;
        } catch (IOException e) {
            log.error("Failed to read snapshot from {}: {}", snapshotFile, e.getMessage());
            throw new IOException("Failed to read snapshot: " + e.getMessage(), e);
        }
    }

    private SnapshotService() {
        // Utility class - prevent instantiation
    }
}

