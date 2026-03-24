package com.tbg.wms.cli.commands.rail;

import com.tbg.wms.core.rail.RailFamilyFootprint;
import com.tbg.wms.core.rail.RailLabelPlanner;
import com.tbg.wms.core.rail.RailStopRecord;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RailHelperOutputSupportTest {

    private final RailHelperOutputSupport support = new RailHelperOutputSupport();

    @Test
    void buildSummary_shouldReportMissingAndOverflowRows() {
        RailStopRecord record = new RailStopRecord(
                "03-22-26",
                "142",
                "TRAIN-A",
                "CAR-100",
                "D-2",
                "LOAD-1",
                List.of(
                        new RailStopRecord.ItemQuantity("01832", 100),
                        new RailStopRecord.ItemQuantity("09999", 50)
                )
        );
        RailLabelPlanner.PlannedRailLabel planned = new RailLabelPlanner(1).planOne(
                record,
                Map.of("01832", new RailFamilyFootprint("01832", "DOM", 100))
        );

        String summary = support.buildSummary(List.of(planned), 1);

        assertTrue(summary.contains("Rows exported: 1"));
        assertTrue(summary.contains("Rows with missing footprint: 1"));
        assertTrue(summary.contains("Rows exceeding item slot limit: 1"));
        assertTrue(summary.contains("Missing items: 09999"));
    }

    @Test
    void buildSuccessLines_shouldIncludeTemplateWhenPresent() {
        List<String> lines = support.buildSuccessLines(
                Path.of("out", "_TrainDetail.csv"),
                Path.of("out", "rail-helper-summary.txt"),
                Path.of("out", "template.docx")
        );

        assertEquals("Rail helper output generated:", lines.get(0));
        assertTrue(lines.get(3).contains("template.docx"));
    }
}
