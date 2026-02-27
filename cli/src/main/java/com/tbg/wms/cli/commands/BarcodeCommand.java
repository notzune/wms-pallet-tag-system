/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.1.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.barcode.BarcodeZplBuilder;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.BarcodeRequest;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Orientation;
import com.tbg.wms.core.barcode.BarcodeZplBuilder.Symbology;
import com.tbg.wms.core.print.NetworkPrintService;
import com.tbg.wms.core.print.PrinterConfig;
import com.tbg.wms.core.print.PrinterRoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.regex.Pattern;

/**
 * Generates a standalone barcode label and optionally prints it.
 */
@Command(
        name = "barcode",
        description = "Generate a standalone barcode label (ZPL) and optionally print it"
)
public final class BarcodeCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(BarcodeCommand.class);
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final Pattern NON_ALNUM_PATTERN = Pattern.compile("[^a-z0-9]+");

    @Option(
            names = {"-d", "--data"},
            required = true,
            description = "Barcode payload (raw data)."
    )
    private String data;

    @Option(
            names = {"-t", "--type"},
            defaultValue = "CODE128",
            description = "Barcode symbology: CODE128 or GS1_128."
    )
    private Symbology symbology;

    @Option(
            names = {"-o", "--orientation"},
            defaultValue = "PORTRAIT",
            description = "Field orientation: PORTRAIT or LANDSCAPE."
    )
    private Orientation orientation;

    @Option(
            names = {"--label-width-dots"},
            defaultValue = "812",
            description = "Label width in dots (default 812 for 4x6 at 203 DPI)."
    )
    private int labelWidthDots;

    @Option(
            names = {"--label-height-dots"},
            defaultValue = "1218",
            description = "Label height in dots (default 1218 for 4x6 at 203 DPI)."
    )
    private int labelHeightDots;

    @Option(
            names = {"--origin-x"},
            defaultValue = "40",
            description = "X origin in dots."
    )
    private int originX;

    @Option(
            names = {"--origin-y"},
            defaultValue = "40",
            description = "Y origin in dots."
    )
    private int originY;

    @Option(
            names = {"--module-width"},
            defaultValue = "2",
            description = "Barcode module width."
    )
    private int moduleWidth;

    @Option(
            names = {"--module-ratio"},
            defaultValue = "3",
            description = "Wide-to-narrow bar ratio."
    )
    private int moduleRatio;

    @Option(
            names = {"--barcode-height"},
            defaultValue = "120",
            description = "Barcode height in dots."
    )
    private int barcodeHeight;

    @Option(
            names = {"--human-readable"},
            defaultValue = "true",
            description = "Include human readable text under barcode."
    )
    private boolean humanReadable;

    @Option(
            names = {"--copies"},
            defaultValue = "1",
            description = "Number of copies to print."
    )
    private int copies;

    @Option(
            names = {"--dry-run"},
            defaultValue = "false",
            description = "Generate ZPL only; do not print."
    )
    private boolean dryRun;

    @Option(
            names = {"-p", "--printer"},
            defaultValue = "",
            description = "Printer ID from routing config (required unless --dry-run)."
    )
    private String printerId;

    @Option(
            names = {"--output-dir"},
            defaultValue = "./barcodes",
            description = "Output directory for ZPL files."
    )
    private String outputDir;

    @Option(
            names = {"--print-to-file", "--ptf"},
            defaultValue = "false",
            description = "Write ZPL to /out next to the JAR and skip printing."
    )
    private boolean printToFile;

    @Override
    public Integer call() {
        String jobId = UUID.randomUUID().toString().substring(0, 8);
        log.info("Generating barcode label (jobId={})", jobId);

        BarcodeRequest request = buildBarcodeRequest();
        String zpl = BarcodeZplBuilder.build(request);

        if (printToFile) {
            dryRun = true;
            outputDir = resolveJarOutputDir().toString();
        }

        Path zplFile = writeZplFile(zpl);
        if (zplFile == null) {
            return 2;
        }

        log.info("ZPL file written: {}", zplFile.toAbsolutePath());

        if (dryRun) {
            System.out.println("Dry run enabled. ZPL generated at: " + zplFile.toAbsolutePath());
            return 0;
        }

        if (printerId == null || printerId.isBlank()) {
            System.err.println("Error: --printer is required unless --dry-run is specified.");
            return 2;
        }

        PrinterConfig printer = resolvePrinter();
        if (printer == null) {
            return 2;
        }

        NetworkPrintService printService = new NetworkPrintService();
        try {
            printService.print(printer, zpl, "barcode-" + jobId);
        } catch (Exception e) {
            log.error("Barcode print failed", e);
            System.err.println("Error: Failed to print barcode: " + e.getMessage());
            return 6;
        }

        System.out.println("Printed barcode label to printer " + printer.getId() + " (" + printer.getEndpoint() + ")");
        return 0;
    }

    private BarcodeRequest buildBarcodeRequest() {
        return new BarcodeRequest(
                data,
                symbology,
                orientation,
                labelWidthDots,
                labelHeightDots,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                humanReadable,
                copies
        );
    }

    private Path writeZplFile(String zpl) {
        Path outputPath = Paths.get(outputDir);
        try {
            Files.createDirectories(outputPath);
        } catch (Exception e) {
            log.error("Failed to create output directory: {}", outputPath, e);
            System.err.println("Error: Unable to create output directory: " + outputPath);
            return null;
        }

        String fileName = String.format("barcode-%s-%s.zpl", TS.format(LocalDateTime.now()), safeSlug(data));
        Path zplFile = outputPath.resolve(fileName);
        try {
            Files.writeString(zplFile, zpl);
            return zplFile;
        } catch (Exception e) {
            log.error("Failed to write ZPL file: {}", zplFile, e);
            System.err.println("Error: Unable to write ZPL file: " + zplFile);
            return null;
        }
    }

    private PrinterConfig resolvePrinter() {
        AppConfig config = RootCommand.config();
        String site = config.activeSiteCode();

        PrinterRoutingService routing;
        try {
            routing = PrinterRoutingService.load(site, Paths.get("config"));
        } catch (Exception e) {
            log.error("Failed to load printer routing configuration", e);
            System.err.println("Error: Unable to load printer routing configuration.");
            return null;
        }

        PrinterConfig printer = routing.findPrinter(printerId.trim()).orElse(null);
        if (printer == null || !printer.isEnabled()) {
            System.err.println("Error: Printer not found or disabled: " + printerId);
            return null;
        }
        return printer;
    }

    private static Path resolveJarOutputDir() {
        try {
            Path codeSource = Paths.get(Objects.requireNonNull(BarcodeCommand.class
                    .getProtectionDomain()
                    .getCodeSource())
                    .getLocation()
                    .toURI());
            Path baseDir = Files.isDirectory(codeSource) ? codeSource : codeSource.getParent();
            return baseDir.resolve("out");
        } catch (Exception e) {
            return Paths.get("out");
        }
    }

    private static String safeSlug(String value) {
        if (value == null) {
            return "data";
        }
        String slug = NON_ALNUM_PATTERN.matcher(value.trim().toLowerCase(Locale.ROOT)).replaceAll("-");
        if (slug.isEmpty()) {
            return "data";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }
}
