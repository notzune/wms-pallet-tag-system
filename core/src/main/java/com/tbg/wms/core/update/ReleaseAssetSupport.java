package com.tbg.wms.core.update;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Matches related release assets such as installer binaries and checksum sidecars.
 */
public final class ReleaseAssetSupport {
    private ReleaseAssetSupport() {
        // Utility class.
    }

    public static ReleaseCheckService.ReleaseAsset findChecksumAsset(
            List<ReleaseCheckService.ReleaseAsset> assets,
            ReleaseCheckService.ReleaseAsset targetAsset
    ) {
        Objects.requireNonNull(assets, "assets cannot be null");
        Objects.requireNonNull(targetAsset, "targetAsset cannot be null");

        String expectedName = (targetAsset.name() + ".sha256").toLowerCase(Locale.ROOT);
        return assets.stream()
                .filter(asset -> asset.name().toLowerCase(Locale.ROOT).equals(expectedName))
                .findFirst()
                .orElse(null);
    }
}
