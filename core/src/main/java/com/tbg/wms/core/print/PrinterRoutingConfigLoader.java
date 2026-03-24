/*
 * Copyright (c) 2026 Zeyad Rashed
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.core.print;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads printer and routing configuration from site YAML files.
 */
final class PrinterRoutingConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(PrinterRoutingConfigLoader.class);
    private static final int DEFAULT_PRINTER_PORT = 9100;

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    LoadedPrinterRoutingConfig load(String siteCode, Path configBaseDir) throws IOException {
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

        PrintersYaml printersYaml = mapper.readValue(printersFile.toFile(), PrintersYaml.class);
        validateOptionalSiteCode(siteCode, printersYaml.siteCode, printersFile);
        Map<String, PrinterConfig> printers = loadPrinters(printersYaml, printersFile);

        RoutingYaml routingYaml = mapper.readValue(routingFile.toFile(), RoutingYaml.class);
        validateOptionalSiteCode(siteCode, routingYaml.siteCode, routingFile);
        String defaultPrinterId = requireYamlValue(routingYaml.defaultPrinterId, "defaultPrinterId", routingFile);
        List<RoutingRule> rules = loadRules(routingYaml, routingFile);

        return new LoadedPrinterRoutingConfig(printers, rules, defaultPrinterId);
    }

    private Map<String, PrinterConfig> loadPrinters(PrintersYaml printersYaml, Path printersFile) {
        List<PrinterEntry> printersList = printersYaml.printers == null ? List.of() : printersYaml.printers;
        Map<String, PrinterConfig> printers = new LinkedHashMap<>();
        for (PrinterEntry printerEntry : printersList) {
            String id = requireYamlValue(printerEntry.id, "printers[].id", printersFile);
            String name = requireYamlValue(printerEntry.name, "printers[].name", printersFile);
            String ip = requireYamlValue(printerEntry.ip, "printers[].ip", printersFile);
            int port = printerEntry.port == null ? DEFAULT_PRINTER_PORT : printerEntry.port;
            List<String> tags = printerEntry.tags == null ? Collections.emptyList() : printerEntry.tags;
            List<String> capabilities = printerEntry.capabilities == null ? Collections.emptyList() : printerEntry.capabilities;
            String locationHint = printerEntry.locationHint;
            boolean enabled = printerEntry.enabled == null || printerEntry.enabled;

            PrinterConfig printer = new PrinterConfig(id, name, ip, port, tags, capabilities, locationHint, enabled);
            printers.put(id, printer);
            log.debug("Loaded printer: {}", printer);
        }
        return printers;
    }

    private List<RoutingRule> loadRules(RoutingYaml routingYaml, Path routingFile) {
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

            RuleConditionEntry condition = selectSingleDefinedCondition(conditions, id, routingFile);
            if (condition == null) {
                log.warn("Skipping rule {} with only empty conditions", id);
                continue;
            }

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
        return rules;
    }

    private static String requireYamlValue(String value, String field, Path sourceFile) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required field '" + field + "' in " + sourceFile);
        }
        return value;
    }

    private static RuleConditionEntry selectSingleDefinedCondition(
            List<RuleConditionEntry> conditions,
            String ruleId,
            Path sourceFile
    ) {
        RuleConditionEntry selected = null;
        for (RuleConditionEntry condition : conditions) {
            if (condition != null) {
                if (selected != null) {
                    throw new IllegalArgumentException(
                            "Routing rule '" + ruleId + "' in " + sourceFile + " defines multiple conditions; only one is supported."
                    );
                }
                selected = condition;
            }
        }
        return selected;
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

    static final class LoadedPrinterRoutingConfig {
        final Map<String, PrinterConfig> printers;
        final List<RoutingRule> rules;
        final String defaultPrinterId;

        private LoadedPrinterRoutingConfig(
                Map<String, PrinterConfig> printers,
                List<RoutingRule> rules,
                String defaultPrinterId
        ) {
            this.printers = Map.copyOf(printers);
            this.rules = List.copyOf(rules);
            this.defaultPrinterId = defaultPrinterId;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class PrintersYaml {
        public Integer version;
        public String siteCode;
        public List<PrinterEntry> printers;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class PrinterEntry {
        public String id;
        public String name;
        public String ip;
        public Integer port;
        public List<String> tags;
        public List<String> capabilities;
        public String locationHint;
        public Boolean enabled;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RoutingYaml {
        public Integer version;
        public String siteCode;
        public String defaultPrinterId;
        public List<RuleEntry> rules;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RuleEntry {
        public String id;
        public Boolean enabled;
        public RuleWhen when;
        public RuleThen then;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RuleWhen {
        public List<RuleConditionEntry> all;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RuleConditionEntry {
        public String field;
        public String op;
        public String value;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class RuleThen {
        public String printerId;
    }
}
