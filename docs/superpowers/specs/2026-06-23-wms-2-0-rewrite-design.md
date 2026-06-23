# WMS Pallet Tag System 2.0 Rewrite Design

Date: 2026-06-23
Status: Approved direction, ready for implementation planning

## Purpose

Version 2.0 is a clean rewrite of the current WMS Pallet Tag System around the workflows that proved useful in production. The goal is not to preserve the current class layout. The goal is to preserve supported operator behavior while replacing the current GUI-centered architecture with a workflow-centered, testable, maintainable modular application.

The rewrite must keep the system simple, deterministic, and operationally safe:

- Oracle access remains read-only.
- Operators preview before printing.
- Print-to-file remains the default safe verification path.
- Live printer submission remains explicit.
- Packaging keeps portable and packaged install paths.
- Public release artifacts do not include live Tropicana credentials.

## Explicit 2.0 Scope

2.0 keeps these workflows:

- Runtime config inspection.
- Oracle DB connectivity diagnostics.
- Shipment pallet label preview and printing.
- Carrier move label preview and printing.
- Per-label selection before print.
- Optional info tags.
- Mixed shipment/carrier queue processing.
- Interrupted print checkpoint resume.
- ZPL preview/rendering for generated shipment, carrier, and barcode documents.
- Barcode generation and related operator tooling, including practical presets.
- Rail label generation from WMS train data, selected-card PDF generation, and printing.
- GUI settings, advanced non-secret runtime settings, update checks, guided install, uninstall/clean-install prep.
- Release smoke coverage for repo and packaged modes.
- Portable ZIP, packaged installer, Tropicana portable, and Tropicana config install paths.

2.0 does not port these failed or deferred experiments:

- Analyzer framework.
- Daily Operations dashboard.
- Open Loads, All Dock Doors, Unpicked Partials, and other analyzer tools.
- Developer-mode analyzer gating.
- SSCC-specific ZPL generation.
- SSCC GUI import/manual-entry tooling.

SSCC may be revisited after 2.0 as a separate design with a narrower user story.

## Current Architectural Problems

The current application is useful but has drifted into a GUI-centered architecture:

- `LabelGuiFrame` still owns too much state and too many workflows.
- Business workflows such as shipment preparation, carrier move preparation, print planning, queue execution, and resume logic currently live in the `gui` module.
- CLI commands depend on GUI package services.
- Workflow DTOs are nested inside GUI-oriented classes.
- Many small helper classes now orbit a still-large Swing frame, improving local testability but not fully fixing direction.
- UI presentation, use-case orchestration, data access, rendering, artifact writing, and print dispatch do not have consistently enforced boundaries.

2.0 must fix those boundaries rather than reproduce them with new filenames.

## Target Architecture

Use a modular monolith with Clean Architecture / Ports and Adapters.

Recommended modules:

```text
wms-domain
wms-application
wms-oracle
wms-printing
wms-files
wms-cli
wms-desktop
wms-smoke
```

No domain or application module may depend on Swing, JavaFX, Picocli, Oracle JDBC, filesystem implementation classes, network printing implementation classes, or GitHub release implementation classes.

During the side-by-side migration, new code uses the `com.tbg.wms.v2.*` package namespace. This keeps 2.0 classes clearly separated from the existing `core`, `db`, `gui`, and `cli` modules until cutover.

### `wms-domain`

Pure business models and deterministic rules.

Responsibilities:

- Shipment, LPN, SKU, pallet math, carrier move, stop group, label selection, rail card, barcode request, printer option, print task, and print plan models.
- Pure validation and calculation.
- No side effects.

Candidate packages:

```text
com.tbg.wms.domain.shipment
com.tbg.wms.domain.carriermove
com.tbg.wms.domain.label
com.tbg.wms.domain.print
com.tbg.wms.domain.rail
com.tbg.wms.domain.barcode
com.tbg.wms.domain.config
```

### `wms-application`

Use cases and ports. This is the center of 2.0.

Responsibilities:

- Coordinate domain services.
- Declare ports for repositories, config stores, artifact stores, printer dispatch, release lookup, and checkpoint persistence.
- Own workflow request/response DTOs.
- Return typed outcomes that CLI and desktop can present consistently.

Candidate use cases:

```text
PrepareShipmentLabels
PrepareCarrierMoveLabels
BuildShipmentPrintPlan
BuildCarrierMovePrintPlan
ExecutePrintPlan
ParseQueueInput
PrepareQueue
ExecuteQueue
ListResumeCandidates
ResumePrintJob
GenerateBarcodeLabel
PrepareRailLabels
GenerateRailPdf
PrintRailPdf
InspectRuntimeConfig
TestDatabaseConnection
CheckForUpdates
DownloadAndVerifyInstaller
PlanUninstall
```

