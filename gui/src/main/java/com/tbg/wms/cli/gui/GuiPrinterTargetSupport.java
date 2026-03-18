package com.tbg.wms.cli.gui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Locale;

/**
 * Shared GUI helpers for workflow-scoped printer target lists.
 */
public final class GuiPrinterTargetSupport {
    public static final String FILE_PRINTER_ID = "FILE";
    public static final String SYSTEM_DEFAULT_PRINTER_ID = "SYSTEM_DEFAULT";
    public static final String CAPABILITY_ZPL = "ZPL";
    public static final String CAPABILITY_RAIL = "RAIL";

    private GuiPrinterTargetSupport() {
    }

    public static List<LabelWorkflowService.PrinterOption> filterLabelScreenPrinters(List<LabelWorkflowService.PrinterOption> printers) {
        return filterPrintersByCapability(printers, CAPABILITY_ZPL);
    }

    public static List<LabelWorkflowService.PrinterOption> filterRailToolPrinters(List<LabelWorkflowService.PrinterOption> printers) {
        return filterPrintersByCapability(printers, CAPABILITY_RAIL);
    }

    public static boolean isPrintToFile(LabelWorkflowService.PrinterOption selected) {
        return selected != null && FILE_PRINTER_ID.equals(selected.getId());
    }

    public static boolean isSystemDefaultPrinter(LabelWorkflowService.PrinterOption selected) {
        return selected != null && SYSTEM_DEFAULT_PRINTER_ID.equals(selected.getId());
    }

    public static LabelWorkflowService.PrinterOption buildPrintToFileOption(Path outputDir) {
        Objects.requireNonNull(outputDir, "outputDir cannot be null");
        return new LabelWorkflowService.PrinterOption(FILE_PRINTER_ID, "Print to file", outputDir.toString(), List.of());
    }

    public static LabelWorkflowService.PrinterOption buildSystemDefaultPrinterOption() {
        return new LabelWorkflowService.PrinterOption(
                SYSTEM_DEFAULT_PRINTER_ID,
                "System default printer",
                "Host default",
                List.of()
        );
    }

    public static boolean hasCapability(LabelWorkflowService.PrinterOption option, String capability) {
        Objects.requireNonNull(option, "option cannot be null");
        String normalized = normalizeCapability(capability);
        if (normalized.isEmpty()) {
            return false;
        }
        for (String candidate : option.getCapabilities()) {
            if (normalized.equals(normalizeCapability(candidate))) {
                return true;
            }
        }
        return false;
    }

    private static List<LabelWorkflowService.PrinterOption> filterPrintersByCapability(
            List<LabelWorkflowService.PrinterOption> printers,
            String capability
    ) {
        Objects.requireNonNull(printers, "printers cannot be null");
        List<LabelWorkflowService.PrinterOption> filtered = new ArrayList<>();
        for (LabelWorkflowService.PrinterOption option : printers) {
            if (option != null && hasCapability(option, capability)) {
                filtered.add(option);
            }
        }
        return filtered;
    }

    private static String normalizeCapability(String capability) {
        return capability == null ? "" : capability.trim().toUpperCase(Locale.ROOT);
    }
}
