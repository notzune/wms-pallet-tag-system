package com.tbg.wms.core.ems;

import java.util.Objects;

/**
 * Result of a lightweight socket probe against an integration endpoint.
 */
public final class EndpointProbeResult {
    private final String host;
    private final int port;
    private final boolean reachable;
    private final String bannerPreview;
    private final String error;

    public EndpointProbeResult(String host, int port, boolean reachable, String bannerPreview, String error) {
        this.host = Objects.requireNonNull(host, "host cannot be null");
        this.port = port;
        this.reachable = reachable;
        this.bannerPreview = bannerPreview == null ? "" : bannerPreview;
        this.error = error == null ? "" : error;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public boolean isReachable() {
        return reachable;
    }

    public String getBannerPreview() {
        return bannerPreview;
    }

    public String getError() {
        return error;
    }
}