Candidate ports:

```text
ShipmentRepository
CarrierMoveRepository
RailRepository
PrinterCatalog
PrintDispatcher
PdfPrintDispatcher
ArtifactStore
CheckpointStore
RuntimeConfigStore
RuntimeSettingsStore
ReleaseRepository
Clock
```

### `wms-oracle`

Oracle implementations of application ports.

Responsibilities:

- Hikari datasource lifecycle.
- Read-only Oracle repositories.
- SQL definitions.
- Row mappers.
- DB health checks.

Rules:

- SQL text, execution, and row mapping are separate.
- Query services return domain/application DTOs.
- No UI formatting.
- No print planning.

### `wms-printing`

Rendering and dispatch adapters.

Responsibilities:

- ZPL document rendering.
- Rail PDF rendering.
- Network ZPL printing.
- PDF/system-default printing.
- Printer route loading and reachability checks.
- Barcode ZPL rendering.

### `wms-files`

Runtime file and configuration adapters.

Responsibilities:

- Config precedence.
- Runtime settings persistence.
- App-home path resolution.
- Output directory retention.
- Artifact writing.
- Checkpoint file store.
- Packaged resource lookup.

### `wms-cli`

Picocli adapter.

Commands to preserve:

- `config`
- `db-test`
- `run`
- `barcode`
- `rail-helper`
- `rail-print`
- `gui`
- `version`
- `ems-recon` if still required by release scope

CLI commands call application use cases and format output only.

### `wms-desktop`

Desktop UI adapter.

The preferred UI technology for 2.0 is JavaFX, because the current Swing shell has become difficult to evolve and the future screens are naturally state-driven. The architecture must still keep the UI replaceable; JavaFX classes do not belong in domain or application modules.

Recommended desktop screens:

```text
Label Print
Queue
Resume
Rail Labels
Barcode
ZPL Preview
Settings
Update Manager
```

Pattern:

```text
View -> ViewModel/Presenter -> Application Use Case -> State -> View
```

No desktop view or controller may directly query Oracle, build print tasks, write artifacts, or dispatch printer jobs.

### `wms-smoke`

Java-owned smoke harness plus small PowerShell wrappers.

Responsibilities:

- Repo-mode smoke.
- Packaged-mode smoke.
- JSON and text reports.
- Production-safe identifier resolution.
- No-live-print default policy.
- Config precedence checks.
- Packaged/Tropicana checks.

## Supported Workflow Designs

### Shipment Labels

Flow:

```text
Shipment ID
 -> PrepareShipmentLabels
 -> PreparedShipmentLabels
 -> LabelSelection
 -> BuildShipmentPrintPlan
 -> PrintPlan
 -> ExecutePrintPlan
```

Preserve:

- WMS shipment loading.
- SKU matrix lookup.
- Location matrix lookup.
- Label template rendering.
- Printer route selection.
- Virtual label fallback when no LPN rows exist but SKU rows exist.
- Label subset selection.
- Shipment info tag.
- Print-to-file output.
- Live ZPL printing when explicitly selected.

Future improvements:

- Search/filter selected labels by LPN or SKU.
- Persist last print options.
- Export print plan JSON.
- Better preview of selected vs total output.

### Carrier Move Labels

Flow:

```text
Carrier Move ID
 -> PrepareCarrierMoveLabels
 -> PreparedCarrierMoveLabels
 -> CarrierMoveLabelSelection
 -> BuildCarrierMovePrintPlan
 -> PrintPlan
 -> ExecutePrintPlan
```

Preserve:

- Stop grouping.
- Shipment preparation per carrier move stop.
- Printable shipment filtering.
- Stop-order printing.
- Stop info tags.
- Final carrier-move info tag.
- Label subset selection across stops.

Future improvements:

- Stop-level select/deselect.
- Print one stop.
- Warnings for missing or skipped shipments.

### Queue

Flow:

```text
Queue text
 -> ParseQueueInput
 -> QueueRequest[]
 -> PrepareQueue
 -> PreparedQueue
 -> ExecuteQueue
 -> QueueResult
```

Preserve:

- Mixed shipment/carrier jobs.
- `S:` and `C:` prefixes.
- Numeric auto-detection.
- Queue size limits.
- Ordered preparation and ordered execution.
- Aggregate print totals.

Future improvements:

- Save/load queue files.
- Per-item result audit.
- Continue-on-error mode.
- Release smoke path for queue print-to-file.

### Resume / Checkpoint

Checkpointing is part of print execution, not UI state.

