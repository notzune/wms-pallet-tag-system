/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.AppConfig;
import com.tbg.wms.core.rail.*;
import com.tbg.wms.db.DbConnectionPool;
import com.tbg.wms.db.OracleDbQueryRepository;
import com.tbg.wms.db.WmsRailDbRepository;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * End-to-end WMS rail label workflow with preview and PDF rendering.
 */
@Command(
        name = "rail-print",
        mixinStandardHelpOptions = true,
        description = "Query WMS rail rows, preview CAN/DOM pallets, render rail cards, and optionally print"
)
public final class RailPrintCommand implements Callable<Integer> {
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final BufferedReader CONSOLE_IN = new BufferedReader(new InputStreamReader(System.in));
    private final SystemDefaultPrinterValidator systemDefaultPrinterValidator;
    private final RailPrintCliSupport cliSupport;

    public RailPrintCommand() {
        this(() -> new RailPrintService().validateSystemDefaultPrinter(), new RailPrintCliSupport());
    }

    RailPrintCommand(SystemDefaultPrinterValidator systemDefaultPrinterValidator) {
        this(systemDefaultPrinterValidator, new RailPrintCliSupport());
    }

    RailPrintCommand(SystemDefaultPrinterValidator systemDefaultPrinterValidator, RailPrintCliSupport cliSupport) {
        this.systemDefaultPrinterValidator = systemDefaultPrinterValidator;
        this.cliSupport = cliSupport;
    }

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    private boolean helpRequested;

    @Option(names = {"--train"}, description = "Full train ID (example: JC08312025)")
    private String trainId;

    @Option(names = {"--output-dir"}, defaultValue = "out/rail-print", description = "Output folder")
    private Path outputDir;

    @Option(names = {"--print"}, defaultValue = "false", description = "Send rendered PDF to the default printer")
    private boolean print;

    @Option(names = {"--validate-system-default-print"}, defaultValue = "false",
            description = "Validate system-default rail printer availability without sending a print job")
    private boolean validateSystemDefaultPrint;

    @Option(names = {"--template"}, defaultValue = "false",
            description = "Generate a 10-position rail label alignment template PDF and exit")
    private boolean template;

    @Option(names = {"--yes", "-y"}, defaultValue = "false", description = "Skip interactive confirmation")
    private boolean yes;

    /**
     * Executes live WMS rail planning and card rendering for one train.
     *
     * @return exit code (0 on success)
     */
    @Override
    public Integer call() throws Exception {
        String optionError = cliSupport.validateOptions(validateSystemDefaultPrint, template, trainId, print);
        if (optionError != null) {
            System.err.println(optionError);
            return 2;
        }

        if (validateSystemDefaultPrint) {
            System.out.println(systemDefaultPrinterValidator.validate());
            return 0;
        }

        AppConfig config = new AppConfig();

        if (template) {
            Files.createDirectories(outputDir);
            Path templatePdf = outputDir.resolve("rail-label-alignment-template-" + TS.format(LocalDateTime.now()) + ".pdf");
            cliSupport.railRenderer(config).renderAlignmentTemplate(templatePdf);
            System.out.println("Alignment template generated: " + templatePdf.toAbsolutePath());
            return 0;
        }

        RailWorkflowService.RailWorkflowResult result;

        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            RailDbRepository repository = new WmsRailDbRepository(new OracleDbQueryRepository(pool.getDataSource()));
            RailWorkflowService workflowService = new RailWorkflowService(repository);
            result = workflowService.prepare(trainId);
        }

        System.out.print(cliSupport.buildPreviewText(result));

        if (!yes && !confirm("Continue and generate rail cards PDF? [y/N]: ")) {
            System.out.println("Cancelled.");
            return 0;
        }

        Files.createDirectories(outputDir);
        String fileName = "rail-cards-" + result.getTrainId() + "-" + TS.format(LocalDateTime.now()) + ".pdf";
        Path pdfPath = outputDir.resolve(fileName);

        RailCardRenderer renderer = cliSupport.railRenderer(config);
        renderer.renderPdf(result.getCards(), pdfPath);
        System.out.println("PDF generated: " + pdfPath.toAbsolutePath());

        if (print) {
            if (!yes && !confirm("Send PDF to default printer now? [y/N]: ")) {
                System.out.println("Print skipped.");
                return 0;
            }
            new RailPrintService().print(pdfPath, config);
            System.out.println("Print command sent.");
        }
        return 0;
    }

    private boolean confirm(String prompt) {
        System.out.print(prompt);
        try {
            String value = CONSOLE_IN.readLine();
            if (value == null) {
                return false;
            }
            String normalized = value.trim();
            return normalized.equalsIgnoreCase("y") || normalized.equalsIgnoreCase("yes");
        } catch (Exception ex) {
            return false;
        }
    }

    @FunctionalInterface
    interface SystemDefaultPrinterValidator {
        String validate() throws Exception;
    }
}
