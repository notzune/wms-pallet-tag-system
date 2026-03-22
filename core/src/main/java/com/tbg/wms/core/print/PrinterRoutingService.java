/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Service for printer routing based on YAML configuration.
 * <p>
 * Loads printer definitions and routing rules from site-specific YAML files,
 * then evaluates rules against runtime context to select the appropriate printer.
 * <p>
 * Thread-safe once initialized (immutable configuration).
 *
 * @since 1.0.0
 */
public final class PrinterRoutingService {

    private static final Logger log = LoggerFactory.getLogger(PrinterRoutingService.class);

    private final Map<String, PrinterConfig> printers;
    private final List<RoutingRule> rules;
    private final String defaultPrinterId;
    private final String siteCode;

    /**
     * Creates a new printer routing service.
     *
     * @param printers         available printers by ID
     * @param rules            routing rules (evaluated in order)
     * @param defaultPrinterId fallback printer ID if no rules match
     * @param siteCode         site code for this configuration
     */
    public PrinterRoutingService(Map<String, PrinterConfig> printers,
                                 List<RoutingRule> rules,
                                 String defaultPrinterId,
                                 String siteCode) {
        this.printers = Map.copyOf(Objects.requireNonNull(printers, "printers cannot be null"));
        this.rules = List.copyOf(Objects.requireNonNull(rules, "rules cannot be null"));
        this.defaultPrinterId = Objects.requireNonNull(defaultPrinterId, "defaultPrinterId cannot be null");
        this.siteCode = Objects.requireNonNull(siteCode, "siteCode cannot be null");

        if (!printers.containsKey(defaultPrinterId)) {
            throw new IllegalArgumentException("Default printer not found: " + defaultPrinterId);
        }

        log.info("Initialized printer routing for site {} with {} printers and {} rules",
                siteCode, printers.size(), rules.size());
    }

    /**
     * Loads printer routing configuration from YAML files.
     * <p>
     * Expects two files in config/{siteCode}/:
     * - printers.yaml (printer definitions)
     * - printer-routing.yaml (routing rules)
     *
     * @param siteCode      site code (e.g., "TBG3002")
     * @param configBaseDir base config directory (typically "./config")
     * @return configured PrinterRoutingService
     * @throws IOException              if files cannot be read or parsed
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static PrinterRoutingService load(String siteCode, Path configBaseDir) throws IOException {
        PrinterRoutingConfigLoader.LoadedPrinterRoutingConfig loaded =
                new PrinterRoutingConfigLoader().load(siteCode, configBaseDir);
        return new PrinterRoutingService(loaded.printers, loaded.rules, loaded.defaultPrinterId, siteCode);
    }

    /**
     * Selects a printer based on routing context.
     * <p>
     * Evaluates rules in order. First matching rule wins.
     * If no rules match, returns default printer.
     *
     * @param context routing context (e.g., {"stagingLocation": "ROSSI"})
     * @return selected printer configuration
     * @throws IllegalStateException if selected printer not found or disabled
     */
    public PrinterConfig selectPrinter(Map<String, String> context) {
        Objects.requireNonNull(context, "context cannot be null");

        log.debug("Evaluating printer routing with context: {}", context);

        // Evaluate rules in order
        for (RoutingRule rule : rules) {
            if (rule.matches(context)) {
                log.info("Routing rule matched: {} -> printer {}", rule.getId(), rule.getPrinterId());
                return getPrinter(rule.getPrinterId());
            }
        }

        // No rules matched, use default
        log.info("No routing rules matched, using default printer: {}", defaultPrinterId);
        return getPrinter(defaultPrinterId);
    }

    /**
     * Gets printer by ID with validation.
     *
     * @param printerId printer identifier
     * @return printer configuration
     * @throws IllegalStateException if printer not found or disabled
     */
    private PrinterConfig getPrinter(String printerId) {
        PrinterConfig printer = printers.get(printerId);
        if (printer == null) {
            throw new IllegalStateException("Printer not found: " + printerId);
        }
        if (!printer.isEnabled()) {
            throw new IllegalStateException("Printer is disabled: " + printerId);
        }
        return printer;
    }

    /**
     * Gets printer by ID (for manual override).
     *
     * @param printerId printer identifier
     * @return printer configuration, or empty if not found
     */
    public Optional<PrinterConfig> findPrinter(String printerId) {
        PrinterConfig printer = printers.get(printerId);
        if (printer != null && printer.isEnabled()) {
            return Optional.of(printer);
        }
        return Optional.empty();
    }

    public Map<String, PrinterConfig> getPrinters() {
        return printers;
    }

    public List<RoutingRule> getRules() {
        return rules;
    }

    public String getDefaultPrinterId() {
        return defaultPrinterId;
    }

    public String getSiteCode() {
        return siteCode;
    }
}

