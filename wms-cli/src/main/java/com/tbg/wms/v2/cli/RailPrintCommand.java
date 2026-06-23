package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.rail.PlanRailCards;
import com.tbg.wms.v2.app.rail.RailPdfRequest;
import com.tbg.wms.v2.app.rail.RailPlan;
import com.tbg.wms.v2.app.rail.RenderRailPdf;
import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;
import com.tbg.wms.v2.printing.pdf.RailPdfRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(name = "rail-print", description = "Generate rail label PDFs")
final class RailPrintCommand implements Callable<Integer> {
    @ParentCommand
    private WmsCli root;

    @Spec
    private CommandSpec spec;

    @Option(names = "--template")
    private boolean template;

    @Option(names = "--train")
    private String train;

    @Option(names = "--yes")
    private boolean yes;

    @Option(names = "--output-dir", defaultValue = "out/rail-print")
    private Path outputDir;

    @Override
    public Integer call() {
        PrintWriter out = spec.commandLine().getOut();
        PrintWriter err = spec.commandLine().getErr();
        try {
            if (template) {
                Path output = outputDir.resolve("rail-alignment-template.pdf");
                RailPlan plan = new RailPlan("TEMPLATE", List.of(templateCard()), List.of());
                new RenderRailPdf(new RailPdfRenderer()).render(new RailPdfRequest(plan, List.of(), output));
                out.println("Wrote rail alignment template: " + output.toAbsolutePath().normalize());
                return 0;
            }
            if (train == null || train.isBlank()) {
                err.println("Error: --train is required unless --template is used.");
                return 2;
            }
            if (!yes) {
                err.println("Error: --yes is required for non-interactive 2.0 rail printing.");
                return 2;
            }
            String trainId = train.trim().toUpperCase(Locale.ROOT);
            List<RailStopRecord> stops = root.dependencies().railRepository().findStopsByTrainId(trainId);
            Map<String, RailFamilyFootprint> footprints = root.dependencies().railRepository()
                    .findFootprintsByShortCode(shortCodes(stops));
            RailPlan plan = new PlanRailCards().plan(trainId, stops, footprints);
            Path output = outputDir.resolve("rail-" + safeSlug(train) + ".pdf");
            new RenderRailPdf(new RailPdfRenderer()).render(new RailPdfRequest(plan, List.of(), output));
            out.println("Wrote rail labels: " + output.toAbsolutePath().normalize());
            out.println("Railcars: " + plan.cards().size());
            if (!plan.missingFootprintItems().isEmpty()) {
                out.println("Missing in card math: " + String.join(", ", plan.missingFootprintItems()));
            }
            return 0;
        } catch (IllegalArgumentException ex) {
            err.println("Error: " + ex.getMessage());
            return 2;
        } catch (RuntimeException ex) {
            err.println("Rail print failed: " + ex.getMessage());
            return 5;
        }
    }

    private static List<String> shortCodes(List<RailStopRecord> stops) {
        Set<String> shortCodes = new LinkedHashSet<>();
        for (RailStopRecord stop : stops) {
            for (RailItemQuantity item : stop.items()) {
                shortCodes.add(item.itemNumber());
            }
        }
        return List.copyOf(shortCodes);
    }

    private static com.tbg.wms.v2.domain.rail.RailCarCard templateCard() {
        return new com.tbg.wms.v2.domain.rail.RailCarCard(
                "TEMPLATE",
                "001",
                "RAILCAR",
                "LOAD",
                "TEMPLATE LOAD",
                List.of(new RailItemQuantity("ITEM", 48)),
                0,
                1,
                0,
                List.of("DOM:100"),
                List.of()
        );
    }

    private static String safeSlug(String value) {
        String slug = value.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9._-]+", "-");
        slug = slug.replaceAll("^-+", "").replaceAll("-+$", "");
        return slug.isBlank() ? "train" : slug;
    }
}
