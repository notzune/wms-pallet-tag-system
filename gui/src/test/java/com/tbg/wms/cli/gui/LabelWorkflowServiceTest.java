package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LabelWorkflowServiceTest {

    @Test
    void loadPrinters_shouldReadEnabledPrintersFromPrintersYaml(@TempDir Path tempDir) throws Exception {
        Path siteDir = Files.createDirectories(tempDir.resolve("TBG3002"));

        Files.writeString(siteDir.resolve("printers.yaml"), String.join("\n",
                "version: 1",
                "siteCode: TBG3002",
                "printers:",
                "  - id: OFFICE",
                "    name: Office_Test",
                "    ip: 10.19.64.106",
                "    port: 9100",
                "  - id: RAIL_OFFICE",
                "    name: RAIL OFFICE",
                "    ip: 10.19.64.16",
                "    port: 9100",
                "  - id: ORDER_PICK",
                "    name: ORDER PICK",
                "    ip: 10.19.64.52",
                "    port: 9100",
                "    enabled: false",
                ""
        ), StandardCharsets.UTF_8);

        Files.writeString(siteDir.resolve("printer-routing.yaml"), String.join("\n",
                "version: 1",
                "siteCode: TBG3002",
                "defaultPrinterId: OFFICE",
                "rules: []",
                ""
        ), StandardCharsets.UTF_8);

        LabelWorkflowService service = new LabelWorkflowService(new AppConfig(), tempDir);

        List<LabelWorkflowService.PrinterOption> printers = service.loadPrinters();

        assertEquals(2, printers.size());
        assertEquals(List.of("OFFICE", "RAIL_OFFICE"),
                printers.stream().map(LabelWorkflowService.PrinterOption::getId).collect(Collectors.toList()));
        assertTrue(printers.stream().anyMatch(option -> option.toString().contains("Office_Test")));
        assertTrue(printers.stream().noneMatch(option -> option.getId().equals("ORDER_PICK")));
    }
}
