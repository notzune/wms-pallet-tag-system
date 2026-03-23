package com.tbg.wms.cli.gui.analyzers.unpicked;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;

import java.util.List;

public final class UnpickedPartialsColumns implements AnalyzerColumnSet<UnpickedPartialsRow> {

    @Override
    public List<Column<UnpickedPartialsRow>> columns() {
        return List.of(
                new Column<>("WH ID", UnpickedPartialsRow::warehouseId),
                new Column<>("APPT", UnpickedPartialsRow::appointment),
                new Column<>("ORDNUM", UnpickedPartialsRow::orderNumber),
                new Column<>("STCUST", UnpickedPartialsRow::soldToCustomer),
                new Column<>("ORDQTY", UnpickedPartialsRow::orderedQuantity),
                new Column<>("ALLOC", UnpickedPartialsRow::allocatedQuantity),
                new Column<>("UNALLOC", UnpickedPartialsRow::unallocatedQuantity),
                new Column<>("COMP QTY", UnpickedPartialsRow::completedQuantity),
                new Column<>("REMAIN QTY", UnpickedPartialsRow::remainingQuantity),
                new Column<>("ADRNAM", UnpickedPartialsRow::warehouseName),
                new Column<>("SOLD TO NAME", UnpickedPartialsRow::soldToName)
        );
    }
}
