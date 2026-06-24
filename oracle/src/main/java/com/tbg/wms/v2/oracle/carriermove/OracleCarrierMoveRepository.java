package com.tbg.wms.v2.oracle.carriermove;

import com.tbg.wms.v2.app.ports.CarrierMoveRepository;
import com.tbg.wms.v2.app.ports.ShipmentRepository;
import com.tbg.wms.v2.domain.carriermove.CarrierMoveLabels;
import com.tbg.wms.v2.domain.carriermove.PreparedStopGroup;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Provides oracle carrier move repository behavior for WMS 2.0 workflows.
 */
public final class OracleCarrierMoveRepository implements CarrierMoveRepository {
    private static final String STOP_SHIPMENTS_SQL = """
            SELECT st.CAR_MOVE_ID,
                   st.STOP_ID,
                   st.STOP_SEQ AS STOP_SEQUENCE,
                   s.SHIP_ID
            FROM WMSP.STOP st
            INNER JOIN WMSP.SHIPMENT s ON s.STOP_ID = st.STOP_ID
            WHERE st.CAR_MOVE_ID = ?
            ORDER BY st.STOP_SEQ ASC, s.SHIP_ID ASC
            """;

    private final DataSource dataSource;
    private final ShipmentRepository shipmentRepository;
    private final CarrierMoveStopShipmentRowMapper mapper;

    /**
     * Creates an Oracle-backed carrier-move repository.
     *
     * @param dataSource the data source.
     * @param shipmentRepository the shipment repository.
     */
    public OracleCarrierMoveRepository(DataSource dataSource, ShipmentRepository shipmentRepository) {
        this(dataSource, shipmentRepository, new CarrierMoveStopShipmentRowMapper());
    }

    OracleCarrierMoveRepository(
            DataSource dataSource,
            ShipmentRepository shipmentRepository,
            CarrierMoveStopShipmentRowMapper mapper
    ) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.shipmentRepository = Objects.requireNonNull(shipmentRepository, "shipmentRepository cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    /**
     * Finds by carrier move id.
     *
     * @param carrierMoveId the carrier move id.
     * @return prepared carrier-move labels, when present
     */
    @Override
    public CarrierMoveLabels findByCarrierMoveId(String carrierMoveId) {
        String normalizedCarrierMoveId = normalizeId(carrierMoveId, "carrierMoveId");
        Map<StopKey, List<PreparedShipmentLabels>> shipmentsByStop = new LinkedHashMap<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(STOP_SHIPMENTS_SQL)) {
            statement.setString(1, normalizedCarrierMoveId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    CarrierMoveStopShipmentRow row = mapper.map(rows);
                    StopKey key = new StopKey(row.stopId(), row.stopSequence());
                    shipmentsByStop.computeIfAbsent(key, ignored -> new ArrayList<>())
                            .add(shipmentRepository.findByShipmentId(row.shipmentId()));
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load carrier move labels for " + normalizedCarrierMoveId, ex);
        }

        List<PreparedStopGroup> stops = new ArrayList<>();
        int stopPosition = 1;
        for (Map.Entry<StopKey, List<PreparedShipmentLabels>> entry : shipmentsByStop.entrySet()) {
            stops.add(PreparedStopGroup.of(stopPosition, entry.getKey().stopSequence(), entry.getValue()));
            stopPosition++;
        }
        return CarrierMoveLabels.of(normalizedCarrierMoveId, stops, true);
    }

    private static String normalizeId(String value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private record StopKey(String stopId, int stopSequence) {
    }
}
