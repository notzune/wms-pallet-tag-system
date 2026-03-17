package com.tbg.wms.cli.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Shared GUI helpers for workflow-scoped printer target lists.
 */
public final class GuiPrinterTargetSupport {
    public static final String FILE_PRINTER_ID = "FILE";
    private static final Set<String> LABEL_SCREEN_PRINTER_IDS = orderedSet("OFFICE", "DISPATCH");
    private static final Set<String> RAIL_TOOL_PRINTER_IDS = orderedSet("ORDER_PICK", "RAIL_OFFICE");

    private GuiPrinterTargetSupport() {
    }

    public static List<LabelWorkflowService.PrinterOption> filterLabelScreenPrinters(List<LabelWorkflowService.PrinterOption> printers) {
        return filterPrinters(printers, LABEL_SCREEN_PRINTER_IDS);
    }

    public static List<LabelWorkflowService.PrinterOption> filterRailToolPrinters(List<LabelWorkflowService.PrinterOption> printers) {
        return filterPrinters(printers, RAIL_TOOL_PRINTER_IDS);
    }

    public static boolean isPrintToFile(LabelWorkflowService.PrinterOption selected) {
        return selected != null && FILE_PRINTER_ID.equals(selected.getId());
    }

    public static LabelWorkflowService.PrinterOption buildPrintToFileOption(Path outputDir) {
        Objects.requireNonNull(outputDir, "outputDir cannot be null");
        return new LabelWorkflowService.PrinterOption(FILE_PRINTER_ID, "Print to file", outputDir.toString());
    }

    private static List<LabelWorkflowService.PrinterOption> filterPrinters(List<LabelWorkflowService.PrinterOption> printers, Set<String> allowedIds) {
        Objects.requireNonNull(printers, "printers cannot be null");
        List<LabelWorkflowService.PrinterOption> filtered = new ArrayList<>();
        for (LabelWorkflowService.PrinterOption option : printers) {
            if (option != null && allowedIds.contains(option.getId())) {
                filtered.add(option);
            }
        }
        return filtered;
    }

    private static Set<String> orderedSet(String... values) {
        Set<String> set = new LinkedHashSet<>();
        for (String value : values) {
            set.add(value);
        }
        return Set.copyOf(set);
    }
}
