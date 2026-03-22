package com.tbg.wms.db;

import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.NormalizationService;
import com.tbg.wms.core.model.Shipment;
import com.tbg.wms.core.model.ShipmentSkuFootprint;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Shipment-oriented Oracle query support used by {@link OracleDbQueryRepository}.
 *
 * <p>This type owns shipment hydration, pallet/line-item loading, and SKU footprint description
 * enrichment so the main repository can focus on higher-level query routing.</p>
 */
final class OracleShipmentQuerySupport {
    private final DataSource dataSource;
    private final PrtmstDescriptionColumnResolver prtmstColumnResolver;
    private final ShipmentDestinationSupport shipmentDestinationSupport = new ShipmentDestinationSupport();
    private final ShipmentDescriptionSupport shipmentDescriptionSupport = new ShipmentDescriptionSupport();
    private final ShipmentLpnHydrationSupport shipmentLpnHydrationSupport = new ShipmentLpnHydrationSupport();

    OracleShipmentQuerySupport(DataSource dataSource, PrtmstDescriptionColumnResolver prtmstColumnResolver) {
        this.dataSource = dataSource;
        this.prtmstColumnResolver = prtmstColumnResolver;
    }

    Shipment loadShipment(String shipmentId) throws SQLException {
        Shipment shipment = fetchShipmentHeader(shipmentId);
        if (shipment == null) {
            return null;
        }
        List<Lpn> lpns;
        try (Connection conn = dataSource.getConnection()) {
            lpns = shipmentLpnHydrationSupport.fetchLpnsWithLineItems(conn, shipmentId);
        }
        return new Shipment(
                shipment.getShipmentId(),
                shipment.getExternalId(),
                shipment.getOrderId(),
                shipment.getWarehouseId(),
                shipment.getShipToName(),
                shipment.getShipToAddress1(),
                shipment.getShipToAddress2(),
                shipment.getShipToAddress3(),
                shipment.getShipToCity(),
                shipment.getShipToState(),
                shipment.getShipToZip(),
                shipment.getShipToCountry(),
                shipment.getShipToPhone(),
                shipment.getCarrierCode(),
                shipment.getServiceLevel(),
                shipment.getDocumentNumber(),
                shipment.getTrackingNumber(),
                shipment.getDestinationLocation(),
                shipment.getCustomerPo(),
                shipment.getLocationNumber(),
                shipment.getDepartmentNumber(),
                shipment.getStopId(),
                shipment.getStopSequence(),
                shipment.getCarrierMoveId(),
                shipment.getProNumber(),
                shipment.getBolNumber(),
                shipment.getStatus(),
                shipment.getShipDate(),
                shipment.getDeliveryDate(),
                shipment.getCreatedDate(),
                lpns
        );
    }

