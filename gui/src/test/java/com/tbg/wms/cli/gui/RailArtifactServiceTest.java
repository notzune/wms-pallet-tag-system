/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.5.2
 */
package com.tbg.wms.cli.gui;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailArtifactServiceTest {

    @Test
    void generateWordArtifactsReturnsSkippedWhenTemplateMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("rail-artifacts-test");
        Path mergeCsv = tempDir.resolve("_TrainDetail.csv");
        Files.writeString(mergeCsv, "DATE,SEQ\n03-02-26,1\n");

        RailArtifactService service = new RailArtifactService();
        RailArtifactService.WordArtifactResult result =
                service.generateWordArtifacts(tempDir.resolve("missing.docx"), mergeCsv, tempDir);

        assertNull(result.getMergedDocx());
        assertNull(result.getMergedPdf());
        assertNull(result.getMergedPrn());
        assertEquals(1, result.getWarnings().size());
        assertTrue(result.getWarnings().get(0).contains("Template DOCX"));
    }
}
