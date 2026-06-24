package com.tbg.wms.v2.smoke;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SmokeManifestTest {
    @Test
    void defaultManifestIncludesCurrent2ReleaseWorkflows() {
        SmokeManifest manifest = SmokeManifest.defaultManifest();

        assertScenario(manifest, "config", "config");
        assertScenario(manifest, "db-test", "db-test");
        assertScenario(manifest, "shipment-print-to-file", "run --shipment-id {shipmentId} --print-to-file --output-dir {outputDir}");
        assertScenario(manifest, "carrier-move-print-to-file", "run --carrier-move-id {carrierMoveId} --print-to-file --output-dir {outputDir}");
        assertScenario(manifest, "barcode-dry-run", "barcode --data GUI-BARCODE-123 --dry-run --output-dir {outputDir}");
        assertScenario(manifest, "rail-template", "rail-print --template --output-dir {outputDir}");
        assertScenario(manifest, "rail-print-to-file", "rail-print --train {trainId} --yes --output-dir {outputDir}");
        assertScenario(manifest, "packaged-config-precedence", "config");
    }

    @Test
    void defaultManifestKeepsSmokeSafeForRoutineRuns() {
        SmokeManifest manifest = SmokeManifest.defaultManifest();

        assertTrue(manifest.scenarios().stream().allMatch(scenario -> scenario.expectedExitCode() == 0));
        assertTrue(manifest.scenarios().stream()
                .filter(scenario -> scenario.tier() == 1)
                .allMatch(scenario -> scenario.modes().contains(SmokeMode.REPO)
                        || scenario.modes().contains(SmokeMode.PACKAGED)));
        assertFalse(manifest.scenarios().stream().anyMatch(SmokeScenario::releaseOnly));
        assertFalse(manifest.scenarios().stream()
                .map(SmokeScenario::command)
                .anyMatch(command -> command.contains("--printer") || command.contains("validate-system-default-print")));
    }

    @Test
    void manifestCanRenderJsonForPowerShellWrapper() {
        String json = SmokeManifest.defaultManifest().toJson();

        assertTrue(json.contains("\"version\":2"));
        assertTrue(json.contains("\"name\":\"shipment-print-to-file\""));
        assertTrue(json.contains("\"modes\":[\"repo\",\"packaged\"]"));
        assertTrue(json.contains("\"expectedArtifacts\":[\"*.zpl\"]"));
    }

    private static void assertScenario(SmokeManifest manifest, String name, String command) {
        SmokeScenario scenario = manifest.findByName(name).orElseThrow();
        assertNotNull(scenario);
        assertEquals(command, scenario.command());
        assertEquals(0, scenario.expectedExitCode());
        assertTrue(scenario.modes().containsAll(Set.of(SmokeMode.REPO, SmokeMode.PACKAGED))
                        || scenario.modes().contains(SmokeMode.PACKAGED),
                "scenario should run in repo+packaged or packaged mode: " + name);
    }
}