    List<ShipmentSkuFootprint> loadShipmentSkuFootprints(String shipmentId) throws SQLException {
        List<ShipmentSkuFootprint> rows = new ArrayList<>();
        String sql = "WITH sku_units AS (" +
                "  SELECT " +
                "    ol.PRTNUM AS PRTNUM, " +
                "    MAX(ol.PRT_CLIENT_ID) AS PRT_CLIENT_ID, " +
                "    MAX(s.WH_ID) AS WH_ID, " +
                "    MAX(ol.CSTPRT) AS ITEM_DESCRIPTION, " +
                "    SUM(COALESCE(" +
                "      NULLIF(sl.SHPQTY, 0), " +
                "      NULLIF(sl.STGQTY, 0), " +
                "      NULLIF(sl.PCKQTY, 0), " +
                "      NULLIF(sl.INPQTY, 0), " +
                "      NULLIF(sl.TOT_PLN_QTY, 0), " +
                "      NULLIF(ol.ORDQTY, 0), " +
                "      0)) AS TOTAL_UNITS " +
                "  FROM WMSP.SHIPMENT_LINE sl " +
                "  INNER JOIN WMSP.SHIPMENT s ON s.SHIP_ID = sl.SHIP_ID " +
                "  INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM " +
                "    AND sl.ORDLIN = ol.ORDLIN AND sl.ORDSLN = ol.ORDSLN AND sl.CLIENT_ID = ol.CLIENT_ID " +
                "  WHERE sl.SHIP_ID = ? " +
                "  GROUP BY ol.PRTNUM" +
                ") " +
                "SELECT " +
                "  su.PRTNUM, " +
                "  su.PRT_CLIENT_ID, " +
                "  su.WH_ID, " +
                "  su.ITEM_DESCRIPTION, " +
                "  su.TOTAL_UNITS, " +
                "  MAX(CASE WHEN d.CAS_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_CASE, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.UNTQTY END) AS UNITS_PER_PALLET, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.LEN END) AS PALLET_LEN, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.WID END) AS PALLET_WID, " +
                "  MAX(CASE WHEN d.PAL_FLG = 1 THEN d.HGT END) AS PALLET_HGT " +
                "FROM sku_units su " +
                "LEFT JOIN WMSP.PRTFTP pf ON pf.PRTNUM = su.PRTNUM " +
                "  AND pf.PRT_CLIENT_ID = su.PRT_CLIENT_ID " +
                "  AND pf.WH_ID = su.WH_ID " +
                "  AND pf.DEFFTP_FLG = 1 " +
                "LEFT JOIN WMSP.PRTFTP_DTL d ON d.PRTNUM = pf.PRTNUM " +
                "  AND d.PRT_CLIENT_ID = pf.PRT_CLIENT_ID " +
                "  AND d.WH_ID = pf.WH_ID " +
                "  AND d.FTPCOD = pf.FTPCOD " +
                "GROUP BY su.PRTNUM, su.PRT_CLIENT_ID, su.WH_ID, su.ITEM_DESCRIPTION, su.TOTAL_UNITS";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            List<String> descriptionColumns = prtmstColumnResolver.getColumns(conn);
            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String sku = NormalizationService.normalizeSku(rs.getString("PRTNUM"));
                    String fallbackDescription = NormalizationService.normalizeString(rs.getString("ITEM_DESCRIPTION"));
                    String itemDescription = resolveItemDescription(
                            conn,
                            sku,
                            NormalizationService.normalizeString(rs.getString("PRT_CLIENT_ID")),
                            NormalizationService.normalizeString(rs.getString("WH_ID")),
                            fallbackDescription,
                            descriptionColumns
                    );

                    rows.add(new ShipmentSkuFootprint(
                            sku,
                            itemDescription,
                            rs.getInt("TOTAL_UNITS"),
                            nullableInt(rs, "UNITS_PER_CASE"),
                            nullableInt(rs, "UNITS_PER_PALLET"),
                            nullableDouble(rs, "PALLET_LEN"),
                            nullableDouble(rs, "PALLET_WID"),
                            nullableDouble(rs, "PALLET_HGT")
                    ));
                }
            }
        }
        return rows;
    }

    private Shipment fetchShipmentHeader(String shipmentId) throws SQLException {
        String sql = "SELECT " +
                "s.SHIP_ID, s.HOST_EXT_ID, s.WH_ID, s.SHPSTS, " +
                "s.CARCOD, s.SRVLVL, s.DOC_NUM, s.TRACK_NUM, s.STOP_ID, s.DSTLOC, " +
                "s.EARLY_SHPDTE, s.LATE_DLVDTE, s.ADDDTE, s.TMS_MOVE_ID, " +
                "a.ADRNAM, a.ADRLN1, a.ADRLN2, a.ADRLN3, " +
                "a.ADRCTY, a.ADRSTC, a.ADRPSZ, a.CTRY_NAME, a.PHNNUM, a.ATTN_NAME, a.HOST_EXT_ID AS ADR_HOST_EXT_ID, " +
                "(SELECT MAX(o2.CPONUM) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.CPONUM IS NOT NULL) AS CPONUM, " +
                "(SELECT MAX(o2.DEST_NUM) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.DEST_NUM IS NOT NULL) AS DEST_NUM, " +
                "(SELECT MAX(o2.VC_DEST_ID) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.VC_DEST_ID IS NOT NULL) AS VC_DEST_ID, " +
                "(SELECT MAX(o2.DEPTNO) FROM WMSP.SHIPMENT_LINE sl2 " +
                "  INNER JOIN WMSP.ORD o2 ON sl2.ORDNUM = o2.ORDNUM AND sl2.CLIENT_ID = o2.CLIENT_ID " +
                "  WHERE sl2.SHIP_ID = s.SHIP_ID AND o2.DEPTNO IS NOT NULL) AS DEPTNO, " +
                "st.STOP_SEQ, " +
                "cm.DOC_NUM as CARRIER_BOL, cm.TRACK_NUM as CARRIER_PRO " +
                "FROM WMSP.SHIPMENT s " +
                "INNER JOIN WMSP.ADRMST a ON s.RT_ADR_ID = a.ADR_ID " +
                "LEFT JOIN WMSP.STOP st ON s.STOP_ID = st.STOP_ID " +
                "LEFT JOIN WMSP.CAR_MOVE cm ON s.TMS_MOVE_ID = cm.CAR_MOVE_ID " +
                "WHERE s.SHIP_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }

                LocalDateTime shipDate = nullableLocalDateTime(rs, "EARLY_SHPDTE");
                LocalDateTime deliveryDate = nullableLocalDateTime(rs, "LATE_DLVDTE");
                LocalDateTime createdDate = nullableLocalDateTime(rs, "ADDDTE");
                if (createdDate == null) {
                    createdDate = LocalDateTime.now();
                }

                Integer stopSeq = nullableInt(rs, "STOP_SEQ");
                String destinationNumber = shipmentDestinationSupport.resolveLocationNumber(
                        rs.getString("DEST_NUM"),
                        rs.getString("VC_DEST_ID"),
                        rs.getString("ADRNAM"),
                        rs.getString("ADR_HOST_EXT_ID")
                );

                return new Shipment(
                        NormalizationService.normalizeString(rs.getString("SHIP_ID")),
                        NormalizationService.normalizeString(rs.getString("HOST_EXT_ID")),
                        extractFirstOrderNumber(conn, shipmentId),
                        NormalizationService.normalizeString(rs.getString("WH_ID")),
                        NormalizationService.normalizeString(rs.getString("ADRNAM")),
                        NormalizationService.normalizeString(rs.getString("ADRLN1")),
                        NormalizationService.normalizeString(rs.getString("ADRLN2")),
                        NormalizationService.normalizeString(rs.getString("ADRLN3")),
                        NormalizationService.normalizeString(rs.getString("ADRCTY")),
                        NormalizationService.normalizeToUppercase(rs.getString("ADRSTC")),
                        NormalizationService.normalizeString(rs.getString("ADRPSZ")),
                        NormalizationService.normalizeString(rs.getString("CTRY_NAME")),
                        NormalizationService.normalizeString(rs.getString("PHNNUM")),
                        NormalizationService.normalizeCarrierCode(rs.getString("CARCOD")),
                        NormalizationService.normalizeToUppercase(rs.getString("SRVLVL")),
                        NormalizationService.normalizeString(rs.getString("DOC_NUM")),
                        NormalizationService.normalizeString(rs.getString("TRACK_NUM")),
                        NormalizationService.normalizeOptionalStagingLocation(rs.getString("DSTLOC")),
                        NormalizationService.normalizeString(rs.getString("CPONUM")),
                        destinationNumber,
                        NormalizationService.normalizeString(rs.getString("DEPTNO")),
                        NormalizationService.normalizeString(rs.getString("STOP_ID")),
                        stopSeq,
                        NormalizationService.normalizeString(rs.getString("TMS_MOVE_ID")),
                        NormalizationService.normalizeString(rs.getString("CARRIER_PRO")),
                        NormalizationService.normalizeString(rs.getString("CARRIER_BOL")),
                        NormalizationService.normalizeToUppercase(rs.getString("SHPSTS")),
                        shipDate,
                        deliveryDate,
                        createdDate,
                        new ArrayList<>()
                );
            }
        }
    }

    private String extractFirstOrderNumber(Connection conn, String shipmentId) throws SQLException {
        String sql = "SELECT sl.ORDNUM FROM WMSP.SHIPMENT_LINE sl WHERE sl.SHIP_ID = ? AND ROWNUM <= 1";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return NormalizationService.normalizeString(rs.getString("ORDNUM"));
                }
            }
        }
        return null;
    }

    private String resolveItemDescription(Connection conn,
                                          String sku,
                                          String prtClientId,
                                          String whId,
                                          String fallbackDescription,
                                          List<String> descriptionColumns) {
        return shipmentDescriptionSupport.resolveItemDescription(conn, sku, prtClientId, whId, fallbackDescription, descriptionColumns);
    }

    private static Integer nullableInt(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private static Double nullableDouble(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        return rs.wasNull() ? null : value;
    }

    private static LocalDateTime nullableLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

}
