package com.tbg.wms.cli.gui.analyzers.dockdoors;

import com.tbg.wms.cli.gui.analyzers.AnalyzerColumnSet;

import java.util.List;

public final class AllDockDoorsColumns implements AnalyzerColumnSet<AllDockDoorsRow> {

    @Override
    public List<Column<AllDockDoorsRow>> columns() {
        return List.of(
                new Column<>("Door", AllDockDoorsRow::door),
                new Column<>("D/L", AllDockDoorsRow::dropLive),
                new Column<>("Inb", AllDockDoorsRow::inboundFlag),
                new Column<>("Trailer", AllDockDoorsRow::trailerId),
                new Column<>("Move", AllDockDoorsRow::moveNumber),
                new Column<>("Appt", AllDockDoorsRow::appointmentTime),
                new Column<>("Check In", AllDockDoorsRow::checkInTime),
                new Column<>("Moved", AllDockDoorsRow::movedToDoorAt),
                new Column<>("Min At Door", AllDockDoorsRow::minutesAtDoor),
                new Column<>("Shp %", AllDockDoorsRow::shipmentPercentComplete),
                new Column<>("Appt Over", AllDockDoorsRow::appointmentOverHours),
                new Column<>("Stops", AllDockDoorsRow::stops),
                new Column<>("Short", AllDockDoorsRow::shortFlag),
                new Column<>("Customer", AllDockDoorsRow::customer),
                new Column<>("Sold To", AllDockDoorsRow::soldToNumber),
                new Column<>("Airbag", AllDockDoorsRow::airbagFlag),
                new Column<>("Rossi Pals", AllDockDoorsRow::rossiPallets),
                new Column<>("Rossi Cmppcks", AllDockDoorsRow::rossiCompletedPicks)
        );
    }
}
