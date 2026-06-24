package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.ports.CarrierMoveRepository;
import com.tbg.wms.v2.app.ports.RailRepository;
import com.tbg.wms.v2.app.ports.ShipmentRepository;
import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.carriermove.PreparedStopGroup;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;
import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CliCommandParityTest {
    @TempDir
    Path tempDir;

    @Test
    void configPrintsEffectiveRuntimeWithoutSecrets() {
        Harness harness = new Harness(tempDir);

        Result result = harness.execute("config");

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("WMS Pallet Tag System 2.0 Configuration"));
        assertTrue(result.stdout().contains("Site: TBG3002"));
        assertTrue(result.stdout().contains("Environment: test"));
        assertTrue(result.stdout().contains("Loaded config files:"));
        assertTrue(result.stdout().contains("oracle.example:1521/WMS"));
        assertTrue(result.stdout().contains("Password: ********"));
        assertFalse(result.stdout().contains("super-secret"));
    }

    @Test
    void dbTestReportsConnectivitySuccess() {
        Harness harness = new Harness(tempDir);
        harness.dbHealthCheck.delegate = () -> new DbHealthCheck.Result(true, "Connected to Oracle");

        Result result = harness.execute("db-test");

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Database connectivity: OK"));
        assertTrue(result.stdout().contains("Connected to Oracle"));
    }

    @Test
    void dbTestReportsConnectivityFailure() {
        Harness harness = new Harness(tempDir);
        harness.dbHealthCheck.delegate = () -> {
            throw new SQLException("listener refused connection");
        };

        Result result = harness.execute("db-test");

        assertEquals(3, result.exitCode());
        assertTrue(result.stderr().contains("Database connectivity failed"));
        assertTrue(result.stderr().contains("listener refused connection"));
    }

    @Test
    void runShipmentPrintToFileWritesPlannedArtifacts() throws Exception {
        Harness harness = new Harness(tempDir);

        Result result = harness.execute(
                "run",
                "--shipment-id", "800123",
                "--print-to-file",
                "--output-dir", tempDir.resolve("shipment").toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Artifacts written: 3"));
        assertEquals(List.of("800123_LPN-1_1_of_2.zpl", "800123_LPN-2_2_of_2.zpl", "info-shipment-800123.zpl"),
                fileNames(tempDir.resolve("shipment")));
        assertEquals("800123", harness.shipmentRepository.lastShipmentId);
    }

    @Test
    void runCarrierMovePrintToFileWritesStopAndFinalArtifacts() throws Exception {
        Harness harness = new Harness(tempDir);

        Result result = harness.execute(
                "run",
                "--carrier-move-id", "456",
                "--print-to-file",
                "--output-dir", tempDir.resolve("carrier").toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Artifacts written: 7"));
        assertEquals(List.of(
                        "800123_LPN-1_1_of_1.zpl",
                        "800124_LPN-3_1_of_1.zpl",
                        "info-final-cmid-456.zpl",
                        "info-shipment-800123.zpl",
                        "info-shipment-800124.zpl",
                        "info-stop-01-of-02.zpl",
                        "info-stop-02-of-02.zpl"),
                fileNames(tempDir.resolve("carrier")));
        assertEquals("456", harness.carrierMoveRepository.lastCarrierMoveId);
    }

    @Test
    void barcodeDryRunWritesBarcodeZpl() throws Exception {
        Harness harness = new Harness(tempDir);

        Result result = harness.execute(
                "barcode",
                "--data", "HELLO-WORLD-123",
                "--dry-run",
                "--output-dir", tempDir.resolve("barcode").toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Wrote barcode label"));
        List<String> names = fileNames(tempDir.resolve("barcode"));
        assertEquals(List.of("barcode-hello-world-123.zpl"), names);
        assertTrue(Files.readString(tempDir.resolve("barcode").resolve(names.get(0))).contains("^FDHELLO-WORLD-123^FS"));
    }

    @Test
    void railPrintTemplateWritesTemplatePdf() throws Exception {
        Harness harness = new Harness(tempDir);

        Result result = harness.execute(
                "rail-print",
                "--template",
                "--output-dir", tempDir.resolve("rail-template").toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Wrote rail alignment template"));
        assertEquals(List.of("rail-alignment-template.pdf"), fileNames(tempDir.resolve("rail-template")));
    }

    @Test
    void railPrintTrainYesWritesTrainPdf() throws Exception {
        Harness harness = new Harness(tempDir);

        Result result = harness.execute(
                "rail-print",
                "--train", "tr-77",
                "--yes",
                "--output-dir", tempDir.resolve("rail").toString()
        );

        assertEquals(0, result.exitCode());
        assertTrue(result.stdout().contains("Wrote rail labels"));
        assertEquals(List.of("rail-tr-77.pdf"), fileNames(tempDir.resolve("rail")));
        assertEquals("TR-77", harness.railRepository.lastTrainId);
    }

    private static List<String> fileNames(Path dir) throws Exception {
        try (var stream = Files.list(dir)) {
            return stream
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }

    private static final class Harness {
        private final CliDependencies dependencies;
        private final FakeShipmentRepository shipmentRepository;
        private final FakeCarrierMoveRepository carrierMoveRepository;
        private final FakeRailRepository railRepository;
        private final MutableDbHealthCheck dbHealthCheck;

        private Harness(Path tempDir) {
            shipmentRepository = new FakeShipmentRepository();
            carrierMoveRepository = new FakeCarrierMoveRepository();
            railRepository = new FakeRailRepository();
            dbHealthCheck = new MutableDbHealthCheck();
            dependencies = FakeDependencies.create(
                    tempDir,
                    shipmentRepository,
                    carrierMoveRepository,
                    railRepository,
                    dbHealthCheck
            );
        }

        private Result execute(String... args) {
            ByteArrayOutputStream stdout = new ByteArrayOutputStream();
            ByteArrayOutputStream stderr = new ByteArrayOutputStream();
            CommandLine cli = new CommandLine(new WmsCli(dependencies));
            cli.setOut(new PrintWriter(stdout, true));
            cli.setErr(new PrintWriter(stderr, true));
            int exitCode = cli.execute(args);
            return new Result(exitCode, stdout.toString(), stderr.toString());
        }
    }

    private record Result(int exitCode, String stdout, String stderr) {
    }

    private static final class FakeDependencies {
        private static CliDependencies create(
                Path root,
                FakeShipmentRepository shipmentRepository,
                FakeCarrierMoveRepository carrierMoveRepository,
                FakeRailRepository railRepository,
                DbHealthCheck dbHealthCheck
        ) {
            return new CliDependencies(
                    RuntimeConfigView.builder()
                            .siteCode("TBG3002")
                            .environment("test")
                            .loadedConfigFiles(List.of(root.resolve("wms-tags.env")))
                            .oracleJdbcUrl("jdbc:oracle:thin:@//oracle.example:1521/WMS")
                            .oracleUsername("wms_user")
                            .oraclePassword("super-secret")
                            .build(),
                    shipmentRepository,
                    carrierMoveRepository,
                    railRepository,
                    dbHealthCheck,
                    new CliClock.Fixed("20260623-120000")
            );
        }
    }

    private static final class MutableDbHealthCheck implements DbHealthCheck {
        private DbHealthCheck delegate = () -> new Result(true, "Connected");

        @Override
        public Result check() throws Exception {
            return delegate.check();
        }
    }

    private static final class FakeShipmentRepository implements ShipmentRepository {
        private String lastShipmentId;

        @Override
        public PreparedShipmentLabels findByShipmentId(String shipmentId) {
            lastShipmentId = shipmentId;
            return PreparedShipmentLabels.of(
                    shipmentId,
                    List.of(LabelSelectionRef.palletLabel("LPN-1", 1), LabelSelectionRef.palletLabel("LPN-2", 2)),
                    true
            );
        }
    }

    private static final class FakeCarrierMoveRepository implements CarrierMoveRepository {
        private String lastCarrierMoveId;

        @Override
        public CarrierMoveLabels findByCarrierMoveId(String carrierMoveId) {
            lastCarrierMoveId = carrierMoveId;
            return CarrierMoveLabels.of(
                    carrierMoveId,
                    List.of(
                            PreparedStopGroup.of(1, 10, List.of(PreparedShipmentLabels.of(
                                    "800123",
                                    List.of(LabelSelectionRef.palletLabel("LPN-1", 1)),
                                    true
                            ))),
                            PreparedStopGroup.of(2, 20, List.of(PreparedShipmentLabels.of(
                                    "800124",
                                    List.of(LabelSelectionRef.palletLabel("LPN-3", 1)),
                                    true
                            )))
                    ),
                    true
            );
        }
    }

    private static final class FakeRailRepository implements RailRepository {
        private String lastTrainId;

        @Override
        public List<RailStopRecord> findStopsByTrainId(String trainId) {
            lastTrainId = trainId;
            return List.of(new RailStopRecord(
                    "2026-06-23",
                    "001",
                    trainId,
                    "CAR-1",
                    "WH",
                    "LOAD-1",
                    List.of(new RailItemQuantity("ITEM1", 96))
            ));
        }

        @Override
        public Map<String, RailFamilyFootprint> findFootprintsByShortCode(List<String> shortCodes) {
            Map<String, RailFamilyFootprint> footprints = new HashMap<>();
            footprints.put("ITEM1", new RailFamilyFootprint("ITEM1", "DOM", 48));
            return footprints;
        }
    }
}
