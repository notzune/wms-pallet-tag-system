package com.tbg.wms.cli.gui;

import javax.swing.Icon;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Small circular status LED for footer connectivity state.
 */
final class StatusLedIcon implements Icon {
    private final int size;
    private final Color fillColor;

    StatusLedIcon(int size, Color fillColor) {
        this.size = size;
        this.fillColor = fillColor;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(fillColor);
            g2.fillOval(x + 1, y + 1, size - 2, size - 2);
            g2.setColor(new Color(255, 255, 255, 120));
            g2.drawOval(x + 1, y + 1, size - 2, size - 2);
        } finally {
            g2.dispose();
        }
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }
}
