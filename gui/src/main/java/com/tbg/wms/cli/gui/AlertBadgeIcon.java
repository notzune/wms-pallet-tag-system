package com.tbg.wms.cli.gui;

import javax.swing.*;
import java.awt.*;

/**
 * Small circular alert badge used to indicate update availability.
 */
public final class AlertBadgeIcon implements Icon {
    private final int size;

    public AlertBadgeIcon(int size) {
        this.size = size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g2 = (Graphics2D) graphics.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(200, 36, 36));
            g2.fillOval(x, y, size, size);
            g2.setColor(Color.WHITE);
            g2.setFont(component.getFont().deriveFont(Font.BOLD, Math.max(10f, size - 5f)));
            FontMetrics metrics = g2.getFontMetrics();
            String text = "!";
            int textX = x + ((size - metrics.stringWidth(text)) / 2);
            int textY = y + ((size - metrics.getHeight()) / 2) + metrics.getAscent();
            g2.drawString(text, textX, textY);
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
