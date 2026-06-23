/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.1.0
 */

package com.tbg.wms.cli.commands;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.RuntimePathResolver;
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
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.Callable;

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
    private static final int JOB_ID_LENGTH = 8;
    private final BarcodeCommandSupport commandSupport = new BarcodeCommandSupport();

    @Option(
            names = {"-d", "--data"},
            required = false,
            description = "Barcode payload (raw data)."
    )
    private String data;

    @Option(
            names = {"-preset", "--preset"},
            required = false,
            description = "Quick terminal preset (${COMPLETION-CANDIDATES}). "
                    + "BREAK_SHEET prints START and STOP stacked on one label. Overrides --data."
    )
    private TerminalPreset preset;

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
            defaultValue = "60",
            description = "X origin in dots (recommended: 60 for scanner quiet-zone margin)."
    )
    private int originX;

    @Option(
            names = {"--origin-y"},
            defaultValue = "60",
            description = "Y origin in dots (recommended: 60 for scanner quiet-zone margin)."
    )
    private int originY;

    @Option(
            names = {"--module-width"},
            defaultValue = "3",
            description = "Barcode module width (recommended: 3 for long-range scanner reliability)."
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
            defaultValue = "220",
            description = "Barcode height in dots (recommended: 220 for long-range scanner reliability)."
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

    /**
     * Executes barcode generation and optional print/file output flow.
     *
     * @return exit code (0 success, non-zero failure)
     */
    @Override
    public Integer call() {
        if (preset != null && preset.combinedSheet) {
            return callCombinedBreakSheet();
        }

        String barcodeData = resolveBarcodeData();
        if (barcodeData == null) {
            System.err.println("Error: --data is required unless --preset is specified.");
            return 2;
        }

        String validationError = commandSupport.validateOptions(
                barcodeData,
                labelWidthDots,
                labelHeightDots,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                copies
        );
        if (validationError != null) {
            System.err.println(validationError);
            return 2;
        }

        String jobId = UUID.randomUUID().toString().substring(0, JOB_ID_LENGTH);
        log.info("Generating barcode label (jobId={})", jobId);

        BarcodeRequest request = buildBarcodeRequest(barcodeData);
        String zpl = BarcodeZplBuilder.build(request);

        boolean effectiveDryRun = dryRun || printToFile;
        String effectiveOutputDir = printToFile
                ? RuntimePathResolver.resolveJarSiblingDir(BarcodeCommand.class, "out").toString()
                : outputDir;
        Path zplFile = commandSupport.writeZplFile(zpl, effectiveOutputDir, artifactSlug(), TS, log);
        if (zplFile == null) {
            return 2;
        }

        log.info("ZPL file written: {}", zplFile.toAbsolutePath());

        if (effectiveDryRun) {
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

    private Integer callCombinedBreakSheet() {
        String validationError = commandSupport.validateOptions(
                preset.fileSlug.toUpperCase(),
                labelWidthDots,
                labelHeightDots,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                copies
        );
        if (validationError != null) {
            System.err.println(validationError);
            return 2;
        }

        String jobId = UUID.randomUUID().toString().substring(0, JOB_ID_LENGTH);
        log.info("Generating barcode label (jobId={})", jobId);

        String zpl = BarcodeZplBuilder.buildDual(
                buildTerminalRequest("BREAK START", TerminalPreset.BREAK_START),
                buildTerminalRequest("BREAK STOP", TerminalPreset.BREAK_STOP)
        );

        boolean effectiveDryRun = dryRun || printToFile;
        String effectiveOutputDir = printToFile
                ? RuntimePathResolver.resolveJarSiblingDir(BarcodeCommand.class, "out").toString()
                : outputDir;
        Path zplFile = commandSupport.writeZplFile(zpl, effectiveOutputDir, artifactSlug(), TS, log);
        if (zplFile == null) {
            return 2;
        }

        log.info("ZPL file written: {}", zplFile.toAbsolutePath());

        if (effectiveDryRun) {
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

    private String resolveBarcodeData() {
        if (data != null && !data.isBlank()) {
            return data.trim();
        }
        if (preset == null) {
            return null;
        }
        return preset.rawData;
    }

    private BarcodeRequest buildBarcodeRequest(String barcodeData) {
        String caption = preset == null ? null : preset.caption;
        // Terminal presets carry a short alphanumeric trigger (e.g. BRKSTART);
        // encode it as a plain Code 128 so it scans reliably.
        Symbology effectiveSymbology = preset != null ? Symbology.CODE128 : symbology;
        return new BarcodeRequest(
                barcodeData,
                effectiveSymbology,
                orientation,
                labelWidthDots,
                labelHeightDots,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                preset == null && humanReadable,
                copies,
                caption,
                false
        );
    }

    private BarcodeRequest buildTerminalRequest(String caption, TerminalPreset terminalPreset) {
        return new BarcodeRequest(
                terminalPreset.rawData,
                Symbology.CODE128,
                Orientation.PORTRAIT,
                labelWidthDots,
                labelHeightDots,
                originX,
                originY,
                moduleWidth,
                moduleRatio,
                barcodeHeight,
                false,
                1,
                caption,
                false
        );
    }

    private String artifactSlug() {
        if (preset != null) {
            return preset.fileSlug;
        }
        return data;
    }

    private PrinterConfig resolvePrinter() {
        AppConfig config = RootCommand.config();
        String site = config.activeSiteCode();

        PrinterRoutingService routing;
        try {
            routing = PrinterRoutingService.load(
                    site,
                    RuntimePathResolver.resolveWorkingDirOrJarSiblingDir(BarcodeCommand.class, "config")
            );
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

    /**
     * Predefined operator barcode payloads for quick terminal workflows.
     *
     * <p>Each label carries only a short, distinctive <em>trigger</em> string (for
     * example {@code BRKSTART}). The actual key sequence is not in the barcode:
     * Honeywell Velocity does not interpret key-command tokens embedded directly in
     * scanned data (it types them literally), but it does honor them inside a
     * <em>scan handler</em> macro. So the device is configured with a scan handler
     * that matches each trigger and plays the stored key macro:
     * <pre>
     * BRKSTART -&gt; {F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}START{return}
     * BRKSTOP  -&gt; {F7}{pause:500}0{pause:500}3{pause:500}BREAK{tab}STOP{return}
     * </pre>
     * The macro maps the manual sequence (F7 to Tools menu, {@code 0} next page,
     * {@code 3} Activity Login, type {@code BREAK}, Tab, type {@code START}/{@code STOP},
     * submit). VT-220 key codes: {@code {F7}}={@code E041}, {@code {tab}}={@code 0009},
     * {@code {return}}={@code 000D} (the host profile is VT-220, so the submit key is
     * {@code {return}}, not the 3270-only {@code {enter}}).
     */
    enum TerminalPreset {
        BREAK_START("BRKSTART", "BREAK START", "break-start"),
        BREAK_STOP("BRKSTOP", "BREAK STOP", "break-stop"),
        BREAK_SHEET("BREAK SHEET", "BREAK SHEET", "break-sheet", true);

        private final String rawData;
        private final String caption;
        private final String fileSlug;
        private final boolean combinedSheet;

        TerminalPreset(String rawData, String caption, String fileSlug) {
            this(rawData, caption, fileSlug, false);
        }

        TerminalPreset(String rawData, String caption, String fileSlug, boolean combinedSheet) {
            this.rawData = rawData;
            this.caption = caption;
            this.fileSlug = fileSlug;
            this.combinedSheet = combinedSheet;
        }
    }
}
