package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.barcode.BarcodeLabel;
import com.tbg.wms.v2.app.barcode.GenerateBarcodeLabel;
import com.tbg.wms.v2.domain.barcode.BarcodePreset;
import com.tbg.wms.v2.domain.barcode.BarcodeRequest;
import com.tbg.wms.v2.domain.barcode.BarcodeSymbology;
import com.tbg.wms.v2.printing.zpl.BarcodeZplRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.Callable;

@Command(name = "barcode", description = "Generate a standalone barcode label")
final class BarcodeCommand implements Callable<Integer> {
    @ParentCommand
    private WmsCli root;

    @Spec
    private CommandSpec spec;

    @Option(names = "--data")
    private String data;

    @Option(names = "--preset")
    private String preset;

    @Option(names = "--dry-run")
    private boolean dryRun;

    @Option(names = "--output-dir", defaultValue = "barcodes")
    private Path outputDir;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        if (isBlank(data) && isBlank(preset)) {
            err.println("Error: --data is required unless --preset is specified.");
            return 2;
        }
        if (!dryRun) {
            err.println("Error: live barcode printing is not wired in the 2.0 CLI track; use --dry-run.");
            return 2;
        }
        try {
            BarcodeLabel label = generateLabel();
            Files.createDirectories(outputDir);
            Path output = outputDir.resolve(label.artifactName()).toAbsolutePath().normalize();
            Files.writeString(output, label.zpl());
            out.println("Wrote barcode label: " + output);
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println("Error: " + ex.getMessage());
            return 2;
        } catch (IOException ex) {
            err.println("Error: Failed to write barcode label: " + ex.getMessage());
            return 5;
        }
    }

    private BarcodeLabel generateLabel() {
        GenerateBarcodeLabel generator = new GenerateBarcodeLabel(new BarcodeZplRenderer());
        if (!isBlank(preset)) {
            return generator.generate(BarcodePreset.valueOf(preset.trim().toUpperCase(Locale.ROOT)));
        }
        return generator.generate(new BarcodeRequest(
                data,
                BarcodeSymbology.CODE128,
                BarcodeRequest.Orientation.PORTRAIT,
                812,
                1218,
                40,
                40,
                2,
                3,
                120,
                false,
                1,
                null,
                false
        ));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
