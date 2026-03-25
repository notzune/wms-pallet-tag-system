package com.tbg.wms.cli.gui.analyzers.openloads;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;

import java.util.List;

public final class OpenLoadsColumns implements AnalyzerColumnSet<OpenLoadsRow> {

    @Override
    public List<Column<OpenLoadsRow>> columns() {
        return List.of(
                new Column<>("Move", OpenLoadsRow::carrierMoveId),
                new Column<>("Ship ID", OpenLoadsRow::shipmentId),
                new Column<>("Order", OpenLoadsRow::orderNumber),
                new Column<>("Appt", OpenLoadsRow::appointment),
                new Column<>("Customer", OpenLoadsRow::customer),
                new Column<>("Status", OpenLoadsRow::shipmentStatus),
                new Column<>("D/L", OpenLoadsRow::dropLive),
                new Column<>("Trailer", OpenLoadsRow::trailerNumber),
                new Column<>("Yard", OpenLoadsRow::yardLocation),
                new Column<>("Picks", OpenLoadsRow::casePicks),
                new Column<>("Done", OpenLoadsRow::completedCasePicks),
                new Column<>("Remain", OpenLoadsRow::picksRemaining),
                new Column<>("Platform", OpenLoadsRow::platform),
                new Column<>("Staged", OpenLoadsRow::staged),
                new Column<>("Short", OpenLoadsRow::shortFlag)
        );
    }
}
