package com.tbg.wms.v2.smoke;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Describes the smoke scenarios used to verify a WMS 2.0 release build.
 *
 * @param version manifest schema version
 * @param description human-readable manifest description
 * @param scenarios smoke scenarios to run
 */
public record SmokeManifest(int version, String description, List<SmokeScenario> scenarios) {
    private static final Set<SmokeMode> ALL_MODES = Set.of(SmokeMode.REPO, SmokeMode.PACKAGED);

    public SmokeManifest {
        scenarios = List.copyOf(scenarios);
    }

    /**
     * Creates the default WMS 2.0 smoke manifest.
     *
     * @return default smoke manifest
     */
    public static SmokeManifest defaultManifest() {
        List<SmokeScenario> scenarios = new ArrayList<>();
        scenarios.add(scenario("config", 1, "config", ALL_MODES, "config"));
        scenarios.add(scenario("db-test", 1, "db-test", ALL_MODES, "db-test"));
        scenarios.add(scenario("shipment-print-to-file", 1, "shipment-print-to-file", ALL_MODES,
                "run --shipment-id {shipmentId} --print-to-file --output-dir {outputDir}", "*.zpl"));
        scenarios.add(scenario("carrier-move-print-to-file", 1, "carrier-move-print-to-file", ALL_MODES,
                "run --carrier-move-id {carrierMoveId} --print-to-file --output-dir {outputDir}", "*.zpl"));
        scenarios.add(scenario("barcode-dry-run", 1, "barcode-dry-run", ALL_MODES,
                "barcode --data GUI-BARCODE-123 --dry-run --output-dir {outputDir}", "*.zpl"));
        scenarios.add(scenario("rail-template", 1, "rail-template", ALL_MODES,
                "rail-print --template --output-dir {outputDir}", "*.pdf"));
        scenarios.add(scenario("rail-print-to-file", 1, "rail-print-to-file", ALL_MODES,
                "rail-print --train {trainId} --yes --output-dir {outputDir}", "*.pdf"));
        scenarios.add(new SmokeScenario(
                "packaged-config-precedence",
                1,
                "packaged-config-precedence",
                Set.of(SmokeMode.PACKAGED),
                "config",
                0,
                List.of(),
                false,
                "Tropicana"));
        return new SmokeManifest(2, "WMS Pallet Tag System 2.0 routine smoke manifest", scenarios);
    }

    /**
     * Finds a scenario by name.
     *
     * @return matching scenario, when present
     */
    public Optional<SmokeScenario> findByName(String name) {
        return scenarios.stream().filter(scenario -> scenario.name().equals(name)).findFirst();
    }

    /**
     * Serializes this manifest to JSON.
     *
     * @return JSON representation
     */
    public String toJson() {
        StringBuilder builder = new StringBuilder();
        builder.append('{');
        property(builder, "version", version);
        builder.append(',');
        property(builder, "description", description);
        builder.append(",\"scenarios\":[");
        for (int i = 0; i < scenarios.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            scenarioJson(builder, scenarios.get(i));
        }
        builder.append("]}");
        return builder.toString();
    }

    private static SmokeScenario scenario(
            String name,
            int tier,
            String kind,
            Set<SmokeMode> modes,
            String command,
            String... expectedArtifacts
    ) {
        return new SmokeScenario(name, tier, kind, modes, command, 0, List.of(expectedArtifacts), false, "");
    }

    private static void scenarioJson(StringBuilder builder, SmokeScenario scenario) {
        builder.append('{');
        property(builder, "name", scenario.name());
        builder.append(',');
        property(builder, "tier", scenario.tier());
        builder.append(',');
        property(builder, "kind", scenario.kind());
        builder.append(",\"modes\":[");
        List<SmokeMode> modes = List.of(SmokeMode.REPO, SmokeMode.PACKAGED).stream()
                .filter(scenario.modes()::contains)
                .toList();
        for (int i = 0; i < modes.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            quote(builder, modes.get(i).wireName());
        }
        builder.append("],");
        property(builder, "command", scenario.command());
        builder.append(',');
        property(builder, "expectedExitCode", scenario.expectedExitCode());
        builder.append(",\"expectedArtifacts\":[");
        for (int i = 0; i < scenario.expectedArtifacts().size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            quote(builder, scenario.expectedArtifacts().get(i));
        }
        builder.append("],");
        property(builder, "releaseOnly", scenario.releaseOnly());
        if (!scenario.expectedConfigSourcePattern().isBlank()) {
            builder.append(',');
            property(builder, "expectedConfigSourcePattern", scenario.expectedConfigSourcePattern());
        }
        builder.append('}');
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
