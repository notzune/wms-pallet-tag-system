package com.tbg.wms.core.update;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReleaseAssetSupportTest {

    @Test
    void findChecksumAsset_shouldMatchInstallerSidecar() {
        ReleaseCheckService.ReleaseAsset installer =
                new ReleaseCheckService.ReleaseAsset("WMS Pallet Tag System-1.7.1.exe", "https://example/installer.exe");
        ReleaseCheckService.ReleaseAsset checksum =
                new ReleaseCheckService.ReleaseAsset("WMS Pallet Tag System-1.7.1.exe.sha256", "https://example/installer.exe.sha256");

        ReleaseCheckService.ReleaseAsset matched = ReleaseAssetSupport.findChecksumAsset(List.of(installer, checksum), installer);

        assertEquals(checksum, matched);
    }

    @Test
    void findChecksumAsset_shouldReturnNullWhenMissing() {
        ReleaseCheckService.ReleaseAsset installer =
                new ReleaseCheckService.ReleaseAsset("WMS Pallet Tag System-1.7.1.exe", "https://example/installer.exe");

        assertNull(ReleaseAssetSupport.findChecksumAsset(List.of(installer), installer));
    }
}
