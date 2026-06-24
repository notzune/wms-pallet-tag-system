package com.tbg.wms.v2.printing.pdf;

import com.tbg.wms.v2.domain.rail.RailCarCard;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailPdfRendererTest {
    @TempDir
    Path tempDir;

    @Test
    void render_writesPdfArtifactForRailCards() throws Exception {
        Path output = tempDir.resolve("rail.pdf");

        Path result = new RailPdfRenderer().render(List.of(new RailCarCard(
                "TRAIN1",
                "142",
                "TPIX3004",
                "LOAD1",
                "TRAIN1 BR LOAD1",
                List.of(new RailItemQuantity("01830", 120)),
                0,
                2,
                0,
                List.of("DOM:100"),
                List.of()
        )), output);

        assertEquals(output, result);
        assertTrue(Files.size(output) > 0);
        byte[] header = Files.readAllBytes(output);
        assertEquals("%PDF", new String(header, 0, 4));
    }
}
