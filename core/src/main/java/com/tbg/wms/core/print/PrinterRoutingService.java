/*
 * Copyright © 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.0.0
 */

package com.tbg.wms.core.print;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Service for printer routing based on YAML configuration.
 *
 * Loads printer definitions and routing rules from site-specific YAML files,
 * then evaluates rules against runtime context to select the appropriate printer.
 *
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
     * @param printers available printers by ID
     * @param rules routing rules (evaluated in order)
     * @param defaultPrinterId fallback printer ID if no rules match
     * @param siteCode site code for this configuration
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
     *
     * Expects two files in config/{siteCode}/:
     * - printers.yaml (printer definitions)
     * - printer-routing.yaml (routing rules)
     *
     * @param siteCode site code (e.g., "TBG3002")
     * @param configBaseDir base config directory (typically "./config")
     * @return configured PrinterRoutingService
     * @throws IOException if files cannot be read or parsed
     * @throws IllegalArgumentException if configuration is invalid
     */
    public static PrinterRoutingService load(String siteCode, Path configBaseDir) throws IOException {
        Path siteConfigDir = configBaseDir.resolve(siteCode);
        Path printersFile = siteConfigDir.resolve("printers.yaml");
        Path routingFile = siteConfigDir.resolve("printer-routing.yaml");

        if (!Files.exists(printersFile)) {
            throw new IOException("Printers config not found: " + printersFile);
        }
        if (!Files.exists(routingFile)) {
            throw new IOException("Routing config not found: " + routingFile);
        }

        log.info("Loading printer configuration from {}", siteConfigDir);

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

        // Load printers
        Map<String, Object> printersYaml = mapper.readValue(printersFile.toFile(), Map.class);
        List<Map<String, Object>> printersList = (List<Map<String, Object>>) printersYaml.get("printers");
        Map<String, PrinterConfig> printers = new LinkedHashMap<>();

        for (Map<String, Object> p : printersList) {
            String id = (String) p.get("id");
            String name = (String) p.get("name");
            String ip = (String) p.get("ip");
            int port = p.containsKey("port") ? (Integer) p.get("port") : 9100;
            List<String> tags = (List<String>) p.getOrDefault("tags", Collections.emptyList());
            String locationHint = (String) p.get("locationHint");
            boolean enabled = p.containsKey("enabled") ? (Boolean) p.get("enabled") : true;

            PrinterConfig printer = new PrinterConfig(id, name, ip, port, tags, locationHint, enabled);
            printers.put(id, printer);
            log.debug("Loaded printer: {}", printer);
        }

        // Load routing rules
        Map<String, Object> routingYaml = mapper.readValue(routingFile.toFile(), Map.class);
        String defaultPrinterId = (String) routingYaml.get("defaultPrinterId");
        List<Map<String, Object>> rulesList = (List<Map<String, Object>>) routingYaml.get("rules");
        List<RoutingRule> rules = new ArrayList<>();

        for (Map<String, Object> r : rulesList) {
            String id = (String) r.get("id");
            boolean enabled = r.containsKey("enabled") ? (Boolean) r.get("enabled") : true;
            Map<String, Object> when = (Map<String, Object>) r.get("when");
            Map<String, Object> then = (Map<String, Object>) r.get("then");

            // Parse condition (currently supports "all" with single field condition)
            List<Map<String, Object>> conditions = (List<Map<String, Object>>) when.get("all");
            if (conditions.isEmpty()) {
                log.warn("Skipping rule {} with no conditions", id);
                continue;
            }

            // For now, support single condition per rule
            Map<String, Object> condition = conditions.get(0);
            String field = (String) condition.get("field");
            String operator = (String) condition.get("op");
            String value = (String) condition.get("value");
            String printerId = (String) then.get("printerId");

            RoutingRule rule = new RoutingRule(id, enabled, field, operator, value, printerId);
            rules.add(rule);
            log.debug("Loaded routing rule: {}", rule);
        }

        return new PrinterRoutingService(printers, rules, defaultPrinterId, siteCode);
    }

    /**
     * Selects a printer based on routing context.
     *
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

