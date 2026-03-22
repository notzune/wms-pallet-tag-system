package com.tbg.wms.db;

import com.tbg.wms.core.model.LineItem;
import com.tbg.wms.core.model.Lpn;
import com.tbg.wms.core.model.NormalizationService;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads shipment LPN rows and merges inventory detail rows into stable LPN models.
 */
final class ShipmentLpnHydrationSupport {
    private static final String DEFAULT_LINE_ITEM_UOM = "EA";
    private static final String SHIPMENT_LINE_ITEMS_SQL = "SELECT "
            + "pwd.SHIP_CTNNUM AS LODNUM, "
            + "sl.SHIP_LINE_ID, sl.ORDNUM, sl.ORDLIN, sl.ORDSLN, sl.CONS_BATCH, "
            + "COALESCE("
            + "NULLIF(sl.SHPQTY, 0), "
            + "NULLIF(sl.STGQTY, 0), "
            + "NULLIF(sl.PCKQTY, 0), "
            + "NULLIF(sl.INPQTY, 0), "
            + "NULLIF(sl.TOT_PLN_QTY, 0), "
            + "NULLIF(ol.ORDQTY, 0), "
            + "0) AS EFFECTIVE_QTY, "
            + "ol.PRTNUM, ol.CSTPRT, ol.ORDQTY, ol.SALES_ORDNUM, ol.UNTPAK, "
            + "CAST(NULL AS VARCHAR2(1)) AS LNGDSC, ol.CSTPRT AS SRTDSC, 0 AS NETWGT "
            + "FROM WMSP.PCKWRK_DTL pwd "
            + "INNER JOIN WMSP.SHIPMENT_LINE sl ON pwd.SHIP_LINE_ID = sl.SHIP_LINE_ID "
            + "INNER JOIN WMSP.ORD_LINE ol ON sl.ORDNUM = ol.ORDNUM "
            + "  AND sl.ORDLIN = ol.ORDLIN AND sl.ORDSLN = ol.ORDSLN AND sl.CLIENT_ID = ol.CLIENT_ID "
            + "WHERE pwd.SHIP_ID = ? "
            + "ORDER BY pwd.SHIP_CTNNUM, sl.ORDLIN, sl.ORDSLN";

