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
                        "Choose Carrier Move ID or Shipment ID, type or paste the number, then click Preview.",
                        "For example: choose Shipment ID for 8000582489, or choose Carrier Move ID for a carrier move number.",
                        "Review the labels that appear before printing.",
                        "Use Tools for rail labels, barcode generation, ZPL preview, queue print, resume, and settings."),
                section("Shortcuts",
                        "Ctrl+F runs Preview when the preview button is enabled.",
                        "In text boxes, left-click and drag to highlight text.",
                        "right-click in a text box to copy, paste, cut, or select all when those options are available.",
                        "You can also use Ctrl+C to copy, Ctrl+V to paste, and Ctrl+A to select all."),
                section("Printing",
                        "Confirm Print prints only the labels that are still selected.",
                        "Show Labels lets you look at the generated labels before sending them to the printer.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> railLabels() {
        return List.of(
                section("Workflow",
                        "Type one full train code, or type several full train codes at the same time.",
                        "For example: JC05262026",
                        "For more than one train, type examples like: JC05262026, JC05272026",
                        "Other examples that also work: JC05262026 JC05272026, JC05262026/JC05272026, or JC05262026:JC05272026;JC05282026",
                        "Click Load Preview to gather the rail rows for every train you entered.",
                        "Click Generate PDF to put the selected rail labels into the same PDF."),
                section("Row Selection",
                        "Every row is checked to print when the preview loads.",
                        "Uncheck any row you do not want to print.",
                        "Ctrl-click lets you pick several separate rows.",
                        "Shift-click lets you pick a whole group of rows at once.",
                        "Press Space to check or uncheck the selected rows.",
                        "Select All checks every row. Clear All unchecks every row. Invert switches checked rows to unchecked and unchecked rows to checked."),
                section("Shortcuts",
                        "Ctrl+F runs Load Preview when it is enabled.",
                        "right-click in the Train ID or Output Directory box to copy, paste, cut, or select all when those options are available.")
        );
    }

    public static List<GuiHelpSupport.HelpSection> barcodeGenerator() {
        return List.of(
                section("Workflow",
                        "Type or paste the barcode text, choose the barcode type, enter the number of copies, and choose where it should go.",
                        "For example: type 20554, choose CODE128, set Copies to 1, then click Preview.",
                        "Generate sends the barcode to the selected printer or saves a file when Print to File is selected."),
                section("Tools",
                        "Utility Keyboard gives quick buttons for special keys and symbols.",
                        "Advanced Settings changes label size, barcode position, barcode thickness, and whether readable text prints below the barcode.")
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
