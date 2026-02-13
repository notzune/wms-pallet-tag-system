/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for a physical printer.
 *
 * Represents a Zebra network printer with TCP/IP connectivity.
 * Each printer has a stable ID used in routing rules and a network
 * endpoint for RAW socket printing on port 9100.
 *
 * @since 1.0.0
 */
public final class PrinterConfig {

    private final String id;
    private final String name;
    private final String ip;
    private final int port;
    private final List<String> tags;
    private final String locationHint;
    private final boolean enabled;

    /**
     * Creates a new printer configuration.
     *
     * @param id stable printer identifier (e.g., "DISPATCH", "OFFICE")
     * @param name human-readable printer name
     * @param ip printer IP address
     * @param port printer port (typically 9100 for Zebra RAW protocol)
     * @param tags classification tags (PROD, TEST, DISPATCH, etc.)
     * @param locationHint human-readable physical location
     * @param enabled whether printer is currently active
     */
    public PrinterConfig(String id, String name, String ip, int port,
                        List<String> tags, String locationHint, boolean enabled) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
        this.ip = Objects.requireNonNull(ip, "ip cannot be null");
        this.port = port;
        this.tags = tags != null ? List.copyOf(tags) : Collections.emptyList();
        this.locationHint = locationHint;
        this.enabled = enabled;

        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Invalid port: " + port);
        }
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public List<String> getTags() {
        return tags;
    }

    public String getLocationHint() {
        return locationHint;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getEndpoint() {
        return ip + ":" + port;
    }

    @Override
    public String toString() {
        return "PrinterConfig{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", endpoint='" + getEndpoint() + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}

