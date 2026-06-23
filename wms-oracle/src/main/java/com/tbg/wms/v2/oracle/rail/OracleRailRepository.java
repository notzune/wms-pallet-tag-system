package com.tbg.wms.v2.oracle.rail;

import com.tbg.wms.v2.app.ports.RailRepository;
import com.tbg.wms.v2.domain.rail.RailFamilyFootprint;
import com.tbg.wms.v2.domain.rail.RailItemQuantity;
import com.tbg.wms.v2.domain.rail.RailStopRecord;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class OracleRailRepository implements RailRepository {
    private static final int FOOTPRINT_BATCH_SIZE = 900;

    private static final String RAIL_STOPS_SQL = """
            SELECT
              TO_CHAR(SYSDATE, 'MM-DD-YY') AS RUN_DATE,
              SUBSTR(t.VC_TRAIN_NUM, 3, 4) AS TRAIN_NBR,
              DECODE(ri.SUPNUM, '1011', 'FP', '1000', 'BR', '3230', 'MW', '3322', 'DF') AS DCS_WHSE,
              ri.INVNUM AS LOAD_NBR,
              t.TRLR_NUM AS VEHICLE_ID,
              t.VC_CAR_SEQ AS SEQ,
              ap.ALT_PRTNUM AS SHORT_CODE,
              SUM(NVL(rl.EXPQTY, 0)) AS TOTAL_CASES
            FROM WMSP.TRLR t
            LEFT JOIN WMSP.RCVTRK rt ON rt.TRLR_ID = t.TRLR_ID
            LEFT JOIN WMSP.RCVINV ri ON ri.TRKNUM = rt.TRKNUM
            LEFT JOIN WMSP.RCVLIN rl ON rl.TRKNUM = rt.TRKNUM
            LEFT JOIN WMSP.ALT_PRTMST ap ON ap.PRTNUM = rl.PRTNUM
              AND ap.ALT_PRT_TYP = 'UPC'
            WHERE t.VC_TRAIN_NUM = ?
              AND ri.INVNUM IS NOT NULL
              AND ap.ALT_PRTNUM IS NOT NULL
              AND NVL(rl.EXPQTY, 0) > 0
            GROUP BY SUBSTR(t.VC_TRAIN_NUM, 3, 4),
              DECODE(ri.SUPNUM, '1011', 'FP', '1000', 'BR', '3230', 'MW', '3322', 'DF'),
              ri.INVNUM, t.TRLR_NUM, t.VC_CAR_SEQ, ap.ALT_PRTNUM
            ORDER BY t.VC_CAR_SEQ ASC, ri.INVNUM ASC, ap.ALT_PRTNUM ASC
            """;

    private static final String FOOTPRINTS_SQL = """
            SELECT
              ap.ALT_PRTNUM AS SHORT_CODE,
              ap.PRTNUM AS ITEM_NBR,
              p.PRTFAM AS PRTFAM,
              p.UC_PARS_FLG AS UC_PARS_FLG,
              MAX(CASE WHEN d.PAL_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_PALLET
            FROM WMSP.ALT_PRTMST ap
            LEFT JOIN WMSP.PRTMST p ON p.PRTNUM = ap.PRTNUM
              AND p.PRT_CLIENT_ID = ap.PRT_CLIENT_ID
            LEFT JOIN WMSP.PRTFTP pf ON pf.PRTNUM = ap.PRTNUM
              AND pf.PRT_CLIENT_ID = ap.PRT_CLIENT_ID
              AND pf.DEFFTP_FLG = 1
            LEFT JOIN WMSP.PRTFTP_DTL d ON d.PRTNUM = pf.PRTNUM
              AND d.PRT_CLIENT_ID = pf.PRT_CLIENT_ID
              AND d.WH_ID = pf.WH_ID
              AND d.FTPCOD = pf.FTPCOD
            WHERE ap.ALT_PRT_TYP = 'UPC'
              AND ap.ALT_PRTNUM IN (%s)
            GROUP BY ap.ALT_PRTNUM, ap.PRTNUM, p.PRTFAM, p.UC_PARS_FLG
            """;

    private final DataSource dataSource;
    private final RailStopRowMapper stopMapper;
    private final RailFootprintRowMapper footprintMapper;

    public OracleRailRepository(DataSource dataSource) {
        this(dataSource, new RailStopRowMapper(), new RailFootprintRowMapper());
    }

    OracleRailRepository(
            DataSource dataSource,
            RailStopRowMapper stopMapper,
            RailFootprintRowMapper footprintMapper
    ) {
        this.dataSource = Objects.requireNonNull(dataSource, "dataSource cannot be null");
        this.stopMapper = Objects.requireNonNull(stopMapper, "stopMapper cannot be null");
        this.footprintMapper = Objects.requireNonNull(footprintMapper, "footprintMapper cannot be null");
    }

    @Override
    public List<RailStopRecord> findStopsByTrainId(String trainId) {
        String normalizedTrainId = normalizeId(trainId, "trainId");
        Map<StopKey, MutableStop> stops = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(RAIL_STOPS_SQL)) {
            statement.setString(1, normalizedTrainId);
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    RailStopRecord row = stopMapper.map(rows);
                    StopKey key = new StopKey(row.sequence(), row.trainNumber(), row.vehicleId(), row.warehouse(), row.loadNumber());
                    stops.computeIfAbsent(key, ignored -> new MutableStop(row))
                            .items.addAll(row.items());
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load rail stops for " + normalizedTrainId, ex);
        }
        return stops.values().stream()
                .map(MutableStop::toRecord)
                .toList();
    }

    @Override
    public Map<String, RailFamilyFootprint> findFootprintsByShortCode(List<String> shortCodes) {
        List<String> normalizedShortCodes = normalizeShortCodes(shortCodes);
        if (normalizedShortCodes.isEmpty()) {
            return Map.of();
        }

        Map<String, RailFamilyFootprint> footprintsByShortCode = new LinkedHashMap<>();
        try (Connection connection = dataSource.getConnection()) {
            for (int start = 0; start < normalizedShortCodes.size(); start += FOOTPRINT_BATCH_SIZE) {
                int end = Math.min(start + FOOTPRINT_BATCH_SIZE, normalizedShortCodes.size());
                List<String> batch = normalizedShortCodes.subList(start, end);
                try (PreparedStatement statement = connection.prepareStatement(
                        String.format(FOOTPRINTS_SQL, placeholders(batch.size()))
                )) {
                    for (int i = 0; i < batch.size(); i++) {
                        statement.setString(i + 1, batch.get(i));
                    }
                    try (ResultSet rows = statement.executeQuery()) {
                        while (rows.next()) {
                            String shortCode = normalize(rows.getString("SHORT_CODE"));
                            footprintsByShortCode.put(shortCode, footprintMapper.map(rows));
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Failed to load rail footprints.", ex);
        }
        return footprintsByShortCode;
    }

    private static List<String> normalizeShortCodes(List<String> shortCodes) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String shortCode : shortCodes == null ? List.<String>of() : shortCodes) {
            String value = normalize(shortCode);
            if (!value.isBlank()) {
                normalized.add(value);
            }
        }
        return new ArrayList<>(normalized);
    }

    private static String placeholders(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive.");
        }
        return String.join(",", java.util.Collections.nCopies(count, "?"));
    }

    private static String normalizeId(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private record StopKey(String sequence, String trainNumber, String vehicleId, String warehouse, String loadNumber) {
    }

    private static final class MutableStop {
        private final RailStopRecord firstRow;
        private final List<RailItemQuantity> items = new ArrayList<>();

        private MutableStop(RailStopRecord firstRow) {
            this.firstRow = firstRow;
        }

        private RailStopRecord toRecord() {
            return new RailStopRecord(
                    firstRow.date(),
                    firstRow.sequence(),
                    firstRow.trainNumber(),
                    firstRow.vehicleId(),
                    firstRow.warehouse(),
                    firstRow.loadNumber(),
                    items
            );
        }
    }
}
