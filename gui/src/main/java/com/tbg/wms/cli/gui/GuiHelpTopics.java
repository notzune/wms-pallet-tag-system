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
                        "Choose Carrier Move ID or Shipment ID, enter the identifier, then run Preview.",
                        "Review the label set, clear any labels that should stay out of the run, then use Confirm Print.",
                        "Show Labels opens the generated ZPL preview for the current selected labels.",
                        "Tools keeps rail labels, SSCC labels, barcode generation, ZPL preview, queue print, resume, and settings close to the main workflow."),
                section("Shortcuts",
                        "Ctrl+F runs Preview when Preview is enabled.",
                        "Ctrl+A, Ctrl+C, Ctrl+V, and Ctrl+X use standard text-field editing.",
                        "Tab moves through the active controls; Shift+Tab moves backward."),
                section("Printing",
                        "Confirm Print sends only selected labels to the active printer target.",
                        "DISPATCH is the default production printer and should be used for normal label printing.",
                        "ROSSI is the other production ZPL printer.",
                        "OFFICE and 3002_ZEB0 are backups/testing printers only, kept available for emergencies and debug work.",
                        "Print to file keeps generated labels under the configured output directory.",
                        "Printer routing and print-to-file targets are visible before the run starts.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> railLabels() {
        return List.of(
                section("Workflow",
                        "Enter one train code or a mixed-delimiter list, then run Load Preview.",
                        "Accepted multi-train examples: JC05262026, JC05272026 or JC05262026/JC05272026.",
                        "Generate PDF renders checked rows from every loaded train into one combined PDF.",
                        "Use Print after review when the selected PDF output is ready for the active printer target."),
                section("Selection Shortcuts",
                        "Preview rows load checked by default.",
                        "Ctrl-click adds or removes individual rows from the table selection.",
                        "Shift-click extends the table selection across a range.",
                        "Space toggles the printable checkbox for all selected rows.",
                        "Select All, Clear All, and Invert adjust printable rows without changing the loaded preview."),
                section("Shortcuts",
                        "Ctrl+F runs Load Preview when Load Preview is enabled.",
                        "Ctrl+A, Ctrl+C, Ctrl+V, and Ctrl+X use standard text-field editing in train and output fields.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> ssccLabels() {
        return List.of(
                section("Workflow",
                        "Import a CSV that uses the same headers as the workbook export, or add raw rows manually.",
                        "Grouped Labels shows one row per final pallet label after grouping by Sales Order # and New Received LPN.",
                        "Select a grouped label to preview its final ZPL, then export one label or the full set to files."),
                section("Required CSV Headers",
                        "Sales Order #",
                        "Purchase Order #",
                        "Shipment #",
                        "Carrier Code",
                        "Trailer ID",
                        "Destination",
                        "Destination Address",
                        "Customer Name",
                        "Facility",
                        "Item #",
                        "Level 2 Reference #",
                        "Originally Shipped LPN",
                        "Sum of Ship Cases",
                        "New Received LPN"),
                section("Notes",
                        "Mixed-SKU pallets show MIXED SKU instead of a single item number.",
                        "The ship-from block is fixed to Tropicana Manufacturing Company Inc., 4 Owens Rd., Brockport, NY 14420.",
                        "Preview uses the same ZPL layout the old working export generated.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> barcodeGenerator() {
        return List.of(
                section("Workflow",
                        "Enter barcode data, barcode type, copy count, and printer target before generating.",
                        "Preview renders the exact ZPL output before the barcode is sent or saved.",
                        "Generate sends the barcode to the selected printer or writes the ZPL when Print to File is active."),
                section("Shortcuts And Tools",
                        "Utility Keyboard provides F-keys, navigation keys, and edit shortcuts for scanner or terminal-style entry.",
                        "Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X, Ctrl+Z, and Ctrl+Y are available from the utility keyboard palette.",
                        "Advanced Settings controls label size, barcode origin, module width, ratio, height, and readable text.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> barcodeAdvancedSettings() {
        return List.of(
                section("Workflow",
                        "Tune barcode rendering for the active barcode request, then use Done to return to the generator.",
                        "Keep scanner readability in mind when changing module width, ratio, or barcode height."),
                section("Units",
                        "Label width, label height, and origins use printer dots.",
                        "Readable text, barcode position, and thickness apply to the generated ZPL preview and print output.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> zplPreview() {
        return List.of(
                section("Workflow",
                        "Load ZPL from a file or the editor, then render the preview.",
                        "Use document controls to move through multi-document preview sets.",
                        "DPMM, width, height, and label index control Labelary rendering."),
                section("Shortcuts",
                        "Enter renders immediately while the ZPL editor is focused.",
                        "Shift+Enter inserts a line break in the ZPL editor.",
                        "Render live queues preview updates as ZPL or render settings change.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> queuePrint() {
        return List.of(
                section("Workflow",
                        "Add carrier move or shipment IDs separated by new lines or semicolons.",
                        "Choose the default type for unprefixed IDs.",
                        "Use C: and S: prefixes when a queue mixes carrier moves and shipments."),
                section("Printing",
                        "Preview Queue validates and prepares the queue before print output exists.",
                        "Print Queue sends the prepared queue to the selected main-window printer target.",
                        "Interrupted queues can be resumed from the saved checkpoint list.")
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
                        "Uninstall / Clean Install Prep opens packaged-install maintenance when available.",
                        "Developer mode exposes diagnostic tools without changing the production operator workflow.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> advancedSettings() {
        return List.of(
                section("Workflow",
                        "Select an editable config file, update its contents, then Save.",
                        "Reload discards the editor contents and reloads the current file from disk.",
                        "Open Config Folder opens the folder containing the selected config file."),
                section("Scope",
                        "The editor is limited to runtime YAML, CSV, and ZPL config files.",
                        "Environment secrets stay outside the GUI.",
                        "Use this screen for controlled config edits, not one-off label data.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> updates() {
        return List.of(
                section("Workflow",
                        "Refresh reloads the release catalog.",
                        "Enable experimental / prerelease updates to include prerelease install targets.",
                        "Select an install target, then use Install Selected to launch the configured install action."),
                section("Release Page",
                        "Open Release Page opens the selected target release when available.",
                        "If no target is selected, the latest stable release page is used.",
                        "Installer checksum verification remains part of the guided update path.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> analyzers() {
        return List.of(
                section("Workflow",
                        "Choose an analyzer from the list, then use Refresh to load current data.",
                        "Auto refresh repeats the selected analyzer at the chosen interval.",
                        "Some analyzers render as tables and others render as dashboard panels."),
                section("Diagnostics",
                        "Analyzers are intended for developer and operational diagnostics.",
                        "The status bar reports active load state and failures.",
                        "Use dashboard sections for operational scan-down and table analyzers for row-level investigation.")
        );
    }

    private static GuiHelpSupport.HelpSection section(String heading, String... lines) {
        return new GuiHelpSupport.HelpSection(heading, List.of(lines));
    }
}
