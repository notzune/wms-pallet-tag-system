/*
 * Copyright (c) 2026 Tropicana Brands Group
 *
 * @author Zeyad Rashed
 * @email zeyad.rashed@tropicana.com
 * @since 1.6.0
 */
package com.tbg.wms.cli.gui;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds standardized ZPL payloads for shipment/stop/final info tags.
 */
final class InfoTagZplBuilder {

    private static final Pattern MULTI_SPACE_PATTERN = Pattern.compile("\\s+");

    private InfoTagZplBuilder() {
    }

    static String buildStopInfoTag(String carrierMoveId,
                                   int stopPosition,
                                   int totalStops,
                                   Integer stopSequence,
                                   List<String> shipmentIds,
                                   List<LabelWorkflowService.PreparedJob> jobs) {
        LabelWorkflowService.PreparedJob first = jobs.isEmpty() ? null : jobs.get(0);
        String shipTo = first == null ? "-" : compact(first.getShipment().getShipToName());
        String addr = first == null ? "-"
                : compact(first.getShipment().getShipToAddress1() + " "
                + first.getShipment().getShipToCity() + ", "
                + first.getShipment().getShipToState() + " "
                + first.getShipment().getShipToZip());
        String seqText = stopSequence == null ? "-" : stopSequence.toString();
        String shipments = shipmentIds.isEmpty() ? "-" : String.join(", ", shipmentIds);
        return "^XA\n^CI28\n^PW812\n^LL1218\n^LH0,0\n"
                + "^FO16,16^GB780,1186,6^FS\n"
                + "^FO30,40^A0N,58,58^FDINFO TAG - DO NOT APPLY^FS\n"
                + "^FO30,120^A0N,32,32^FDCARRIER MOVE: " + esc(carrierMoveId) + "^FS\n"
                + "^FO30,170^A0N,32,32^FDSTOP " + stopPosition + " OF " + totalStops + " (SEQ " + esc(seqText) + ")^FS\n"
                + "^FO30,220^A0N,28,28^FB740,3,6,L,0^FDSHIPMENTS: " + esc(shipments) + "^FS\n"
                + "^FO30,330^A0N,28,28^FB740,2,6,L,0^FDSHIP TO: " + esc(shipTo) + "^FS\n"
                + "^FO30,420^A0N,24,24^FB740,3,4,L,0^FD" + esc(addr) + "^FS\n"
                + "^FO30,1080^A0N,34,34^FDSORT PACKET FOR STOP " + stopPosition + "^FS\n^XZ\n";
    }

    static String buildFinalInfoTag(AdvancedPrintWorkflowService.PreparedCarrierMoveJob job) {
        StringBuilder list = new StringBuilder();
        for (AdvancedPrintWorkflowService.PreparedStopGroup stop : job.getStopGroups()) {
            if (list.length() > 0) {
                list.append("\\&");
            }
            list.append("Stop ").append(stop.getStopPosition()).append(": ").append(buildShipmentIdList(stop.getShipmentJobs()));
        }
        return "^XA\n^CI28\n^PW812\n^LL1218\n^LH0,0\n"
                + "^FO16,16^GB780,1186,6^FS\n"
                + "^FO30,40^A0N,58,58^FDFINAL INFO TAG - DO NOT APPLY^FS\n"
                + "^FO30,120^A0N,32,32^FDCARRIER MOVE: " + esc(job.getCarrierMoveId()) + "^FS\n"
                + "^FO30,170^A0N,32,32^FDTOTAL STOPS: " + job.getTotalStops() + "^FS\n"
                + "^FO30,230^A0N,26,26^FB740,28,4,L,0^FD" + esc(list.toString()) + "^FS\n"
                + "^FO30,1040^A0N,28,28^FDDocs: CHANGELOG^FS\n"
                + "^FO30,1080^A0N,34,34^FDEND OF CARRIER MOVE " + esc(job.getCarrierMoveId()) + "^FS\n^XZ\n";
    }

    static String buildShipmentInfoTag(LabelWorkflowService.PreparedJob job) {
        String shipmentId = job.getShipment().getShipmentId();
        String shipTo = compact(job.getShipment().getShipToName());
        String addr = compact(job.getShipment().getShipToAddress1() + " " + job.getShipment().getShipToCity() + ", "
                + job.getShipment().getShipToState() + " " + job.getShipment().getShipToZip());
        String carrierMove = job.getShipment().getCarrierMoveId() == null || job.getShipment().getCarrierMoveId().isBlank()
                ? "-"
                : job.getShipment().getCarrierMoveId();
        int labels = job.getLpnsForLabels().size();

        return "^XA\n^CI28\n^PW812\n^LL1218\n^LH0,0\n"
                + "^FO16,16^GB780,1186,6^FS\n"
                + "^FO30,40^A0N,58,58^FDINFO TAG - DO NOT APPLY^FS\n"
                + "^FO30,120^A0N,32,32^FDSHIPMENT ID: " + esc(shipmentId) + "^FS\n"
                + "^FO30,170^A0N,32,32^FDCARRIER MOVE: " + esc(carrierMove) + "^FS\n"
                + "^FO30,220^A0N,32,32^FDLABELS IN JOB: " + labels + "^FS\n"
                + "^FO30,280^A0N,28,28^FB740,2,6,L,0^FDSHIP TO: " + esc(shipTo) + "^FS\n"
                + "^FO30,360^A0N,24,24^FB740,3,4,L,0^FD" + esc(addr) + "^FS\n"
                + "^FO30,1080^A0N,34,34^FDSHIPMENT PACKET SUMMARY^FS\n^XZ\n";
    }

    private static String buildShipmentIdList(List<LabelWorkflowService.PreparedJob> jobs) {
        if (jobs.isEmpty()) {
            return "-";
        }
        StringBuilder ids = new StringBuilder();
        for (LabelWorkflowService.PreparedJob job : jobs) {
            if (ids.length() > 0) {
                ids.append(", ");
            }
            ids.append(job.getShipmentId());
        }
        return ids.toString();
    }

    private static String compact(String value) {
        if (value == null) {
            return "-";
        }
        return MULTI_SPACE_PATTERN.matcher(value.trim()).replaceAll(" ");
    }

    private static String esc(String value) {
        if (value == null) {
            return " ";
        }
        return value.replace("~", "~~").replace("^", "~~^").replace("{", "{{").replace("}", "}}");
    }
}
