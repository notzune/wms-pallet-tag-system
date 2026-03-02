/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.1
 */
package com.tbg.wms.cli.gui.rail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generates Word/PDF/PRN artifacts from merge CSV output.
 */
public final class RailArtifactService {

    public WordArtifactResult generateWordArtifacts(Path templateDocx, Path mergeCsv, Path outputDir) throws Exception {
        Objects.requireNonNull(mergeCsv, "mergeCsv cannot be null");
        Objects.requireNonNull(outputDir, "outputDir cannot be null");

        if (templateDocx == null || !Files.exists(templateDocx) || !Files.isReadable(templateDocx)) {
            return WordArtifactResult.skipped("Template DOCX was not provided or is unreadable.");
        }

        Path mergedDocx = outputDir.resolve("Print-Merged.docx");
        Path mergedPdf = outputDir.resolve("Print-Merged.pdf");
        Path mergedPrn = outputDir.resolve("Print-Merged.prn");

        Path script = writeWordMergeScript(outputDir);
        ProcessBuilder pb = new ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.toString(),
                "-TemplateDocx", templateDocx.toAbsolutePath().toString(),
                "-MergeCsv", mergeCsv.toAbsolutePath().toString(),
                "-OutDocx", mergedDocx.toAbsolutePath().toString(),
                "-OutPdf", mergedPdf.toAbsolutePath().toString(),
                "-OutPrn", mergedPrn.toAbsolutePath().toString()
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        List<String> warnings = new ArrayList<>();
        if (exitCode != 0) {
            warnings.add("Word merge automation failed. Output: " + output.trim());
        }
        if (!Files.exists(mergedDocx)) {
            warnings.add("Merged DOCX was not created.");
        }
        if (!Files.exists(mergedPdf)) {
            warnings.add("Merged PDF was not created.");
        }
        if (!Files.exists(mergedPrn)) {
            warnings.add("PRN file was not created. Verify default printer supports print-to-file.");
        }

        return new WordArtifactResult(
                mergedDocx,
                mergedPdf,
                mergedPrn,
                output,
                warnings
        );
    }

    private Path writeWordMergeScript(Path outputDir) throws Exception {
        Path script = outputDir.resolve("rail-word-merge.ps1");
        String body = String.join("\n",
                "param(",
                "  [Parameter(Mandatory=$true)][string]$TemplateDocx,",
                "  [Parameter(Mandatory=$true)][string]$MergeCsv,",
                "  [Parameter(Mandatory=$true)][string]$OutDocx,",
                "  [Parameter(Mandatory=$true)][string]$OutPdf,",
                "  [Parameter(Mandatory=$true)][string]$OutPrn",
                ")",
                "$ErrorActionPreference = 'Stop'",
                "$word = $null",
                "$templateDoc = $null",
                "$mergedDoc = $null",
                "try {",
                "  $word = New-Object -ComObject Word.Application",
                "  $word.Visible = $false",
                "  $templateDoc = $word.Documents.Open($TemplateDocx, $false, $true)",
                "  $templateDoc.MailMerge.SuppressBlankLines = $true",
                "  $templateDoc.MailMerge.OpenDataSource($MergeCsv)",
                "  $templateDoc.MailMerge.Destination = 0",
                "  $templateDoc.MailMerge.Execute($false)",
                "  $mergedDoc = $word.ActiveDocument",
                "  $wdFormatDocumentDefault = 16",
                "  $mergedDoc.SaveAs([ref]$OutDocx, [ref]$wdFormatDocumentDefault)",
                "  $wdExportFormatPDF = 17",
                "  $mergedDoc.ExportAsFixedFormat($OutPdf, $wdExportFormatPDF)",
                "  $mergedDoc.PrintOut($false, $false, 0, '', '', $true, '', 1, '', '', 0, $true, $OutPrn)",
                "  Write-Output 'Word merge/export completed.'",
                "} finally {",
                "  if ($mergedDoc -ne $null) { $mergedDoc.Close([ref]$false) }",
                "  if ($templateDoc -ne $null) { $templateDoc.Close([ref]$false) }",
                "  if ($word -ne $null) { $word.Quit() }",
                "}"
        );
        Files.writeString(script, body, StandardCharsets.UTF_8);
        return script;
    }

    public static final class WordArtifactResult {
        private final Path mergedDocx;
        private final Path mergedPdf;
        private final Path mergedPrn;
        private final String commandOutput;
        private final List<String> warnings;

        private WordArtifactResult(Path mergedDocx,
                                   Path mergedPdf,
                                   Path mergedPrn,
                                   String commandOutput,
                                   List<String> warnings) {
            this.mergedDocx = mergedDocx;
            this.mergedPdf = mergedPdf;
            this.mergedPrn = mergedPrn;
            this.commandOutput = commandOutput == null ? "" : commandOutput;
            this.warnings = List.copyOf(warnings);
        }

        private static WordArtifactResult skipped(String reason) {
            return new WordArtifactResult(null, null, null, "", List.of(reason));
        }

        public Path getMergedDocx() {
            return mergedDocx;
        }

        public Path getMergedPdf() {
            return mergedPdf;
        }

        public Path getMergedPrn() {
            return mergedPrn;
        }

        public String getCommandOutput() {
            return commandOutput;
        }

        public List<String> getWarnings() {
            return warnings;
        }
    }
}
