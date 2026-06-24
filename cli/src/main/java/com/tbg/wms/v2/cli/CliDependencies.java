package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.ports.CarrierMoveRepository;
import com.tbg.wms.v2.app.ports.RailRepository;
import com.tbg.wms.v2.app.ports.ShipmentRepository;

import java.time.Clock;
import java.util.Objects;

/**
 * Bundles command-line adapters and repositories used to wire WMS 2.0 CLI commands.
 */
public final class CliDependencies {
    private final RuntimeConfigView config;
    private final ShipmentRepository shipmentRepository;
    private final CarrierMoveRepository carrierMoveRepository;
    private final RailRepository railRepository;
    private final DbHealthCheck dbHealthCheck;
    private final CliClock clock;

    /**
     * Creates a CLI dependency container.
     *
     * @param config runtime configuration exposed to commands
     * @param shipmentRepository repository for shipment label lookups
     * @param carrierMoveRepository repository for carrier-move label lookups
     * @param railRepository repository for rail label lookups
     * @param dbHealthCheck database health check used by diagnostics commands
     * @param clock timestamp source used for generated artifact names
     */
    public CliDependencies(
            RuntimeConfigView config,
            ShipmentRepository shipmentRepository,
            CarrierMoveRepository carrierMoveRepository,
            RailRepository railRepository,
            DbHealthCheck dbHealthCheck,
            CliClock clock
    ) {
        this.config = Objects.requireNonNull(config, "config");
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository, "shipmentRepository");
        this.carrierMoveRepository = Objects.requireNonNull(carrierMoveRepository, "carrierMoveRepository");
        this.railRepository = Objects.requireNonNull(railRepository, "railRepository");
        this.dbHealthCheck = Objects.requireNonNull(dbHealthCheck, "dbHealthCheck");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * Creates dependencies that fail fast for integrations not wired into the current process.
     *
     * @return dependency container backed by unsupported repository defaults
     */
    public static CliDependencies unsupportedDefaults() {
        RuntimeConfigView config = RuntimeConfigView.builder().build();
        ShipmentRepository shipments = id -> {
            throw new UnsupportedOperationException("Shipment repository is not configured.");
        };
        CarrierMoveRepository carrierMoves = id -> {
            throw new UnsupportedOperationException("Carrier move repository is not configured.");
        };
        RailRepository rail = new UnsupportedRailRepository();
        return new CliDependencies(
                config,
                shipments,
                carrierMoves,
                rail,
                () -> new DbHealthCheck.Result(false, "Database health check is not configured."),
                new CliClock.SystemClock(Clock.systemDefaultZone())
        );
    }

    /**
     * Returns the runtime configuration view.
     *
     * @return runtime configuration view
     */
    public RuntimeConfigView config() {
        return config;
    }

    /**
     * Returns the shipment repository adapter.
     *
     * @return shipment repository
     */
    public ShipmentRepository shipmentRepository() {
        return shipmentRepository;
    }

    /**
     * Returns the carrier-move repository adapter.
     *
     * @return carrier-move repository
     */
    public CarrierMoveRepository carrierMoveRepository() {
        return carrierMoveRepository;
    }

    /**
     * Returns the rail repository adapter.
     *
     * @return rail repository
     */
    public RailRepository railRepository() {
        return railRepository;
    }

    /**
     * Returns the configured database health check.
     *
     * @return database health check
     */
    public DbHealthCheck dbHealthCheck() {
        return dbHealthCheck;
    }

    /**
     * Returns the CLI timestamp source.
     *
     * @return CLI clock
     */
    public CliClock clock() {
        return clock;
    }
}
