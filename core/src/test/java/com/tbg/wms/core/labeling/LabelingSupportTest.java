/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.core.labeling;

import com.tbg.wms.core.RuntimePathResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class LabelingSupportTest {

    @Test
    void testResolveSkuMatrixCsvUsesConfiguredAppHome(@TempDir Path tempDir) throws Exception {
        Path appHome = tempDir.resolve("app-home");
        Path configDir = appHome.resolve("config");
        Path skuMatrix = configDir.resolve("walmart-sku-matrix.csv");
        Files.createDirectories(configDir);
        Files.writeString(skuMatrix, "sku,data\n", StandardCharsets.UTF_8);

        String previousAppHome = System.getProperty(RuntimePathResolver.APP_HOME_PROP);
        String previousUserDir = System.getProperty("user.dir");
        System.setProperty(RuntimePathResolver.APP_HOME_PROP, appHome.toString());
        System.setProperty("user.dir", tempDir.resolve("different-working-dir").toString());
        try {
            Path resolved = LabelingSupport.resolveSkuMatrixCsv();
            assertNotNull(resolved);
            assertEquals(skuMatrix.toAbsolutePath().normalize(), resolved.toAbsolutePath().normalize());
        } finally {
            if (previousAppHome == null) {
                System.clearProperty(RuntimePathResolver.APP_HOME_PROP);
            } else {
                System.setProperty(RuntimePathResolver.APP_HOME_PROP, previousAppHome);
            }
            if (previousUserDir == null) {
                System.clearProperty("user.dir");
            } else {
                System.setProperty("user.dir", previousUserDir);
            }
        }
    }
}