Use cases:

```text
ListResumeCandidates
ResumePrintJob
InspectCheckpoint
DeleteCheckpoint
```

Preserve:

- Incomplete job discovery.
- Safe resume.
- Print-to-file and printer resume.

Future improvements:

- Explicit resume policy: reprint last completed task vs continue next task.
- Checkpoint viewer.
- Checkpoint cleanup.

### Barcode

Flow:

```text
BarcodeRequest
 -> GenerateBarcodeLabel
 -> BarcodeDocument
 -> Preview or Print
```

Preserve:

- CLI barcode generation.
- GUI barcode generation.
- ZPL preview integration.
- Optional printing.
- Utility keyboard and practical operator preset direction.

Future improvements:

- First-class preset registry.
- Barcode operator sheets.
- Shared validation between CLI and desktop.

### Rail Labels

Flow:

```text
Train input
 -> ParseRailTrainInput
 -> PrepareRailLabels
 -> RailCardSelection
 -> GenerateRailPdf
 -> PrintRailPdf
```

Preserve:

- WMS as source of truth.
- Per-railcar pallet math using `ceil(cases / casesPerPallet)`.
- CAN, DOM, and KEV totals.
- Multiple train input.
- Explicit printable-card selection.
- Combined PDF output.
- Alignment template.
- Print-to-file and printer output.

Future improvements:

- Visual PDF page preview.
- Better footprint diagnostics.
- Export rail preparation snapshot.

### ZPL Preview

Preserve:

- Raw ZPL paste/open.
- Generated document preview for shipment, carrier move, and barcode.
- Page/document navigation.
- Density and size settings.

Future improvements:

- Save rendered preview as image/PDF.
- Preflight warnings for oversize labels.

### Settings and Updates

Preserve:

- Config precedence.
- Non-secret runtime settings.
- Secret config excluded from GUI editing.
- Output retention.
- Update checks.
- Guided installer download with checksum verification.
- No silent self-replacement.
- Uninstall / clean install prep.

Future update automation remains out of scope until code signing, detached updater, lock handling, and rollback exist.

## Error Handling

2.0 should use typed application errors instead of string-first exception handling.

Candidate errors:

```text
ConfigError
DatabaseUnavailable
ShipmentNotFound
CarrierMoveEmpty
PrinterNotFound
PrintDispatchFailed
ArtifactWriteFailed
CheckpointCorrupt
UpdateUnavailable
ChecksumMismatch
```

Desktop maps these to operator messages. CLI maps these to exit codes. Logs keep technical details. Developer/debug mode may expose richer diagnostics, but it should not enable hidden workflows like analyzers.

## Testing Strategy

Before replacing behavior, add characterization coverage around current behavior.

Required parity areas:

- Shipment print-to-file.
- Carrier move print-to-file.
- Label selection and info tags.
- Queue parsing/preparation/execution.
- Resume/checkpoint behavior.
- Barcode ZPL generation.
- Rail PDF generation and selection.
- Config precedence.
- Packaged app resource lookup.
- Update planning and checksum behavior.

Test layers:

- Domain unit tests.
- Application use-case tests with fake ports.
- Adapter tests for Oracle row mapping, artifact stores, printer dispatchers, release repositories.
- Desktop ViewModel tests with fake use cases.
- CLI command tests with fake use cases or test harnesses.
- Smoke tests for repo and packaged modes.

## Migration Strategy

Use a strangled rewrite, not a single giant replacement.

Each phase must produce working, testable software:

1. Freeze current behavior with characterization tests.
2. Create module skeleton.
3. Move pure domain models and rules.
4. Build application use cases and ports.
5. Implement Oracle, file, printing, and release adapters.
6. Port CLI workflows.
7. Build the desktop shell.
8. Port workflow screens one by one.
9. Replace smoke harness boundaries.
10. Remove old `gui` workflow services, analyzer code, and SSCC-specific tooling.

## Acceptance Criteria

2.0 is ready for prerelease only when:

- CLI and desktop both use application use cases.
- No CLI code depends on desktop/GUI packages.
- Domain/application modules do not depend on UI, Oracle JDBC, Picocli, filesystem implementation, or printer implementation classes.
- Shipment print-to-file parity passes.
- Carrier move print-to-file parity passes.
- Queue/resume tests pass.
- Barcode generation parity passes.
- Rail PDF generation parity passes.
- Config precedence tests pass.
- Repo smoke passes.
- Packaged smoke passes.
- Tropicana config precedence passes.
- Analyzer tools are removed.
- SSCC-specific ZPL tooling is not ported.
- README, changelog, release checklist, and smoke matrix reflect the 2.0 scope.
