/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import com.tbg.wms.core.exception.WmsPrintException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Network printing service for Zebra printers via TCP port 9100.
 *
 * Sends ZPL data directly to printers using RAW socket protocol.
 * Implements retry logic with exponential backoff for transient failures.
 *
 * Thread-safe (stateless operations).
 *
 * @since 1.0.0
 */
public final class NetworkPrintService {

    private static final Logger log = LoggerFactory.getLogger(NetworkPrintService.class);

    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10000;
    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final int DEFAULT_RETRY_DELAY_MS = 1000;

    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final int maxRetries;
    private final int retryDelayMs;

    /**
     * Creates a new network print service with default timeouts and retry settings.
     */
    public NetworkPrintService() {
        this(DEFAULT_CONNECT_TIMEOUT_MS, DEFAULT_READ_TIMEOUT_MS,
             DEFAULT_MAX_RETRIES, DEFAULT_RETRY_DELAY_MS);
    }

    /**
     * Creates a new network print service with custom settings.
     *
     * @param connectTimeoutMs connection timeout in milliseconds
     * @param readTimeoutMs read timeout in milliseconds
     * @param maxRetries maximum number of retry attempts
     * @param retryDelayMs base delay between retries (exponential backoff)
     */
    public NetworkPrintService(int connectTimeoutMs, int readTimeoutMs,
                              int maxRetries, int retryDelayMs) {
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
    }

    /**
     * Prints ZPL content to a network printer.
     *
     * Sends data via TCP socket on port 9100 (Zebra RAW protocol).
     * Implements retry logic with exponential backoff for transient failures.
     *
     * @param printer target printer configuration
     * @param zplContent ZPL content to print
     * @param labelId label identifier for logging
     * @throws WmsPrintException if printing fails after all retries
     */
    public void print(PrinterConfig printer, String zplContent, String labelId) {
        Objects.requireNonNull(printer, "printer cannot be null");
        Objects.requireNonNull(zplContent, "zplContent cannot be null");
        Objects.requireNonNull(labelId, "labelId cannot be null");

        log.info("Sending label {} to printer {} ({})", labelId, printer.getId(), printer.getEndpoint());

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries + 1; attempt++) {
            try {
                sendToPrinter(printer, zplContent);
                log.info("Successfully sent label {} to printer {}", labelId, printer.getId());
                return;
            } catch (IOException e) {
                lastException = e;

                if (attempt <= maxRetries) {
                    int delay = computeRetryDelay(attempt);
                    log.warn("Print attempt {} failed for label {}, retrying in {}ms: {}",
                            attempt, labelId, delay, e.getMessage());

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new WmsPrintException(
                                "Print interrupted during retry delay",
                                ie,
                                "Check printer connectivity and network status"
                        );
                    }
                } else {
                    log.error("Print failed for label {} after {} attempts", labelId, attempt);
                }
            }
        }

        // All retries exhausted
        throw new WmsPrintException(
                String.format("Failed to print label %s to printer %s after %d attempts",
                        labelId, printer.getId(), maxRetries + 1),
                lastException,
                String.format("Check printer status: %s (%s). Verify network connectivity and printer power.",
                        printer.getName(), printer.getEndpoint())
        );
    }

    private int computeRetryDelay(int attempt) {
        // attempt starts at 1, so first retry uses base delay.
        int shift = Math.max(0, attempt - 1);
        int cappedShift = Math.min(shift, 30);
        long delay = ((long) retryDelayMs) << cappedShift;
        return delay >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) delay;
    }

    /**
     * Sends ZPL content to printer via TCP socket.
     *
     * @param printer target printer
     * @param zplContent ZPL content
     * @throws IOException if network communication fails
     */
    private void sendToPrinter(PrinterConfig printer, String zplContent) throws IOException {
        InetSocketAddress address = new InetSocketAddress(printer.getIp(), printer.getPort());

        try (Socket socket = new Socket()) {
            socket.connect(address, connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            try (OutputStream out = socket.getOutputStream()) {
                byte[] data = zplContent.getBytes(StandardCharsets.UTF_8);
                out.write(data);
                out.flush();

                log.debug("Sent {} bytes to printer {} ({})",
                        data.length, printer.getId(), printer.getEndpoint());
            }
        }
    }

    /**
     * Tests connectivity to a printer without sending actual print data.
     *
     * @param printer printer to test
     * @return true if printer is reachable, false otherwise
     */
    public boolean testConnectivity(PrinterConfig printer) {
        Objects.requireNonNull(printer, "printer cannot be null");

        log.debug("Testing connectivity to printer {} ({})", printer.getId(), printer.getEndpoint());

        try (Socket socket = new Socket()) {
            InetSocketAddress address = new InetSocketAddress(printer.getIp(), printer.getPort());
            socket.connect(address, connectTimeoutMs);
            log.info("Printer {} is reachable", printer.getId());
            return true;
        } catch (IOException e) {
            log.warn("Printer {} is unreachable: {}", printer.getId(), e.getMessage());
            return false;
        }
    }
}

