package com.tbg.wms.v2.smoke;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class SmokeReportWriter {
    public void write(SmokeReport report, Path outputDirectory) throws IOException {
        Files.createDirectories(outputDirectory);
        Files.writeString(outputDirectory.resolve("smoke-report.txt"), toText(report));
        Files.writeString(outputDirectory.resolve("smoke-report.json"), toJson(report));
    }

    private static String toText(SmokeReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append("Mode: ").append(report.mode().wireName()).append(System.lineSeparator());
        builder.append("Passed: ").append(report.passed()).append(System.lineSeparator());
        for (SmokeScenarioResult result : report.results()) {
            builder.append(result.passed() ? "PASS " : "FAIL ");
            builder.append(result.scenario());
            if (!result.passed()) {
                builder.append(" exit=").append(result.exitCode());
            }
            if (!result.output().isBlank()) {
                builder.append(" - ").append(result.output());
            }
            builder.append(System.lineSeparator());
        }
        return builder.toString();
    }

    private static String toJson(SmokeReport report) {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        property(builder, "mode", report.mode().wireName());
        builder.append(',');
        property(builder, "passed", report.passed());
        builder.append(",\"results\":[");
        for (int i = 0; i < report.results().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            SmokeScenarioResult result = report.results().get(i);
            builder.append('{');
            property(builder, "scenario", result.scenario());
            builder.append(',');
            property(builder, "passed", result.passed());
            builder.append(',');
            property(builder, "exitCode", result.exitCode());
            builder.append(',');
            property(builder, "output", result.output());
            builder.append('}');
        }
        builder.append("]}");
        return builder.toString();
    }

    private static void property(StringBuilder builder, String name, String value) {
        quote(builder, name);
        builder.append(':');
        quote(builder, value);
    }

    private static void property(StringBuilder builder, String name, int value) {
        quote(builder, name);
        builder.append(':').append(value);
    }

    private static void property(StringBuilder builder, String name, boolean value) {
        quote(builder, name);
        builder.append(':').append(value);
    }

    private static void quote(StringBuilder builder, String value) {
        builder.append('"');
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            switch (character) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(character);
            }
        }
        builder.append('"');
    }
}
