package com.tbg.wms.cli.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tbg.wms.core.ems.EmsReconAnalysisReport;
import com.tbg.wms.core.ems.EmsReconAnalysisService;
import com.tbg.wms.core.ems.EmsReconFinding;
import com.tbg.wms.core.ems.EmsReconReportParser;
import com.tbg.wms.core.ems.EmsReconRow;
import com.tbg.wms.core.ems.EndpointProbeResult;
import com.tbg.wms.core.ems.EndpointProbeService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Analyzes the daily EMS reconciliation report and emits an operator-friendly mismatch report.
 */
@Command(
        name = "ems-recon",
        description = "Analyze an EMS reconciliation XLS and generate a detailed mismatch report"
)
public final class EmsReconCommand implements Callable<Integer> {
    private static final DateTimeFormatter OUTPUT_TS = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    @Option(names = "--report", required = true, description = "Path to the EMS reconciliation .xls file")
    private Path reportPath;

    @Option(names = "--output-dir", defaultValue = "out/ems-recon", description = "Directory for generated output")
    private Path outputDir;

    @Option(names = "--format", defaultValue = "BOTH", description = "Output format: ${COMPLETION-CANDIDATES}")
    private OutputFormat format;

    @Option(names = "--probe-qa", defaultValue = "false", description = "Probe the QA telnet endpoint and capture reachability evidence")
    private boolean probeQa;

    @Option(names = "--qa-host", defaultValue = "10.19.96.122", description = "QA host to probe when --probe-qa is enabled")
    private String qaHost;

    @Option(names = "--qa-port", defaultValue = "4550", description = "QA port to probe when --probe-qa is enabled")
    private int qaPort;

    @Override
    public Integer call() throws Exception {
        Files.createDirectories(outputDir);

        EmsReconReportParser parser = new EmsReconReportParser();
        List<EmsReconRow> rows = parser.parse(reportPath);
        EndpointProbeResult probe = probeQa
                ? new EndpointProbeService().probe(qaHost, qaPort, 3000, 1000)
                : null;
        EmsReconAnalysisReport analysis = new EmsReconAnalysisService().analyze(reportPath.toAbsolutePath().toString(), rows, probe);

        String stamp = analysis.getGeneratedAt().format(OUTPUT_TS);
        Path txtPath = outputDir.resolve("ems-recon-" + stamp + ".txt");
        Path jsonPath = outputDir.resolve("ems-recon-" + stamp + ".json");

        if (format == OutputFormat.TXT || format == OutputFormat.BOTH) {
            Files.writeString(txtPath, renderText(analysis));
            System.out.println("Wrote text report: " + txtPath.toAbsolutePath());
        }
        if (format == OutputFormat.JSON || format == OutputFormat.BOTH) {
            objectMapper().writeValue(jsonPath.toFile(), analysis);
            System.out.println("Wrote JSON report: " + jsonPath.toAbsolutePath());
        }

        System.out.println();
        System.out.println("Findings: " + analysis.getTotalFindings());
        for (String note : analysis.getNotes()) {
            System.out.println("- " + note);
        }
        return 0;
    }

    private String renderText(EmsReconAnalysisReport analysis) {
        StringBuilder sb = new StringBuilder();
        sb.append("EMS / ASRS Reconciliation Analysis").append(System.lineSeparator());
        sb.append("Generated: ").append(analysis.getGeneratedAt()).append(System.lineSeparator());
        sb.append("Source: ").append(analysis.getSourceFile()).append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("Summary").append(System.lineSeparator());
        for (String note : analysis.getNotes()) {
            sb.append("- ").append(note).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
        sb.append("Findings").append(System.lineSeparator());
        for (EmsReconFinding finding : analysis.getFindings()) {
            sb.append(finding.getPassLabel())
                    .append(" | row ").append(finding.getSheetRowNumber())
                    .append(" | ").append(finding.getCategory().name().toLowerCase(Locale.ROOT))
                    .append(System.lineSeparator());
            sb.append("  container=").append(value(finding.getContainerId()))
                    .append(" location=").append(value(finding.getCurrentLocationId()))
                    .append(" asrs=").append(value(finding.getAsrsLocNum()))
                    .append(" lodnum=").append(value(finding.getLodnum()))
                    .append(" prtnum=").append(value(finding.getMaxPrtnum()))
                    .append(" sku=").append(value(finding.getSku()))
                    .append(System.lineSeparator());
            sb.append("  explanation: ").append(finding.getExplanation()).append(System.lineSeparator());
            sb.append("  action: ").append(finding.getRecommendation()).append(System.lineSeparator());
            sb.append("  qa-fix-template: ").append(finding.getSuggestedFixCommand()).append(System.lineSeparator());
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "<missing>" : value;
    }

    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    private enum OutputFormat {
        TXT,
        JSON,
        BOTH
    }
}
