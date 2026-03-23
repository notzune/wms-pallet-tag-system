package com.tbg.wms.cli.gui.analyzers;

import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnalyzerDialogTest {

    @Test
    void renderer_shouldApplyAnalyzerRowColors() {
        AnalyzerRowStyler<String> styler = row -> AnalyzerRowStyle.of(Color.YELLOW, Color.BLACK);
        AnalyzerTableModel<String> model = new AnalyzerTableModel<>(row -> row, List.of("value"));
        model.setRows(List.of("value"));
        JTable table = new JTable(model);
        AnalyzerTableCellRenderer<String> renderer = new AnalyzerTableCellRenderer<>(model, styler);

        Component component = renderer.getTableCellRendererComponent(table, "value", false, false, 0, 0);

        assertEquals(Color.YELLOW, component.getBackground());
        assertEquals(Color.BLACK, component.getForeground());
    }
}
