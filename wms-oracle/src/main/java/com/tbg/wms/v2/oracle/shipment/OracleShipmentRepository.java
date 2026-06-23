package com.tbg.wms.v2.oracle.shipment;

import com.tbg.wms.v2.app.ports.ShipmentRepository;
import com.tbg.wms.v2.domain.label.LabelSelectionRef;
import com.tbg.wms.v2.domain.label.PreparedShipmentLabels;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class OracleShipmentRepository implements ShipmentRepository {
    private static final String LABELS_SQL = """
            SELECT SHIP_ID, LABEL_ID, SOURCE_SEQUENCE
            FROM (
                SELECT labels.SHIP_ID,
                       labels.LABEL_ID,
                       ROW_NUMBER() OVER (ORDER BY labels.LABEL_ID) AS SOURCE_SEQUENCE
                FROM (
                    SELECT DISTINCT pwd.SHIP_ID,
                           pwd.SHIP_CTNNUM AS LABEL_ID
                    FROM WMSP.PCKWRK_DTL pwd
                    WHERE pwd.SHIP_ID = ?
                      AND pwd.SHIP_CTNNUM IS NOT NULL
                ) labels
            )
            ORDER BY SOURCE_SEQUENCE
            """;

    private final DataSource dataSource;
    private final ShipmentLabelRowMapper mapper;

    public OracleShipmentRepository(DataSource dataSource) {
        this(dataSource, new ShipmentLabelRowMapper());
    }

    OracleShipmentRepository(DataSource dataSource, ShipmentLabelRowMapper mapper) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper cannot be null");
    }

    @Override
    public PreparedShipmentLabels findByShipmentId(String shipmentId) {
        String normalizedShipmentId = normalizeId(shipmentId, "shipmentId");
        List<LabelSelectionRef> labels = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(LABELS_SQL)) {
            statement.setString(1, normalizedShipmentId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    labels.add(mapper.map(rows).label());
                }
            }
            return PreparedShipmentLabels.of(normalizedShipmentId, labels, true);
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load shipment labels for " + normalizedShipmentId, ex);
        }
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
}
