/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link PrinterRoutingService}.
 */
class PrinterRoutingServiceTest {

    @Test
    void selectPrinter_shouldRouteToDispatch_whenStagingLocationIsROSSI() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", true);
        PrinterConfig dispatch = new PrinterConfig("DISPATCH", "Dispatch_Prod", "10.19.64.53", 9100,
                List.of("PROD", "DISPATCH"), "Dispatch office", true);

        Map<String, PrinterConfig> printers = Map.of(
                "OFFICE", office,
                "DISPATCH", dispatch
        );

        RoutingRule rule = new RoutingRule("staging-rossi-dispatch", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");

        List<RoutingRule> rules = List.of(rule);

        PrinterRoutingService service = new PrinterRoutingService(printers, rules, "OFFICE", "TBG3002");

        // Act
        Map<String, String> context = Map.of("stagingLocation", "ROSSI");
        PrinterConfig selected = service.selectPrinter(context);

        // Assert
        assertEquals("DISPATCH", selected.getId());
        assertEquals("10.19.64.53:9100", selected.getEndpoint());
    }

    @Test
    void selectPrinter_shouldRouteToDefault_whenNoRulesMatch() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", true);
        PrinterConfig dispatch = new PrinterConfig("DISPATCH", "Dispatch_Prod", "10.19.64.53", 9100,
                List.of("PROD", "DISPATCH"), "Dispatch office", true);

        Map<String, PrinterConfig> printers = Map.of(
                "OFFICE", office,
                "DISPATCH", dispatch
        );

        RoutingRule rule = new RoutingRule("staging-rossi-dispatch", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");

        List<RoutingRule> rules = List.of(rule);

        PrinterRoutingService service = new PrinterRoutingService(printers, rules, "OFFICE", "TBG3002");

        // Act
        Map<String, String> context = Map.of("stagingLocation", "UNKNOWN");
        PrinterConfig selected = service.selectPrinter(context);

        // Assert
        assertEquals("OFFICE", selected.getId());
        assertEquals("10.19.64.106:9100", selected.getEndpoint());
    }

    @Test
    void selectPrinter_shouldThrowException_whenSelectedPrinterNotFound() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", true);

        Map<String, PrinterConfig> printers = Map.of("OFFICE", office);

        RoutingRule rule = new RoutingRule("staging-rossi-dispatch", true,
                "stagingLocation", "EQUALS", "ROSSI", "DISPATCH");

        List<RoutingRule> rules = List.of(rule);

        PrinterRoutingService service = new PrinterRoutingService(printers, rules, "OFFICE", "TBG3002");

        // Act & Assert
        Map<String, String> context = Map.of("stagingLocation", "ROSSI");
        assertThrows(IllegalStateException.class, () -> service.selectPrinter(context));
    }

    @Test
    void constructor_shouldThrowException_whenDefaultPrinterNotFound() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", true);

        Map<String, PrinterConfig> printers = Map.of("OFFICE", office);
        List<RoutingRule> rules = List.of();

        // Act & Assert
        assertThrows(IllegalArgumentException.class,
                () -> new PrinterRoutingService(printers, rules, "DISPATCH", "TBG3002"));
    }

    @Test
    void findPrinter_shouldReturnPrinter_whenPrinterExistsAndEnabled() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", true);

        Map<String, PrinterConfig> printers = Map.of("OFFICE", office);
        List<RoutingRule> rules = List.of();

        PrinterRoutingService service = new PrinterRoutingService(printers, rules, "OFFICE", "TBG3002");

        // Act
        var result = service.findPrinter("OFFICE");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("OFFICE", result.get().getId());
    }

    @Test
    void findPrinter_shouldReturnEmpty_whenPrinterNotFound() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", true);

        Map<String, PrinterConfig> printers = Map.of("OFFICE", office);
        List<RoutingRule> rules = List.of();

        PrinterRoutingService service = new PrinterRoutingService(printers, rules, "OFFICE", "TBG3002");

        // Act
        var result = service.findPrinter("DISPATCH");

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void findPrinter_shouldReturnEmpty_whenPrinterDisabled() {
        // Arrange
        PrinterConfig office = new PrinterConfig("OFFICE", "Office_Test", "10.19.64.106", 9100,
                List.of("TEST", "QA"), "Admin office", false);

        Map<String, PrinterConfig> printers = Map.of("OFFICE", office);
        List<RoutingRule> rules = List.of();

        PrinterRoutingService service = new PrinterRoutingService(printers, rules, "OFFICE", "TBG3002");

        // Act
        var result = service.findPrinter("OFFICE");

        // Assert
        assertTrue(result.isEmpty());
    }
}

