package com.tbg.wms.core.ems;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Lightweight socket probe for documenting reachable integration endpoints.
 */
public final class EndpointProbeService {

    public EndpointProbeResult probe(String host, int port, int connectTimeoutMs, int readTimeoutMs) {
        Objects.requireNonNull(host, "host cannot be null");

        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), connectTimeoutMs);
            socket.setSoTimeout(readTimeoutMs);

            byte[] buffer = new byte[16];
            InputStream inputStream = socket.getInputStream();
            int read = inputStream.read(buffer);
            String banner = read <= 0 ? "CONNECTED_NO_BANNER" : preview(buffer, read);
            return new EndpointProbeResult(host, port, true, banner, "");
        } catch (Exception e) {
            return new EndpointProbeResult(host, port, false, "", e.getMessage());
        }
    }

    private String preview(byte[] buffer, int read) {
        String ascii = new String(buffer, 0, read, StandardCharsets.US_ASCII).replaceAll("\\p{Cntrl}", ".");
        if (ascii.trim().isEmpty()) {
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < read; i++) {
                if (i > 0) {
                    hex.append(' ');
                }
                hex.append(String.format(Locale.ROOT, "%02X", buffer[i] & 0xFF));
            }
            return hex.toString();
        }
        return ascii.trim();
    }
}
