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

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show this help message and exit")
    private boolean helpRequested;

    @Option(names = {"--train"}, required = true, description = "Train ID (full or 4-digit rail train number)")
    private String trainId;

    @Option(names = {"--output-dir"}, defaultValue = "out/rail-print", description = "Output folder")
    private Path outputDir;

    @Option(names = {"--print"}, defaultValue = "false", description = "Send rendered PDF to the default printer")
    private boolean print;

    @Option(names = {"--yes", "-y"}, defaultValue = "false", description = "Skip interactive confirmation")
    private boolean yes;

    /**
     * Executes live WMS rail planning and card rendering for one train.
     *
     * @return exit code (0 on success)
     */
    @Override
    public Integer call() throws Exception {
        AppConfig config = new AppConfig();
        RailWorkflowService.RailWorkflowResult result;

        try (DbConnectionPool pool = new DbConnectionPool(config)) {
            RailDbRepository repository = new WmsRailDbRepository(new OracleDbQueryRepository(pool.getDataSource()));
            RailWorkflowService workflowService = new RailWorkflowService(repository);
            result = workflowService.prepare(trainId);
        }

        printPreview(result);

        if (!yes && !confirm("Continue and generate rail cards PDF? [y/N]: ")) {
            System.out.println("Cancelled.");
            return 0;
        }

        Files.createDirectories(outputDir);
        String fileName = "rail-cards-" + result.getTrainId() + "-" + TS.format(LocalDateTime.now()) + ".pdf";
        Path pdfPath = outputDir.resolve(fileName);

        RailCardRenderer renderer = new RailCardRenderer();
        renderer.renderPdf(result.getCards(), pdfPath);
        System.out.println("PDF generated: " + pdfPath.toAbsolutePath());

        if (print) {
            if (!yes && !confirm("Send PDF to default printer now? [y/N]: ")) {
                System.out.println("Print skipped.");
                return 0;
            }
            new RailPrintService().print(pdfPath);
            System.out.println("Print command sent.");
        }
        return 0;
    }

    private void printPreview(RailWorkflowService.RailWorkflowResult result) {
        List<RailCarCard> cards = result.getCards();
        System.out.println();
        System.out.println("SEQ   VEHICLE      CAN   DOM");
        for (RailCarCard card : cards) {
            System.out.printf("%-5s %-12s %4d %4d%n",
                    card.getSequence(),
                    card.getVehicleId(),
                    card.getCanPallets(),
                    card.getDomPallets());
        }
        System.out.println();
        System.out.println("Railcars: " + cards.size());
        System.out.println("WMS rows: " + result.getRawRows().size());
        System.out.println("Resolved footprints: " + result.getResolvedFootprints().size());
        System.out.println("Unresolved short codes: " + result.getUnresolvedShortCodes().size());
        if (!result.getMissingItemsInCards().isEmpty()) {
            System.out.println("Missing in card math: " + String.join(", ", result.getMissingItemsInCards()));
        }
        System.out.println();
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
}
