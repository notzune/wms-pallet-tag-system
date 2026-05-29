package com.tbg.wms.cli.gui;

import java.util.List;

/**
 * Central catalog of operator help text for GUI views and tools.
 *
 * <p>Keeping copy here lets windows depend on stable topic methods instead of each view also
 * owning formatting text. This preserves a single reason to change when shortcuts or workflow
 * instructions change.</p>
 */
public final class GuiHelpTopics {
    private GuiHelpTopics() {
    }

    public static List<GuiHelpSupport.HelpSection> mainWindow() {
        return List.of(
                section("Workflow",
                        "Choose Carrier Move ID or Shipment ID, enter the ID, then load the preview.",
                        "Review the generated label math and label selection before printing.",
                        "Use Tools for rail labels, barcode generation, ZPL preview, queue print, resume, and settings."),
                section("Shortcuts",
                        "Ctrl+F runs Preview when the preview button is enabled.",
                        "Text fields use terminal-like mouse clipboard behavior where supported."),
                section("Printing",
                        "Confirm Print prints only the selected preview labels.",
                        "Show Labels opens a generated ZPL preview for the currently selected labels.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> railLabels() {
        return List.of(
                section("Workflow",
                        "Enter one or more train codes separated by comma, space, colon, slash, semicolon, or combinations of those characters.",
                        "Load Preview builds one combined rail label job for all entered trains.",
                        "Generate PDF creates one PDF containing only rows whose PRINT checkbox is selected."),
                section("Row Selection",
                        "All rows are printable by default after preview loads.",
                        "Ctrl-click adds or removes individual table rows from the current selection.",
                        "Shift-click selects a row range.",
                        "Press Space to toggle the PRINT checkbox for selected rows.",
                        "Select All, Clear All, and Invert update which rail labels will be generated."),
                section("Shortcuts",
                        "Ctrl+F runs Load Preview when it is enabled.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> barcodeGenerator() {
        return List.of(
                section("Workflow",
                        "Enter barcode data, choose type, copies, and printer target.",
                        "Preview opens a live ZPL preview for the current barcode request.",
                        "Generate prints to the selected printer or writes ZPL when Print to File is selected."),
                section("Tools",
                        "Utility Keyboard opens scanner-friendly symbol entry.",
                        "Advanced Settings adjusts orientation, dimensions, origin, module width, ratio, and human-readable text.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> barcodeAdvancedSettings() {
        return List.of(
                section("Workflow",
                        "Adjust barcode rendering settings for the active barcode request.",
                        "Use Done to keep the current settings and return to the generator."),
                section("Units",
                        "Label width, label height, and origins are in printer dots.",
                        "Module width, ratio, and barcode height affect scanner readability.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> zplPreview() {
        return List.of(
                section("Workflow",
                        "Paste ZPL or open a .zpl file, then render the preview.",
                        "Use document controls to move through generated multi-document previews.",
                        "DPMM, width, height, and label index control how Labelary renders the preview."),
                section("Shortcuts",
                        "Enter renders immediately while the ZPL text area is focused.",
                        "Shift+Enter inserts a new line in the ZPL text area.",
                        "Render live queues preview updates as the ZPL or render settings change.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> queuePrint() {
        return List.of(
                section("Workflow",
                        "Paste carrier move or shipment IDs separated by new lines or semicolons.",
                        "Choose the default type for unprefixed IDs.",
                        "Use C: or S: prefixes when a queue contains mixed carrier moves and shipments."),
                section("Printing",
                        "Preview Queue validates and prepares the queue before anything prints.",
                        "Print Queue prints the prepared queue to the currently selected main-window printer target.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> settings() {
        return List.of(
                section("Workflow",
                        "Set the default print-to-file output directory and generated-output retention policy.",
                        "Use Update Manager for version visibility and install target selection.",
                        "Advanced Settings opens editable runtime config files."),
                section("Maintenance",
                        "Clean Old Output Now applies the retention policy immediately.",
                        "Uninstall / Clean Install Prep launches packaged-install maintenance when available.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> advancedSettings() {
        return List.of(
                section("Workflow",
                        "Select an editable config file, change its contents, then Save.",
                        "Reload discards the editor contents and reloads the current file from disk.",
                        "Open Config Folder opens the folder containing the selected config file."),
                section("Scope",
                        "This editor only changes runtime YAML, CSV, and ZPL config files.",
                        "Environment secrets stay outside the GUI.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> updates() {
        return List.of(
                section("Workflow",
                        "Refresh reloads the release catalog.",
                        "Enable experimental / prerelease updates to include prerelease install targets.",
                        "Select an install target, then Install Selected to launch the configured install action."),
                section("Release Page",
                        "Open Release Page opens the selected target release when available.",
                        "If no target is selected, the latest stable release page is used.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> analyzers() {
        return List.of(
                section("Workflow",
                        "Choose an analyzer from the list and Refresh to load current data.",
                        "Auto refresh repeats the selected analyzer at the chosen interval.",
                        "Some analyzers render as tables and others render as dashboard panels."),
                section("Usage",
                        "Analyzers are intended for developer and operational diagnostics.",
                        "The status bar reports the active load state and failures.")
        );
    }

    private static GuiHelpSupport.HelpSection section(String heading, String... lines) {
        return new GuiHelpSupport.HelpSection(heading, List.of(lines));
    }
}
