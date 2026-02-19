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

        PrintersYaml printersYaml = mapper.readValue(printersFile.toFile(), PrintersYaml.class);
        validateOptionalSiteCode(siteCode, printersYaml.siteCode, printersFile);
        List<PrinterEntry> printersList = printersYaml.printers == null ? List.of() : printersYaml.printers;
        Map<String, PrinterConfig> printers = new LinkedHashMap<>();

        for (PrinterEntry printerEntry : printersList) {
            String id = requireYamlValue(printerEntry.id, "printers[].id", printersFile);
            String name = requireYamlValue(printerEntry.name, "printers[].name", printersFile);
            String ip = requireYamlValue(printerEntry.ip, "printers[].ip", printersFile);
            // Match historical behavior: YAML omits these often, so keep deterministic defaults.
            int port = printerEntry.port == null ? 9100 : printerEntry.port;
            List<String> tags = printerEntry.tags == null ? Collections.emptyList() : printerEntry.tags;
            String locationHint = printerEntry.locationHint;
            boolean enabled = printerEntry.enabled == null || printerEntry.enabled;

            PrinterConfig printer = new PrinterConfig(id, name, ip, port, tags, locationHint, enabled);
            printers.put(id, printer);
            log.debug("Loaded printer: {}", printer);
        }

        RoutingYaml routingYaml = mapper.readValue(routingFile.toFile(), RoutingYaml.class);
        validateOptionalSiteCode(siteCode, routingYaml.siteCode, routingFile);
        String defaultPrinterId = requireYamlValue(routingYaml.defaultPrinterId, "defaultPrinterId", routingFile);
        List<RuleEntry> rulesList = routingYaml.rules == null ? List.of() : routingYaml.rules;
        List<RoutingRule> rules = new ArrayList<>();

        for (RuleEntry ruleEntry : rulesList) {
            String id = requireYamlValue(ruleEntry.id, "rules[].id", routingFile);
            boolean enabled = ruleEntry.enabled == null || ruleEntry.enabled;

            List<RuleConditionEntry> conditions = ruleEntry.when == null || ruleEntry.when.all == null
                    ? List.of()
                    : ruleEntry.when.all;
            if (conditions.isEmpty()) {
                log.warn("Skipping rule {} with no conditions", id);
                continue;
            }

            // For now, preserve one-condition semantics used by existing routing configs.
            RuleConditionEntry condition = conditions.get(0);
            String field = requireYamlValue(condition.field, "rules[].when.all[].field", routingFile);
            String operator = requireYamlValue(condition.op, "rules[].when.all[].op", routingFile);
            String value = requireYamlValue(condition.value, "rules[].when.all[].value", routingFile);
            String printerId = requireYamlValue(
                    ruleEntry.then == null ? null : ruleEntry.then.printerId,
                    "rules[].then.printerId",
                    routingFile
            );

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

    private static String requireYamlValue(String value, String field, Path sourceFile) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field '" + field + "' in " + sourceFile);
        }
        return value;
    }

    private static void validateOptionalSiteCode(String expectedSiteCode, String configuredSiteCode, Path sourceFile) {
        if (configuredSiteCode == null || configuredSiteCode.isBlank()) {
            return;
        }

        if (!expectedSiteCode.equalsIgnoreCase(configuredSiteCode)) {
            throw new IllegalArgumentException(
                    "Site code mismatch in " + sourceFile
                            + ": expected '" + expectedSiteCode + "' but found '" + configuredSiteCode + "'"
            );
        }
    }

    private static final class PrintersYaml {
        public Integer version;
        public String siteCode;
        public List<PrinterEntry> printers;
    }

    private static final class PrinterEntry {
        public String id;
        public String name;
        public String ip;
        public Integer port;
        public List<String> tags;
        public String locationHint;
        public Boolean enabled;
    }

    private static final class RoutingYaml {
        public Integer version;
        public String siteCode;
        public String defaultPrinterId;
        public List<RuleEntry> rules;
    }

    private static final class RuleEntry {
        public String id;
        public Boolean enabled;
        public RuleWhen when;
        public RuleThen then;
    }

    private static final class RuleWhen {
        public List<RuleConditionEntry> all;
    }

    private static final class RuleConditionEntry {
        public String field;
        public String op;
        public String value;
    }

    private static final class RuleThen {
        public String printerId;
    }
}

