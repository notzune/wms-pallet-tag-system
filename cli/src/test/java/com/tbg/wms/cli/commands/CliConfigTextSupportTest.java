package com.tbg.wms.cli.commands;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CliConfigTextSupportTest {

    private final CliConfigTextSupport support = new CliConfigTextSupport();

    @Test
    void buildConfigReport_shouldIncludePrinterAndDatabaseSections() {
        CliConfigTextSupport.ConfigSnapshot snapshot = new CliConfigTextSupport.ConfigSnapshot(
                "TBG3002",
                "Bradenton",
                "PROD",
                ".env",
                "10.19.68.61",
                1521,
                "WMSP",
                "wmsp",
                "***123",
                "jdbc:oracle:thin:@//10.19.68.61:1521/WMSP",
                "-",
                "jdbc:a | jdbc:b",
                10,
                5000,
                1000,
                "config/printers.yml",
                "DISPATCH",
                "(none)"
        );

        String report = support.buildConfigReport(snapshot);

        assertTrue(report.contains("=== WMS Pallet Tag System Configuration ==="));
        assertTrue(report.contains("Database Configuration:"));
        assertTrue(report.contains("Printer Configuration:"));
        assertTrue(report.contains("Default Printer: DISPATCH"));
    }

    @Test
    void buildDbTestConfiguration_shouldIncludeHeaderAndPoolSection() {
        CliConfigTextSupport.ConfigSnapshot snapshot = new CliConfigTextSupport.ConfigSnapshot(
                "TBG3002",
                "Bradenton",
                "PROD",
                ".env",
                "10.19.68.61",
                1521,
                "WMSP",
                "wmsp",
                "***123",
                "jdbc:oracle:thin:@//10.19.68.61:1521/WMSP",
                "WMSP",
                "jdbc:a | jdbc:b",
                10,
                5000,
                1000,
                "config/printers.yml",
                "DISPATCH",
                "(none)"
        );

        String report = support.buildDbTestConfiguration(snapshot);

        assertTrue(report.contains("Database Connectivity Test"));
        assertTrue(report.contains("Pool Configuration:"));
        assertTrue(report.contains("Active Site:     TBG3002 (Bradenton)"));
        assertTrue(report.contains("JDBC Candidates: jdbc:a | jdbc:b"));
    }
}
