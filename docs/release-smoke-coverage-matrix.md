# Release Smoke Coverage Matrix

This matrix defines the current release smoke boundary for `1.7.5` hardening work.

Status values:

- `covered`: release smoke path exists and should be required
- `partial`: smoke path exists but does not fully represent operator behavior
- `gap`: no deterministic smoke path yet

| GUI / Operator Action | Backing Path | Smoke Path | Status | Notes |
| --- | --- | --- | --- | --- |
| View resolved runtime config | `ShowConfigCommand` | `config` command in repo and packaged modes | covered | Validates config source precedence and redaction path |
| Validate DB connectivity | `DbTestCommand` | `db-test` command in repo and packaged modes | covered | Uses production-safe read-only connectivity check |
| Shipment label generation with `Print to file` | `RunCommand`, `AdvancedPrintWorkflowService`, `LabelWorkflowService` | `run --shipment-id ... --print-to-file` | covered | Verifies artifact generation without printer I/O |
| Carrier move label generation with `Print to file` | `RunCommand`, `AdvancedPrintWorkflowService` | `run --carrier-move-id ... --print-to-file` | covered | Verifies multi-stop orchestration and artifacts |
| GUI shipment preview math | `LabelWorkflowService.prepareJob` | shipment print-to-file smoke plus targeted service/unit tests | partial | CLI path covers output path, not Swing presentation |
| GUI carrier move preview math | `AdvancedPrintWorkflowService.prepareCarrierMoveJob` | carrier move print-to-file smoke plus targeted service/unit tests | partial | CLI path covers orchestration, not Swing presentation |
| Rail preview and PDF generation | `RailWorkflowService`, `RailPrintCommand` | `rail-print --train ... --yes` | covered | Verifies WMS-backed rail aggregation and PDF generation |
| Rail alignment template generation | `RailPrintCommand`, `RailCardRenderer` | `rail-print --template` | covered | Verifies deterministic template output |
| Rail `System default printer` path | `RailWorkflowService.generatePdf`, `RailPrintService.print(Path)`, `RailPrintCommand` | `rail-print --validate-system-default-print` plus targeted unit tests | covered | Must not depend on Windows PDF shell association or send a live print job during smoke |
| Rail explicit printer route | `RailWorkflowService.generatePdf`, `RailPrintService.print(Path, PrinterConfig)` | reachability checks plus unit/service coverage | partial | Default smoke avoids live print jobs |
| Shipment routed printer selection | `RunCommand.resolvePrinterId`, `PrinterRoutingService` | print-to-file path plus routing/service tests | partial | Default smoke does not send ZPL |
| Packaged SKU matrix resolution | `LabelingSupport.resolveSkuMatrixCsv`, `LabelWorkflowService.loadSkuMapping` | packaged shipment smoke | covered | Guards packaged app-home config resolution |
| Packaged location matrix resolution | `LabelDataBuilder` location mapping load | packaged shipment smoke | covered | Verified through packaged label generation path |
| Packaged printer YAML resolution | `PrinterRoutingService.load` | packaged `config`, `run`, and rail smoke | covered | Guards app-home config discovery |
| Tropicana per-user config precedence | `ConfigFileLocator`, `AppConfig` | packaged `config` smoke with `%LOCALAPPDATA%` config | covered | Verifies persistent per-user config precedence |
| Tropicana bootstrap installer flow | installer scripts and bootstrapper | packaged smoke with `-IncludeInstallerScenarios` | covered | Builds an isolated Tropicana bootstrap, performs a real local install, writes per-user config, and verifies installed `config` plus `db-test` |
| Queue / resume GUI actions | `AdvancedPrintWorkflowService` queue APIs | targeted service-level verification | gap | No release smoke runner path yet |
| Barcode Generator GUI | `BarcodeCommand`, barcode services | `barcode --data ... --dry-run --output-dir ...` in repo and packaged modes | covered | Verifies deterministic ZPL generation and file artifact output without printer I/O |
| Swing-specific selection UX (`Ctrl+F`, table selection, dialogs) | Swing dialog classes | manual verification only | gap | Outside first-line backend smoke boundary |
