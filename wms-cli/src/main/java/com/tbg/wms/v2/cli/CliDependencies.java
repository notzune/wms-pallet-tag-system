package com.tbg.wms.v2.cli;

import com.tbg.wms.v2.app.ports.CarrierMoveRepository;
import com.tbg.wms.v2.app.ports.RailRepository;
import com.tbg.wms.v2.app.ports.ShipmentRepository;

import java.time.Clock;
import java.util.Objects;

public final class CliDependencies {
    private final RuntimeConfigView config;
    private final ShipmentRepository shipmentRepository;
    private final CarrierMoveRepository carrierMoveRepository;
    private final RailRepository railRepository;
    private final DbHealthCheck dbHealthCheck;
    private final CliClock clock;

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

    public RuntimeConfigView config() {
        return config;
    }

    public ShipmentRepository shipmentRepository() {
        return shipmentRepository;
    }

    public CarrierMoveRepository carrierMoveRepository() {
        return carrierMoveRepository;
    }

    public RailRepository railRepository() {
        return railRepository;
    }

    public DbHealthCheck dbHealthCheck() {
        return dbHealthCheck;
    }

    public CliClock clock() {
        return clock;
    }
}
