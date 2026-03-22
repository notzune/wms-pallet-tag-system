package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.print.PrinterConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LabelWorkflowAssetSupportTest {

    @Test
    void resolvePrinter_shouldCacheResolvedPrinterBySite(@TempDir Path tempDir) throws Exception {
        Path siteDir = Files.createDirectories(tempDir.resolve("TBG3002"));
        Files.writeString(siteDir.resolve("printers.yaml"), String.join("\n",
                "version: 1",
                "siteCode: TBG3002",
                "printers:",
                "  - id: OFFICE",
                "    name: Office_Test",
                "    ip: 10.19.64.106",
                "    port: 9100",
                ""
        ), StandardCharsets.UTF_8);
        Files.writeString(siteDir.resolve("printer-routing.yaml"), String.join("\n",
                "version: 1",
                "siteCode: TBG3002",
                "defaultPrinterId: OFFICE",
                "rules: []",
                ""
        ), StandardCharsets.UTF_8);

        LabelWorkflowAssetSupport support = new LabelWorkflowAssetSupport(new AppConfig(), tempDir);

        PrinterConfig first = support.resolvePrinter("TBG3002", "OFFICE");
        PrinterConfig second = support.resolvePrinter("TBG3002", "OFFICE");

        assertNotNull(first);
        assertSame(first, second);
        assertEquals("OFFICE", first.getId());
    }
}
