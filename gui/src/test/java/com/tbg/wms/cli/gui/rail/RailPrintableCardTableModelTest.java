/*
 * Copyright (c) 2026 Tropicana Brands Group
 */
package com.tbg.wms.cli.gui.rail;

import com.tbg.wms.core.rail.RailCarCard;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailPrintableCardTableModelTest {

    @Test
    void setCardsShouldDefaultAllRowsToPrintable() {
        RailPrintableCardTableModel model = new RailPrintableCardTableModel();

        model.setCards(List.of(card("TRAIN1", "1"), card("TRAIN1", "2")));

        assertEquals(2, model.getRowCount());
        assertEquals(Boolean.TRUE, model.getValueAt(0, 0));
        assertEquals(2, model.selectedCount());
        assertEquals(2, model.selectedCards().size());
    }

    @Test
    void checkboxColumnShouldBeEditableWithoutEditingCardData() {
        RailPrintableCardTableModel model = new RailPrintableCardTableModel();
        model.setCards(List.of(card("TRAIN1", "1")));

        model.setValueAt(Boolean.FALSE, 0, 0);

        assertFalse(Boolean.TRUE.equals(model.getValueAt(0, 0)));
        assertTrue(model.selectedCards().isEmpty());
        assertTrue(model.isCellEditable(0, 0));
        assertFalse(model.isCellEditable(0, 1));
    }

    @Test
    void bulkActionsShouldUpdatePrintableRows() {
        RailPrintableCardTableModel model = new RailPrintableCardTableModel();
        model.setCards(List.of(card("TRAIN1", "1"), card("TRAIN1", "2"), card("TRAIN1", "3")));

        model.clearAllPrintable();
        assertEquals(0, model.selectedCount());

        model.setAllPrintable();
        assertEquals(3, model.selectedCount());

        model.invertPrintable();
        assertEquals(0, model.selectedCount());
    }

    @Test
    void togglePrintableRowsShouldAffectOnlyProvidedModelRows() {
        RailCarCard first = card("TRAIN1", "1");
        RailCarCard second = card("TRAIN1", "2");
        RailPrintableCardTableModel model = new RailPrintableCardTableModel();
        model.setCards(List.of(first, second));

        model.togglePrintableRows(new int[]{1});

        assertSame(first, model.selectedCards().get(0));
        assertEquals(1, model.selectedCount());
    }

    @Test
    void consistColumnShouldExposeMarkerText() {
        RailPrintableCardTableModel model = new RailPrintableCardTableModel();
        RailCarCard card = new RailCarCard("TRAIN1", "1", "CAR1", "LOAD1",
                "TRAIN FP LOAD1", "06-01-26", "D-4 C-3",
                List.of(), 1, 2, 0, List.of(), List.of());

        model.setCards(List.of(card));

        assertEquals("CONSIST", model.getColumnName(3));
        assertEquals("D-4 C-3", model.getValueAt(0, 3));
    }

    private static RailCarCard card(String train, String sequence) {
        return new RailCarCard(train, sequence, "CAR" + sequence, "LOAD" + sequence,
                List.of(), 1, 2, 0, List.of(), List.of());
    }
}
