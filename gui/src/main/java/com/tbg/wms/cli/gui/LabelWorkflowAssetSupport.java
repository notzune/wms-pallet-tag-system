/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.label.SiteConfig;
import com.tbg.wms.core.labeling.LabelingSupport;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import com.tbg.wms.core.sku.SkuMappingService;
import com.tbg.wms.core.template.LabelTemplate;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Caches and loads reusable workflow assets such as routing, site config, template, and SKU mapping.
 */
final class LabelWorkflowAssetSupport {

    private final AppConfig config;
    private final Path configBaseDir;
    private final ConcurrentMap<String, PrinterRoutingService> routingBySite = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, ConcurrentMap<String, PrinterConfig>> printersBySite = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, SiteConfig> siteConfigBySite = new ConcurrentHashMap<>();
    private volatile SkuMappingService cachedSkuMapping;
    private volatile LabelTemplate cachedTemplate;

    LabelWorkflowAssetSupport(AppConfig config, Path configBaseDir) {
        this.config = config;
        this.configBaseDir = configBaseDir;
    }

    void clearCaches() {
        routingBySite.clear();
        printersBySite.clear();
        siteConfigBySite.clear();
        cachedSkuMapping = null;
        cachedTemplate = null;
    }

    PrinterRoutingService loadRouting(String siteCode) throws Exception {
        PrinterRoutingService cached = routingBySite.get(siteCode);
        if (cached != null) {
            return cached;
        }
        PrinterRoutingService routing = PrinterRoutingService.load(siteCode, configBaseDir);
        PrinterRoutingService prior = routingBySite.putIfAbsent(siteCode, routing);
        return prior == null ? routing : prior;
    }

    PrinterConfig resolvePrinter(String siteCode, String printerId) throws Exception {
        if (printerId == null || printerId.isBlank()) {
            return null;
        }
        String normalizedPrinterId = printerId.trim();
        ConcurrentMap<String, PrinterConfig> sitePrinters = printersBySite
                .computeIfAbsent(siteCode, ignored -> new ConcurrentHashMap<>());
        PrinterConfig cached = sitePrinters.get(normalizedPrinterId);
        if (cached != null) {
            return cached;
        }
        PrinterConfig printer = loadRouting(siteCode).findPrinter(normalizedPrinterId).orElse(null);
        if (printer != null) {
            sitePrinters.putIfAbsent(normalizedPrinterId, printer);
        }
        return printer;
    }

    SiteConfig loadSiteConfig(String siteCode) {
        return siteConfigBySite.computeIfAbsent(siteCode, this::createSiteConfig);
    }

    SkuMappingService loadSkuMapping() throws Exception {
        SkuMappingService cached = cachedSkuMapping;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedSkuMapping == null) {
                Path csvPath = LabelingSupport.resolveSkuMatrixCsv();
                if (csvPath == null) {
                    throw new IllegalStateException("SKU mapping CSV not found.");
                }
                cachedSkuMapping = new SkuMappingService(csvPath);
            }
            return cachedSkuMapping;
        }
    }

    LabelTemplate loadTemplate() throws Exception {
        LabelTemplate cached = cachedTemplate;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (cachedTemplate == null) {
                Path templatePath = configBaseDir.resolve("templates").resolve("walmart-canada-label.zpl");
                if (!Files.exists(templatePath)) {
                    throw new IllegalStateException("ZPL template not found: " + templatePath);
                }
                cachedTemplate = new LabelTemplate("WALMART_CANADA", Files.readString(templatePath));
            }
            return cachedTemplate;
        }
    }

    private SiteConfig createSiteConfig(String siteCode) {
        return new SiteConfig(
                config.siteShipFromName(siteCode),
                config.siteShipFromAddress(siteCode),
                config.siteShipFromCityStateZip(siteCode)
        );
    }
}
