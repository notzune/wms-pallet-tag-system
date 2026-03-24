/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.7.5
 */
package com.tbg.wms.cli.gui;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Font;
import java.util.Objects;

/**
 * Builds preview UI sections for shipment and carrier-move rendering.
 */
final class PreviewRenderSupport {

    private final LabelPreviewFormatter previewFormatter;
    private final int maxPreviewStops;
    private final int maxPreviewShipmentsPerStop;
    private final int maxPreviewSkuRowsPerShipment;

    PreviewRenderSupport(
            LabelPreviewFormatter previewFormatter,
            int maxPreviewStops,
            int maxPreviewShipmentsPerStop,
            int maxPreviewSkuRowsPerShipment
    ) {
        this.previewFormatter = Objects.requireNonNull(previewFormatter, "previewFormatter cannot be null");
        this.maxPreviewStops = maxPreviewStops;
        this.maxPreviewShipmentsPerStop = maxPreviewShipmentsPerStop;
        this.maxPreviewSkuRowsPerShipment = maxPreviewSkuRowsPerShipment;
    }

    void renderShipmentPreview(
            JPanel shipmentPreviewPanel,
            JTextArea shipmentArea,
            JComponent labelSelectionPanel
    ) {
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.add(labelSelectionPanel);
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    void renderCarrierMovePreview(
            JPanel shipmentPreviewPanel,
            JTextArea shipmentArea,
            JComponent labelSelectionPanel,
            AdvancedPrintWorkflowService.PreparedCarrierMoveJob job
    ) {
        Objects.requireNonNull(job, "job cannot be null");
        shipmentPreviewPanel.removeAll();
        shipmentPreviewPanel.add(shipmentArea);
        shipmentPreviewPanel.add(labelSelectionPanel);
        addStopPreviewSections(shipmentPreviewPanel, job);
        shipmentPreviewPanel.revalidate();
        shipmentPreviewPanel.repaint();
    }

    int addStopPreviewSections(JPanel shipmentPreviewPanel, AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        int shown = 0;
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            if (shown >= maxPreviewStops) {
                break;
            }
            shipmentPreviewPanel.add(buildStopPreviewSection(stop));
            shown++;
        }
        return shown;
    }

    JComponent buildStopPreviewSection(AdvancedPrintWorkflowService.PreparedStopGroup stop) {
        Objects.requireNonNull(stop, "stop cannot be null");
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setBorder(BorderFactory.createEmptyBorder(6, 0, 8, 0));

        String label = previewFormatter.stopPreviewLabel(stop);
        JToggleButton toggle = new JToggleButton(label + "  [expanded]", true);
        toggle.setFocusPainted(false);
        toggle.setHorizontalAlignment(SwingConstants.LEFT);
        container.add(toggle, BorderLayout.NORTH);

        JTextArea details = new JTextArea();
        details.setEditable(false);
        details.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        details.setRows(Math.max(16, stop.getShipmentJobs().size() * 16));
        details.setText(previewFormatter.buildStopDetailsText(
                stop,
                maxPreviewShipmentsPerStop,
                maxPreviewSkuRowsPerShipment
        ));

        JScrollPane detailsScroll = new JScrollPane(details);
        detailsScroll.setBorder(BorderFactory.createEmptyBorder());
        container.add(detailsScroll, BorderLayout.CENTER);

        toggle.addActionListener(e -> {
            boolean expanded = toggle.isSelected();
            toggle.setText(label + (expanded ? "  [expanded]" : "  [collapsed]"));
            detailsScroll.setVisible(expanded);
            container.revalidate();
            container.repaint();
        });
        return container;
    }
}
