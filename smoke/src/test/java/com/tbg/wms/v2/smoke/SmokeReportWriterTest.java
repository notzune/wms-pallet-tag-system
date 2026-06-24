package com.tbg.wms.v2.smoke;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmokeReportWriterTest {
    @TempDir
    Path tempDir;

    @Test
    void writerEmitsTextAndJsonReports() throws Exception {
        SmokeReportWriter writer = new SmokeReportWriter();
        SmokeReport report = new SmokeReport(
                SmokeMode.REPO,
                List.of(
                        SmokeScenarioResult.pass("config", "configuration resolved"),
                        SmokeScenarioResult.fail("db-test", 3, "listener refused connection")
                )
        );

        writer.write(report, tempDir);

        String text = Files.readString(tempDir.resolve("smoke-report.txt"));
        String json = Files.readString(tempDir.resolve("smoke-report.json"));
        assertTrue(text.contains("Mode: repo"));
        assertTrue(text.contains("PASS config"));
        assertTrue(text.contains("FAIL db-test exit=3"));
        assertTrue(json.contains("\"mode\":\"repo\""));
        assertTrue(json.contains("\"passed\":false"));
        assertTrue(json.contains("\"scenario\":\"db-test\""));
    }
}