    List<Lpn> fetchLpnsWithLineItems(Connection conn, String shipmentId) throws SQLException {
        String lpnSql = "SELECT DISTINCT "
                + "il.LODNUM, il.LODUCC, il.STOLOC, il.LODWGT, "
                + "id.LOTNUM, id.SUP_LOTNUM, id.MANDTE, id.EXPIRE_DTE "
                + "FROM WMSP.PCKWRK_DTL pwd "
                + "INNER JOIN WMSP.INVDTL id ON pwd.DTLNUM = id.DTLNUM "
                + "INNER JOIN WMSP.INVSUB isub ON id.SUBNUM = isub.SUBNUM "
                + "INNER JOIN WMSP.INVLOD il ON isub.LODNUM = il.LODNUM "
                + "WHERE pwd.SHIP_ID = ? "
                + "ORDER BY il.LODNUM";

        try (PreparedStatement stmt = conn.prepareStatement(lpnSql)) {
            Map<String, List<LineItem>> lineItemsByLpn = fetchLineItemsByLpn(conn, shipmentId);
            LinkedHashMap<String, MutableLpnRow> lpnsById = new LinkedHashMap<>();

            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String normalizedLpnId = NormalizationService.normalizeString(rs.getString("LODNUM"));
                    if (normalizedLpnId.isBlank()) {
                        continue;
                    }
                    MutableLpnRow row = lpnsById.computeIfAbsent(
                            normalizedLpnId,
                            ignored -> new MutableLpnRow(
                                    normalizedLpnId,
                                    NormalizationService.normalizeString(shipmentId),
                                    lineItemsByLpn.getOrDefault(normalizedLpnId, List.of())
                            )
                    );
                    row.merge(rs);
                }
            }
            List<Lpn> lpns = new ArrayList<>(lpnsById.size());
            for (MutableLpnRow row : lpnsById.values()) {
                lpns.add(row.toLpn());
            }
            return lpns;
        }
    }

    private Map<String, List<LineItem>> fetchLineItemsByLpn(Connection conn, String shipmentId) throws SQLException {
        Map<String, List<LineItem>> lineItemsByLpn = new HashMap<>();
        try (PreparedStatement stmt = conn.prepareStatement(SHIPMENT_LINE_ITEMS_SQL)) {
            stmt.setString(1, shipmentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String lpnId = NormalizationService.normalizeString(rs.getString("LODNUM"));
                    LineItem item = new LineItem(
                            NormalizationService.normalizeString(rs.getString("ORDLIN")),
                            NormalizationService.normalizeString(rs.getString("ORDSLN")),
                            NormalizationService.normalizeSku(rs.getString("PRTNUM")),
                            NormalizationService.normalizeString(rs.getString("SRTDSC")),
                            NormalizationService.normalizeString(rs.getString("CSTPRT")),
                            NormalizationService.normalizeString(rs.getString("ORDNUM")),
                            NormalizationService.normalizeString(rs.getString("CONS_BATCH")),
                            NormalizationService.normalizeString(rs.getString("SALES_ORDNUM")),
                            rs.getInt("EFFECTIVE_QTY"),
                            rs.getInt("UNTPAK"),
                            DEFAULT_LINE_ITEM_UOM,
                            rs.getDouble("NETWGT"),
                            null,
                            null,
                            null
                    );
                    lineItemsByLpn.computeIfAbsent(lpnId, ignored -> new ArrayList<>()).add(item);
                }
            }
        }
        return lineItemsByLpn;
    }

    private static LocalDate nullableLocalDate(ResultSet rs, String column) throws SQLException {
        Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    private static String preferNonBlank(String currentValue, String candidateValue) {
        return currentValue == null || currentValue.isBlank() ? candidateValue : currentValue;
    }

    private static final class MutableLpnRow {
        private final String lpnId;
        private final String shipmentId;
        private final List<LineItem> lineItems;
        private String sscc;
        private double weight;
        private String stagingLocation;
        private String warehouseLot;
        private String customerLot;
        private LocalDate manufactureDate;
        private LocalDate bestByDate;

        private MutableLpnRow(String lpnId, String shipmentId, List<LineItem> lineItems) {
            this.lpnId = lpnId;
            this.shipmentId = shipmentId;
            this.lineItems = List.copyOf(lineItems);
        }

        private void merge(ResultSet rs) throws SQLException {
            sscc = preferNonBlank(sscc, NormalizationService.normalizeBarcode(rs.getString("LODUCC")));
            if (weight == 0.0d) {
                weight = rs.getDouble("LODWGT");
            }
            stagingLocation = preferNonBlank(stagingLocation,
                    NormalizationService.normalizeOptionalStagingLocation(rs.getString("STOLOC")));
            warehouseLot = preferNonBlank(warehouseLot, NormalizationService.normalizeString(rs.getString("LOTNUM")));
            customerLot = preferNonBlank(customerLot, NormalizationService.normalizeString(rs.getString("SUP_LOTNUM")));
            if (manufactureDate == null) {
                manufactureDate = nullableLocalDate(rs, "MANDTE");
            }
            if (bestByDate == null) {
                bestByDate = nullableLocalDate(rs, "EXPIRE_DTE");
            }
        }

        private Lpn toLpn() {
            return new Lpn(
                    lpnId,
                    shipmentId,
                    sscc,
                    0,
                    0,
                    weight,
                    stagingLocation,
                    warehouseLot,
                    customerLot,
                    manufactureDate,
                    bestByDate,
                    lineItems
            );
        }
    }
}
