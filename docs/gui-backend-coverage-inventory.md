# GUI to Backend Coverage Inventory

This inventory captures the current `1.7.6` GUI surface and maps each operator path to its backend/service entrypoint and current automated coverage.

It is intended to complement `docs/release-smoke-coverage-matrix.md` by making the remaining manual-only and partially covered GUI behaviors explicit.

## Release-backed GUI actions

| GUI action | Primary backend path | Automated coverage | Notes |
| --- | --- | --- | --- |
| Shipment preview + print-to-file | `LabelWorkflowService.prepareJob`, `RunCommand` | release smoke (`run --shipment-id ... --print-to-file`) + `LabelWorkflowServiceTest` | Smoke covers WMS load, pallet math, config assets, and ZPL artifact output. Swing rendering remains outside smoke. |
| Carrier move preview + print-to-file | `AdvancedPrintWorkflowService.prepareCarrierMoveJob`, `RunCommand` | release smoke (`run --carrier-move-id ... --print-to-file`) + `AdvancedPrintWorkflowServiceTest` | Smoke covers stop grouping, shipment orchestration, and artifact generation. Swing stop expansion remains outside smoke. |
| Barcode Generator | `BarcodeCommand`, `BarcodeZplBuilder`, `BarcodeDialogFactory` | release smoke (`barcode --data ... --dry-run --output-dir ...`) + `BarcodeCommandTest` + `BarcodeZplBuilderTest` | Covers deterministic barcode artifact generation without live printer I/O. |
| Rail Labels preview/PDF | `RailPrintCommand`, `RailWorkflowService`, `RailCardRenderer` | release smoke (`rail-print --train ... --yes`) + `RailArtifactServiceTest` + `RailPrintCommandTest` | Covers WMS-backed rail aggregation and PDF output. Swing preview panel remains manual. |
| Rail alignment template | `RailPrintCommand`, `RailCardRenderer` | release smoke (`rail-print --template`) | Deterministic PDF template generation is fully covered. |
| Rail system-default printer validation | `RailPrintCommand`, `RailPrintService.print(Path)` | release smoke (`rail-print --validate-system-default-print`) + `RailPrintCommandTest` | Non-destructive validation only; no live print job is sent during smoke. |
| Settings: update check / uninstall prep / advanced settings launchers | `ReleaseCheckService`, `InstallMaintenanceService`, `AdvancedSettingsDialog`, `MainSettingsDialog` | `MainSettingsDialogTest`, `AdvancedSettingsDialogTest`, targeted unit/service coverage | Confirms settings-button callback wiring and advanced-settings file targeting without relying on manual clicking. |
| Developer mode / Analyzers tool gating | `RuntimeSettings`, `LabelGuiFrameToolMenuSupport`, `LabelGuiFrame` | `RuntimeSettingsTest`, `LabelGuiFrameToolMenuSupportTest`, `AdvancedSettingsDialogTest`, `LabelGuiFrameStartupTest` | Confirms the persisted toggle exists, analyzers stay hidden by default, and the main GUI still starts cleanly with the new gate in place. |
| Packaged Tropicana config precedence | `ConfigFileLocator`, `AppConfig` | packaged smoke (`config`) + packaged installer smoke | Verified through packaged and installer smoke with isolated `%LOCALAPPDATA%`. |
| Tropicana bootstrap installer flow | `build-tropicana-installer.ps1`, `install-wms-installer.ps1`, embedded bootstrap scripts | packaged smoke with `-IncludeInstallerScenarios` + PowerShell tests | Current smoke also verifies rerun-safe replacement for the smoke-specific app identity. |

## Partially covered GUI actions

| GUI action | Why partial | Current automated coverage | Remaining manual surface |
| --- | --- | --- | --- |
| Shipment preview presentation | Backend math is covered, but Swing preview layout is not | `LabelWorkflowServiceTest`, shipment smoke | Text layout, label selection panel presentation, operator readability |
| Carrier move preview presentation | Backend orchestration is covered, but stop-section rendering is not | `AdvancedPrintWorkflowServiceTest`, carrier move smoke | Stop collapse/expand UX, large preview readability |
| Rail explicit printer route | Reachability is covered, not live device output | release smoke host/TCP checks | Real printer selection and PDF dispatch on the operator workstation |
| Shipment routed printer selection | Routing data is covered, but live GUI dropdown behavior is indirect | `GuiPrinterTargetSupportTest`, shipment smoke | Final operator-facing printer selection in the main screen |
| `Ctrl+F` preview shortcut | Key-binding helper is tested, not full dialog focus behavior | `WorkflowShortcutBinderTest` | Focus edge cases across the actual Swing windows |

## Manual-only GUI surface

| GUI action | Current state | Why still manual |
| --- | --- | --- |
| Queue Print dialog end-to-end | service path tested, no release smoke path | Requires building queue input, previewing queue text, and validating operator sequencing in Swing |
| Resume Incomplete Job dialog end-to-end | checkpoint/service path tested, no release smoke path | Resume candidate discovery and chooser flow are GUI-specific |
| Auto-resume prompt on startup | manual only | Trigger depends on startup dialog timing and Swing prompt behavior |
| Barcode advanced settings dialog UX | manual only | Layout, field enable/disable behavior, and user guidance are Swing-only concerns |
| Rail Labels dialog presentation | manual only | Preview table, diagnostics panel, card preview panel, and dialog interaction are GUI-only |
| Main Settings dialog layout and actions | partially covered | Callback wiring and save validation are tested, but final operator flow remains visual/manual |
| Advanced Settings dialog editing UX | partially covered | Editable file targeting is tested against an isolated app-home, but editor usability and safety cues remain manual |
| Alert badge / update notification presentation | manual only | Requires a GUI visual state check even though update backend services exist |

## Release interpretation

- No Tier 1 release smoke scenario remains uncovered.
- The remaining gaps are concentrated in Swing presentation and operator interaction, not backend release mechanics.
- Queue/resume remains the highest-value future GUI-only hardening area because it executes real print-task orchestration outside the current smoke boundary.
